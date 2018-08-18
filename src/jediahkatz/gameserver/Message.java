package jediahkatz.gameserver;

import processing.data.JSONObject;

public class Message {
	private int senderId;
	private JSONObject body;
	
	public Message(int senderId, JSONObject body) {
		this.senderId = senderId;
		this.body = body;
	}
	
	public int getSenderId() {
		return senderId;
	}
	
	public JSONObject getBody() {
		return body;
	}
}
