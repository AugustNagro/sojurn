package us.nagro.august.sojurn.server;

interface ServiceInstaller {
    void install(int port) throws Exception;
    void uninstall() throws Exception;
}
