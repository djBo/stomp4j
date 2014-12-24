package org.stomp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Rory Slegtenhorst <rory.slegtenhorst@gmail.com>
 *
 */
public class StompConnection extends URLConnection implements Stomp {

    private final SocketAddress mSocketAddress;
    private final Socket mSocket;

    private String mUsername = null;
    private String mPassword = null;
    private String mTransaction = null;

    private static int mHeartBeatRecvDelay = 0;
    private static int mHeartBeatSendDelay = 0;

    private Boolean mConnected = false;

    private Integer mLastSubscriptionId = 0;
    private final Map<String, Integer> mSubscriptionIds = new LinkedHashMap<String, Integer>();
    private final Map<Integer, String> mIdSubscriptions = new LinkedHashMap<Integer, String>();
    private final Map<String, List<StompListener>> mSubscriptions = new LinkedHashMap<String, List<StompListener>>();

    @SuppressWarnings("rawtypes")
    private static Future mFuture;
    private static final ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor(); // Heart-beat writer

    private StompListenerThread mListenerThread = null;
    private Listener mListener = null;

    protected StompConnection() {
        super(null);
        mSocketAddress = null;
        mSocket = null;
    }

    protected StompConnection(URL url) {
        super(url);
        int port = url.getPort();
        if (port == -1) port = url.getDefaultPort();
        mSocketAddress = new InetSocketAddress(url.getHost(), port);
        mSocket = new Socket();
    }

    public void abort() throws IOException {
        if (mSocket != null && !mSocket.isConnected()) throw new ConnectException(MSG_NOT_CONNECTED);
        if (mTransaction != null)
            sendAbort();
    }

    public void begin(String transaction) throws IOException {
        if (mSocket != null && !mSocket.isConnected()) throw new ConnectException(MSG_NOT_CONNECTED);
        if (mTransaction == null && transaction != null) {
            sendBegin(transaction);
        }
    }

    public void commit() throws IOException {
        if (mSocket != null && !mSocket.isConnected()) throw new ConnectException(MSG_NOT_CONNECTED);
        if (mTransaction != null)
            sendCommit();
    }

    @Override
    public void connect() throws IOException {
        if (mSocket != null && mSocket.isConnected()) throw new ConnectException(MSG_ALREADY_CONNECTED);

        if (mUsername == null && mPassword == null) {
            String userInfo = url.getUserInfo();
            if (userInfo != null) {
                int pos = userInfo.indexOf(":");
                if (pos == -1) throw new MalformedURLException(MSG_INVALID_AUTH);
                String user = userInfo.substring(0, pos);
                String pass = userInfo.substring(pos + 1);
                if (user == null || pass == null || user.equals("") || pass.equals(""))
                    throw new MalformedURLException(MSG_INVALID_AUTH);
                mUsername = user;
                mPassword = pass;
            }
        }

        if (mListener != null) mListener.onConnecting();

        if (mSocket != null) {
            try {
                mSocket.connect(mSocketAddress, getConnectTimeout());
            } catch (IOException e) {
                e.printStackTrace();
                throw e;
            }
        }
        sendConnect();
    }

    public void disconnect() throws IOException {
        if (mSocket != null && !mSocket.isConnected()) throw new ConnectException(MSG_NOT_CONNECTED);
        if (mListenerThread != null) {
            mListenerThread.interrupt();
            mListenerThread = null;
        }
        if (mSocket != null) mSocket.close();
    }

    public void send(StompMessage message) throws IOException {
        if (mSocket != null && !mSocket.isConnected()) throw new ConnectException(MSG_NOT_CONNECTED);
        if (message != null) {
            sendMessage(message);
        }
    }

    public void setCredentials(String username, String password) throws IOException {
        if (mSocket != null && mSocket.isConnected()) throw new ConnectException(MSG_ALREADY_CONNECTED);
        mUsername = username;
        mPassword = password;
    }

    public void setHeartBeat(int recvDelay, int sendDelay) throws IOException {
    	if (mSocket != null && mSocket.isConnected()) throw new ConnectException(MSG_ALREADY_CONNECTED);
    	if (recvDelay > 0) mHeartBeatRecvDelay = recvDelay;
        if (sendDelay > 0) mHeartBeatSendDelay = sendDelay;
    }

    public void setEventListener(Listener listener) {
        mListener = listener;
    }

    public void subscribe(String destination, StompListener... listeners) throws IOException {
        if (mSocket != null && !mSocket.isConnected()) throw new ConnectException(MSG_NOT_CONNECTED);
        if (destination != null && listeners != null && listeners.length > 0) {

            if (mSubscriptions.containsKey(destination)) {
                // Existing subscription, add listeners to the list
                for (StompListener listener : listeners)
                    mSubscriptions.get(destination).add(listener);
            } else {
                List<StompListener> list = new Vector<StompListener>(listeners.length);
                for (StompListener listener : listeners)
                    list.add(listener);
                final Integer id = getUniqueSubscriberId();
                mIdSubscriptions.put(id, destination);
                mSubscriptionIds.put(destination, id);
                mSubscriptions.put(destination, list);
            }

            sendSubscribe(destination);
        }
    }

