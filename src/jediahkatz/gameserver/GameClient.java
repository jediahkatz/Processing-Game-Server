package jediahkatz.gameserver;

import processing.net.*;
import processing.core.*;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** A client that can connect to a server and send messages.
 * @author jediahkatz
 */
public class GameClient {
	// Beep character - data separator
	private final char SEP = (char) 7;
	// The maximum time in msec to wait for data before throwing an exception
	private final int TIMEOUT = 1000;
	private final DataFetcher thread;
	
	private final int id;
	private Integer roomId = null;
	private Client client;
	// Maps action to a buffer containing data objects for those actions
	private Map<String, Queue<JSONObject>> dataBuffer = new ConcurrentHashMap<>();
	
	/**
	 * 
	 * @param parent the current sketch (this)
	 * @param host the hostname of the server
	 * @param port the port to transfer data over
	 */
	public GameClient(PApplet parent, String host, int port) {
		parent.registerMethod("dispose", this);
		client = new Client(parent, host, port);
		if (!connected()) {
			throw new RuntimeException("Failed to connect to the server.");
		}
		
		// Start a new thread for this client to fetch data
		thread = new DataFetcher(this);
		new Thread(thread).start();
				
		JSONObject response = waitForFirstAction(ActionCode.REGISTER_CLIENT);
		if (response.getString("status").equals("success")) {
			id = response.getInt("clientId");
		} else {
			throw new RuntimeException("Failed to register this client with the server.");
		}
	}
	
	/**
	 * Disconnect if we haven't already.
	 */
	public void dispose() {
		if (connected()) {
			disconnect();
		}
	}
	
	/**
	 * Let the server know that we're disconnecting, and disconnect.
	 */
	public void disconnect() {
		JSONObject request = new JSONObject();
		setAction(request, ActionCode.DISCONNECT);
		request.setInt("clientId", id);
		send(request);
		thread.stop();
		client.stop();
	}
	
	/**
	 * Check if this client is currently connected to the server.
	 * @return true if this client is connected, otherwise false
	 */
	public boolean connected() {
		return client != null && client.active();
	}
	
	/**
	 * Get the unique identifier for this client.
	 * @return the client id
	 */
	public int id() {
		return id;
	}
	
	/**
	 * Get the id of the room that this client is currently in, or null if this client is not in a room.
	 * @return the id of this client's room, or null if not in a room
	 */
	public Integer roomId() {
		return roomId;
	}
	
	/**
	 * Create a new room.
	 * @param capacity the maximum number of clients allowed in the room
	 * @return the unique id of the newly created room
	 */
	public int createRoom(int capacity) {
		JSONObject request = new JSONObject();
		setAction(request, ActionCode.REGISTER_ROOM);
		request.setInt("capacity", capacity);
		send(request);
		JSONObject response = waitForFirstAction(ActionCode.REGISTER_ROOM);
		if (response.getString("status").equals("success")) {
			return response.getInt("roomId");
		}
		throw new RuntimeException("Failed to create new room.");
	}
	
	/**
	 * Join an existing room.
	 * @param roomId the unique id of the room to join
	 * @return an object containing info about the room joined
	 * @throws NoSuchElementException if no room exists with the given id
	 * @throws RoomFullException if the room is already full
	 * @throws AlreadyInRoomException if this client is currently in a room
	 */
	public RoomInfo joinRoom(int roomId) {
		if (this.roomId != null) {
			throw new AlreadyInRoomException("Can't join a room while already in a room.");
		}
		
		JSONObject request = new JSONObject();
		setAction(request, ActionCode.REGISTER_ROOM);
		request.setInt("roomId", roomId);
		send(request);
		JSONObject response = waitForFirstAction(ActionCode.REGISTER_ROOM);
		if (response.getString("status").equals("error")) {
			switch (ErrorCode.valueOf(response.getString("error"))) {
			case ALREADY_IN_ROOM: // This hopefully should never happen
				throw new AlreadyInRoomException("Can't join a room while already in a room.");
			case ROOM_FULL:
				throw new RoomFullException("Tried to join a room that is already full.");
			case ROOM_NOT_FOUND:
				throw new NoSuchElementException("No room exists with id: " + roomId);
			default:
				throw new RuntimeException("Failed to join room.");
			}
		}
		
		this.roomId = roomId;
		return constructRoomInfo(response);
	}
	
