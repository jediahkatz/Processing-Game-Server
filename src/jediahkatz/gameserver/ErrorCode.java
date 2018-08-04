package jediahkatz.gameserver;

enum ErrorCode {
	/** No room exists with the specified id **/
	ROOM_NOT_FOUND,
	/** Attempting to join a room that is already full **/
	ROOM_FULL,
	/** Attempting to join a room while already in a room **/
	ALREADY_IN_ROOM,
}
