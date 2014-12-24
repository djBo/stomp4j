package org.stomp;

import java.io.IOException;
import java.io.IOStream;
import java.io.InputStream;
import java.io.OutputStream;

public class StompServer extends StompConnection {

    private final IOStream io = new IOStream();

    @Override
    protected InputStream getInput() throws IOException {
        return io.getInputStream();
    }

    @Override
    protected OutputStream getOutput() throws IOException {
        return io.getOutputStream();
    }

    @Override
    protected void handleServerMessage(StompFrame frame) {
        final String cmd = frame.getCommand();

        if (CONNECT.equals(cmd)) {
            // Return CONNECTED frame
        } else
        if (SEND.equals(cmd)) {
            // Return MESSAGE frame
        } else
        if (ACK.equals(cmd)) {
            // Clear last MESSAGE
        } else
        if (NACK.equals(cmd)) {
            // Resend last message
        } else
        if (SUBSCRIBE.equals(cmd) || UNSUBSCRIBE.equals(cmd) || BEGIN.equals(cmd) || COMMIT.equals(cmd) || ABORT.equals(cmd) || DISCONNECT.equals(cmd) || DISCONNECT.equals(cmd)) {
            // Do nothing
        }

    }
}
