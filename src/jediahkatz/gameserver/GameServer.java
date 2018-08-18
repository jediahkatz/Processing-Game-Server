package jediahkatz.gameserver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import jediahkatz.gameserver.GameClient.DataFetcher;

import java.util.Objects;

import processing.core.*;
import processing.net.*;
import processing.data.JSONArray;
import processing.data.JSONObject;

/** A multiplayer game server.
 * @author jediahkatz
 */
public class GameServer {
	// Beep character - data separator
	private final char SEP = (char) 7;
	private Server server;
	// Incrementing unique identifier to assign to clients and rooms
	private int nextClientId = 0;
	private int nextRoomId = 0;
	private JSONObject attributes = new JSONObject();
	// Data structures storing rooms/clients
	private HashMap<Integer, Room> rooms = new HashMap<>();
	private HashMap<Integer, processing.net.Client> clients = new HashMap<>();
	private HashMap<Integer, Integer> clientIdToRoomId = new HashMap<>();
	// Keep track of which clients have disconnected safely - this stores their hashcodes
	private HashSet<Integer> disconnected = new HashSet<>();
	
	private final ServerRunner thread;
	
	/**
	 * 
	 * @param parent the current sketch (this)
	 * @param port the port to transfer data over
	 */
	public GameServer(PApplet parent, int port) {
		// Unfortunately the generic registerMethod is no longer supported. These will need to be called manually.
		//parent.registerMethod("serverEvent", this);
		//parent.registerMethod("disconnectEvent", this);
		parent.registerMethod("dispose", this);
		server = new Server(parent, port);
		
		// Start a new thread to run the server on
		thread = new ServerRunner(this);
		new Thread(thread).start();
	}
	
	/**
	 * Run the server.
	 */
	private void run() {
		Client client = server.available();
		while (client != null) {
			JSONObject data = getData(client);
			handleData(client, data);
			client = server.available();
		}
	}
	
	public void dispose() {
		stop();
	}
	
	/**
	 * Shut down this server.
	 */
	public void stop() {
		server.stop();
		thread.stop();
	}
	
	/**
	 * This function is called automatically when a client connects to the server.
	 * We automatically register the client.
	 */
	public void serverEvent(Server server, Client client) {
		JSONObject response = registerClient(client);
		send(client, response);
	}
	
	/**
	 * This function is called automatically when a client disconnects.
	 * We remove the client from our data structures if it hasn't already been.
	 */
	public void disconnectEvent(Client client) {
		if (!disconnected.contains(client.hashCode())) {
			// Get the clientId from the client
			Entry<Integer, Client> clientAndId = clients.entrySet().stream()
					.filter(entry -> Objects.equals(entry.getValue(), client))
					.findFirst().orElse(null);
			if (clientAndId != null) {
				int clientId = clientAndId.getKey();
			    disconnect(clientId);
			}
		}
	}
	
	/**
	 * Send data to the specified client.
	 * @param client the recipient of the data
	 * @param data the message to send
	 */
	private void send(Client client, JSONObject data) {
		String messageStr = data.toString();
		client.write(messageStr + SEP);
	}
	
	/**
	 * Return the client's data as a JSONObject.
	 * @param client the client with available data as a JSON string
	 * @return JSONObject an object containing the client's data
	 */
	private JSONObject getData(Client client) {
		return JSONObject.parse(client.readStringUntil(SEP));
	}
	
