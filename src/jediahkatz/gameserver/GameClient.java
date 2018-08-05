package jediahkatz.gameserver;

import processing.net.*;
import processing.core.*;
import processing.data.JSONObject;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;

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
				
		JSONObject response = waitForFirstAction("registerClient");
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
		request.setString("action", "disconnect");
		request.setInt("clientId", id);
		send(request);
		thread.stop();
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
		JSONObject response = waitForFirstAction("registerRoom");
		if (response.getString("status").equals("success")) {
			return response.getInt("roomId");
		}
		throw new RuntimeException("Failed to create new room.");
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
					appendAction(action, data);
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
	private void appendAction(String action, JSONObject data) {
		Queue<JSONObject> buffer = dataBuffer.get(action);
		if (buffer == null) {
			buffer = new ConcurrentLinkedQueue<>();
			dataBuffer.put(action, buffer);
		}
		buffer.add(data);
	}
	
	/**
	 * Get the first data object received with the given action type, and remove it from the buffer.
	 * @param action the type of action to search for
	 * @returns the first data received with given action type, or null if none exists
	 */
	private JSONObject getFirstAction(String action) {
		Queue<JSONObject> buffer = dataBuffer.get(action);
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
	private JSONObject waitForFirstAction(String action) {
		long startTime = System.currentTimeMillis();
		JSONObject data;
		do {
			if (System.currentTimeMillis() - startTime >= TIMEOUT) {
				throw new RuntimeException("Timed out waiting for " + action + " action");
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
		
}
