/** A simple chatroom with usernames. Note that the usernames
    are not stored on the server; we could have chosen to do so
    in order to prevent users from picking the same name, for example. **/
import jediahkatz.gameserver.*;
import processing.net.*;

GameClient client = new GameClient(this, "127.0.0.1", 4321);
String message = "";
String username = "";
boolean nameSet = false;

void setup() {
  size(400, 200);
  fill(0);
  // Join or create one big room
  client.autojoinRoom(Integer.MAX_VALUE);
}

void draw() {
  background(255);
  if (!nameSet) {
    text("Choose a username and press ENTER to start chatting.", 20, 70);
    text("Username: " + username, 20, 100);
  } else {
    text("Your username is: " + username, 20, 50);
    text("Type your message and press ENTER to send it.", 20, 70);
    text("Message: " + message, 20, 100);
  }
  for (Message m : client.getMessages()) {
    println(m.getString("username") + ": " + m.getString("message"));
  }
}

void keyPressed() {
  if (key == BACKSPACE) {
    // Delete last character
    if (!nameSet && username.length() > 0) {
      username = username.substring(0, username.length()-1);
    } else if (message.length() > 0) {
      message = message.substring(0, message.length()-1);
    }
  } else if (key == ENTER) {
    // Set username if we haven't yet
    if (!nameSet) {
      nameSet = true;
    } else {
      // Send message to room
      JSONObject msg = new JSONObject();
      msg.setString("message", message);
      msg.setString("username", username);
      client.broadcastMessage(msg);
      // Reset message text
      message = "";
    }
  } else if (key != CODED) {
    // Add character
    if (!nameSet) {
      username += key;
    } else {
      message += key;
    }
  }
}