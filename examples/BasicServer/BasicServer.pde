import jediahkatz.gameserver.*;
import processing.net.*;

GameServer server = new GameServer(this, 4321);

void setup() {}
void draw() {}

void serverEvent(Server s, Client c) {
  server.serverEvent(s, c);
}

void disconnectEvent(Client c) {
  server.disconnectEvent(c);
}