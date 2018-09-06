package jediahkatz.gameserver;

import java.util.Arrays;

import processing.data.JSONObject;

/**
 * An uninstantiable wrapper class that holds information about a room.
 * Data is only accurate up to the time the RoomInfo object was created.
 * @author jediahkatz
 *
 */
public class RoomInfo {
	private final int id;
	private final int capacity;
	private final int size;
	private final JSONObject attributes;
	private final int[] clientIds;
	private final String clientsString;
	
	RoomInfo(int id, int capacity, int size, JSONObject attributes, int[] clientIds) {
		this.id = id;
		this.capacity = capacity;
		this.size = size;
		this.attributes = attributes;
		this.clientIds = clientIds;
		clientsString = Arrays.toString(clientIds);
	}
	
	/** Get this room's unique id. **/
	public int id() {
		return id;
	}
	
	/** Get the number of clients currently in this room. **/
	public int size() {
		return size;
	}
	
	/** Get the maximum number of clients allowed in this room. **/
	public int capacity() {
		return capacity;
	}
	
	/** Get an array containing the unique ids of all clients currently in this room.
	 * Modifying this array will modify the array in this RoomInfo instance, but will
	 * not modify the actual room in any way. 
	**/
	public int[] clients() {
		return clientIds;
	}
	
	/** Get a JSONObject containing the attributes for this room.
	 * Modifying this JSONObject will modify the attributes in this RoomInfo
	 * instance, but will not modify the actual room in any way. 
	**/
	public JSONObject attributes() {
		return attributes;
	}
	
	@Override
	public String toString() {
		return "Room " + id + " (capacity " + capacity + "): {\nClients: " 
				+ clientsString + "\nAttributes: " + attributes.toString() + "\n}";
	}
}
