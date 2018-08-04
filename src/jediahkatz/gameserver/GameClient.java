package jediahkatz.gameserver;

import processing.net.*;
import processing.core.*;
import processing.data.JSONObject;

/** A client that can connect to a server and send messages.
 * @author jediahkatz
 */
public class GameClient {
	// Beep character - data separator
	private final char SEP = (char) 7;
	private final int id;
	private Client client;
	
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
		JSONObject response; 
		do {
			response = getData();
		} while (response == null);
		
		if (response.getString("action").equals("registerClient") 
				&& response.getString("status").equals("success")) {
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
		request.setString("action", "disconnect");
		request.setInt("clientId", id);
		send(request);
		client.stop();
	}
	
	public boolean connected() {
		return client != null && client.active();
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
		send(request);
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
				String s = client.readStringUntil(SEP);
				return JSONObject.parse(s);
			} catch (RuntimeException e) {
				// Invalid JSON string
			}
		}
		return null;
	}

	/**
	 * Send data to the server.
	 * @param data the data to send
	 */
	private void send(JSONObject data) {
		String messageStr = data.toString();
		client.write(messageStr + SEP);
	}
		
}
