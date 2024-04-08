package genericiotinfrastructure;

import com.sun.media.sound.InvalidDataException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import genericiotinfrastructure.crud.MongoCRUD;
import org.json.JSONObject;
import threadpool.ThreadPool;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;


public class GatewayServer {
    private final ConnectionManager connectionManager;
    private final RequestHandler requestHandler;
    private final PlugAndPlay plugAndPlay;
    private final MongoCRUD mongoCRUD;
    private final String plugAndPlayFolderPath;

    public GatewayServer(String plugAndPlayFolderPath) {
        this.connectionManager = new ConnectionManager();
        this.requestHandler = new RequestHandler();
        this.plugAndPlayFolderPath = plugAndPlayFolderPath;
        this.plugAndPlay = new PlugAndPlay(plugAndPlayFolderPath);
        this.mongoCRUD = new MongoCRUD();
    }

    public void start() {
        connectionManager.start();
        if(plugAndPlayFolderPath != null){
            new Thread(plugAndPlay).start();
        }
    }

    public void stop(int maxWaitingTimeForServerStop) {
        plugAndPlay.stop();
        connectionManager.stop(maxWaitingTimeForServerStop);
    }

    public void addTCPConnection(int port) throws IOException {
        connectionManager.addTCPConnection(port);
    }

    public void addUDPConnection(int port) throws IOException {
        connectionManager.addUDPConnection(port);
    }

    public void addHTTPConnection(int port) throws IOException {
        connectionManager.addHTTPConnection(port);
    }

    private void handle(ByteBuffer buffer, Communicator communicator) {
        requestHandler.handle(buffer, communicator);
    }

    private interface Communicator {
        ByteBuffer receive() throws IOException;

        void send(int statusCode, JSONObject body) throws IOException;

    }

    private interface Command {
        void exec(MongoCRUD mongoCRUD) throws InvalidDataException;
    }

    private class RequestHandler {
        private final ThreadPool threadPool;
        private final Factory<String, JSONObject> factory;

        private RequestHandler() {
            this.threadPool = new ThreadPool(Runtime.getRuntime().
                    availableProcessors());

            this.factory = new Factory<>();

            addCommandsToFactory();
        }

        // -------- COMMANDS --------

        private class RegisterCompanyCommand implements Command {

            private final JSONObject request;

            public RegisterCompanyCommand(JSONObject request) {

                this.request = request;
            }

            @Override
            public void exec(MongoCRUD mongoCRUD) throws InvalidDataException {

                if (!request.has("companyName")) {
                    throw new InvalidDataException("missing companyName " +
                            "field");
                }

                mongoCRUD.registerCompanyCRUD(request);
            }
        }

        private class RegisterProductCommand implements Command {
            private final JSONObject request;

            private RegisterProductCommand(JSONObject request) {

                this.request = request;
            }

            @Override
            public void exec(MongoCRUD mongoCRUD) throws InvalidDataException {

                if (!request.has("companyName")) {
                    throw new InvalidDataException("missing companyName field");
                }

                if (!request.has("productName")) {
                    throw new InvalidDataException("missing productName field");
                }

                if (!mongoCRUD.isCompanyRegistered(request)) {
                    throw new InvalidDataException("company is not registered");
                }

                mongoCRUD.registerProductCRUD(request);
            }
        }

        private class RegisterDeviceCommand implements Command {
            private final JSONObject request;

            private RegisterDeviceCommand(JSONObject request) {

                this.request = request;
            }

            @Override
            public void exec(MongoCRUD mongoCRUD) throws InvalidDataException {

                if (!request.has("companyName")) {
                    throw new InvalidDataException("missing companyName field");
                }

                if (!request.has("productName")) {
                    throw new InvalidDataException("missing productName field");
                }

                if (!mongoCRUD.isCompanyRegistered(request)) {
                    throw new InvalidDataException("company is not registered");
                }

                if (!mongoCRUD.isProductRegistered(request)) {
                    throw new InvalidDataException("product is not registered");
                }

                mongoCRUD.registerIotCRUD(request);
            }
        }

        private class SendUpdateCommand implements Command {

            private final JSONObject request;

            private SendUpdateCommand(JSONObject request) {

                this.request = request;
            }

