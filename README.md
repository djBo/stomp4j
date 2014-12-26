# stomp4j

STOMP is the **S**imple (or Streaming) **T**ext **O**rientated **M**essaging **P**rotocol.
See [here][stomp]  for more details.

This implementation for Java, uses the [URLConnnection] class mechanism for creating new connections.

###Usage

Connections are made using the following url:

    stomp://[<username>:<password>]@hostname[:port]/

Example code to register the stomp protocol handler, required once at application launch:

    StompStreamHandlerFactory.register();

Example code for making a connection:

    URL url = new URL("http://myhost.org:61613/");
    StompConnection con = (StompConnection) url.openConnection();
    con.setConnectTimeout(1000);
    con.setEventListener(new StompConnection.Listener() {...});
    con.connect();

Example code for subscribing:

    con.subscribe("/queue/test", new StompListener() {
        @Override
        public boolean onMessage(StompMessage message) {
            /* do your thing */
            return true;
        }
    });

Example code for running the connection in a thread:

    ...
    while (!interrupted) {
        try {
            synchronized(con) {
                con.wait();
            }
            // The connection got closed
            ...
        } catch (InterruptedException e) {
            // You want to stop
            interrupted = true;
        }
    }

###API
 - Stomp
   - Package private interface
   - Contains all strings used in the implementation
 - StompConnection
   - Public class
   - Handles the actual socket connection
   - Starts a separate thread to perform socket reads
 - StompConnection.Listener
   - Public interface
   - Used for relaying connect events
 - StompConnection.StompListenerThread
   - Class private Runnable implementation
 - StompFrame
   - Package private class
   - Common protocol communication object
 - StompListener
   - Public interface
   - Used for relaying incoming messages
 - StompMessage
   - Public class
   - Handles possible text character sets
 - StompStreamHandler
   - Package private class
   - The glue that makes URL.openConnection work
 - StompStreamHandlerFactory
   - Public class
   -- Registers the stomp protocol during runtime
   

###Testing
At the moment, only [ActiveMQ] and [RabbitMQ] are used to test the functionality of this implementation.
Unfortunately, ActiveMQ hasn't implemented the protocol as per the specification. This has caused
some headache when trying to send/receive binary messages. Thanks to RabbitMQ, the code was adjusted to
send received messages based on subscriber id (and not destination).

###Version

0.1

###Todo's

 - Write Tests
 - Add Code Comments
 - Implement heart-beats (*)
   - Currently, only client->server heart-beats are supported
 - Add SSL support
 - Allow to send a message StompConnection using a loopback to aid in debugging/testing (*)
   - This allows the client to send messages to itself without a server present
 - Send DISCONNECT frame (this probably wont happen)
 - Add Debugging statements
 - Fix binary messages in combination with ActiveMQ
 - Make StompFrame implement Serializable, using it's own read/write methods? (this probably wont happen)
   - This would move the actual frame reading/writing back to StompFrame
   - This will make implementing heart-beats etc. a lot harder

**\*** Work-in-progress

License
----

MIT

**Free Software, Hell Yeah!**

[stomp]:http://stomp.github.io/
[URLConnnection]:https://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html
[ActiveMQ]:http://activemq.apache.org/
[RabbitMQ]:http://www.rabbitmq.com/
