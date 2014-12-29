package org.stomp4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ProtocolException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class contains the {@code readFrame} and {@code writeFrame} methods using {@link InputStream} or
 * {@link OutputStream} respectively. Also contained, is a {@link Listener} interface for more fine-tuned control during the read and write methods.
 * 
 * Last but not least are the frameToMessage and messageToFrame conversion routines that serve as the
 * glue between the api and the code using it.
 * 
 * @author Rory Slegtenhorst <rory.slegtenhorst@gmail.com>
 *
 */
class StompIO implements Stomp {

	private StompIO() {}
	
    private static boolean isClientCommand(String command) {
    	return Arrays.asList(COMMANDS_CLIENT).contains(command);
    }

    private static boolean isServerCommand(String command) {
    	return Arrays.asList(COMMANDS_SERVER).contains(command);
    }
	
    /**
     * Read a frame from the input stream.
     * @param input
     * @return StompFrame
     * @throws java.io.IOException
     */
    static StompFrame readFrame(InputStream input) throws IOException {
    	return readFrame(input, null);
    }

    static StompFrame readFrame(InputStream input, Listener listener) throws IOException {
        if (DEBUG) System.out.println("StompConnection.readFrame +");
        boolean isFinalized = false;
        try {
        	if (listener != null) listener.onReadBegin();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String command = "";
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            	if ("".equals(line)) {
            		//TODO:Update heart-beat mechanism here
            		if (DEBUG) System.out.println("StompConnection.readFrame Skip empty line");
            		if (listener != null) listener.onReadEmpty();
            	} else {
            		if (DEBUG) System.out.println("StompConnection.readFrame Command: " + line);

            		if (isServerCommand(line)) {
            			command = line;
            			break;
            		} else if (isClientCommand(line)) {
            			if (DEBUG && VERBOSE) System.out.println("StompConnection.readFrame WARNING: READ CLIENT COMMAND");
            			command = line;
            			break;
            		} else {
            			if (DEBUG) System.out.println("StompConnection.readFrame Skipping invalid command");
            		}
            	}
            }

            final Map<String, String> headers = new LinkedHashMap<String, String>();

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            	if (line.equals("")) {
            		if (DEBUG) System.out.println("StompConnection.readFrame Break on empty line");
            		break;
            	}
            	if (DEBUG) System.out.println("StompConnection.readFrame Header: " + line);
            	int pos = line.indexOf(":");
            	if (pos != -1) {
            		if (DEBUG && VERBOSE) System.out.println("StompConnection.readFrame Found :");
            		String header = line.substring(0, pos);
            		String value = line.substring(pos + 1);
            		if (DEBUG && VERBOSE) System.out.println("StompConnection.readFrame Posting header: " + header + ", value: " + value);
            		headers.put(header, value);
            	} else {
            		if (DEBUG && VERBOSE) System.out.println("StompConnection.readFrame Could not find :");
            	}
            }
            
