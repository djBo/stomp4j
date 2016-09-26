package org.stomp4j;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * StompStreamHandler Class
 * 
 * <p>Used in conjunction with {@link StompStreamHandlerFactory}.
 * 
 * @author Rory Slegtenhorst <rory.slegtenhorst@gmail.com>
 */
class StompStreamHandler extends URLStreamHandler implements Stomp {

    public StompStreamHandler() {}

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new StompConnection(url);
    }

	@Override
	protected int getDefaultPort() {
		return DEFAULT_PORT;
	}

}