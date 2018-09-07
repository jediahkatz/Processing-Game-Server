# GameServer
## An easy-to-use server for creating multiplayer games in Processing

![A sample Rock Paper Scissors game](https://github.com/jediahkatz/Processing-Game-Server/blob/master/assets/rock_paper_scissors.gif
"A sample Rock Paper Scissors game")

#### Check out the [project site](https://jediahkatz.github.io/Processing-Game-Server/) and the [Javadocs](https://jediahkatz.github.io/Processing-Game-Server/reference/index.html)

### Installation
Download the entire project as a zip file by clicking `Clone or Download > Download ZIP` 
on GitHub. Go to your Processing libraries folder (by default it's at 
`C:\Users\YourName\Documents\Processing\libraries`) and extract the zip file
 you just downloaded into a folder called `GameServer`. Finally, you should be 
 able to access the library from your Processing IDE by clicking 
 `Sketch > Import Library > GameServer` and also `Sketch > Import Library > Network`.
 
### Tutorial

Every game will be composed of two types of sketches:

**Server** A sketch that continuously runs, sending out and receiving data
to and from the clients. There will generally be only one unique server sketch,
and in most cases there's nothing to program - you can just copy and paste the 15-line
`BasicServer.pde` sketch included in the Examples (and reproduced below).

**Client** A sketch that interacts with an actual player of your game,
sending out the results of the player's actions to the server and receiving
server updates about the other clients. There are generally many client sketches,
typically just copies of the same sketch, which will need to be programmed by you.

#### Running the server

The following `BasicServer.pde` sketch should satisfy most of your server needs.
Just keep it as a separate sketch and make sure it's running before you run any clients.
Don't forget to put in the correct port, if you plan to change it.
```processing
import jediahkatz.gameserver.*;
import processing.net.*;
// Replace 4321 with your port
GameServer server = new GameServer(this, 4321);
void setup() {}
void draw() {}
void serverEvent(Server s, Client c) {
  server.serverEvent(s, c);
}
void disconnectEvent(Client c) {
  server.disconnectEvent(c);
}
```

### Building a basic client

Here's a small sample of what your client can do.
Read the documentation to learn about all the other functionality.
Also don't forget to check out the provided examples.

```processing
// Import this library and the processing.net library that it's built on
import jediahkatz.gameserver.*;
import processing.net.*;

// Instantiate your client
GameClient client = new GameClient(this, "127.0.0.1", 4321);

void setup() {
	// Automatically join an empty room, or if there are none
	// then create and join a room with capacity of 5.
	client.autojoinRoom(5);
	
	// Each client and room has a unique id.
	int clientId = client.id();
	int roomId = client.roomId();
	
	// We can request a RoomInfo object containing info about a room.
	RoomInfo myRoomInfo = client.getRoomInfo(roomId);
	// Just some of the information we can retrieve...
	int size = myRoomInfo.size();
	int capacity = myRoomInfo.capacity();
	int[] clientIds = myRoomInfo.clients();
	
	// We can also get info about every room that exists.
	RoomInfo[] allRoomsInfo = client.getRoomsInfo();
	RoomInfo someRoom = allRoomsInfo[0];
	
	// We can leave our room and join another.
	client.leaveRoom();
	client.joinRoom(someRoom.id());
	
	// Let's disconnect the client before the sketch ends.
	// This isn't really necessary; it will happen automatically.
	client.disconnect();
}
```

### Messages and attributes: JSON-based features

The GameServer library has a robust system for sending messages to other clients.
It's based on [Processing's JSONObject](https://processing.org/reference/JSONObject.html), an object that stores key-value mappings.
Messages are sent as JSONObjects, not strings.

We can create a JSONObject as follows:
```processing
// Construct an empty JSONObject
JSONObject data = new JSONObject();
// We can add key-value pairs, where keys are strings
// and values can be a variety of types.
data.setInt("myInt", 5);
data.setFloat("e", 2.81);
data.setString("message", "hello world");
data.setBoolean("myBool", true);
// We can also retrieve values by their keys.
int myInt = data.getInt("myInt");
// If we don't know the type of a value, we can
// retrieve it as a generic object.
Object myThing = data.get("message");
/**
Our JSONObject looks like this:
{
 "myInt": 5,
 "e": 2.81,
 "message": "hello world",
 "myBool": true,
}
**/
```

We can set many different types as a JSONObject value, including
other JSONObjects, and JSONArrays (arrays of JSONObjects). Read
the [JSONObject documentation](https://processing.org/reference/JSONObject.html) for an overview.

Once we have a JSONObject, we can send it to other clients.
```processing
// We can send our object to a specific client...
int clientId = 5;
client.sendMessage(5, data);
// Or to a list of clients...
int[] clientIds = {1, 2, 3};
client.sendMessage(clientIds, data);
// Or to every client in the same room as us.
client.broadcastMessage(data);
```

We can also receive messages from other clients. Messages will be wrapped
in a Message object, which contains the id of the sender and the message body.

```processing
// Get the first unread message
Message first = client.getNextMessage();
// Get all the unread messages
Message[] all = client.getMessages();
// We can get the sender's id, and there are convenient
// methods to retrieve data values from keys directly.
if (first != null) {
	int senderId = first.getSenderId();
	JSONObject data = first.getBody();
	// These two lines are equivalent:
	int myInt = data.getInt('myInt');
	int myInt2 = first.getInt('myInt');
}
```

Finally, JSONObjects are also the backbone of a powerful feature called attributes.
Attributes are just JSONObjects that can be attached to a room (or the entire server),
and they can be retrieved by all clients.

```processing
// We can set all the attributes at once with a JSONObject
int roomId = client.roomId();
JSONObject data = new JSONObject();
data.setString("message", "hello world");
client.setRoomAttributes(roomId, data);
client.setServerAttributes(data);
// Or we can set them individually
client.putRoomAttribute(roomId, "e", 2.81);
// We can access server attributes directly, and room attributes from the RoomInfo
JSONObject serverAttr = client.getServerAttributes();
RoomInfo myRoomInfo = client.getRoomInfo(roomId);
JSONObject roomAttr = myRoomInfo.attributes();
```

### Connecting over a network

#### On the localhost

The simplest way to connect over the internet is on one computer (which is not that useful).
To do this, just connect to your local IP ("127.0.0.1") over any unused port.

```processing
// In BasicServer.pde
GameServer server = new GameServer(this, 4321);
// In your client
GameClient client = new GameClient(this, "127.0.0.1", 4321);
```

#### On a local network

To connect over your local network, you'll need to do some port forwarding.
Log in to your router settings page (usually at address 192.168.0.1) and find 
the section called Port Forwarding. Assuming you use port 4321, forward the inbound
port 4321 to the local port 4321 on your computer's private IP address.

#### Over the internet

Instructions coming soon!

### What's next

* Add `putServerAttribute` methods
* No longer require importing `processing.net.*` (not sure if this is possible)