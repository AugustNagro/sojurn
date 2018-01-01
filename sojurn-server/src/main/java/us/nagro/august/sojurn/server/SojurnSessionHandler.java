package us.nagro.august.sojurn.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import jdk.jshell.tool.JavaShellToolBuilder;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import us.nagro.august.sojurn.api.SojurnServerReq;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.spi.ToolProvider;

class SojurnSessionHandler implements Runnable {

    private static final String JFX_QUALIFIED_NAME = Application.class.getName();

    private static final ScriptEngine  JS      = buildNashornEngine();
    private static final ReentrantLock JS_LOCK = new ReentrantLock();

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

            SojurnServerReq req = SojurnServerReq.reqType(in);
            switch (req) {
                case JAR:
                    runJar(req.deserialize(in));
                    break;
                case JSHELL:
                    runJshell(req.deserialize(in));
                    break;
                case JJS:
                    runJS(req.deserialize(in));
                    break;
                case UNKNOWN:
                    out.println("Unknown method type");
                    break;
                default:
                    runJdkTool(req, req.deserialize(in));
                    break;
            }
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runJar(String[] args) throws Exception {
        Path jarPath = Paths.get(args[0]);
        Class<?> mainClass = CachedClassloader.getMainClass(jarPath);

        if (mainClass.getSuperclass().getName().equals(JFX_QUALIFIED_NAME)) {
            Application app = mainClass.asSubclass(Application.class).getDeclaredConstructor().newInstance();
            Platform.runLater(() -> {
                try {
                    app.init();
                    app.start(new Stage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            String[] jarArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

            Method main = mainClass.getMethod("main", String[].class);
            main.invoke(null, new Object[]{ jarArgs });
        }
    }

    private void runJdkTool(SojurnServerReq req, String[] args) {
        Optional<ToolProvider> toolProvider = ToolProvider.findFirst(req.getExecutableName());
        toolProvider.ifPresentOrElse(
                tool -> tool.run(System.out, System.err, args),
                () -> System.err.println("Must run server on JDK, not JRE..")
        );
    }

    private void runJshell(String[] args) throws Exception {
        JavaShellToolBuilder jshell = JavaShellToolBuilder.builder()
                .in(System.in, null)
                .out(System.out)
                .err(System.err);

        if (args == null || args.length == 0) jshell.run();
        else jshell.run(args);
    }

    private void runJS(String[] args) throws InterruptedException, ScriptException {
        String cmd = String.join(" ", args);

        Object res = null;
        // try to use cached engine if possible
        if (JS_LOCK.tryLock(2, TimeUnit.SECONDS)) {
            try {
                res = JS.eval(cmd);
                System.out.println(res);
            } catch (ScriptException e) {
                e.printStackTrace();
            }
            JS_LOCK.unlock();
        } else {
            ScriptEngine engine = buildNashornEngine();
            res = engine.eval(cmd);
            System.out.println(res);
        }
    }

    private static ScriptEngine buildNashornEngine() {
        return new NashornScriptEngineFactory().getScriptEngine("-ot");
    }
}
