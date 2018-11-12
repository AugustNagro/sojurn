package us.nagro.august.sojurn.server;

import us.nagro.august.sojurn.api.ReqType;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.spi.ToolProvider;

class CachedProgram {

    private static ConcurrentHashMap<Path, CachedProgram> cachedPrograms = new ConcurrentHashMap<>();

    private Class<?> mainClass;
    private FileTime lastModified;

    private CachedProgram(Class<?> mainClass, FileTime lastModified) {
        this.mainClass = mainClass;
        this.lastModified = lastModified;
    }

    static Class<?> getMainClass(Path path, ReqType reqType) throws IOException, ClassNotFoundException {
        FileTime age = Files.getLastModifiedTime(path);

        if (cachedPrograms.containsKey(path)) {
            CachedProgram cp = cachedPrograms.get(path);
            if (cp.lastModified.compareTo(age) == 0) return cp.mainClass;
        }

        Class<?> mainClass;

        if (reqType == ReqType.JAR) {
            JarFile jar = new JarFile(path.toString());
            String mainClassName = jar.getManifest().getMainAttributes().getValue("Main-Class");
            URLClassLoader classLoader = new URLClassLoader(new URL[]{path.toUri().toURL()});
            mainClass = classLoader.loadClass(mainClassName);

        } else if (reqType == ReqType.JAVA) {
            Path tempDirectory = Files.createTempDirectory(null);
            ToolProvider javac = ToolProvider.findFirst("javac").get();
            int exitCode = javac.run(System.out, System.err, "-d", tempDirectory.toString(), path.toString());
            if (exitCode != 0) return null;

            String mainClassName = path.getFileName().toString().replace(".java", "");
            URLClassLoader classLoader = new URLClassLoader(new URL[]{tempDirectory.toUri().toURL()});
            mainClass = classLoader.loadClass(mainClassName);

        } else return null;

        CachedProgram cp = new CachedProgram(mainClass, age);
        cachedPrograms.put(path, cp);

        return mainClass;
    }

}
