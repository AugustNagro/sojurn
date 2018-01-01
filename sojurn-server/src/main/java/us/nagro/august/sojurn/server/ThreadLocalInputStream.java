package us.nagro.august.sojurn.server;

import java.io.IOException;
import java.io.InputStream;

/**
 * Used to provide unique System.in for each thread
 */
class ThreadLocalInputStream extends InputStream {

    private InheritableThreadLocal<InputStream> stream = new InheritableThreadLocal<>();

    /**
     * @param defaultSysIn should be System.in
     */
    ThreadLocalInputStream(InputStream defaultSysIn) {
        set(defaultSysIn);
    }

    void set(InputStream currentThreadInputStream) {
        stream.set(currentThreadInputStream);
    }


    @Override
    public int available() throws IOException {
        return stream.get().available();
    }

    @Override
    public void close() throws IOException {
        stream.get().close();
    }

    @Override
    public void mark(int readlimit) {
        stream.get().mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return stream.get().markSupported();
    }

    @Override
    public int read() throws IOException {
        return stream.get().read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return stream.get().read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return stream.get().read(b, off, len);
    }

    @Override
    public synchronized void reset() throws IOException {
        stream.get().reset();
    }

    @Override
    public long skip(long n) throws IOException {
        return stream.get().skip(n);
    }
}
