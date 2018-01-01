package us.nagro.august.sojurn.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class Main extends Application {

    static final ThreadLocalInputStream THREAD_LOCAL_IN  = new ThreadLocalInputStream(System.in);
    static final ThreadLocalPrintStream THREAD_LOCAL_OUT = new ThreadLocalPrintStream(System.out);

    private static final int DEFAULT_PORT = 8081;

    public static void main(String[] args) {
        Platform.setImplicitExit(false);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Map<String, String> namedParams = getParameters().getNamed();
        List<String> rawParams = getParameters().getRaw();

        int port;
        if (namedParams.containsKey("port")) port = Integer.parseInt(namedParams.get("port"));
        else port = DEFAULT_PORT;

        boolean install = rawParams.contains("--install");
        boolean uninstall = rawParams.contains("--uninstall");
        if (install || uninstall) {
            ServiceInstaller si = new ServiceInstallerFactory().getServiceInstaller();
            if (si != null) {
                try {
                    if (uninstall) {
                        si.uninstall();
                        System.out.println("uninstalled service");
                    }
                    if (install) {
                        si.install(port);
                        System.out.println("installed as service");
                    }
                } catch (Exception e) {
                    System.err.println("could not install service");
                    e.printStackTrace();
                }
            } else {
                System.err.println("No ServiceInstaller for this OS");
            }
            System.exit(0);
        } else {
            serve(port);
        }
    }

    private void serve(int port) {
        System.setIn(THREAD_LOCAL_IN);
        System.setOut(THREAD_LOCAL_OUT);
        System.setErr(THREAD_LOCAL_OUT);

        Thread server = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(port)) {
                while (true) {
                    Socket reqSocket = ss.accept();
                    Thread reqHandler = new Thread(new SojurnSessionHandler(reqSocket));
                    reqHandler.start();
                }
            } catch (Exception e) {
                System.err.println("Could not listen on port " + port + "; must specify --port=<num>");
                e.printStackTrace();
                System.exit(-1);
            }
        });
        server.start();
    }

}
