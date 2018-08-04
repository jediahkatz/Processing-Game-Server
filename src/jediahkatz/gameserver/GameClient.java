package jediahkatz.gameserver;

import java.io.IOException;

import processing.net.*;
import processing.core.*;
import processing.data.JSONObject;

/** A client that can connect to a server and send messages.
 * @author jediahkatz
 */
public class GameClient {
	private final int id;
	private Client client;
	
	/**
	 * 
	 * @param parent the current sketch (this)
	 * @param host the hostname of the server
	 * @param port the port to transfer data over
	 */
	public GameClient(PApplet parent, String host, int port) {
		client = new Client(parent, host, port);
		JSONObject response;
		// We can't do anything until we've gotten our ID.
		do {
			response = getData();
		} while (response == null);
		
		if (response.getString("action").equals("registerClient") && response.getString("status").equals("success")) {
			id = response.getInt("clientId");
		} else {
			throw new RuntimeException("Failed to register this client with the server.");
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
		if (client.available() > 0) {
			try {
				return JSONObject.parse(client.readString());
			} catch (RuntimeException e) {
				// Invalid JSON string
			}
		}
		return null;
	}
		
}