    /**
     * Un-subscribe from the destination on the server and remove all listeners.
     * @param destination
     * @throws IOException
     */
    public void unsubscribe(String destination) throws IOException {
        if (mSocket != null && !mSocket.isConnected()) throw new ConnectException(MSG_NOT_CONNECTED);
        if (mSubscriptionIds.containsKey(destination)) {
            final Integer id = mSubscriptionIds.get(destination);
            if (id != null) {
                mSubscriptions.get(destination).clear();
                mSubscriptions.remove(destination);
                mSubscriptionIds.remove(destination);
                mIdSubscriptions.remove(id);
                sendUnsubscribe(id);
            }
        }
    }

    /**
     * Remove one or more listeners from the destination.
     * If the listeners list is empty after this operation, we un-subscribe on the server.
     * @param destination
     * @param listeners
     * @throws IOException
     */
    public void unsubscribe(String destination, StompListener... listeners) throws IOException {
        if (mSocket != null && !mSocket.isConnected()) throw new ConnectException(MSG_NOT_CONNECTED);
        if (mSubscriptions.containsKey(destination)) {
            for (StompListener listener : listeners)
                mSubscriptions.get(destination).remove(listener);
            if (mSubscriptions.get(destination).size() == 0)
                unsubscribe(destination);
        }
    }

    protected InputStream getInput() throws IOException {
        if (mSocket != null) return mSocket.getInputStream();
        return null;
    }

    protected OutputStream getOutput() throws IOException {
        if (mSocket != null) return mSocket.getOutputStream();
        return null;
    }

    /**
     * Returns if the connection is up and running. This value is set to {@code true} once the CONNECTED frame has been received.
     * @return
     */
    public Boolean isConnected() {
        return mConnected;
    }

    private Integer getUniqueSubscriberId() {
        return ++mLastSubscriptionId;
    }

    private void sendAbort() throws IOException {
        StompFrame frame = new StompFrame(ABORT);
        frame.addHeader(HEADER_TRANSACTION, mTransaction);
        try {
            writeFrame(frame);
        } finally {
            mTransaction = null;
        }
    }

    private void sendAck(String sub, String id) throws IOException {
        StompFrame frame = new StompFrame(ACK);
        frame.addHeader(HEADER_SUBSCRIPTION, sub);
        frame.addHeader(HEADER_MESSAGE_ID, id);
        if (mTransaction != null)
            frame.addHeader(HEADER_TRANSACTION, mTransaction);
        writeFrame(frame);
    }

    private void sendBegin(String transaction) throws IOException {
        mTransaction = transaction;
        StompFrame frame = new StompFrame(BEGIN);
        frame.addHeader(HEADER_TRANSACTION, mTransaction);
        writeFrame(frame);
    }

    private void sendCommit() throws IOException {
        StompFrame frame = new StompFrame(COMMIT);
        frame.addHeader(HEADER_TRANSACTION, mTransaction);
        try {
            writeFrame(frame);
        } finally {
            mTransaction = null;
        }
    }

    private void sendConnect() throws IOException {
        if (mListenerThread == null) {
            mListenerThread = new StompListenerThread(this, getInput());
        }

        StompFrame frame = new StompFrame(CONNECT);
        frame.addHeader(HEADER_ACCEPT_VERSION, VALUE_ACCEPT_VERSION);
        frame.addHeader(HEADER_HOST, url.getHost());
        frame.addHeader(HEADER_HEARTBEAT, mHeartBeatRecvDelay + "," +mHeartBeatSendDelay);
        if (mUsername != null && mPassword != null) {
        	frame.addHeader(HEADER_USERNAME, mUsername);
        	frame.addHeader(HEADER_PASSWORD, mPassword);
        }

        writeFrame(frame);
    }

    private void sendHeartBeat() throws IOException {
    	StompFrame frame = new StompFrame(HEARTBEAT);
    	writeFrame(frame);
    }

    private void sendMessage(StompMessage message) throws IOException {
        StompFrame frame = StompMessage.toStompFrame(message);

        if (mTransaction != null)
            frame.addHeader(HEADER_TRANSACTION, mTransaction);

        writeFrame(frame);
    }

    private void sendNack(String sub, String id) throws IOException {
        StompFrame frame = new StompFrame(NACK);
        frame.addHeader(HEADER_SUBSCRIPTION, sub);
        frame.addHeader(HEADER_MESSAGE_ID, id);
        if (mTransaction != null)
            frame.addHeader(HEADER_TRANSACTION, mTransaction);
        writeFrame(frame);
    }

    private void sendSubscribe(String destination) throws IOException {
        StompFrame frame = new StompFrame(SUBSCRIBE);
        frame.addHeader(HEADER_ID, mSubscriptionIds.get(destination).toString());
        frame.addHeader(HEADER_DESTINATION, destination);
        frame.addHeader(HEADER_ACK, VALUE_ACK);
        writeFrame(frame);
    }

    private void sendUnsubscribe(Integer id) throws IOException {
        StompFrame frame = new StompFrame(UNSUBSCRIBE);
        frame.addHeader(HEADER_ID, id.toString());
        writeFrame(frame);
    }

