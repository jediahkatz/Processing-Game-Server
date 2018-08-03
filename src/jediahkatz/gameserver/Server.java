package jediahkatz.gameserver;


import processing.core.*;
import processing.net.*;

/** A multiplayer game server.
 * @author jediahkatz
 */
public class Server {
	private PApplet parent;
	private processing.net.Server server;
	
	/**
	 * 
	 * @param parent the current sketch (this)
	 * @param port the port to transfer data over
	 */
	public Server(PApplet parent, int port) {
		this.parent = parent;
		parent.registerMethod("dispose", this);
		parent.registerMethod("serverevent", this);
		
		this.server = new processing.net.Server(parent, port);
	}
	
	public void dispose() {
		server.stop();
	}
	
	public void serverEvent(processing.net.Server server, processing.net.Client client) {
		
	}
}

