package jediahkatz.gameserver;

import java.util.HashMap;

import processing.core.*;
import processing.net.*;

/** A room that can hold up to a fixed number of clients.
 * @author jediahkatz
 */
public class Room {
	private PApplet parent;
	private final int capacity;
	private HashMap<Integer, Client> clients;
	
	public Room(int capacity) {
		this.capacity = capacity;
	}

}