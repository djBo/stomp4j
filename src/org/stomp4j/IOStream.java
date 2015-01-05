package org.stomp4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * A circular unbound array of bytes.
 *
 * <a name="backing_array"><h3>Backing Array</h3></a>
 * This class is implemented using a {@code byte[]}. The length of the array is dynamic.
 * Access is thread-safe using the read/write methods on the stream. The read and write
 * methods are redirected to two internal InputStream/OutputStream implementations
 * operating on the shared byte array.
 *
 * <a name="warning"><h3>Blocking</h3></a>
 * The stream supports both modes, defaulting to blocking. When non-blocking mode is enabled,
 * any read operation will return -1 when there is no data available.
 *
 * <a name="warning"><h3>Warning</h3></a>
 * There is absolutely NO bounds checking whatsoever. The implementation makes an intermediate
 * copy during the operations. Also note that there is no thread-safe way retrieve the actual
 * size of the array.
 *
 * @author Rory Slegtenhorst <rory.slegtenhorst@gmail.com>
 */
class IOStream {

    private final Object mBlockingLock = new Object();

    private final ByteArrayBackedInputStream mInputStream;
    private final ByteArrayBackedOutputStream mOutputStream;

    private byte[] mBuffer = new byte[0];
    private final Boolean mIsBlocking;
    private int mBytesNeeded = -1;

    public IOStream() {
        this(true);
    }

    public IOStream(Boolean blocking) {
        mIsBlocking = blocking;
        mInputStream = new ByteArrayBackedInputStream();
        mOutputStream = new ByteArrayBackedOutputStream();
    }

    public InputStream getInputStream() {
        return mInputStream;
    }

    public OutputStream getOutputStream() {
        return mOutputStream;
    }

    public void clear() {
        if (mBuffer.length > 0)
        synchronized(mBlockingLock) {
            mBuffer = new byte[0];
        }
    }

    public Boolean isBlocking() {
        return mIsBlocking;
    }

    private int readByte() throws IOException {
        if (mBuffer.length == 0) {
            if (!mIsBlocking) return -1;
            try {
                mBytesNeeded = 1;
                synchronized (mBlockingLock) {
                    mBlockingLock.wait();
                }
                mBytesNeeded = -1;
            } catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
        }

        synchronized (mBlockingLock) {
            try {
                return mBuffer[0];
            } finally {
                mBuffer = Arrays.copyOfRange(mBuffer, 1, mBuffer.length);
            }
        }
    }

    private int readBytes(byte[] buffer, int offset, int len) throws IOException {
        if (mBuffer.length == 0) {
            if (!mIsBlocking) return -1;
            try {
                mBytesNeeded = len;
                synchronized (mBlockingLock) {
                    mBlockingLock.wait();
                }
                mBytesNeeded = -1;
            } catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
        }
        synchronized (mBlockingLock) {
            final int cnt = (len > mBuffer.length) ? mBuffer.length : len;
            try {
                System.out.println("IOStream.readBytes: byte[" + buffer.length + "]@" + offset + " " + len + "/" + cnt);
                System.arraycopy(mBuffer, 0, buffer, offset, cnt);
                return cnt;
            } finally {
                mBuffer = Arrays.copyOfRange(mBuffer, cnt, mBuffer.length);
            }
        }
    }

    private void writeByte(int oneByte) {
        synchronized (mBlockingLock) {
            byte[] result = new byte[mBuffer.length + 1];
            System.arraycopy(mBuffer, 0, result, 0, mBuffer.length);
            result[result.length - 1] = (byte) oneByte;
            mBuffer = result;
            if (mBytesNeeded != -1) mBlockingLock.notify();
        }
    }

    private void writeBytes(byte[] buffer, int offset, int count) {
        System.out.println("IOStream.writeBytes: byte[" + buffer.length + "]@" + offset + " " + count);
        synchronized (mBlockingLock) {
            byte[] result = new byte[mBuffer.length + count];
            System.arraycopy(mBuffer, 0, result, 0, mBuffer.length);
            System.arraycopy(buffer, offset, result, mBuffer.length, count);
            mBuffer = result;
            if (mBytesNeeded != -1) mBlockingLock.notify();
        }
    }

    class ByteArrayBackedInputStream extends InputStream {

        @Override
        synchronized public int read() throws IOException {
            return readByte();
        }

        @Override
        public int read(byte[] buffer, int offset, int len) throws IOException {
            return readBytes(buffer, offset, len);
        }
    }

    class ByteArrayBackedOutputStream extends OutputStream {

        @Override
        synchronized public void write(int oneByte) throws IOException {
            writeByte(oneByte);
        }

        @Override
        public void write(byte[] buffer, int offset, int count) throws IOException {
            writeBytes(buffer, offset, count);
        }
    }
}