    /**
     * Handles the incoming STOMP frames from the server
     * and sends appropriate ACK and NACK responses.
     * @param frame
     */
    private void handleStompFrame(StompFrame frame) throws IOException {
    	final String cmd = frame.getCommand();

    	if (CONNECTED.equals(cmd)) {
            mConnected = true;
            if (mListener != null) mListener.onConnected();
    	} else if (MESSAGE.equals(cmd)) {
            String id = frame.getHeaders().get(HEADER_SUBSCRIPTION);
            if (mIdSubscriptions.containsKey(Integer.parseInt(id))) {
                String destination = mIdSubscriptions.get(Integer.parseInt(id));
                boolean isAcknowledged = true; // All listeners need to return true for the message to be acknowledged
                try {
                    for (StompListener listener : mSubscriptions.get(destination)) {
                        try {
                            // AND isAcknowledged with the result and store it. It will never flip
                            // back to true...
                            isAcknowledged &= listener.onMessage(StompMessage.fromStompFrame(frame));
                        } catch (Exception e) {
                            // Ignore any listener exceptions
                        }
                    }
                } finally {
                    if (isAcknowledged) sendAck(id, frame.getHeaders().get(HEADER_MESSAGE_ID));
                    else sendNack(id, frame.getHeaders().get(HEADER_MESSAGE_ID));
                }
            } else {
                sendNack(id, frame.getHeaders().get(HEADER_MESSAGE_ID));
            }
    	} else if (RECEIPT.equals(cmd)) {
            if (mListener != null)
                mListener.onReceipt(frame.getHeaders().get(HEADER_RECEIPT));
    	} else if (ERROR.equals(cmd)) {
            if (mListener != null)
                mListener.onError(StompMessage.fromStompFrame(frame));
    	} else {

            if (mSocket == null) {
                handleServerMessage(frame);
            } else {
                if (mListener != null)
                    mListener.onUnknownCommand(StompMessage.fromStompFrame(frame));
            }
    	}
    }

    protected void handleServerMessage(StompFrame frame) throws IOException {}

    private void onException(Exception e) {
        if (mListener == null) {
            if (!MSG_SOCKET_CLOSED.equals(e.getMessage())) e.printStackTrace();
        } else {
            if (MSG_SOCKET_CLOSED.equals(e.getMessage())) {
                mListener.onDisconnected();
            } else {
                mListener.onException(e);
            }
        }
    }

    private void preWrite(StompFrame frame) {
        // Fetch any existing heart-beat future, and cancel it.
        if (!HEARTBEAT.equals(frame.getCommand())) {
            if (mFuture != null) mFuture.cancel(true);
        }
    }

    private void postWrite() {
        // Create a new heart-beat future here
        if (mHeartBeatSendDelay > 0) {
            mFuture = mExecutor.schedule(new StompHeartBeatWriter(this), mHeartBeatSendDelay, TimeUnit.MILLISECONDS);
        }
    }

    synchronized private void writeFrame(StompFrame frame) throws IOException {
        preWrite(frame);
        try {
            // StompFrame's reader.ReadLine is blocking for the duration of mSocket.getReadTimeout() which defaults to 0.
            // When implementing heart-beats, we have to modify the read timeout and throw exceptions when the
            // heart-beat timeout has been reached.
            // Worst of all, if properly implemented, each explicit read statement should be equipped with additional
            // heart-beat detection code. As we are using a simple BufferedReader, access to each individual read statement
        	// is virtually impossible, unless we roll our own readLine method using read(byte).
            StompFrame.writeFrame(frame, getOutput());
        } finally {
            postWrite();
        }
    }

    private static class StompHeartBeatWriter implements Runnable {
    	private StompConnection mConnection;
    	public StompHeartBeatWriter(StompConnection connection) {
    		mConnection = connection;
    	}
		@Override
		public void run() {
			try {
				mConnection.sendHeartBeat();
			} catch (IOException e) {
				// Do something here!!!
			}
		}
    }

    private static class StompListenerThread implements Runnable {

        private InputStream mInput;

        private Thread mThread;
        private StompConnection mConnection;

        public StompListenerThread(StompConnection connection, InputStream input) {
            mConnection = connection;
            mInput = input;
            mThread = new Thread(this);
            mThread.start();
        }

        @Override
        public void run() {
            try {
                while (true) {
                    StompFrame frame = StompFrame.readFrame(mInput);
                    if ("".equals(frame.getCommand())) throw new SocketException(MSG_SOCKET_CLOSED);
                    mConnection.handleStompFrame(frame);
                }
            } catch (IOException e) {
                mConnection.onException(e);
            } finally {
                synchronized(mConnection) {
                    mConnection.notify();
                }
            }
        }

        public void interrupt() {
            mThread.interrupt();
        }
    }

    public interface Listener {
        public void onConnecting();
        public void onConnected();
        public void onDisconnected();
        public void onError(StompMessage error);
        public void onException(Exception e);
        public void onReceipt(String receipt);
        public void onUnknownCommand(StompMessage unknown);
    }

}
