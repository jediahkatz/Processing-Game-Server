/* In this version of rock paper scissors, we use room attributes
 * to keep track of the game state (i.e., the players' choices). 
 * One upside of this method is that game state is stored on the
 * server, so as long as the server remains running, information
 * can always be retrieved from there even if a client disconnects. */
import jediahkatz.gameserver.*;
import processing.net.*;

GameClient client = new GameClient(this, "127.0.0.1", 4321);
boolean playing = false;
boolean chosen = false;
int opponentId;
String message;
int time = -1;

void setup() {
  size(300, 300);
  textAlign(CENTER);
  // Autojoin a room with capacity of 2
  client.autojoinRoom(2);
  message = "Waiting for an opponent...";
  reset();
}

void draw() {
  background(255);
  RoomInfo info = client.getRoomInfo(client.roomId());
  boolean hasOpponent = info.clients().length == 2;
  if (!playing && hasOpponent) {
    // Start the game
    playing = true;
    // Which of the two clients in the room isn't me?
    int[] clients = info.clients();
    if (clients[0] != client.id()) {
      opponentId = clients[0];
    } else {
      opponentId = clients[1];
    }
  } else if (playing && !hasOpponent) {
    message = "Opponent disconnected. Waiting for another...";
    playing = false;
    reset();
  } else if (playing) {
    // Get our move and opponent's move from the server
    String oppChoice = info.attributes().getString(str(opponentId));
    String choice = info.attributes().getString(str(client.id()));
    if (choice != null) {
      message = "You chose " + choice + "!";
      if (oppChoice != null) {
        // If the game is over, count down from 5 and restart
        if (time == -1) {
          time = second() + 5;
        }
        int timeLeft = (time - second()) % 60;
        if (timeLeft <= 0) {
          reset();
        } else {
          message += " Your opponent chose " + oppChoice + "!";
          message += "\n" + result(choice, oppChoice);
          message += " New game in " + timeLeft + "...";
        }
      }
    } else {
      message = "Make your choice!";
    }
  }
  fill(0);
  text(message, 150, 30);
  drawButtons();
}

void mouseClicked(MouseEvent e) {
  // Update your move in the room attributes when a button is clicked
  int x = e.getX();
  int y = e.getY();
  String choice = null;
  if (playing && !chosen) {
    if (dist(x, y, 75, 150) < 40) {
      choice = "rock";
    } else if (dist(x, y, 150, 150) < 40) {
      choice = "paper";
    } else if (dist(x, y, 225, 150) < 40) {
      choice = "scissors"; 
    }
    
    // We're going to use the client's id as the attribute key
    if (choice != null) {
      client.putRoomAttribute(client.roomId(), str(client.id()), choice);
      chosen = true;
    }
  }
}

String result(String myChoice, String oppChoice) {
  String win = "You won!";
  String lose = "You lost!";
  String tie = "You tied!";
  int me = choiceToNum(myChoice);
  int opp = choiceToNum(oppChoice);
  if (me == (opp + 1) % 3) return win;
  if (me == opp) return tie;
  return lose;
}

int choiceToNum(String choice) {
   switch (choice) {
     case "rock":
       return 0;
     case "paper":
       return 1;
     default:
       return 2;
   }
}

void drawButtons() {
  fill(0);
  text("Rock",75,100);
  text("Paper",150,100);
  text("Scissors",225,100);
  fill(255,0,0);
  ellipse(75,150,40,40);
  ellipse(150,150,40,40);
  ellipse(225,150,40,40);
}

void reset() {
  client.setRoomAttributes(client.roomId(), new JSONObject());
  time = -1;
  chosen = false;
}