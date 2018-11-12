package us.nagro.august.sojurn.api;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static us.nagro.august.sojurn.api.ReqType.UNKNOWN;

public class SojurnReq {

    public final ReqType reqType;
    public final Path path;
    public final String[] runArgs;

    public SojurnReq(ReqType reqType, Path path, String[] runArgs) {
        this.reqType = reqType;
        this.path = path;
        this.runArgs = runArgs;
    }

    /**
     * Serializes using the following schema:
     * 1. Request Type (byte)
     * 2. Program argument length, ie. length of #4 (byte)
     * 3. Path to .jar/.java (until newline)
     * 4. Program arguments (separated by newline until end of stream)
     */
    public static byte[] serialize(SojurnReq req) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos, true);

        ps.write(req.reqType.ordinal());
        ps.write(req.runArgs.length);
        ps.println(req.path.toAbsolutePath().toString());
        for (String arg : req.runArgs) ps.println(arg);

        return bos.toByteArray();
    }

    public static SojurnReq deserialize(InputStream is) {
        try {
            // do not close, as this will inadvertently close the socket's stream
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            ReqType reqType = ReqType.values()[br.read()];
            int argLength = br.read();

            String programPath = br.readLine();
            Path p = Paths.get(programPath);

            String[] runArgs = new String[argLength];
            for (int i = 0; i < argLength; i++) {
                runArgs[i] = br.readLine();
            }

            return new SojurnReq(reqType, p, runArgs);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new SojurnReq(UNKNOWN, null, null);
    }

}
