package us.nagro.august.sojurn.api;

import java.io.*;

public enum SojurnServerReq {
    UNKNOWN,

    JAVAC {
        private final String[] PATH_OPTIONS = {
                "-classpath", "-Djava.ext.dirs=", "-Djava.endorsed.dirs=", "-d",
                "-sourcepath", "-bootclasspath", "-Xbootclasspath/p:", "-Xbootclasspath/a:", "-Xstdout"
        };

        @Override
        public String[] getOptionsContainingPaths() {
            return PATH_OPTIONS;
        }
    },

    /** Executes Jar files; for JDK's jar executable, see JARTOOL **/
    JAR {
        @Override
        public String getExecutableName() {
            return "java -jar";
        }
    },

    JARTOOL {
        private final String[] PATH_OPTIONS = {
                "-C", "-f", "--file=", "-m", "--manifest=", "-p", "--module-path"
        };

        @Override
        public String[] getOptionsContainingPaths() {
            return PATH_OPTIONS;
        }

        @Override
        public String getExecutableName() {
            return "jar";
        }
    },

    JLINK {
        private final String[] PATH_OPTIONS = {
                "-p", "--module-path", "--output"
        };

        @Override
        public String[] getOptionsContainingPaths() {
            return PATH_OPTIONS;
        }
    },

    JAVADOC {
        private final String[] PATH_OPTIONS = {
                "--bootclasspath", "--class-path", "-classpath", "-cp", "-dockletpath",
                "-extdirs", "--module-path", "-p", "--module-source-path", "--source-path",
                "-sourcepath", "--system", "--upgrade-module-path"
        };

        @Override
        public String[] getOptionsContainingPaths() {
            return PATH_OPTIONS;
        }
    },

    JARSIGNER {
        private final String[] PATH_OPTIONS = {
                "-certchain", "-sigfile", "-signedjar", "-altsignerpath"
        };

        @Override
        public String[] getOptionsContainingPaths() {
            return PATH_OPTIONS;
        }
    },

    JDEPS {
        private final String[] PATH_OPTIONS = {
                "-dotoutput", "--generate-module-info", "--generate-open-module",
                "-cp", "-classpath", "--class-path", "--module-path",
                "--upgrade-module-path", "--system"
        };

        @Override
        public String[] getOptionsContainingPaths() {
            return PATH_OPTIONS;
        }
    },

    JJS {
        private final String[] PATH_OPTIONS = {
                "-cp", "-classpath", "--module-path"
        };

        @Override
        public String[] getOptionsContainingPaths() {
            return PATH_OPTIONS;
        }
    },

    JSHELL {
        private final String[] PATH_OPTIONS = {
                "--class-path", "--module-path", "--add-modules", "--startup"
        };

        @Override
        public String[] getOptionsContainingPaths() {
            return PATH_OPTIONS;
        }
    },

    JAVAP {
        private final String[] PATH_OPTIONS = {
                "--module-path", "-m", "--system", "--class-path", "-classpath", "-cp",
                "-bootclasspath"
        };

        @Override
        public String[] getOptionsContainingPaths() {
            return PATH_OPTIONS;
        }
    };

    /**
     * Serialize the arguments, with the format:
     * 1st byte: Ordinal of request
     * 2nd byte: Length of arguments (max 127)
     * Remaining bytes: arguments separated by newline byte
     *
     * @param args May be empty, but not null
     * @return The serialized request
     */
    public byte[] serialize(String[] args) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter ps = new PrintWriter(bos, true);

        bos.write(ordinal());
        bos.write(args.length);
        for (String arg : args) ps.println(arg);
        byte[] serialized = bos.toByteArray();

        ps.close();
        return serialized;
    }

    public String[] deserialize(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        int argLenght = is.read();
        String[] args = new String[argLenght];

        String arg;
        for (int i = 0; i < argLenght; ++i) {
            arg = br.readLine();
            if (arg != null) args[i] = arg;
            else throw new RuntimeException("Malformed Request");
        }

        return args;
    }

    public String[] getOptionsContainingPaths() {
        return new String[0];
    }

    public String getExecutableName() {
        return name().toLowerCase();
    }

    /**
     * The msg number must have already been read
     */
    public static SojurnServerReq reqType(InputStream is) {
        try {
            int msgNum = is.read();
            if (msgNum != -1 && msgNum < values().length) {
                return values()[msgNum];
            }

            return UNKNOWN;
        } catch (IOException e) {
            e.printStackTrace();
            return UNKNOWN;
        }
    }
}
