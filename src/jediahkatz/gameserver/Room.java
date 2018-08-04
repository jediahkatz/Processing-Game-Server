package jediahkatz.gameserver;

import java.util.HashSet;

import processing.data.JSONArray;
import processing.data.JSONObject;

/** A room that can hold up to a fixed number of clients.
 * @author jediahkatz
 */
public class Room {
	private final int id;
	private final int capacity;
	private int size = 0;
	private JSONObject attributes = new JSONObject();
	private Server server;
	private HashSet<Integer> clientIds = new HashSet<>();
	
	public Room(Server server, int roomId, int capacity) {
		this.server = server;
		this.id = roomId;
		this.capacity = capacity;
	}
	
	/**
	 * Add a client to this room.
	 * @throws IllegalStateException if the room is full
	 */
	public void addClient(int clientId) {
		if (isFull()) {
			throw new IllegalStateException("Room full");
		}
		// TODO
	}
	
	/**
	 * Return true if this room is full.
	 */
	public boolean isFull() {
		return size == capacity;
	}
	
	/**
	 * Return the attributes associated with this room.
	 */
	public JSONObject getAttributes() {
		return attributes;
	}
	
	/**
	 * Set the attributes associated with this room.
	 * @param attributes the object to set as the new attributes
	 */
	public void setAttributes(JSONObject attributes) {
		this.attributes = attributes;
	}
	
	/**
	 * Adds an attribute to this room, or overwrites it if the key already exists.
	 * @param key the key to associate to the value
	 * @param value the value to be associated with the key
	 */
	public void putAttribute(String key, int value) {
		this.attributes.setInt(key, value);
	}
	
	/**
	 * Adds an attribute to this room, or overwrites it if the key already exists.
	 * @param key the key to associate to the value
	 * @param value the value to be associated with the key
	 */
	public void putAttribute(String key, String value) {
		this.attributes.setString(key, value);
	}
	
	/**
	 * Adds an attribute to this room, or overwrites it if the key already exists.
	 * @param key the key to associate to the value
	 * @param value the value to be associated with the key
	 */
	public void putAttribute(String key, boolean value) {
		this.attributes.setBoolean(key, value);
	}
	
	/**
	 * Adds an attribute to this room, or overwrites it if the key already exists.
	 * @param key the key to associate to the value
	 * @param value the value to be associated with the key
	 */
	public void putAttribute(String key, JSONObject value) {
		this.attributes.setJSONObject(key, value);
	}
	
	/**
	 * Adds an attribute to this room, or overwrites it if the key already exists.
	 * @param key the key to associate to the value
	 * @param value the value to be associated with the key
	 */
	public void putAttribute(String key, JSONArray value) {
		this.attributes.setJSONArray(key, value);
	}
	
	/**
	 * Adds an attribute to this room, or overwrites it if the key already exists.
	 * @param key the key to associate to the value
	 * @param value the value to be associated with the key
	 */
	public void putAttribute(String key, float value) {
		this.attributes.setFloat(key, value);
	}
	
	/**
	 * Adds an attribute to this room, or overwrites it if the key already exists.
	 * @param key the key to associate to the value
	 * @param value the value to be associated with the key
	 */
	public void putAttribute(String key, double value) {
		this.attributes.setDouble(key, value);
	}
	
	/**
	 * Adds an attribute to this room, or overwrites it if the key already exists.
	 * @param key the key to associate to the value
	 * @param value the value to be associated with the key
	 */
	public void putAttribute(String key, long value) {
		this.attributes.setLong(key, value);
	}

}