            @Override
            public void exec(MongoCRUD mongoCRUD) throws InvalidDataException {

                if (!request.has("companyName")) {
                    throw new InvalidDataException("missing companyName field");
                }

                if (!request.has("productName")) {
                    throw new InvalidDataException("missing productName field");
                }

                if (!request.has("iotUpdate")) {
                    throw new InvalidDataException("missing iotUpdate field");
                }

                if (!mongoCRUD.isCompanyRegistered(request)) {
                    throw new InvalidDataException("company is not registered");
                }

                if (!mongoCRUD.isProductRegistered(request)) {
                    throw new InvalidDataException("product is not registered");
                }

                if (!mongoCRUD.isIOTRegistered(request)) {
                    throw new InvalidDataException("iot is not registered");
                }

                mongoCRUD.registerIotCRUD(request);
            }
        }

        // -------- END OF COMMANDS --------

        private void addCommandsToFactory() {
            //add 4 commands for the API: register company, register
            // product, register iot, update

            //Create functions for the 4 classes that implement Command and
            // add them to the map using the appropriate key.

            factory.add("RegisterCompany", RegisterCompanyCommand::new);
            factory.add("RegisterProduct", RegisterProductCommand::new);
            factory.add("RegisterIoT", RegisterDeviceCommand::new);
            factory.add("Update", SendUpdateCommand::new);
        }

        private void handle(ByteBuffer buffer, Communicator communicator) {
            //create task (runnable). the task calls the parser. the parser
            // creates a key-value pair. the key is the text before the @.
            // for example, to register a company named Tadiran, the client
            // sends the following text: "RegisterCompany@Tadiran".
            // "RegisterCompany" is the key and "Tadiran" is the value.
            // after creating an entry, the task uses the Factory to get the
            // command that needs to be run and executes it using the
            // command's exec() method.

            //after creating the task, which is a Runnable, submit it to the
            // ThreadPool.

            if (null == buffer) {
                return;
            }

            Runnable task = Task(buffer, communicator);
            threadPool.submit(task, ThreadPool.Priority.DEFAULT);
        }

        private JSONObject byteBufferToJSONObject(ByteBuffer buffer) {
            byte[] byteArray = new byte[buffer.remaining()];
            buffer.get(byteArray);

            String jsonString = new String(byteArray, StandardCharsets.UTF_8);
            System.out.println("jsonString: " + jsonString);
            return new JSONObject(jsonString);
        }

        private Entry<String, JSONObject> parse(JSONObject request) throws InvalidDataException {

            //check request validity
            if (!request.has("request") || !request.has("data")) {
                throw new InvalidDataException("request is missing 'request' " +
                        "or 'data' fields");
            }

            //extract request
            String requestType = request.getString("request");

            //extract request data
            JSONObject requestData = request.getJSONObject("data");

            return new AbstractMap.SimpleEntry<>(requestType, requestData);

            //request JSON format (for RegisterCompany):
            /*
            "request": "RegisterCompany"
            data{ (this is an inner Json)
                "companyName": "Tadiran"
            }
            */

            //example for RegisterProduct
            /*
            "request": "RegisterProduct"
            data{ (this is an inner Json)
                "companyName": "Tadiran",
                "productName": "someAirConditioner",
                "productDescription": "a great AC"
            }
            */

            //example for RegisterIoT
            /*
            "request": "RegisterIoT"
            data{ (this is an inner Json)
                "companyName": "Tadiran",
                "productName": "someAirConditioner",

                iotData{
                    "email": "whatever@gmail.com",
                    "username": "some user",
                    "serial number": "12345"
                    }
            }
            */

            //example for Update
            /*
            "request": "Update"
            data{ (this is an inner Json)
                "companyName": "Tadiran",
                "productName": "someAirConditioner",

                iotUpdate{

                    //whatever the company wants to store in its database.
                    it's best to include the serial number of the device to
                    be able to relate the update to a specific user
                }
            }
            */
        }

        private Runnable Task(ByteBuffer buffer,
                              Communicator communicator) {

            return () -> {
                JSONObject userRequest = byteBufferToJSONObject(buffer);
                System.out.println("userRequest in Task: " + userRequest);
                String message = null;
                int status = 200;

                try {
                    Entry<String, JSONObject> entry = parse(userRequest);

                    Command command = factory.create(entry.getKey(),
                            entry.getValue());

                    command.exec(mongoCRUD);

                } catch (InvalidDataException e) {
                    status = 400;
                    message = e.getMessage();

                } finally {

                    JSONObject responseBody = new JSONObject();
                    responseBody.put("message", message);
                    try {
                        communicator.send(status, responseBody);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    System.out.println("data sent from gateway:");
                    System.out.println("status: " + status);
                    System.out.println("body (response): " + message);
                }
            };
        }

        private class Factory<K, D> {
            private final Map<K, Function<D, Command>> commands;

            public Factory() {
                this.commands = new HashMap<>();
            }

            private void add(K key, Function<D, Command> func) {
                this.commands.put(key, func);
            }

            private Command create(K key) {
                return create(key, null);
            }

            private Command create(K key, D data) {
                Function<D, Command> func = commands.get(key);
                if (null == func) {
                    throw new IllegalArgumentException("No such command");
                }

                return func.apply(data);
            }
        }
    }

