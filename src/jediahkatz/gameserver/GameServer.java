package jediahkatz.gameserver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
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
	private int clientId = 0;
	private int roomId = 0;
	private JSONObject attributes = new JSONObject();
	// Data structures storing rooms/clients
	private HashMap<Integer, Room> rooms = new HashMap<>();
	private HashMap<Integer, processing.net.Client> clients = new HashMap<>();
	private HashMap<Integer, Integer> clientIdToRoomId = new HashMap<>();
	// Keep track of which clients have disconnected safely - this stores their hashcodes
	private HashSet<Integer> disconnected = new HashSet<>();
	
	/**
	 * 
	 * @param parent the current sketch (this)
	 * @param port the port to transfer data over
	 */
	public GameServer(PApplet parent, int port) {
		// Unfortunately the generic registerMethod is no longer supported. These will need to be called manually.
		//parent.registerMethod("serverEvent", this);
		//parent.registerMethod("disconnectEvent", this);
		server = new Server(parent, port);
	}
	
	/**
	 * Run the server.
	 */
	public void run() {
		Client client = server.available();
		while (client != null) {
			JSONObject data = getData(client);
			handleData(client, data);
			client = server.available();
		}
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
		response.setString("action", "registerClient");
		response.setString("status", "success");
		Integer id = clientId++;
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
		response.setString("action", "registerRoom");
		response.setString("status", "success");
		int id = roomId++;
		response.setInt("roomId", id);
		
		Room room = new Room(this, id, capacity);
		rooms.put(id, room);
		return response;
	}
	
	private JSONObject getRoomIds() {
		JSONObject response = new JSONObject();
		response.setString("action", "getRoomIds");
		return null;
	}
	
	/**
	 * Add a client to a room.
	 * @param clientId the id of the client to add to the room
	 * @param roomId the id of the room to add the client to
	 * @return the response to send to the client
	 */
	private JSONObject joinRoom(int clientId, int roomId) {
		JSONObject response = new JSONObject();
		response.setString("action", "joinRoom");
		
		if (clientIdToRoomId.containsKey(clientId)) {
			response.setString("status", "error");
			response.setInt("errorCode", ErrorCode.ALREADY_IN_ROOM.ordinal());
		} else {
			Room room = rooms.get(roomId);
			if (room == null) {
				response.setString("status", "error");
				response.setInt("errorCode", ErrorCode.ROOM_NOT_FOUND.ordinal());
			} else if (room.isFull()) {
				response.setString("status", "error");
				response.setInt("errorCode", ErrorCode.ROOM_FULL.ordinal());
			} else {
				response.setString("status", "success");
				room.addClient(clientId);
			}
		}
		
		return response;
	}
	
	/**
	 * Get the attributes of a room.
	 * @param roomId the id of the room
	 * @return the response to send to the client, containing "attributes" key
	 */
	private JSONObject getRoomAttributes(int roomId) {
		JSONObject response = new JSONObject();
		response.setString("action", "getRoomAttributes");

		Room room = rooms.get(roomId);
		if (room != null) {
			response.setString("status", "success");
			response.setJSONObject("attributes", room.getAttributes());
		} else {
			response.setString("status", "error");
			response.setInt("errorCode", ErrorCode.ROOM_NOT_FOUND.ordinal());
		}
		
		return response;
	}
	
	/**
	 * Set the attributes of a room.
	 * @param roomId the id of the room
	 * @param attributes the object to set as the new attributes
	 * @return the response to send to the client
	 */
	private JSONObject setRoomAttributes(int roomId, JSONObject attributes) {
		JSONObject response = new JSONObject();
		response.setString("action", "setRoomAttributes");

		Room room = rooms.get(roomId);
		if (room != null) {
			room.setAttributes(attributes);
			response.setString("status", "success");
		} else {
			response.setString("status", "error");
			response.setInt("errorCode", ErrorCode.ROOM_NOT_FOUND.ordinal());
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
		response.setString("action", "putRoomAttribute");

		Room room = rooms.get(roomId);
		if (room != null) {
			response.setString("status", "success");
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
			response.setString("status", "error");
			response.setInt("errorCode", ErrorCode.ROOM_NOT_FOUND.ordinal());
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
		response.setString("action", "setServerAttributes");
		response.setString("status", "success");
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
		response.setString("action", "putServerAttribute");
		response.setString("status", "success");
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
}

