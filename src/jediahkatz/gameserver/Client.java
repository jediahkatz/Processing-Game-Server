package jediahkatz.gameserver;

import processing.core.*;

/** A client that can connect to a server and send messages.
 * @author jediahkatz
 */
public class Client {
	private final int id = 0; // TODO: don't keep this
	private processing.net.Client client;
	
	public Client(PApplet parent, String host, int port) {
		client = new processing.net.Client(parent, host, port);
	}
	
	public int getId() {
		return id;
	}
}
