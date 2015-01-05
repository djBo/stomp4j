package org.stomp4j;

/**
 * StompListener Interface
 * 
 * <p>Needs to be implemented by classes wanting to receive messages when calling {@link StompConnection#subscribe(String destination, StompListener... listeners)}
 * 
 * @author Rory Slegtenhorst <rory.slegtenhorst@gmail.com>
 */
public interface StompListener extends Stomp {

    public boolean onMessage(StompMessage message);

}