    // ---------------------------------------------------------------------- //

    private class ConnectionManager {
        private final CommunicationManager communicationManger;
        private final MessageManager messageManager;

        public ConnectionManager() {
            this.communicationManger = new CommunicationManager();
            messageManager = new MessageManager();
        }

        public void addTCPConnection(int clientPort) throws IOException {
            this.communicationManger.addTCPConnection(clientPort);
        }

        public void addUDPConnection(int clientPort) throws IOException {
            this.communicationManger.addUDPConnection(clientPort);
        }

        public void addHTTPConnection(int clientPort) throws IOException {
            this.communicationManger.addHTTPConnection(clientPort);
        }

        public void stop(int maxWaitingTimeForServerStop) {
            this.communicationManger.stop(maxWaitingTimeForServerStop);
        }


        public void start() {
            this.communicationManger.start();
        }

        /*==========================================================================================================*/
        /*===================================== Massage Handlers ===================================================*/

        private class MessageManager {

            public void handle(ByteBuffer receivedMessage, Communicator communicator) throws IOException {

                //ByteBuffer byteBuffer = communicator.receive();
                if (receivedMessage != null && receivedMessage.hasRemaining()) {

                    GatewayServer.this.handle(receivedMessage, communicator);
                }
            }
        }

        private class CommunicationManager {

            private final Selector selector;
            private boolean isRunning;
            private final SelectorRunner selectorRunner;
            private HTTPServer httpServer;

