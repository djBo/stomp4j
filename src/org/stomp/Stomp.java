package org.stomp;

/**
 * This interface for the Stomp protocol contains all the static
 * and non-translatable strings to use in the protocol.
 * 
 * @author Rory Slegtenhorst <rory.slegtenhorst@gmail.com>
 * 
 */
interface Stomp {

    static final Boolean DEBUG = true;
    static final Boolean VERBOSE = false;

    static final int DEFAULT_PORT = 61613;
    
    static final String SCHEMA_STOMP = "stomp";
    static final String SCHEMA_STOMP_NIO = "stomp+nio";
    static final String SCHEMA_STOMP_SSL = "stomp+ssl";
    
    static final String CONNECT = "CONNECT";
    static final String SEND = "SEND";
    static final String SUBSCRIBE = "SUBSCRIBE";
    static final String UNSUBSCRIBE = "UNSUBSCRIBE";
    static final String BEGIN = "BEGIN";
    static final String COMMIT = "COMMIT";
    static final String ABORT = "ABORT";
    static final String ACK = "ACK";
    static final String NACK = "NACK";
    static final String DISCONNECT = "DISCONNECT";
    static final String HEARTBEAT = "HEARTBEAT";

    public static final String[] COMMANDS_CLIENT = new String[] {CONNECT, SEND, SUBSCRIBE, UNSUBSCRIBE, BEGIN, COMMIT, ABORT, ACK, NACK, DISCONNECT};
    
    static final String CONNECTED = "CONNECTED";
    static final String MESSAGE = "MESSAGE";
    static final String RECEIPT = "RECEIPT";
    static final String ERROR = "ERROR";

    public static final String[] COMMANDS_SERVER = new String[] {CONNECTED, MESSAGE, RECEIPT, ERROR};
    
    static final String HEADER_ACCEPT_VERSION = "accept-version";
    static final String HEADER_HOST = "host";
    static final String HEADER_USERNAME = "login";
    static final String HEADER_PASSWORD = "passcode";
    static final String HEADER_ID = "id";
    static final String HEADER_DESTINATION = "destination";
    static final String HEADER_SUBSCRIPTION = "subscription";
    static final String HEADER_ACK = "ack";
    static final String HEADER_MESSAGE_ID = "message-id";
    static final String HEADER_CONTENT_TYPE = "content-type";
    static final String HEADER_CONTENT_LENGTH = "content-length";
    static final String HEADER_TRANSACTION = "transaction";
    static final String HEADER_RECEIPT = "receipt-id";
    static final String HEADER_CHARSET = "charset";
    static final String HEADER_HEARTBEAT = "heart-beat";
    static final String HEADER_PERSISTENT = "persistent";
    static final String HEADER_SESSION = "session";
    static final String HEADER_SERVER = "server";
    static final String HEADER_VERSION = "version";
    static final String HEADER_MESSAGE = "message";

    static final String VALUE_ACCEPT_VERSION = "1.1";
    static final String VALUE_ACK = "client";
    static final String VALUE_CONTENT_TYPE = "text/plain";
    static final String VALUE_CHARSET = "UTF-8";
    static final String VALUE_PERSISTENT = "true";
    static final String VALUE_HEARTBEAT = "0,0";

    static final String MSG_NOT_CONNECTED = "Not connected";
    static final String MSG_ALREADY_CONNECTED = "Already connected";
    static final String MSG_SOCKET_CLOSED = "Socket closed";
    static final String MSG_SOCKET_ERROR = "Socket error";
    static final String MSG_INVALID_AUTH = "Invalid authentication";
    static final String MSG_HEADER_REQ = "Required header missing";
    static final String MSG_INVALID_VALUE = "Invalid value";
    static final String MSG_INVALID_COMMAND = "Invalid command";

}