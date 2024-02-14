package genericiotinfrastructure;

import com.sun.media.sound.InvalidDataException;
import genericiotinfrastructure.crud.MongoCRUD;
import org.json.JSONObject;
import threadpool.ThreadPool;

import java.io.File;
import java.io.IOException;
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
    private final MultiProtocolServer multiProtocolServer;
    private final RequestHandler requestHandler;
    //private final PlugAndPlay plugAndPlay;
    private final MongoCRUD mongoCRUD;

    public GatewayServer() {
        this.multiProtocolServer = new MultiProtocolServer();
        this.requestHandler = new RequestHandler();
//        this.plugAndPlay = new PlugAndPlay("/home/yonathan/Downloads" +
//                "/folderToTrack");

        this.mongoCRUD = new MongoCRUD();
    }

    private static String convertBytesToString(ByteBuffer buffer) {
        // Create a new ByteBuffer to store the copied bytes.
        ByteBuffer copy = buffer.duplicate();

        // Create a byte array to store the copied bytes.
        byte[] bytes = new byte[copy.remaining()];

        // Copy the bytes from the copy buffer to the byte array.
        copy.get(bytes);
        // Convert the byte array to a String using the UTF-8 encoding.
        return new String(bytes);
    }


    public void start() throws IOException, ClassNotFoundException {

        multiProtocolServer.start();
        //new Thread(plugAndPlay).start();

    }

    public void stop() {
        //plugAndPlay.stop();
        multiProtocolServer.stop();
    }

    public void addTCPConnection(int port) throws IOException {
        multiProtocolServer.addTCPConnection(port);
    }

    public void addUDPConnection(int port) throws IOException {
        multiProtocolServer.addUDPConnection(port);
    }

    private void handle(ByteBuffer buffer, Communicator communicator) {
        //buffer is the message that was received from the client.
        requestHandler.handle(buffer, communicator);
    }

    //package private for testing only! should be private.
    private interface Communicator {
        ByteBuffer receive();

        void send(ByteBuffer buffer);

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

        private String[] getSubstrings(String str) {
            return str.split("\\$");
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

            Runnable task = createRunnable(buffer, communicator);
            threadPool.submit(task, ThreadPool.Priority.DEFAULT);
        }

        private String byteBufferToString(ByteBuffer buffer) {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        private JSONObject byteBufferToJSONObject(ByteBuffer buffer) {
            byte[] byteArray = new byte[buffer.remaining()];
            buffer.get(byteArray);

            String jsonString = new String(byteArray, StandardCharsets.UTF_8);
            System.out.println("jsonString: " + jsonString);
            return new JSONObject(jsonString);
        }

        private ByteBuffer JSONObjectToByteBuffer(JSONObject jsonObject) {
            String jsonString = jsonObject.toString();
            byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);

            return ByteBuffer.wrap(bytes);
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

        private Runnable createRunnable(ByteBuffer buffer,
                                        Communicator communicator) {

            return new Runnable() {
                @Override
                public void run() {
                    //create JSONObject from buffer
                    JSONObject userRequest = byteBufferToJSONObject(buffer);
                    String response = null;

                    try {
                        //create entry using parser
                        Entry<String, JSONObject> entry = parse(userRequest);

                        // get command to run
                        Command command = factory.create(entry.getKey(),
                                entry.getValue());

                        // execute command
                        command.exec(mongoCRUD);

                        //add status code 200 to user request
                        userRequest.put("status", 200);

                    } catch (InvalidDataException e) {
                        //add status code 400 (bad request) to request, and a
                        // field to indicate the cause
                        userRequest.put("status", 400);
                        userRequest.put("cause", e.getMessage());

                    } finally {

                        // send response to client via Communicator
                        ByteBuffer responseBuffer =
                                JSONObjectToByteBuffer(userRequest);


                        communicator.send(responseBuffer);
                        System.out.println("data sent from gateway: " + responseBuffer);
                    }
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

    private class MultiProtocolServer {
        private final CommunicationManager communicationManger;
        private final MessageManager messageManager;

        public MultiProtocolServer() {
            this.communicationManger = new CommunicationManager();
            messageManager = new MessageManager();
        }

        public void addTCPConnection(int clientPort) throws IOException {
            this.communicationManger.addTCPConnection(clientPort);
        }

        public void addUDPConnection(int clientPort) throws IOException {
            this.communicationManger.addUDPConnection(clientPort);
        }

        public void stop() {
            this.communicationManger.stop();
        }


        public void start() {
            this.communicationManger.start();
        }

        /*==========================================================================================================*/
        /*===================================== Massage Handlers ===================================================*/

        private class MessageManager {

            public void handle(ByteBuffer receivedMessage, Communicator communicator) throws IOException, ClassNotFoundException {

                //ByteBuffer byteBuffer = communicator.receive();
                if (receivedMessage != null && receivedMessage.hasRemaining()) {

                    GatewayServer.this.handle(receivedMessage, communicator);
                }
            }
        }

        /*=========================================================================================================*/
        /*===================================== Communication Manager =============================================*/

        private class CommunicationManager {

            private final Selector selector;
            private boolean isRunning;
            private final SelectorRunner selectorRunner;


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

            public void start() {
                new Thread(this.selectorRunner).start();
            }

            public void stop() {
                this.isRunning = false;
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
                                            MultiProtocolServer.this.messageManager.handle(receivedMessage, tcpCommunicator);
                                        }
                                    } else { // UDP
                                        DatagramChannel datagramChannel = (DatagramChannel) channel;
                                        //datagramChannel.disconnect();
                                        UDPCommunicator udpCommunicator = new UDPCommunicator(datagramChannel);
                                        ByteBuffer receivedMessage = udpCommunicator.receive();
                                        MultiProtocolServer.this.messageManager.handle(receivedMessage, udpCommunicator);

                                    }
                                }
                                iterator.remove();
                            }
                        } catch (IOException | ClassNotFoundException e) {
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
                public void send(ByteBuffer buffer) {
                    try {
                        if (!clientSocketChannel.isOpen() || !clientSocketChannel.isConnected()) {
                            return;
                        }
                        buffer.limit(buffer.array().length);

                        /* String content = new String(buffer.array(), Charset.defaultCharset());
                        Entry<String,String> entry = GatewayServer.this.requestHandler.parse(content);
                        assert entry != null;
                        Message<String,String> message = new MessageImpl(entry.getKey(), entry.getValue());
                        buffer = serialize(message);*/

                        while (buffer.hasRemaining()) {
                            clientSocketChannel.write(buffer);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            private class UDPCommunicator implements Communicator {

                private final DatagramChannel clientDatagramChannel;
                private SocketAddress clientAddress; //?

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
                public void send(ByteBuffer buffer) {
                    try {
                        /*buffer.limit(buffer.array().length);
                        String content = new String(buffer.array(), Charset.defaultCharset());
                        Entry<String,String> entry = GatewayServer.this.requestHandler.parse(content);
                        assert entry != null;
                        Message<String,String> message = new MessageImpl(entry.getKey(), entry.getValue());
                        buffer = serialize(message);*/

                        this.clientDatagramChannel.send(buffer, clientAddress);
                        buffer.flip();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
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
                        //TODO what if we add a JAR that includes a Callable
                        // that doesn't return a String?

                        // create Command from the Callable
                        CallableCommand callableCommand =
                                new CallableCommand(callable);


                        // create a Function from the Command
                        Function<JSONObject, Command> func = string -> {

                            //TODO how to send the String to the callable?
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

                        //get name of newly created file
                        Path foundFile = (Path) event.context();
                        Path fullPath = watchableDir.resolve(foundFile);

                        //return path of new file as String
                        return fullPath.toString();

                        //key.reset();
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

                //open jar file
                try (JarFile jarFile = new JarFile(file)) {
                    //traverse entries of the file
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
                            //check if class implements the specified interface
                            for (Class<?> anInterface : loadedClass.getInterfaces()) {
                                if (anInterface.getName().equals(interfaceName)) {
                                    //found class that implements the interface
                                    //add class to list
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

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        GatewayServer server = new GatewayServer();

        server.addTCPConnection(8090);
        server.start();
    }
}

