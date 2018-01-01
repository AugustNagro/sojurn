package us.nagro.august.sojurn.server;

import java.io.PrintStream;
import java.util.Locale;

/**
 * Used to provide unique System.in for each thread
 */
class ThreadLocalPrintStream extends PrintStream {

    private InheritableThreadLocal<PrintStream> stream = new InheritableThreadLocal<>();

    /**
     * @param defaultSysOut should be `System.in
     */
    ThreadLocalPrintStream(PrintStream defaultSysOut) {
        super(defaultSysOut);
        set(defaultSysOut);
    }

    void set(PrintStream currentThreadPrintStream) {
        stream.set(currentThreadPrintStream);
    }


    @Override
    public PrintStream append(char c) {
        return stream.get().append(c);
    }

    @Override
    public PrintStream append(CharSequence csq) {
        return stream.get().append(csq);
    }

    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        return stream.get().append(csq, start, end);
    }

    @Override
    public boolean checkError() {
        return stream.get().checkError();
    }

    @Override
    public void close() {
        stream.get().close();
    }

    @Override
    public void flush() {
        stream.get().flush();
    }

    @Override
    public PrintStream format(String format, Object... args) {
        return stream.get().format(format, args);
    }

    @Override
    public PrintStream format(Locale l, String format, Object... args) {
        return stream.get().format(l, format, args);
    }

    @Override
    public void print(boolean b) {
        stream.get().print(b);
    }

    @Override
    public void print(char c) {
        stream.get().print(c);
    }

    @Override
    public void print(char[] s) {
        stream.get().print(s);
    }

    @Override
    public void print(double d) {
        stream.get().print(d);
    }

    @Override
    public void print(float f) {
        stream.get().print(f);
    }

    @Override
    public void print(int i) {
        stream.get().print(i);
    }

    @Override
    public void print(long l) {
        stream.get().print(l);
    }

    @Override
    public void print(Object obj) {
        stream.get().print(obj);
    }

    @Override
    public void print(String s) {
        stream.get().print(s);
    }

    @Override
    public PrintStream printf(Locale l, String format, Object... args) {
        return stream.get().printf(l, format, args);
    }

    @Override
    public PrintStream printf(String format, Object... args) {
        return stream.get().printf(format, args);
    }

    @Override
    public void println() {
        stream.get().println();
    }

    @Override
    public void println(boolean b) {
        stream.get().println(b);
    }

    @Override
    public void println(char c) {
        stream.get().println(c);
    }

    @Override
    public void println(char[] s) {
        stream.get().println(s);
    }

    @Override
    public void println(double d) {
        stream.get().println(d);
    }

    @Override
    public void println(float f) {
        stream.get().println(f);
    }

    @Override
    public void println(int i) {
        stream.get().println(i);
    }

    @Override
    public void println(long l) {
        stream.get().println(l);
    }

    @Override
    public void println(Object obj) {
        stream.get().println(obj);
    }

    @Override
    public void println(String s) {
        stream.get().println(s);
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        stream.get().write(buf, off, len);
    }

    @Override
    public void write(int b) {
        stream.get().write(b);
    }
}
