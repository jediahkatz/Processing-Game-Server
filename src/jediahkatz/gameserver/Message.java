package jediahkatz.gameserver;

import processing.data.JSONArray;
import processing.data.JSONObject;

public class Message {
	private int senderId;
	private JSONObject body;
	
	Message(int senderId, JSONObject body) {
		this.senderId = senderId;
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
	
	/** Convenience method for getBody().getString(key). **/
	public String getString(String key) {
		return body.getString(key);
	}
	
	/** Convenience method for getBody().getInt(key). **/
	public int getInt(String key) {
		return body.getInt(key);
	}
	
	/** Convenience method for getBody().getBoolean(key). **/
	public boolean getBoolean(String key) {
		return body.getBoolean(key);
	}
	
	/** Convenience method for getBody().getJSONObject(key). **/
	public JSONObject getJSONObject(String key) {
		return body.getJSONObject(key);
	}
	
	/** Convenience method for getBody().getJSONArray(key). **/
	public JSONArray getJSONArray(String key) {
		return body.getJSONArray(key);
	}
	
	/** Convenience method for getBody().getFloat(key). **/
	public float getFloat(String key) {
		return body.getFloat(key);
	}
	
	/** Convenience method for getBody().getDouble(key). **/
	public double getDouble(String key) {
		return body.getDouble(key);
	}
	
	/** Convenience method for getBody().getLong(key). **/
	public long getLong(String key) {
		return body.getLong(key);
	}
}