	/**
	 * Leave the room that this client is currently in.
	 * If the client is not in a room this method does nothing.
	 */
	public void leaveRoom() {
		if (roomId != null) {
			JSONObject request = new JSONObject();
			setAction(request, ActionCode.LEAVE_ROOM);
			send(request);
			JSONObject response = waitForFirstAction(ActionCode.LEAVE_ROOM);
			if (response.getString("status").equals("success")) {
				roomId = null;
			} else {
				throw new RuntimeException("Failed to leave room.");
			}
		}
	}
	
	/**
	 * Join any room that isn't full, or create a new room if all rooms are full.
	 * @param capacity the maximum number of clients allowed in a new room, if one is created
	 * @return an object containing info about the room joined
	 * @throws AlreadyInRoomException if this client is currently in a room
	 */
	public RoomInfo autojoinRoom(int capacity) {
		if (this.roomId != null) {
			throw new AlreadyInRoomException("Can't join a room while already in a room.");
		}
		
		JSONObject request = new JSONObject();
		setAction(request, ActionCode.AUTOJOIN_ROOM);
		request.setInt("capacity", capacity);
		send(request);
		JSONObject response = waitForFirstAction(ActionCode.AUTOJOIN_ROOM);
		if (response.getString("status").equals("error")) {
			switch (ErrorCode.valueOf(response.getString("error"))) {
			case ALREADY_IN_ROOM: // This hopefully should never happen
				throw new AlreadyInRoomException("Can't join a room while already in a room.");
			default:
				throw new RuntimeException("Failed to autojoin a room.");
			}
		}
		
		RoomInfo info = constructRoomInfo(response);
		this.roomId = info.id();
		return info;
	}
	
	/**
	 * Get info about a room.
	 * @param roomId the unique id of the room to look up
	 * @return an object containing info about the room
	 * @throws NoSuchElementException if no room exists with the given id
	 */
	public RoomInfo getRoomInfo(int roomId) {
		JSONObject request = new JSONObject();
		setAction(request, ActionCode.GET_ROOM_INFO);
		request.setInt("roomId", roomId);
		send(request);
		JSONObject response = waitForFirstAction(ActionCode.GET_ROOM_INFO);
		if (response.getString("status").equals("error")) {
			switch (ErrorCode.valueOf(response.getString("error"))) {
			case ROOM_NOT_FOUND:
				throw new NoSuchElementException("No room exists with id: " + roomId);
			default:
				throw new RuntimeException("Failed to get room info.");
			}
		}
		
		return constructRoomInfo(response);
	}
	
	
	/**
	 * Get info about all rooms on the server.
	 * @return an array of objects containing info about the rooms
	 */
	public RoomInfo[] getRoomsInfo() {
		JSONObject request = new JSONObject();
		setAction(request, ActionCode.GET_ROOMS_INFO);
		send(request);
		JSONObject response = waitForFirstAction(ActionCode.GET_ROOMS_INFO);
		if (response.getString("status").equals("error")) {
			throw new RuntimeException("Failed to get rooms info.");
		}
		
		JSONArray roomsInfo = response.getJSONArray("roomsInfo");
		RoomInfo[] infoArray = new RoomInfo[roomsInfo.size()];
		for (int i=0; i<infoArray.length; i++) {
			infoArray[i] = constructRoomInfo(roomsInfo.getJSONObject(i));
		}
		return infoArray;
	}
	
	/**
	 * Set the attributes for a room with a new JSONObject.
	 * @param roomId the unique id of the room to set attributes for
	 * @param attributes the object containing the attributes to set for the room
	 * @throws NoSuchElementException if no room exists with the given id
	 */
	public void setRoomAttributes(int roomId, JSONObject attributes) {
		JSONObject request = new JSONObject();
		setAction(request, ActionCode.SET_ROOM_ATTRIBUTES);
		request.setInt("roomId", roomId);
		request.setJSONObject("attributes", attributes);
		send(request);
		JSONObject response = waitForFirstAction(ActionCode.SET_ROOM_ATTRIBUTES);
		if (response.getString("status").equals("error")) {
			switch (ErrorCode.valueOf(response.getString("error"))) {
			case ROOM_NOT_FOUND:
				throw new NoSuchElementException("No room exists with id: " + roomId);
			default:
				throw new RuntimeException("Failed to set room attributes.");
			}
		}
	}
	