	/**
	 * Take action based on the content of the received data.
	 * @param client the client that sent the data
	 * @param data the received data 
	 */
	private void handleData(Client client, JSONObject data) {
		if (data.hasKey("action")) {
			JSONObject response;
			ActionCode action;
			try {
				action = ActionCode.valueOf(data.getString("action"));
			} catch (RuntimeException e) {
				//throw new RuntimeException("Invalid action: " + data.getString("action"));
				return;
			}
			switch (action) {
			case DISCONNECT:
				disconnect(data.getInt("clientId"));
				return; // Client is disconnecting, so no response
			case REGISTER_ROOM:
				response = registerRoom(data.getInt("capacity"));
				break;
			case JOIN_ROOM:
				response = joinRoom(data.getInt("clientId"), data.getInt("roomId"));
				break;
			case LEAVE_ROOM:
				response = leaveRoom(data.getInt("clientId"));
				break;
			case AUTOJOIN_ROOM:
				response = autojoinRoom(data.getInt("clientId"), data.getInt("capacity"));
				break;
			case GET_ROOM_INFO:
				response = getRoomInfo(data.getInt("roomId"));
				break;
			case GET_ROOMS_INFO:
				response = getRoomsInfo();
				break;
			case SET_ROOM_ATTRIBUTES:
				response = setRoomAttributes(data.getInt("roomId"), data.getJSONObject("attributes"));
				break;
			case PUT_ROOM_ATTRIBUTE:
				response = putRoomAttribute(data.getInt("roomId"), data.getString("key"), data.get("value"));
				break;
			case SET_SERVER_ATTRIBUTES:
				response = setServerAttributes(data.getJSONObject("attributes"));
				break;
			case PUT_SERVER_ATTRIBUTE:
				response = putServerAttribute(data.getString("key"), data.get("value"));
				break;
			case GET_SERVER_ATTRIBUTES:
				response = getServerAttributes();
				break;
			case SEND_MESSAGE:
				sendMessage(data.getInt("clientId"), data.getJSONArray("recipients"), data.getString("message"));
				return; // No response when sending message
			case BROADCAST_MESSAGE:
				broadcastMessage(data.getInt("clientId"), data.getString("message"));
				return; // No response when sending message
			default:
				//throw new RuntimeException("Invalid action: " + data.getString("action"));
				return;
			}
			
			send(client, response);
		}
		//throw new RuntimeException("Data sent to server must have an 'action' attribute.");
	}
	
	/**
	 * Remove the client from our data structures, including rooms.
	 * @param clientId the id of the client to disconnect
	 */
	private void disconnect(int clientId) {
		Client client = clients.remove(clientId);
		if (client != null) {
			client.stop();
			disconnected.add(client.hashCode());
		}
		Integer roomId = clientIdToRoomId.remove(clientId);
		if (roomId != null) {
			rooms.get(roomId).removeClient(clientId);
		}
	}
	
	/**
	 * Register a client (assign it an ID).
	 * @param client the client to register
	 * @return the response to send to the client, containing "clientId" key
	 */
	private JSONObject registerClient(Client client) {
		JSONObject response = new JSONObject();
		setAction(response, ActionCode.REGISTER_CLIENT);
		setSuccess(response);
		Integer id = nextClientId++;
		response.setInt("clientId", id);
		clients.put(id, client);
		return response;
	}
	
	/**
	 * Register a new room and add it to the list of rooms.
	 * @param capacity the capacity of the room
	 * @return the response to send to the client, containing "roomId" key
	 */
	private JSONObject registerRoom(int capacity) {
		JSONObject response = new JSONObject();
		setAction(response, ActionCode.REGISTER_ROOM);
		setSuccess(response);
		int id = nextRoomId++;
		response.setInt("roomId", id);
		
		Room room = new Room(id, capacity);
		rooms.put(id, room);
		return response;
	}
	
	/**
	 * Add a client to a room.
	 * @param clientId the id of the client to add to the room
	 * @param roomId the id of the room to add the client to
	 * @return the response to send to the client, containing info about the room joined
	 */
	private JSONObject joinRoom(int clientId, int roomId) {
		JSONObject response = new JSONObject();
		setAction(response, ActionCode.JOIN_ROOM);
		
		if (clientIdToRoomId.containsKey(clientId)) {
			setError(response, ErrorCode.ALREADY_IN_ROOM);
		} else {
			Room room = rooms.get(roomId);
			if (room == null) {
				setError(response, ErrorCode.ROOM_NOT_FOUND);
			} else if (room.isFull()) {
				setError(response, ErrorCode.ROOM_FULL);
			} else {
				setSuccess(response);
				addClientToRoom(clientId, room);
				addRoomInfo(response, room);
			}
		}
		
		return response;
	}
	
	/** Helper method to add client to room. **/
	private void addClientToRoom(int clientId, Room room) {
		room.addClient(clientId);
		clientIdToRoomId.put(clientId, room.id());
	}
	
	/**
	 * Remove a client from its room.
	 * @param clientId the id of the client to remove from its room
	 * @return the response to send to the client
	 */
	private JSONObject leaveRoom(int clientId) {
		JSONObject response = new JSONObject();
		setAction(response, ActionCode.LEAVE_ROOM);
		setSuccess(response);
		
		Integer roomId = clientIdToRoomId.get(clientId);
		if (roomId != null) {
			Room room = rooms.get(roomId);
			room.removeClient(clientId);
		}
		return response;
	}
	

