package jediahkatz.gameserver;

import java.util.HashMap;

import processing.core.*;
import processing.data.*;
import processing.net.*;

/** A multiplayer game server.
 * @author jediahkatz
 */
public class Server {
	private processing.net.Server server;
	// Incrementing unique identifier to assign to clients and rooms
	private int clientId = 0;
	private int roomId = 0;
	
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
		String messageStr = data.format(0).replaceAll("\n", "");
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
			JSONObject response = null; 
			switch (data.getString("action")) {
			// The client is requesting the creation of a new room.
			case "registerRoom":
				response = registerRoom(data.getInt("capacity"));
			default:
				break;
			}
			
			if (response != null) {
				send(client, response);
			}
		}
	}
	
	/**
	 * Register a client (assign it an ID).
	 * @param client the client to register
	 * @return the response to send to the client
	 */
	private JSONObject registerClient(processing.net.Client client) {
		JSONObject response = new JSONObject();
		response.setString("action", "registerClient");
		response.setString("status", "success");
		response.setInt("id", clientId++);
		return response;
	}
	
	/**
	 * Register a new room and add it to the list of rooms.
	 * @return
	 */
	private JSONObject registerRoom(int capacity) {
		JSONObject response = new JSONObject();
		response.setString("action", "registerRoom");
		response.setString("status", "success");
		int id = roomId++;
		response.setInt("id", id);
		
		Room room = new Room(id, capacity);
		rooms.put(id, room);
		return response;
	}
}