	/**
	 * Add a single attribute to a room.
	 * @param roomId the unique id of the room to add an attribute to
	 * @param key the key or name of the attribute
	 * @param value the value of the attribute
	 * @throws NoSuchElementException if no room exists with the given id
	 */
	public void putRoomAttribute(int roomId, String key, String value) {
		JSONObject request = new JSONObject();
		setAction(request, ActionCode.PUT_ROOM_ATTRIBUTE);
		request.setInt("roomId", roomId);
		request.setString("key", key);
		request.setString("value", value);
		send(request);
		JSONObject response = waitForFirstAction(ActionCode.PUT_ROOM_ATTRIBUTE);
		if (response.getString("status").equals("error")) {
			switch (ErrorCode.valueOf(response.getString("error"))) {
			case ROOM_NOT_FOUND:
				throw new NoSuchElementException("No room exists with id: " + roomId);
			default:
				throw new RuntimeException("Failed to add room attribute.");
			}
		}
	}
	
	/**
	 * Add a single attribute to a room.
	 * @param roomId the unique id of the room to add an attribute to
	 * @param key the key or name of the attribute
	 * @param value the value of the attribute
	 * @throws NoSuchElementException if no room exists with the given id
	 */
	public void putRoomAttribute(int roomId, String key, int value) {
		JSONObject request = new JSONObject();
		setAction(request, ActionCode.PUT_ROOM_ATTRIBUTE);
		request.setInt("roomId", roomId);
		request.setString("key", key);
		request.setInt("value", value);
		send(request);
		JSONObject response = waitForFirstAction(ActionCode.PUT_ROOM_ATTRIBUTE);
		if (response.getString("status").equals("error")) {
			switch (ErrorCode.valueOf(response.getString("error"))) {
			case ROOM_NOT_FOUND:
				throw new NoSuchElementException("No room exists with id: " + roomId);
			default:
				throw new RuntimeException("Failed to add room attribute.");
			}
		}
	}
	
	/**
	 * Add a single attribute to a room.
	 * @param roomId the unique id of the room to add an attribute to
	 * @param key the key or name of the attribute
	 * @param value the value of the attribute
	 * @throws NoSuchElementException if no room exists with the given id
	 */
	public void putRoomAttribute(int roomId, String key, boolean value) {
		JSONObject request = new JSONObject();
		setAction(request, ActionCode.PUT_ROOM_ATTRIBUTE);
		request.setInt("roomId", roomId);
		request.setString("key", key);
		request.setBoolean("value", value);
		send(request);
		JSONObject response = waitForFirstAction(ActionCode.PUT_ROOM_ATTRIBUTE);
		if (response.getString("status").equals("error")) {
			switch (ErrorCode.valueOf(response.getString("error"))) {
			case ROOM_NOT_FOUND:
				throw new NoSuchElementException("No room exists with id: " + roomId);
			default:
				throw new RuntimeException("Failed to add room attribute.");
			}
		}
	}
	
	/**
	 * Add a single attribute to a room.
	 * @param roomId the unique id of the room to add an attribute to
	 * @param key the key or name of the attribute
	 * @param value the value of the attribute
	 * @throws NoSuchElementException if no room exists with the given id
	 */
	public void putRoomAttribute(int roomId, String key, JSONObject value) {
		JSONObject request = new JSONObject();
		setAction(request, ActionCode.PUT_ROOM_ATTRIBUTE);
		request.setInt("roomId", roomId);
		request.setString("key", key);
		request.setJSONObject("value", value);
		send(request);
		JSONObject response = waitForFirstAction(ActionCode.PUT_ROOM_ATTRIBUTE);
		if (response.getString("status").equals("error")) {
			switch (ErrorCode.valueOf(response.getString("error"))) {
			case ROOM_NOT_FOUND:
				throw new NoSuchElementException("No room exists with id: " + roomId);
			default:
				throw new RuntimeException("Failed to add room attribute.");
			}
		}
	}
	