	/**
	 * Join an arbitrary room, or create a new room if all rooms are full.
	 * @param clientId the id of the client to join a room
	 * @param capacity the capacity of a new room, if one is created
	 * @return the response to send to the client, containing info about the room joined
	 */
	private JSONObject autojoinRoom(int clientId, int capacity) {
		JSONObject response = new JSONObject();
		setAction(response, ActionCode.AUTOJOIN_ROOM);
		setSuccess(response);
		
		// Add to first room that isn't full
		for (Room room : rooms.values()) {
			if (!room.isFull()) {
				room.addClient(clientId);
				addRoomInfo(response, room);
				return response;
			}
		}
		
		// If all rooms full, then make a new one
		int roomId = nextRoomId++;
		Room room = new Room(roomId, capacity);
		rooms.put(roomId, room);
		addClientToRoom(clientId, room);
		addRoomInfo(response, room);
		return response;
		
	}
	
	/**
	 * Get info about a room.
	 * @param roomId the id of the room to get info about
	 * @return the response to send to the client
	 */
	private JSONObject getRoomInfo(int roomId) {
		JSONObject response = new JSONObject();
		setAction(response, ActionCode.GET_ROOM_INFO);
		
		Room room = rooms.get(roomId);
		if (room != null) {
			setSuccess(response);
			addRoomInfo(response, room);
		} else {
			setError(response, ErrorCode.ROOM_NOT_FOUND);
		}
		return response;
	}
	
	/**
	 * Get info about all active rooms.
	 * @return the response to send to the client
	 */
	private JSONObject getRoomsInfo() {
		JSONObject response = new JSONObject();
		setAction(response, ActionCode.GET_ROOMS_INFO);
		setSuccess(response);
		
		JSONArray roomsInfo = new JSONArray();
		
		for (Room room : rooms.values()) {
			JSONObject roomInfo = new JSONObject();
			addRoomInfo(roomInfo, room);
			roomsInfo.append(roomInfo);
		}
		
		response.setJSONArray("roomsInfo", roomsInfo);
		return response;
	}
	
	/** Helper method to add room info to a response. **/
	private void addRoomInfo(JSONObject response, Room room) {
		response.setInt("roomId", room.id());
		response.setInt("capacity", room.capacity());
		response.setInt("size", room.size());
		response.setJSONObject("attributes", room.getAttributes());
		
		JSONArray clientIds = new JSONArray();
		for (int id : room.getClientIds()) {
			clientIds.append(id);
		}
		response.setJSONArray("clientIds", clientIds);
	}
	
	/**
	 * Set the attributes of a room.
	 * @param roomId the id of the room
	 * @param attributes the object to set as the new attributes
	 * @return the response to send to the client
	 */
	private JSONObject setRoomAttributes(int roomId, JSONObject attributes) {
		JSONObject response = new JSONObject();
		setAction(response, ActionCode.SET_ROOM_ATTRIBUTES);

		Room room = rooms.get(roomId);
		if (room != null) {
			room.setAttributes(attributes);
			setSuccess(response);
		} else {
			setError(response, ErrorCode.ROOM_NOT_FOUND);
		}
		
		return response;
	}
	
	/**
	 * Set a single attribute for a room, or overwrite it if the key already exists.
	 * @param roomId the id of the room
	 * @param key the key to associate to the value
	 * @param value the value to be associated with the key
	 * @return the response to send to the client
	 */
	private JSONObject putRoomAttribute(int roomId, String key, Object value) {
		JSONObject response = new JSONObject();
		setAction(response, ActionCode.PUT_ROOM_ATTRIBUTE);

		Room room = rooms.get(roomId);
		if (room != null) {
			setSuccess(response);
			// This is the only way to figure out what kind of object was passed in
			if (value instanceof Integer) {
				room.putAttribute(key, (int) value);
			} else if (value instanceof String) {
				room.putAttribute(key, (String) value);
			} else if (value instanceof Boolean) {
				room.putAttribute(key, (boolean) value);
			} else if (value instanceof JSONObject) {
				room.putAttribute(key, (JSONObject) value);
			} else if (value instanceof JSONArray) {
				room.putAttribute(key, (JSONArray) value);
			} else if (value instanceof Float) {
				room.putAttribute(key, (float) value);
			} else if (value instanceof Double) {
				room.putAttribute(key, (double) value);
			} else if (value instanceof Long) {
				room.putAttribute(key, (long) value);
			}
		} else {
			setError(response, ErrorCode.ROOM_NOT_FOUND);
		}
		
		return response;
	}
	
