# Sojurn
Run JavaFX apps and JDK tools (like `javac`) faster with a local app server.

## Requirements
* Mac or Linux, until Graal's Substrate VM adds Windows Support
* Java 9+ JRE. JDK Required to use features like `-javac` (see below)

## Features
* Execute jar files *fast*: `./sojurn -jar <path>`
    * Supports JavaFX Applications. Standard out cannot be redirected, as all applications run on the same JFX Application thread.
* Run JDK tools without warmup
    * 14ms to launch a hello world JAR.
    * Compiling hello world class with javac reduced to 48ms from 920ms
* Start the server on login:
    * `--install [--port=<number>]`

## Usage
```text
Start the server with:
`java -jar server.jar [options]

Where options include:
--port=<number>     Connect to the server at the given port. Default is 8081.
--install           Installs the server as a service, launched at login.
--uninstall         Uninstalls the service.

Client usage:
./sojurn [options] <command> [arguments]

Available options:
--port=<number> Start the server at a given port. Default is 8081.

Commands:
-jar <file>             Execute a jar. Classloader is cached on the server
                        for speedup on subsequent runs.
-javac <arguments>      Compiles sourcecode, accepting almost all javac arguments
-jshell <arguments>     Launches jshell running in the server vm
-jjs <js string>        Execute javascript with Nashorn
-jartool <arguments>    JDK's jar executable
-jarsigner <arguments>
-jlink <arguments>
-javap <arguments>
-javadoc <arguments>
-jdeps <arguments>

Note: The client will attempt to start a server instance
if there isn't one running at the provided or default port.
To work the server jar file must be placed in the computer's home
directory. This is done automatically with the server's --install flag.

Note: -javac, -jshell, etc, require a JDK installed.
```

## Recommendations
* Set aliases:
`alias javac="/usr/local/bin/sojurn --javac"` 
* Only run with trusted applications on account of the inherent security vulnerability.

## Building Client Executable
* Compile Client with java 1.8
* Install Graal 0.30.0
* Run `native-image -H:Name=sojurn Client.class`

## TODO
* Register jar file association on install (see jnlp.IntegrationService)
* On the client requesting jshell, set the terminal to raw mode via ProcessBuilder to take advantage of tab completion
* Speed up client-server communication. Some possibilities:
    * Named pipes on windows (see Nailgun)
    * JNI native sockets on Unix (see Nailgun)
    * Shared memory-mapped file
* Remove taskbar icon
* Swing, swt support
* Testing