	/**
	 * Add a single attribute to a room.
	 * @param roomId the unique id of the room to add an attribute to
	 * @param key the key or name of the attribute
	 * @param value the value of the attribute
	 * @throws NoSuchElementException if no room exists with the given id
	 */
	public void putRoomAttribute(int roomId, String key, JSONArray value) {
		JSONObject request = new JSONObject();
		setAction(request, ActionCode.PUT_ROOM_ATTRIBUTE);
		request.setInt("roomId", roomId);
		request.setString("key", key);
		request.setJSONArray("value", value);
		send(request);
		JSONObject response = waitForFirstAction(ActionCode.PUT_ROOM_ATTRIBUTE);
		if (response.getString("status").equals("error")) {
			switch (ErrorCode.valueOf(response.getString("error"))) {
			case ROOM_NOT_FOUND:
				throw new NoSuchElementException("No room exists with id: " + roomId);
			default:
				throw new RuntimeException("Failed to add room attribute.");
			}
		}
	}
	
	/**
	 * Add a single attribute to a room.
	 * @param roomId the unique id of the room to add an attribute to
	 * @param key the key or name of the attribute
	 * @param value the value of the attribute
	 * @throws NoSuchElementException if no room exists with the given id
	 */
	public void putRoomAttribute(int roomId, String key, float value) {
		JSONObject request = new JSONObject();
		setAction(request, ActionCode.PUT_ROOM_ATTRIBUTE);
		request.setInt("roomId", roomId);
		request.setString("key", key);
		request.setFloat("value", value);
		send(request);
		JSONObject response = waitForFirstAction(ActionCode.PUT_ROOM_ATTRIBUTE);
		if (response.getString("status").equals("error")) {
			switch (ErrorCode.valueOf(response.getString("error"))) {
			case ROOM_NOT_FOUND:
				throw new NoSuchElementException("No room exists with id: " + roomId);
			default:
				throw new RuntimeException("Failed to add room attribute.");
			}
		}
	}
	
	/**
	 * Add a single attribute to a room.
	 * @param roomId the unique id of the room to add an attribute to
	 * @param key the key or name of the attribute
	 * @param value the value of the attribute
	 * @throws NoSuchElementException if no room exists with the given id
	 */
	public void putRoomAttribute(int roomId, String key, double value) {
		JSONObject request = new JSONObject();
		setAction(request, ActionCode.PUT_ROOM_ATTRIBUTE);
		request.setInt("roomId", roomId);
		request.setString("key", key);
		request.setDouble("value", value);
		send(request);
		JSONObject response = waitForFirstAction(ActionCode.PUT_ROOM_ATTRIBUTE);
		if (response.getString("status").equals("error")) {
			switch (ErrorCode.valueOf(response.getString("error"))) {
			case ROOM_NOT_FOUND:
				throw new NoSuchElementException("No room exists with id: " + roomId);
			default:
				throw new RuntimeException("Failed to add room attribute.");
			}
		}
	}
	
	/**
	 * Add a single attribute to a room.
	 * @param roomId the unique id of the room to add an attribute to
	 * @param key the key or name of the attribute
	 * @param value the value of the attribute
	 * @throws NoSuchElementException if no room exists with the given id
	 */
	public void putRoomAttribute(int roomId, String key, Long value) {
		JSONObject request = new JSONObject();
		setAction(request, ActionCode.PUT_ROOM_ATTRIBUTE);
		request.setInt("roomId", roomId);
		request.setString("key", key);
		request.setLong("value", value);
		send(request);
		JSONObject response = waitForFirstAction(ActionCode.PUT_ROOM_ATTRIBUTE);
		if (response.getString("status").equals("error")) {
			switch (ErrorCode.valueOf(response.getString("error"))) {
			case ROOM_NOT_FOUND:
				throw new NoSuchElementException("No room exists with id: " + roomId);
			default:
				throw new RuntimeException("Failed to add room attribute.");
			}
		}
	}
	
