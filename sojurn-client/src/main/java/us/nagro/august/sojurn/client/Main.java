package us.nagro.august.sojurn.client;

import us.nagro.august.sojurn.api.ReqType;
import us.nagro.august.sojurn.api.SojurnReq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main {

    private static final int    DEFAULT_PORT       = 8081;
    private static final Path   HOME               = Paths.get(System.getProperty("user.home")).toAbsolutePath();
    private static final Path   DEFAULT_SERVER_JAR = HOME.resolve("sojurn.jar");
    private static final String VERSION            = "1.0";
    private static final Path   BREW_SERVER_JAR    =
            Paths.get("/usr/local/Cellar/sojurn/" + VERSION + "/lib/sojurn.jar");

    private static final String USAGE =
            "./sojurn <options> filename.[jar | java]\n" +
                    "\n" +
                    "Available options:\n" +
                    "--port <number> Connect to the server at the given port. Default is 8081.\n" +
                    "--stop          Stops the server, if running.\n" +
                    "\n" +
                    "Note: The client will attempt to start a server instance\n" +
                    "if one isn't running at the provided or default port.\n" +
                    "This requires the server jar be placed in the user's home\n" +
                    "directory, or be installed via package manager\n";

    private static int argIndex = 0;

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || args[0].equals("--help") || args[0].equals("-h")) {
            System.out.println(USAGE);
            return;
        }

        if (args[0].equals("-v") || args[0].equals("--version")) {
            System.out.println("Sojurn Version " + VERSION);
            return;
        }

        int port = DEFAULT_PORT;
        boolean stopServer = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--port") && i + 1 < args.length) {
                port = Integer.parseInt(args[i + 1]);
                i += 1;
                argIndex += 2;
            }

            if (arg.equals("--stop")) {
                stopServer = true;
                argIndex += 1;
            }
        }

        try (
                Socket s = new Socket((String) null, port);
                OutputStream out = s.getOutputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))
        ) {

            if (stopServer) {
                byte[] req = SojurnReq.serialize(new SojurnReq(ReqType.STOP, Paths.get(""), new String[0]));
                out.write(req);

            } else {
                if (argIndex >= args.length) {
                    System.err.println(USAGE);
                    return;
                }
                String file = args[argIndex];
                argIndex += 1;

                ReqType reqType;
                if (file.endsWith(".jar")) reqType = ReqType.JAR;
                else if (file.endsWith(".java")) reqType = ReqType.JAVA;
                else {
                    System.err.println(USAGE);
                    return;
                }

                if (file.startsWith("~")) file = file.replaceFirst("~", HOME.toString());
                Path p = Paths.get(file).toAbsolutePath();

                String[] runArgs = Arrays.copyOfRange(args, argIndex, args.length);

                byte[] req = SojurnReq.serialize(new SojurnReq(reqType, p, runArgs));
                out.write(req);
            }

            String result;
            while ((result = in.readLine()) != null) {
                System.out.println(result);
            }

        } catch (ConnectException ce) {
            if (stopServer) {
                System.err.println("Could not connect to server at port " + port);
                return;
            }
            System.err.println("Could not connect to server at port " + port + "; starting new instance...");

            try {

                Path serverJar = null;
                String os = System.getProperty("os.name");

                if (os.toLowerCase().contains("mac") && Files.exists(BREW_SERVER_JAR)) serverJar = BREW_SERVER_JAR;
                else if (Files.exists(DEFAULT_SERVER_JAR)) serverJar = DEFAULT_SERVER_JAR;

                if (serverJar != null) {
                    ProcessBuilder pb = new ProcessBuilder("java", "-jar", serverJar.toString());
                    Process javaProcess = pb.start();
                    BufferedReader br = new BufferedReader(new InputStreamReader(javaProcess.getInputStream()));
                    String serverStarted = br.readLine();
                    if (serverStarted != null && serverStarted.equals("Started Server")) main(args);
                } else {
                    System.err.println("Server jar not found at\n" + serverJar);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
