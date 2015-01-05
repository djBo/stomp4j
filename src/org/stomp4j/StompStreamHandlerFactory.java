package org.stomp4j;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * StompStreamHandlerFactory Class
 * 
 * <p>Responsible for adding the {@link StompStreamHandler} to the system.
 * 
 * @author Rory Slegtenhorst <rory.slegtenhorst@gmail.com>
 */
public class StompStreamHandlerFactory implements URLStreamHandlerFactory, Stomp {

    private static final Object mInstanceLock = new Object();
    private static StompStreamHandlerFactory mInstance = null;

    private final Map<String, URLStreamHandler> mHandlers = new LinkedHashMap<String, URLStreamHandler>();

    public StompStreamHandlerFactory() {}

    public void addHandler(String protocol, URLStreamHandler urlHandler) {
        mHandlers.put(protocol, urlHandler);
    }

    public URLStreamHandler createURLStreamHandler(String protocol) {
        return mHandlers.get(protocol);
    }

    public static void register() {
        if (mInstance == null) {
            synchronized (mInstanceLock) {
            	URLStreamHandler streamHandler = new StompStreamHandler();
                mInstance = new StompStreamHandlerFactory();
                mInstance.addHandler(SCHEMA_STOMP, streamHandler);
                mInstance.addHandler(SCHEMA_STOMP_NIO, streamHandler);
                mInstance.addHandler(SCHEMA_STOMP_SSL, streamHandler);
                URL.setURLStreamHandlerFactory(mInstance);
            }
        }

    }
}