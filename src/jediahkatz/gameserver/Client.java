package jediahkatz.gameserver;

import java.io.IOException;

import processing.core.*;
import processing.data.JSONObject;

/** A client that can connect to a server and send messages.
 * @author jediahkatz
 */
public class Client {
	private final int id;
	private processing.net.Client client;
	
	/**
	 * 
	 * @param parent the current sketch (this)
	 * @param host the hostname of the server
	 * @param port the port to transfer data over
	 */
	public Client(PApplet parent, String host, int port) throws IOException {
		client = new processing.net.Client(parent, host, port);
		JSONObject response = getData();
		if (response.getString("action").equals("registerClient") && response.getString("status").equals("success")) {
			id = response.getInt("clientId");
		} else {
			throw new IOException("Failed to register this client with the server.");
		}
	}
	
	/**
	 * Get the unique identifier for this client.
	 * @return the client id
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Create a new room.
	 * @param capacity the maximum number of clients allowed in the room
	 * @return the unique id of the newly created room
	 */
	public int createRoom(int capacity) {
		JSONObject request = new JSONObject();
		request.setString("action", "registerRoom");
		request.setInt("capacity", capacity);
		JSONObject response = getData();
		return response.getInt("roomId");
	}
	
	/**
	 * Return the client's data as a JSONObject.
	 * @param client the client with available data as a JSON string
	 * @return JSONObject an object containing the client's data
	 */
	private JSONObject getData() {
		return JSONObject.parse(client.readString());
	}
		
}
