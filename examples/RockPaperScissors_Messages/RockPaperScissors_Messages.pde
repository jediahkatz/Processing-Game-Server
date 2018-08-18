import jediahkatz.gameserver.*;
import processing.net.*;

GameClient client = new GameClient(this, "127.0.0.1", 4321);
boolean playing = false;
String message;
String choice = null;
String oppChoice = null;
int time = -1;

void setup() {
  size(300, 300);
  textAlign(CENTER);
  // Autojoin a room with capacity of 2
  client.autojoinRoom(2);
  message = "Waiting for an opponent...";
}

void draw() {
  background(255);
  RoomInfo info = client.getRoomInfo(client.roomId());
  boolean hasOpponent = info.clients().length == 2;
  if (!playing && hasOpponent) {
    // Start the game
    playing = true;
  } else if (playing && !hasOpponent) {
    message = "Opponent disconnected. Waiting for another...";
    playing = false;
    reset();
  } else if (playing) {
    // Get opponent's move if possible
    Message msg = client.getNextMessage();
    if (msg != null && msg.getSenderId() != client.id()) {
      oppChoice = msg.getString("choice"); 
    }
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
  // Broadcast your move when a button is clicked
  int x = e.getX();
  int y = e.getY();
  if (playing && choice == null) {
    if (dist(x, y, 75, 150) < 40) {
      choice = "rock";
    } else if (dist(x, y, 150, 150) < 40) {
      choice = "paper";
    } else if (dist(x, y, 225, 150) < 40) {
      choice = "scissors"; 
    }
    JSONObject data = new JSONObject();
    data.setString("choice", choice);
    client.broadcastMessage(data);
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
  choice = oppChoice = null;
  time = -1;
}