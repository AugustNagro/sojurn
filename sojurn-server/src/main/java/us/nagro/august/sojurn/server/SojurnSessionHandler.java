package us.nagro.august.sojurn.server;

import us.nagro.august.sojurn.api.SojurnReq;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.Socket;

class SojurnSessionHandler implements Runnable {

    private Socket socket;

    SojurnSessionHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            InputStream in = socket.getInputStream();
            PrintStream out = new PrintStream(socket.getOutputStream(), true);

            Main.THREAD_LOCAL_IN.set(in);
            Main.THREAD_LOCAL_OUT.set(out);

            SojurnReq req = SojurnReq.deserialize(in);

            switch (req.reqType) {
                case JAR:
                case JAVA:
                    Class<?> mainClass = CachedProgram.getMainClass(req.path, req.reqType);
                    if (mainClass == null) return;

                    Method main = mainClass.getDeclaredMethod("main", String[].class);
                    main.setAccessible(true);
                    main.invoke(null, new Object[]{ req.runArgs });
                    break;

                case UNKNOWN:
                    return;
                case STOP:
                    System.out.println("Stopped Server");
                    System.exit(0);
                    break;
            }

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();

            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}
