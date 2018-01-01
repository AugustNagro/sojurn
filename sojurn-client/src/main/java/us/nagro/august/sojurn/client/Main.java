package us.nagro.august.sojurn.client;

import us.nagro.august.sojurn.api.SojurnServerReq;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final int  DEFAULT_PORT         = 8081;
    private static final int  SERVER_START_TIMEOUT = 1;
    private static final int  TRANSFER_BUF_SIZE    = 1024;
    private static final Path HOME                 = Paths.get(System.getProperty("user.home")).toAbsolutePath();
    private static final Path DEFAULT_SERVER_JAR   = HOME.resolve("sojurn.jar");

    private static final String USAGE =
            "./sojurn [options] <command> [arguments]\n" +
            "\n" +
            "Available options:\n" +
            "--port=<number> Connect to the server at the given port. Default is 8081.\n" +
            "\n" +
            "Commands:\n" +
            "-jar <file>             Execute a jar. Classloader is cached on the server\n" +
            "                        for speedup on subsequent runs.\n" +
            "-javac <arguments>      Compiles sourcecode, accepting almost all javac arguments\n" +
            "-jshell <arguments>     Launches jshell running in the server vm\n" +
            "-jjs <js string>        Execute javascript with Nashorn\n" +
            "-jartool <arguments>    JDK's jar executable\n" +
            "-jarsigner <arguments>\n" +
            "-jlink <arguments>\n" +
            "-javap <arguments>\n" +
            "-javadoc <arguments>\n" +
            "-jdeps <arguments>\n" +
            "\n" +
            "Note: The client will attempt to start a server instance\n" +
            "if there isn't one running at the provided or default port.\n" +
            "To work the server jar file must be placed in the computer's home\n" +
            "directory. This is done automatically with the server's --install flag.\n" +
            "\n" +
            "Note: -javac, -jshell, etc, require a JDK installed.";

    private final String pathSeperator;

    private int argumentsStartIndex = 1;

    private Main() {
        if (System.getProperty("os.name").contains("Windows")) pathSeperator = ";";
        else pathSeperator = ":";
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || args[0].equals("--help") || args[0].equals("-h")) {
            System.out.println(USAGE);
            System.exit(0);
        }

        Main client = new Main();

        int port;
        if (args[0].startsWith("--port=")) {
            port = Integer.parseInt(args[0].substring("--port=".length()));
            client.argumentsStartIndex += 1;
        } else {
            port = DEFAULT_PORT;
        }

        String localhost = null;
        try (
                Socket s = new Socket(localhost, port);
                OutputStream out = s.getOutputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))
        ) {
            String requestType = args[client.argumentsStartIndex - 1];
            if (requestType.equals("-jshell")) {
                client.requestJshell(args, s.getInputStream(), out);
            } else {
                try {
                    String enumValue = requestType.substring(1).toUpperCase();
                    SojurnServerReq req = SojurnServerReq.valueOf(enumValue);
                    client.requestJdkTool(in, out, req, args);
                } catch (IllegalArgumentException e) {
                    System.err.println(USAGE);
                }
            }
        } catch (ConnectException ce) {
            System.err.println("Could not connect to server at port " + port + "; starting new instance...");

            try {
                if (DEFAULT_SERVER_JAR.toFile().exists()) {
                    new ProcessBuilder("java", "-jar", DEFAULT_SERVER_JAR.toString())
                            .start().waitFor(SERVER_START_TIMEOUT, TimeUnit.SECONDS);
                    main(args);
                } else {
                    System.err.println("Server not found at\n" + DEFAULT_SERVER_JAR);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void requestJdkTool(BufferedReader in, OutputStream out, SojurnServerReq jdkTool, String[] args) throws IOException {
        String[] options = absoluteArgsString(args, jdkTool);
        byte[] serialized = jdkTool.serialize(options);
        out.write(serialized);
        printResults(in);
    }

    private void requestJshell(String[] args, InputStream in, OutputStream out) throws Exception {
        byte[] serialized;
        if (args.length - 1 < argumentsStartIndex) serialized = SojurnServerReq.JSHELL.serialize(new String[0]);
        else serialized = SojurnServerReq.JSHELL.serialize(absoluteArgsString(args, SojurnServerReq.JSHELL));

        out.write(serialized);

        // todo remove thread w/ loop transferring both
        new Thread(() -> {
            try {
                transferStream(System.in, out);
            } catch (IOException ignored) {
            }
        }).start();

        transferStream(in, System.out);
    }

    private String[] absoluteArgsString(String[] args, SojurnServerReq reqType) throws RuntimeException {
        if (args.length - 1 < argumentsStartIndex) throw new RuntimeException("arguments required");

        for (int a = argumentsStartIndex; a < args.length; ++a) {
            if (args[a].startsWith("@")) {
                args[a] = "@" + absolutePath(args[a].substring(1));
            } else {
                boolean isOption = false;
                for (String pathOption : reqType.getOptionsContainingPaths()) {
                    if (args[a].equals(pathOption)) {
                        args[a + 1] = absolutePath(args[a + 1]);
                        ++a;
                        isOption = true;
                        break;
                    } else if (args[a].startsWith(pathOption)) {
                        String path = pathOption.substring(pathOption.length());
                        args[a] = pathOption + absolutePath(path);
                        isOption = true;
                        break;
                    }
                }
                if (!isOption && (args[a].endsWith(".java") || args[a].endsWith(".jar") || args[a].endsWith(".class"))) {
                    args[a] = absolutePath(args[a]);
                }
            }
        }

        return Arrays.copyOfRange(args, argumentsStartIndex, args.length);
    }

    private String absolutePath(String localPath) {
        String[] paths = localPath.split(pathSeperator);

        for (int p=0; p < paths.length; ++p) {
            String path = paths[p];
            if (path.startsWith("~")) {
                path = path.replaceFirst("~", HOME.toString());
            }
            paths[p] = Paths.get(path).toAbsolutePath().toString();
        }

        return String.join(pathSeperator, paths);
    }

    private void printResults(BufferedReader in) throws IOException {
        String result;
        while ((result = in.readLine()) != null) {
            System.out.println(result);
        }
    }

    /**
     * Picked from Java 9 InputStream.transferTo, as substrateVM only supports Java 8 at this time.
     */
    private void transferStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[TRANSFER_BUF_SIZE];
        int read;
        while ((read = in.read(buf, 0, TRANSFER_BUF_SIZE)) >= 0) {
            out.write(buf, 0, read);
        }
    }
}
