package us.nagro.august.sojurn.server;

import java.nio.file.*;
import java.util.List;

class MacServiceInstaller implements ServiceInstaller {

    private static final String plistPart1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE plist PUBLIC \"-//Apple//DTDPLIST1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\"><plist version=\"1.0\"><dict><key>Label</key><string>us.nagro.august.sojurn</string><key>ProgramArguments</key><array><string>java</string><string>-jar</string>";
    private static final String plistPart2 = "</array><key>RunAtLoad</key><true/><key>KeepAlive</key><true/></dict></plist>";

    private static Path HOME = Paths.get(System.getProperty("user.home"));
    private static Path PLIST_PATH = HOME.resolve("Library/LaunchAgents/sojurn.plist");
    private static Path JAR_PATH = HOME.resolve("sojurn.jar");

    @Override
    public void install(int port) throws Exception {

        // remove process if pre-existing
        uninstall();

        String pathToJar = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        Files.copy(Paths.get(pathToJar), JAR_PATH, StandardCopyOption.REPLACE_EXISTING);

        String fullPlist = plistPart1 + "<string>"+JAR_PATH+"</string>" + "<string>"+port+"</string>" + plistPart2;
        Files.write(PLIST_PATH, List.of(fullPlist));

        new ProcessBuilder("launchctl", "load", PLIST_PATH.toString())
                .inheritIO()
                .start().waitFor();
        new ProcessBuilder("launchctl", "start", "sojurn")
                .inheritIO()
                .start().waitFor();

    }

    @Override
    public void uninstall() throws Exception {
        new ProcessBuilder("launchctl", "stop", "sojurn")
                .start().waitFor();
        new ProcessBuilder("launchctl", "unload", PLIST_PATH.toString())
                .start().waitFor();

        Files.deleteIfExists(PLIST_PATH);
        Files.deleteIfExists(JAR_PATH);
    }
}
