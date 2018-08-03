package jediahkatz.gameserver;

import java.util.HashMap;

import processing.core.*;
import processing.data.JSONObject;
import processing.net.*;

/** A room that can hold up to a fixed number of clients.
 * @author jediahkatz
 */
public class Room {
	private final int id;
	private final int capacity;
	private int size = 0;
	private JSONObject attributes = new JSONObject();
	private HashMap<Integer, Client> clients = new HashMap<>();
	
	public Room(int id, int capacity) {
		this.id = id;
		this.capacity = capacity;
	}
	
	/**
	 * Add a Client to this room.
	 * @throws IllegalStateException if the room is full
	 */
	public void addClient() {
		if (isFull()) {
			throw new IllegalStateException("Room full");
		}
	}
	
	public boolean isFull() {
		return size == capacity;
	}
	
	public JSONObject getAttributes() {
		return attributes;
	}
	
	public void setAttributes(JSONObject attributes) {
		this.attributes = attributes;
	}

}