	/**
	 * Set the attributes associated with this server.
	 * @param attributes the object to set as the new attributes
	 * @return the response to send to the client
	 */
	private JSONObject setServerAttributes(JSONObject attributes) {
		JSONObject response = new JSONObject();
		this.attributes = attributes;
		setAction(response, ActionCode.SET_SERVER_ATTRIBUTES);
		setSuccess(response);
		return response;
	}
	
	/**
	 * Set a single attribute for this server, or overwrite it if the key already exists.
	 * @param key the key to associate to the value
	 * @param value the value to be associated with the key
	 * @return the response to send to the client
	 */
	private JSONObject putServerAttribute(String key, Object value) {
		JSONObject response = new JSONObject();
		setAction(response, ActionCode.PUT_SERVER_ATTRIBUTE);
		setSuccess(response);
		// This is the only way to figure out what kind of object was passed in
		if (value instanceof Integer) {
			attributes.setInt(key, (int) value);
		} else if (value instanceof String) {
			attributes.setString(key, (String) value);
		} else if (value instanceof Boolean) {
			attributes.setBoolean(key, (boolean) value);
		} else if (value instanceof JSONObject) {
			attributes.setJSONObject(key, (JSONObject) value);
		} else if (value instanceof JSONArray) {
			attributes.setJSONArray(key, (JSONArray) value);
		} else if (value instanceof Float) {
			attributes.setFloat(key, (float) value);
		} else if (value instanceof Double) {
			attributes.setDouble(key, (double) value);
		} else if (value instanceof Long) {
			attributes.setLong(key, (long) value);
		}
		
		return response;
	}
	
	/**
	 * Get the attributes associated with this server.
	 * @return the response to send to the client
	 */
	private JSONObject getServerAttributes() {
		JSONObject response = new JSONObject();
		setAction(response, ActionCode.GET_SERVER_ATTRIBUTES);
		setSuccess(response);
		response.setJSONObject("attributes", attributes);
		return response;
	}
	
	/**
	 * Send a message to one or more clients.
	 * @param senderId the id of the sender
	 * @param recipientIds an array containing the ids of all recipients
	 * @param message the message text
	 */
	private void sendMessage(int senderId, JSONArray recipientIds, String message) {
		for (int id : recipientIds.getIntArray()) {
			sendTo(senderId, id, message);
		}
	}
	
	/**
	 * Send a message to all clients in the same room as the sender, including the sender itself.
	 * @param the id of the sender
	 * @param message the message text
	 */
	private void broadcastMessage(int senderId, String message) {
		Integer roomId = clientIdToRoomId.get(senderId);
		if (roomId != null) {
			Room room = rooms.get(roomId);
			for (int id : room.getClientIds()) {
				sendTo(senderId, id, message);
			}
		}
	}
	
	/** Helper method to send a message to a client. */
	private void sendTo(int senderId, int recipientId, String message) {
		Client recipient = clients.get(recipientId);
		if (recipient != null) {
			JSONObject messageData = new JSONObject();
			setAction(messageData, ActionCode.GET_MESSAGE);
			setSuccess(messageData);
			messageData.setString("message", message);
			send(recipient, messageData);
		}
	}
	
	/** Helper method to set action from enum on data object. **/
	private void setAction(JSONObject data, ActionCode action) {
		data.setString("action", action.name());
	}
	
	/** Helper method to set status as success on data object. **/
	private void setSuccess(JSONObject data) {
		data.setString("status", "success");
	}
	
	/** Helper method to set status as error and set error from enum on data object. **/
	private void setError(JSONObject data, ErrorCode error) {
		data.setString("status", "error");
		data.setString("error", error.name());
	}
	
	
	/**
	 * Runs in its own thread and continuously handles the server's jobs.
	 * @author jediahkatz
	 */
	class ServerRunner implements Runnable {
		// How many ms to sleep between fetches
		private final int SLEEP_TIME = 10;
		private volatile boolean shutdown = false;
		private GameServer server;
		
		ServerRunner(GameServer server) {
			this.server = server;
		}

		@Override
		public void run() {
			while (true) {
				server.run();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
				if (shutdown) {
					return;
				}
			}
		}
		
		public void stop() {
			shutdown = true;
		}
	}
}

