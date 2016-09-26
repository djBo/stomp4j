package org.stomp4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * StompFrame Class
 * 
 * <p>This class is the core of Stomp communication.
 * 
 * <p>The static class methods {@code readFrame} and {@code writeFrame} provide the core 
 * of the Stomp communication protocol.
 * 
 * <a name="label_spec"><h3>Stomp Specification 1.1</h3></a>
 * The {@code readFrame} method provides a minimal effort implementation to access frames
 * from an {@link InputStream} using {@link BufferedReader#readLine()}. Validation is performed
 * on reading commands. Headers are parsed using {@link String#split(String regex)}, require a
 * colon, and allows colons in header values (as ActiveMQ actually does with it's session-id header).
 * 
 * <p>The {@code writeFrame} method throws a {@link ProtocolException} for invalid frame commands.
 * The remainder of the frame is not validated for illegal content (headers and payload) and is
 * simply written to the {@link OutputStream} using a {@link BufferedWriter} as-is.
 * 
 * @author Rory Slegtenhorst <rory.slegtenhorst@gmail.com>
 */
class StompFrame implements Stomp {

    private final String mCommand;
    private final Map<String, String> mHeaders;
    private final byte[] mPayload;

    public StompFrame(String command) {
        this(command, null);
    }

    public StompFrame(String command, byte[] payload) {
        this(command, payload, new LinkedHashMap<String, String>());
    }

    public StompFrame(String command, byte[] payload, Map<String, String> headers) {
        mCommand = command;
        mPayload = payload;
        mHeaders = headers;
    }

    public void addHeader(String name, String value) {
    	if (!mHeaders.containsKey(name))
    		mHeaders.put(name, value);
    }

    public String getCommand() {
        return mCommand;
    }

    public String getContentType() {
        return mHeaders.containsKey(HEADER_CONTENT_TYPE) ? mHeaders.get(HEADER_CONTENT_TYPE) : null;
    }

    public byte[] getPayload() {
        return mPayload;
    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.putAll(mHeaders);
        return headers;
    }

    @Override
    public String toString() {
        String result = "StompFrame command: " + mCommand + ", ";
        for (Map.Entry<String, String> header : mHeaders.entrySet())
            result += header.getKey() + ":" + header.getValue() + ", ";
        result += "length: " + (mPayload == null ? -1 : mPayload.length);
        return result;
    }



}
