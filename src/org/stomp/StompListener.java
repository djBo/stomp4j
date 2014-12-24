package org.stomp;

/**
 * 
 * @author Rory Slegtenhorst <rory.slegtenhorst@gmail.com>
 *
 */
public interface StompListener extends Stomp {

    public boolean onMessage(StompMessage message);

}
