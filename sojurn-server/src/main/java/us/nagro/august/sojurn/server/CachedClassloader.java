package us.nagro.august.sojurn.server;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

class CachedClassloader {

    private static ConcurrentHashMap<Path, CachedClassloader> cachedClassLoaders = new ConcurrentHashMap<>();

    private Class<?> mainClass;
    private FileTime lastModified;

    private CachedClassloader(Class<?> mainClass, FileTime lastModified) {
        this.mainClass = mainClass;
        this.lastModified = lastModified;
    }

    static Class<?> getMainClass(Path jarPath) throws IOException, ClassNotFoundException {
        FileTime age = Files.getLastModifiedTime(jarPath);

        if (cachedClassLoaders.containsKey(jarPath)) {
            CachedClassloader ccl = cachedClassLoaders.get(jarPath);
            if (ccl.lastModified.compareTo(age) == 0) return ccl.mainClass;
        }

        JarFile jar = new JarFile(jarPath.toString());
        String mainClassName = jar.getManifest().getMainAttributes().getValue("Main-Class");
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()});
        Class<?> mainClass = classLoader.loadClass(mainClassName);

        CachedClassloader ccl = new CachedClassloader(mainClass, age);
        cachedClassLoaders.put(jarPath, ccl);

        return mainClass;
    }

}