            if (headers.containsKey(HEADER_CONTENT_LENGTH)) {
                if (DEBUG) System.out.println("StompConnection.readFrame Found content-length");

                String sLength = headers.get(HEADER_CONTENT_LENGTH);
                if (DEBUG) System.out.println("StompConnection.readFrame Length: " + sLength);

                Integer length = Integer.parseInt(sLength);
                byte[] payload = new byte[length];
                for (int pos = 0; pos <= length; pos++) {
                	if (pos < length)
                		payload[pos] = (byte) reader.read();
                	else isFinalized = reader.read() == 0; // Any "LINE FEED" character should be picked up by the next StompFrame.read
                }
                if (DEBUG) System.out.println("StompConnection.readFrame Length: " + sLength);
                return new StompFrame(command, payload, headers);
            } else {
                if (DEBUG) System.out.println("StompConnection.readFrame Could not find content-length");

                ByteArrayOutputStream payload = new ByteArrayOutputStream(0);
                for (byte value = (byte) reader.read(); value > 0; value = (byte) reader.read()) { // > 0 catches both EoF and the 0 char
                	payload.write(value);
                }
                isFinalized = true;
                return new StompFrame(command, payload.toByteArray(), headers);
            }
        } finally {
        	if (listener != null) listener.onReadEnd(isFinalized);
            if (DEBUG) System.out.println("StompConnection.readFrame - " + isFinalized);
        }
    }

    static void writeFrame(StompFrame frame, OutputStream output) throws IOException {
    	writeFrame(frame, output, null);
    }

    static void writeFrame(StompFrame frame, OutputStream output, Listener listener) throws IOException {
        if (DEBUG) System.out.println("StompConnection.writeFrame +");
        if ( ! ( isClientCommand(frame.getCommand()) || isServerCommand(frame.getCommand()) || HEARTBEAT.equals(frame.getCommand()) ) )
        	throw new ProtocolException(MSG_INVALID_COMMAND);
        try {
        	if (listener != null) listener.onWriteBegin();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
            try {
                if (DEBUG) System.out.println("StompConnection.writeFrame Command: " + frame.getCommand());
                if (HEARTBEAT.equals(frame.getCommand())) {
                	listener.onWriteEmpty();
                	writer.write(10);
                    return;
                }
                
                writer.write(frame.getCommand() + "\n");
                
                for (Map.Entry<String, String> header : frame.getHeaders().entrySet()) {
                	String value = header.getKey() + ":" + header.getValue();
                	if (DEBUG) System.out.println("StompConnection.writeFrame Header: " + value);
                	writer.write(value + "\n");
                }
                
                if (DEBUG && VERBOSE) System.out.println("StompConnection.writeFrame LF");
                writer.write("\n");
                if (frame.getPayload() != null) {
                	byte[] payload = frame.getPayload();
                	if (DEBUG) System.out.println("StompConnection.writeFrame Payload: " + payload.length);
                	for (int pos = 0; pos <= payload.length; pos++) {
                		if (pos < payload.length)
                			writer.write(payload[pos]);
                		else
                			writer.write(0);
                	}
                } else writer.write(0);
                if (DEBUG && VERBOSE) System.out.println("StompConnection.writeFrame extra LF");
                writer.write("\n");
            } finally {
                if (DEBUG && VERBOSE) System.out.println("StompConnection.writeFrame Flush");
                writer.flush();
            }
        } finally {
        	if (listener != null) listener.onWriteEnd();
            if (DEBUG) System.out.println("StompConnection.writeFrame -");
        }
    }	
	
    static StompMessage frameToMessage(StompFrame frame) {
        StompMessage message = new StompMessage(
                ERROR.equals(frame.getCommand()) ? null : frame.getHeaders().get(HEADER_DESTINATION),
                frame.getPayload(),
                frame.getContentType(),
                frame.getContentType() != null);
        for (Map.Entry<String, String> header : frame.getHeaders().entrySet())
            message.addHeader(header.getKey(), header.getValue());
        return message;
    }

    static StompFrame messageToFrame(StompMessage message) {
        StompFrame frame = new StompFrame(SEND, message.getPayload());
        frame.addHeader(HEADER_DESTINATION, message.getDestination());
        if (message.isPersistent())
        	frame.addHeader(HEADER_PERSISTENT, VALUE_PERSISTENT);
        if (message.getContentType() != null)
            frame.addHeader(HEADER_CONTENT_TYPE, message.getContentType());
        frame.addHeader(HEADER_CONTENT_LENGTH, "" + message.getContentLength());
        for (Map.Entry<String, String> header : message.getHeaders().entrySet())
            frame.addHeader(header.getKey(), header.getValue());
        return frame;
    }
	
    /**
     * Interface for readFrame/writeFrame.
     * 
     * <p>Contains methods that will be called while reading or writing a frame.
     *
     */
	interface Listener extends Stomp {
		void onReadBegin();
		void onReadEmpty();
		void onReadEnd(final boolean finalized);

		void onWriteBegin();
		void onWriteEmpty();
		void onWriteEnd();
	}
}
