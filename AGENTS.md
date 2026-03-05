## Cursor Cloud specific instructions

### Project overview
GameServer is a Java library for Processing that provides multiplayer game server/client networking. It uses Apache Ant for builds and depends on Processing's `core.jar` and `net.jar`.

### Build system
- **Build tool:** Apache Ant (`ant -f resources/build.xml library.run`)
- **Java target:** 1.8 (compiles with warnings on newer JDKs, which is expected)
- **Dependencies:** Processing `core.jar` and `net.jar` must be at `~/eclipse-workspace/libs/` (matches `classpath.local.location` in `resources/build.properties`)
- The Ant build expects `lib/` and `data/` directories to exist at the repo root (they are gitignored)

### Building
Run `ant -f resources/build.xml library.run` from the repo root. This compiles the source, generates the JAR at `~/Documents/Processing/libraries/GameServer/library/GameServer.jar`, and creates a distribution zip under `distribution/`.

### Running tests
There are no automated test suites in the repo. To verify the library works, compile and run a Java program that creates a `GameServer` on a `PApplet`, then connects a `GameClient` from another `PApplet`. Both require named (non-anonymous) PApplet subclasses because Processing uses reflection to call `serverEvent`/`disconnectEvent` and anonymous classes cause `IllegalAccessException`.

### Gotchas
- The `compile` Ant target alone will fail because it expects sources in `tmp/GameServer/src/` (created by `generate.structure`). Always use `library.run` for a full build.
- The javadoc step produces a non-fatal error about `.classpath` files in the src directory; it does not block the build.
- Processing's `Server` class uses reflection to invoke `serverEvent(Server, Client)` on the parent PApplet. If using anonymous PApplet subclasses, this reflection fails silently (prints "Disabling serverEvent()") and clients never get registered.
