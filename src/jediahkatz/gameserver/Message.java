package jediahkatz.gameserver;

import processing.data.JSONObject;

public class Message {
	private int senderId;
	private JSONObject body;
	
	Message(int senderId, JSONObject body) {
		this.body = body;
	}
	
	/**
	 * Get the id of the client who sent this message.
	 * @return the id of the sender
	 */
	public int getSenderId() {
		return senderId;
	}
	
	/**
	 * Get the body of this message as a JSONObject.
	 * @return the message body/data
	 */
	public JSONObject getBody() {
		return body;
	}
}