	/**
	 * Set the attributes for the server with a new JSONObject.
	 * @param attributes the object containing the attributes to set for the room
	 */
	public void setServerAttributes(JSONObject attributes) {
		JSONObject request = new JSONObject();
		setAction(request, ActionCode.SET_SERVER_ATTRIBUTES);
		request.setJSONObject("attributes", attributes);
		send(request);
		JSONObject response = waitForFirstAction(ActionCode.SET_SERVER_ATTRIBUTES);
		if (response.getString("status").equals("error")) {
			throw new RuntimeException("Failed to add server attribute.");
		}
	}
		
	/**
	 * Construct a RoomInfo object from the given data.
	 */
	private RoomInfo constructRoomInfo(JSONObject data) {
		int[] clientIds = data.getJSONArray("clientIds").getIntArray();
		return new RoomInfo(data.getInt("roomId"), data.getInt("capacity"), data.getInt("size"), 
				data.getJSONObject("attributes"), clientIds);
	}
	
	/**
	 * Fetch new data for this client and put it into the buffer.
	 */
	private void fetchData() {
		if (client.available() > 0) {
			try {
				String s = client.readStringUntil(SEP);
				JSONObject data = JSONObject.parse(s);
				if (data.hasKey("action")) {
					String action = data.getString("action");
					appendAction(ActionCode.valueOf(action), data);
				}
			} catch (RuntimeException e) {
				// Invalid JSON string
			}
		}
	}
	
	/**
	 * Add an action to the buffer.
	 * @param action the name of the action
	 * @param data the action/response data
	 */
	private void appendAction(ActionCode action, JSONObject data) {
		String actionStr = action.name();
		Queue<JSONObject> buffer = dataBuffer.get(actionStr);
		if (buffer == null) {
			buffer = new ConcurrentLinkedQueue<>();
			dataBuffer.put(actionStr, buffer);
		}
		buffer.add(data);
	}
	
	/**
	 * Get the first data object received with the given action type, and remove it from the buffer.
	 * @param action the type of action to search for
	 * @returns the first data received with given action type, or null if none exists
	 */
	private JSONObject getFirstAction(ActionCode action) {
		String actionStr = action.name();
		Queue<JSONObject> buffer = dataBuffer.get(actionStr);
		if (buffer != null) {
			return buffer.poll();
		}
		return null;
	}
	
	/**
	 * Get the first data object received with the given action type, and remove it from the buffer.
	 * If no such object exists, this method will wait until one is received or until it times out.
	 * @param action the type of action to search for
	 * @returns the first data received with given action type
	 */
	private JSONObject waitForFirstAction(ActionCode action) {
		long startTime = System.currentTimeMillis();
		JSONObject data;
		do {
			if (System.currentTimeMillis() - startTime >= TIMEOUT) {
				throw new RuntimeException("Timed out waiting for action: " + action);
			}
			data = getFirstAction(action);
		} while (data == null);
		return data;
	}

	/**
	 * Send data to the server.
	 * @param data the data to send
	 */
	private void send(JSONObject data) {
		String messageStr = data.toString();
		client.write(messageStr + SEP);
	}
	
	/** Helper method to set action from enum on data object. **/
	private void setAction(JSONObject data, ActionCode action) {
		data.setInt("clientId", id);
		data.setString("action", action.name());
	}
	
	/**
	 * Runs in its own thread and continuously fetches new data for the client.
	 * @author jediahkatz
	 */
	class DataFetcher implements Runnable {
		// How many ms to sleep between fetches
		private final int SLEEP_TIME = 10;
		private volatile boolean shutdown = false;
		private GameClient client;
		
		DataFetcher(GameClient client) {
			this.client = client;
		}

		@Override
		public void run() {
			while (true) {
				client.fetchData();
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
	
	/**
	 * Exception thrown when trying to join a full room.
	 * @author jediahkatz
	 */
	@SuppressWarnings("serial")
	public class RoomFullException extends IllegalStateException {
		RoomFullException(String message) {
			super(message);
		};
	}
	
	/**
	 * Exception thrown when trying to join a room while already in a room.
	 * @author jediahkatz
	 */
	@SuppressWarnings("serial")
	public class AlreadyInRoomException extends IllegalStateException {
		AlreadyInRoomException(String message) {
			super(message);
		}
	}
		
}
