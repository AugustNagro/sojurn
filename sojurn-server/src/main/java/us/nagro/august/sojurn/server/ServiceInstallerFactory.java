package us.nagro.august.sojurn.server;

class ServiceInstallerFactory {

    /**
     * @return ServiceInstaller for given OS, or null if none available
     */
    ServiceInstaller getServiceInstaller() {
        String os = System.getProperty("os.name");
        if (os.startsWith("Mac")) {
            return new MacServiceInstaller();
        } else {
            return null;
        }

    }

}