            public CommunicationManager() {
                try {
                    this.selector = Selector.open();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                this.isRunning = true;
                this.selectorRunner = new SelectorRunner();
            }

            public void addTCPConnection(int TCPClientPort) throws IOException {
                ServerSocketChannel tcpServerSocket = ServerSocketChannel.open();
                tcpServerSocket.configureBlocking(false);
                tcpServerSocket.bind(new InetSocketAddress("localhost", TCPClientPort));
                tcpServerSocket.register(selector, SelectionKey.OP_ACCEPT);
            }

            public void addUDPConnection(int UDPClientPort) throws IOException {
                DatagramChannel udpServerSocket = DatagramChannel.open();
                udpServerSocket.configureBlocking(false);
                udpServerSocket.bind(new InetSocketAddress("localhost", UDPClientPort));
                udpServerSocket.register(selector, SelectionKey.OP_READ);
            }

            public void addHTTPConnection(int httpPort) throws IOException {
                this.httpServer = new HTTPServer(httpPort);
            }

            public void start() {
                this.isRunning = true;
                new Thread(this.selectorRunner).start();
                this.httpServer.start();
            }

            public void stop(int maxWaitTimeForServerStop) {
                this.isRunning = false;
                this.httpServer.stop(maxWaitTimeForServerStop);
            }

            private ByteBuffer createByteBuffer(int statusCode, JSONObject body) {
                String responseJson = String.format("{\"status\": %d, \"body\": %s}", statusCode, body.toString());
                return ByteBuffer.wrap(responseJson.getBytes());
            }

            private class HTTPServer{
                private final HttpServer server;
                private final int port;

                public HTTPServer(int port) throws IOException {
                    this.port = port;
                    this.server = HttpServer.create(new InetSocketAddress(port), 0);
                    server.createContext("/requests", new Handler());
                    server.setExecutor(null);
                }

                public void start(){
                    server.start();
                    System.out.println("HTTP Server listening on port " + this.port);
                }

                public void stop(int maxWaitTimeForServerStop){
                    server.stop(maxWaitTimeForServerStop);
                }

                private class Handler implements HttpHandler{
                    @Override
                    public void handle(HttpExchange exchange) throws IOException {
                        String requestMethod = exchange.getRequestMethod();
                        if(!requestMethod.equals("POST")){
                            exchange.sendResponseHeaders(405, 0);
                            exchange.close();
                        }else{
                            handlePostRequest(exchange);
                        }
                    }

                    private void handlePostRequest(HttpExchange exchange) throws IOException {

                        HTTPCommunicator communicator = new HTTPCommunicator(exchange);
                        messageManager.handle(communicator.receive(), communicator);
                    }
                }
            }

            /*========================================================================================================*/
            /*================================ Selector Runner =======================================================*/

            private class SelectorRunner implements Runnable {
                private final TCPRegister tcpRegister;

                public SelectorRunner() {

                    this.tcpRegister = new TCPRegister();

                }

                @Override
                public void run() {
                    Set<SelectionKey> selectedKeys = null;
                    while (isRunning) {
                        try {
                            selector.select();
                            selectedKeys = selector.selectedKeys();
                            Iterator<SelectionKey> iterator = selectedKeys.iterator();

                            while (iterator.hasNext()) {
                                SelectionKey key = iterator.next();

                                if (key.isAcceptable()) {
                                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                                    this.tcpRegister.TCPAccept(serverSocketChannel);

                                } else if (key.isReadable()) {
                                    SelectableChannel channel = key.channel();
                                    if (channel instanceof SocketChannel) { // TCP
                                        TCPCommunicator tcpCommunicator = (TCPCommunicator) key.attachment();

                                        ByteBuffer receivedMessage = tcpCommunicator.receive();
                                        if (receivedMessage == null) {
                                            key.cancel();
                                        } else {
                                            ConnectionManager.this.messageManager.handle(receivedMessage, tcpCommunicator);
                                        }
                                    } else { // UDP
                                        DatagramChannel datagramChannel = (DatagramChannel) channel;
                                        //datagramChannel.disconnect();
                                        UDPCommunicator udpCommunicator = new UDPCommunicator(datagramChannel);
                                        ByteBuffer receivedMessage = udpCommunicator.receive();
                                        ConnectionManager.this.messageManager.handle(receivedMessage, udpCommunicator);

                                    }
                                }
                                iterator.remove();
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    }
                    assert selectedKeys != null;
                    selectedKeys.clear();
                }

            }

            /*========================================================================================================*/
            /*======================================== TCP register =================================================*/

            private class TCPRegister {

                public void TCPAccept(ServerSocketChannel serverSocketChannel) {
                    try {
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        socketChannel.configureBlocking(false);
                        SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ);
                        TCPCommunicator tcpCommunicator = new TCPCommunicator(socketChannel);
                        key.attach(tcpCommunicator);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            /*========================================================================================================*/
            /*========================================== Communicators ===============================================*/

            private class TCPCommunicator implements Communicator {
                private final SocketChannel clientSocketChannel;

                public TCPCommunicator(SocketChannel clientSocketChannel) {
                    this.clientSocketChannel = clientSocketChannel;
                }

                @Override
                public ByteBuffer receive() {
                    try {
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int bytesRead = clientSocketChannel.read(buffer);
                        if (bytesRead > 0) {
                            buffer.flip();
                        } else if (bytesRead == -1) {
                            clientSocketChannel.close();
                            return null;
                        }
                        return buffer;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void send(int statusCode, JSONObject body) {
                    try {
                        if (!clientSocketChannel.isOpen() || !clientSocketChannel.isConnected()) {
                            return;
                        }

                        ByteBuffer responseBuffer = createByteBuffer(statusCode, body);

                        while (responseBuffer.hasRemaining()) {
                            clientSocketChannel.write(responseBuffer);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            private class UDPCommunicator implements Communicator {

                private final DatagramChannel clientDatagramChannel;
                private SocketAddress clientAddress;

                public UDPCommunicator(DatagramChannel clientDatagramChannel) {
                    this.clientDatagramChannel = clientDatagramChannel;
                }

                @Override
                public ByteBuffer receive() {
                    try {
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        this.clientAddress = clientDatagramChannel.receive(buffer);
                        buffer.flip();
                        return buffer;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void send(int statusCode, JSONObject body) {
                    try {
                        ByteBuffer responseBuffer = createByteBuffer(statusCode, body);

                        this.clientDatagramChannel.send(responseBuffer, clientAddress);
                        responseBuffer.flip();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            private class HTTPCommunicator implements Communicator {
                private final HttpExchange exchange;

                public HTTPCommunicator(HttpExchange exchange) {
                    this.exchange = exchange;
                }

                @Override
                public ByteBuffer receive() throws IOException {
                    InputStream inputStream = exchange.getRequestBody();

                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }

                    byte[] requestBodyBytes = byteArrayOutputStream.toByteArray();
                    ByteBuffer byteBuffer = ByteBuffer.wrap(requestBodyBytes);

                    byteArrayOutputStream.close();
                    inputStream.close();

                    return byteBuffer;
                }

                @Override
                public void send(int statusCode, JSONObject body) throws IOException {
                    byte[] responseBodyBytes = body.toString().getBytes();

                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(statusCode, responseBodyBytes.length);

                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(responseBodyBytes);
                    outputStream.close();
                }
            }
        }
    }

    private class PlugAndPlay implements Runnable {
        // used to extend functionality by adding commands to Factory in runtime

        private final DirTracker dirTracker;
        private final DynamicJarLoader jarLoader;
        private boolean isRunning = true;

        private PlugAndPlay(String dirToWatch) {
            //create instances for DirTrack and DynamicLoader
            this.dirTracker = new DirTracker(dirToWatch);
            this.jarLoader = new DynamicJarLoader();
        }

        @Override
        public void run() {
            //pseudocode:
            // run dirtracker. once it detects a jar, it should return its path.
            // create a jarLoader (make it static?) and have it load the Jar if
            // it contains a class that implements the Command interface (the
            // loader takes care of checking this).
            // then, once the class is loaded, add it to the Factory.

            while (isRunning) {

                //get path to jar from dirTracker
                String path = dirTracker.track();

                // load classes in JAR that implement Callable
                // and get a list of those classes
                List<Class<?>> classes = jarLoader.load("Callable", path);

                // for each Class object, wrap it with a class that
                // implements Command, create a Function from the Command
                for (Class<?> callableClass : classes) {
                    try {
                        // create Callable from the Class object
                        Callable<String> callable =
                                (Callable<String>) callableClass.newInstance();

                        // create Command from the Callable
                        CallableCommand callableCommand =
                                new CallableCommand(callable);


                        // create a Function from the Command
                        Function<JSONObject, Command> func = string -> {

                            return callableCommand;
                        };

                        // add Function to factory
                        requestHandler.factory.add(callableClass.getName(),
                                func);

                    } catch (InstantiationException |
                             IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        public void stop() {
            isRunning = false;
        }

        private class CallableCommand implements Command {

            private final Callable<String> callable;

            public CallableCommand(Callable<String> callable) {
                this.callable = callable;
            }

            @Override
            public void exec(MongoCRUD mongoCRUD)
                    throws InvalidDataException {
                try {
                    callable.call();
                } catch (Exception e) {
                    throw new InvalidDataException();
                }
            }
        }

        private class DirTracker {
            private final WatchService watcher;
            private final String dirToWatch;

            public DirTracker(String dirToWatch) {
                try {
                    this.watcher = FileSystems.getDefault().newWatchService();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create watcher", e);
                }

                this.dirToWatch = dirToWatch;
            }

            public String track() {
                //create Path object for directory

                Path watchableDir = Paths.get(dirToWatch);
                WatchKey key;

                try {
                    watchableDir.register(watcher, ENTRY_CREATE);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                //wait for key to be signaled

                while (true) {
                    try {
                        key = watcher.take();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (ENTRY_CREATE != kind) {
                            continue;
                        }

                        Path foundFile = (Path) event.context();
                        Path fullPath = watchableDir.resolve(foundFile);

                        return fullPath.toString();
                    }
                }
            }
        }

        private class DynamicJarLoader {

            public List<Class<?>> load(String interfaceName,
                                       String pathToJar) {

                List<Class<?>> implementingClasses = new LinkedList<>();
                File file = new File(pathToJar);
                URL jarURL;
                URLClassLoader classLoader;
                try {
                    jarURL = file.toURI().toURL();
                    classLoader = new URLClassLoader(new URL[]{jarURL});
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }

                try (JarFile jarFile = new JarFile(file)) {
                    for (Enumeration<JarEntry> entries = jarFile.entries();
                         entries.hasMoreElements(); ) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().endsWith(".class")) {
                            //found class file, convert name to name of class only
                            // (without extension)
                            String className =
                                    entry.getName().replace('/', '.')
                                            .substring(0, entry.getName().length() - 6);

                            Class<?> loadedClass = classLoader.loadClass(className);
                            for (Class<?> anInterface : loadedClass.getInterfaces()) {
                                if (anInterface.getName().equals(interfaceName)) {
                                    implementingClasses.add(loadedClass);

                                    break;
                                }
                            }
                        }

                    }

                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                return implementingClasses;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        GatewayServer server = new GatewayServer(null);

        server.addTCPConnection(8090);
        server.addHTTPConnection(9090);
        server.start();
    }
}

