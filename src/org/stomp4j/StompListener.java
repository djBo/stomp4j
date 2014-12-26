package org.stomp4j;

/**
 * 
 * @author Rory Slegtenhorst <rory.slegtenhorst@gmail.com>
 *
 */
public interface StompListener extends Stomp {

    public boolean onMessage(StompMessage message);

}
