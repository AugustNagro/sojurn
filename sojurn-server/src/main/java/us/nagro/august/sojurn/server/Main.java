package us.nagro.august.sojurn.server;

import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    // Used to redirect the Server's System.[in | out | err]. Needs to be set in each SojurnSessionHandler
    static final ThreadLocalInputStream THREAD_LOCAL_IN  = new ThreadLocalInputStream(System.in);
    static final ThreadLocalPrintStream THREAD_LOCAL_OUT = new ThreadLocalPrintStream(System.out);

    private static final int DEFAULT_PORT = 8081;

    public static void main(String[] args) {

        int port = DEFAULT_PORT;
        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            if (arg.equals("--port")) {
                port = Integer.parseInt(args[i+1]);
                i += 1;
            }
        }

        System.setIn(THREAD_LOCAL_IN);
        System.setOut(THREAD_LOCAL_OUT);
        System.setErr(THREAD_LOCAL_OUT);

        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Started Server");

            while (true) {
                Socket reqSocket = ss.accept();
                Thread reqHandler = new Thread(new SojurnSessionHandler(reqSocket));
                reqHandler.start();
            }
        } catch (Exception e) {
            System.err.println("Could not listen on port " + port + "; must specify --port <num>");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
