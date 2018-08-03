package jediahkatz.gameserver;

import java.util.HashMap;

import processing.core.*;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.net.*;

/** A multiplayer game server.
 * @author jediahkatz
 */
public class Server {
	private processing.net.Server server;
	// Incrementing unique identifier to assign to clients and rooms
	private int clientId = 0;
	private int roomId = 0;
	private JSONObject attributes = new JSONObject();
	
	private HashMap<Integer, Room> rooms = new HashMap<>();
	
	/**
	 * 
	 * @param parent the current sketch (this)
	 * @param port the port to transfer data over
	 * @param roomCapacity the maximum number of clients in a room
	 */
	public Server(PApplet parent, int port) {
		parent.registerMethod("dispose", this);
		parent.registerMethod("serverevent", this);
		
		this.server = new processing.net.Server(parent, port);
	}
	
	public void run() {
		processing.net.Client client = server.available();
		JSONObject message = getData(client);
	}
	
	public void dispose() {
		server.stop();
	}
	
	public void serverEvent(processing.net.Server server, processing.net.Client client) {
		JSONObject response = registerClient(client);
		send(client, response);
	}
	
	/**
	 * Send a message to the specified client.
	 * @param client the recipient of the message
	 * @param message the message to send
	 */
	private void send(processing.net.Client client, JSONObject data) {
		String messageStr = data.toString();
		client.write(messageStr);
	}
	
	/**
	 * Return the client's data as a JSONObject.
	 * @param client the client with available data as a JSON string
	 * @return JSONObject an object containing the client's data
	 */
	private JSONObject getData(processing.net.Client client) {
		return JSONObject.parse(client.readString());
	}
	
	/**
	 * Take action based on the content of the received data.
	 * @param client
	 * @param message
	 */
	private void handleData(processing.net.Client client, JSONObject data) {
		if (data.hasKey("action")) {
			JSONObject response; 
			switch (data.getString("action")) {
			case "registerRoom":
				response = registerRoom(data.getInt("capacity"));
				break;
			case "getRoomAttributes":
				response = getRoomAttributes(data.getInt("roomId"));
				break;
			case "setRoomAttributes":
				response = setRoomAttributes(data.getInt("roomId"), data.getJSONObject("attributes"));
				break;
			case "putRoomAttributes":
				response = putRoomAttribute(data.getInt("roomId"), data.getString("key"), data.get("value"));
				break;
			default:
				throw new RuntimeException("Invalid action: " + data.getString("action"));
			}
			
			send(client, response);
		}
		throw new RuntimeException("Data sent to server must have an 'action' attribute.");
	}
	
	/**
	 * Register a client (assign it an ID).
	 * @param client the client to register
	 * @return the response to send to the client, containing "clientId" key
	 */
	private JSONObject registerClient(processing.net.Client client) {
		JSONObject response = new JSONObject();
		response.setString("action", "registerClient");
		response.setString("status", "success");
		response.setInt("clientId", clientId++);
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
		
		Room room = new Room(id, capacity);
		rooms.put(id, room);
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
		response.setString("action", "getRoomAttributes");

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
		response.setString("action", "getRoomAttributes");

		Room room = rooms.get(roomId);
		if (room != null) {
			room.setAttributes(attributes);
			// This is the only way to figure out what kind of object was passed in
			response.setString("status", "success");
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
}

