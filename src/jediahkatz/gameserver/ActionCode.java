package jediahkatz.gameserver;

enum ActionCode {
	/** Disconnect the client from the server. **/
	DISCONNECT,
	/** Create a new room. **/
	REGISTER_ROOM,
	/** Add a client to a room. **/
	JOIN_ROOM,
	/** Remove a client from a room. **/
	LEAVE_ROOM,
	/** Add a client to an arbitrary room or create a new one if all rooms are full. **/
	AUTOJOIN_ROOM,
	/** Get a RoomInfo object containing data about a room. **/
	GET_ROOM_INFO,
	/** Get an array containing the RoomInfo objects for each room. **/
	GET_ROOMS_INFO,
	/** Set the attributes for a room with a new JSONObject. **/
	SET_ROOM_ATTRIBUTES,
	/** Add a single attribute to a room. **/
	PUT_ROOM_ATTRBUTE,
	/** Set the attributes for the server with a new JSONObject. **/
	SET_SERVER_ATTRIBUTES,
	/** Add a single attribute to the server. **/
	PUT_SERVER_ATTRBUTE,
	/** Send a message to some clients, possibly in a different room. **/
	SEND_MESSAGE,
	/** Send a message to all clients in the same room. **/
	BROADCAST_MESSAGE,
}
