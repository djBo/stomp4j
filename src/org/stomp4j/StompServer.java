package org.stomp4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * StompServer Class
 * 
 * <p>Loopback Stomp Server implementation.
 * 
 * <p>This class implements a very limited loopback {@link StompConnection} that acts as it's own server.
 * There is no support for message storage, multiple subscribers to the same destination, sessions or transactions.
 * Any command not listed below is logged, but ignored. 
 * 
 * <a name="cmd_command"><h3>CONNECT</h3></a>
 * The server responds with a correctly formatted {@code CONNECTED} response. The headers are fixed and useless for state.
 * 
 * <a name="cmd_subscribe"><h3>SUBSCRIBE</h3></a>
 * The server registers your request using the supplied id and destination. The server performs limited header 
 * validation, and will respond with appropiate error frames. Note that a subscription to an existing destination
 * will overwrite any previous registered id! Also note that a subscription to an existing id might blow things up. 
 * 
 * <a name="cmd_unsubscribe"><h3>UNSUBSCRIBE</h3></a>
 * The server removes your your previously registered destination for this id. Note the fact that the server uses a
 * simple double linked list for id <-> destination mappings. See the notes above.
 * 
 * <a name="cmd_send"><h3>SEND</h3></a>
 * If the destination was previously used in a {@code SUBSCRIBE} command, the server will respond with a {@code MESSAGE}
 * frame containing a "unique" message id and the registered subscriber id. In any other case, the message is logged and ignored.
 * 
 * 
 * @author Rory Slegtenhorst <rory.slegtenhorst@gmail.com>
 */
public class StompServer extends StompConnection {

    private final IOStream io = new IOStream();
    private final Map<String, String> mSubscribers = new LinkedHashMap<String, String>();
    private final Map<String, String> mSubscriberIds = new LinkedHashMap<String, String>();
    private Integer mLastMessageId = 0;

    @Override
    protected InputStream getInput() throws IOException {
        return io.getInputStream();
    }

    @Override
    protected OutputStream getOutput() throws IOException {
        return io.getOutputStream();
    }

    @Override
    protected void handleServerMessage(StompFrame frame) throws IOException {
        final String cmd = frame.getCommand();

        
        StompFrame result = null;
        if (CONNECT.equals(cmd)) {
            // Return CONNECTED frame
        	result = new StompFrame(CONNECTED);
        	result.addHeader(HEADER_SESSION, "session-loopback");
        	result.addHeader(HEADER_HEARTBEAT, VALUE_HEARTBEAT);
        	result.addHeader(HEADER_SERVER, "server-loopback");
        	result.addHeader(HEADER_VERSION, VALUE_ACCEPT_VERSION);
        } else
        if (SEND.equals(cmd)) {
        	final String destination = frame.getHeaders().get(HEADER_DESTINATION);
    		++mLastMessageId;
        	if (mSubscribers.containsKey(destination)) {
        		// Return MESSAGE frame
        		result = new StompFrame(MESSAGE, frame.getPayload());
        		result.addHeader(HEADER_SUBSCRIPTION, mSubscribers.get(destination));
        		result.addHeader(HEADER_MESSAGE_ID, mLastMessageId.toString());
        		for (Map.Entry<String, String> entry : frame.getHeaders().entrySet()) {
        			result.addHeader(entry.getKey(), entry.getValue());
        		}
        	}
        	if (destination == null) {
        		// Send error frame indicating there is no destination
        		result = newErrorFrame(MSG_HEADER_REQ, "Required header '" + HEADER_DESTINATION + "' missing");
        	}
        } else
        if (SUBSCRIBE.equals(cmd)) {
        	//id:0
        	//destination:/queue/foo
        	//ack:client
        	
        	final String id = frame.getHeaders().get(HEADER_ID);
        	if (id != null) {
        		final String destination = frame.getHeaders().get(HEADER_DESTINATION);
        		if (destination != null) {
        			mSubscribers.put(destination, id);
        			mSubscriberIds.put(id, destination);
        		} else result = newErrorFrame(MSG_HEADER_REQ, "Required header '" + HEADER_DESTINATION + "' missing");
        	} else result = newErrorFrame(MSG_HEADER_REQ, "Required header '" + HEADER_ID + "' missing");
        	
        	
        } else
        if (UNSUBSCRIBE.equals(cmd)) {
        	final String id = frame.getHeaders().get(HEADER_ID);
        	if (id != null) {
        		if (mSubscriberIds.containsKey(id)) {
        			final String destination = mSubscriberIds.get(id);
        			mSubscribers.remove(destination);
        			mSubscriberIds.remove(id);
        		} else result = newErrorFrame(MSG_INVALID_VALUE, MSG_INVALID_VALUE + " for '" + HEADER_ID + "'");
        	} else result = newErrorFrame(MSG_HEADER_REQ, "Required header '" + HEADER_ID + "' missing");
        } else
        if (ACK.equals(cmd)) {
            // Clear last MESSAGE
        } else
        if (NACK.equals(cmd)) {
            // Resend last message
        } else
        if (SUBSCRIBE.equals(cmd) || UNSUBSCRIBE.equals(cmd) || BEGIN.equals(cmd) || COMMIT.equals(cmd) || ABORT.equals(cmd) || DISCONNECT.equals(cmd)) {
            // For now, do nothing
        }
        if (result != null) StompIO.writeFrame(result, getOutput());

    }
    
    private StompFrame newErrorFrame(String message, String body) {
    	StompFrame result = new StompFrame(ERROR, body.getBytes());
    	result.addHeader(HEADER_MESSAGE, message);
    	result.addHeader(HEADER_CONTENT_TYPE, VALUE_CONTENT_TYPE);
    	return result;
    }
}
