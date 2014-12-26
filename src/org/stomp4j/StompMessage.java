package org.stomp4j;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author Rory Slegtenhorst <rory.slegtenhorst@gmail.com>
 *
 */
public class StompMessage implements Stomp {

    private static final String CHARSET_MATCH = ";" + HEADER_CHARSET + "=";

    private final String mDestination;
    private final String mBody;
    private final String mContentType;
    private final Charset mCharset;
    private final Integer mContentLength;
    private final byte[] mPayload;
    private final Map<String, String> mHeaders = new LinkedHashMap<String, String>();

    private Boolean mPersistent = false;
    private Boolean mIsBinary = true;

    public StompMessage(String destination, String message, String type) {
        if (DEBUG) System.out.println("StompMessage.constructor +");
        try {
            if (DEBUG) System.out.println("StompMessage.constructor destination: " + destination + ", message: " + message + ", type: " + type);
            mDestination = destination;
            mBody = message;
            mContentType = type;
            mCharset = Charset.defaultCharset();
            mPayload = message.getBytes(mCharset);
            mContentLength = mPayload.length;
            mIsBinary = false;
            if (DEBUG) System.out.println("StompMessage.constructor payload: " + mContentLength + " (" + mBody.length() + ")");
        } finally {
            if (DEBUG) System.out.println("StompMessage.constructor -");
        }
    }

    public StompMessage(String destination, byte[] payload, String type, boolean text) {
        if (DEBUG) System.out.println("StompMessage.constructor +");
        try {
            if (DEBUG) System.out.println("StompMessage.constructor destination: " + destination + ", type: " + type + ", payload: " + (payload == null ? "null" : "byte[" + payload.length + "]") + ", text: " + text);
            mDestination = destination;
            mContentType = type;
            mCharset = text ? guessCharset(mContentType) : Charset.defaultCharset();
            mPayload = payload;
            mContentLength = mPayload.length;
            mIsBinary = !text;
            mBody = mIsBinary ? null : new String(mPayload, mCharset);
            if (DEBUG) if (mBody != null) System.out.println("StompMessage.constructor body: " + mBody.length());
        } finally {
            if (DEBUG) System.out.println("StompMessage.constructor -");
        }
    }

    private Charset guessCharset(String type) {
        String charset = VALUE_CHARSET;
        if (type != null && type.contains(CHARSET_MATCH)) {
            charset = type.substring(type.indexOf(CHARSET_MATCH) + CHARSET_MATCH.length());
        }

        if (Charset.isSupported(charset)) {
            return Charset.forName(charset);
        }
        return Charset.defaultCharset();
    }

    public void addHeader(String name, String value) {
        if (HEADER_DESTINATION.equalsIgnoreCase(name)) return;     // Delivered as parameter
        if (HEADER_CONTENT_TYPE.equalsIgnoreCase(name)) return;    // Delivered as parameter
        if (HEADER_CONTENT_LENGTH.equalsIgnoreCase(name)) return;  // Calculated when needed
        if (HEADER_PERSISTENT.equalsIgnoreCase(name)) {
        	if (VALUE_PERSISTENT.equalsIgnoreCase(value)) persistent();
        	return;
        }
        mHeaders.put(name, value);
    }

    public String getDestination() {
        return mDestination;
    }

    public String getBody() {
        return mBody;
    }

    public String getContentType() {
        return mContentType;
    }

    //TODO:Make this string?
    public Charset getCharset() {
        //mCharset.name();
        return mCharset;
    }

    public Integer getContentLength() {
        return mContentLength;
    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.putAll(mHeaders);
        return headers;
    }

    public byte[] getPayload() {
        return mPayload;
    }

    public Boolean isBinary() {
        return mIsBinary;
    }

    public Boolean isPersistent() {
    	return mPersistent;
    }

    public Boolean isText() {
        return !mIsBinary;
    }

    public StompMessage persistent() {
    	mPersistent = true;
    	return this;
    }
    
    @Override
    public String toString() {

        String result = "StompMessage type: " + mContentType + ", length: " + mContentLength + ", charset: " + mCharset.displayName() + "\n";
        for (Map.Entry<String, String> header : mHeaders.entrySet())
            result += header.getKey() + ":" + header.getValue() + "\n";

        if (mIsBinary) result += "payload: binary data " + mPayload.length;
        else result += "payload: " + mBody;

        return result;
    }
}
