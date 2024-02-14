package genericiotinfrastructure;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class GatewayTCPClient implements Runnable {

    public void run() {
        try {
            // Create a SocketChannel object.
            SocketChannel socketChannel = SocketChannel.open();

            // Connect the SocketChannel to the server.
            InetSocketAddress serverAddress = new InetSocketAddress("localhost", 8080);
            socketChannel.connect(serverAddress);

            // Create a ByteBuffer object to store the data you want to send.
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
            //CompanyID$CompanyName$address$contactName$contactPhone
            // $contactEmail$serviceFee$cardNumber$cardExpDate$cardCVV
            String regComp =
                    "RegisterCompany$123456$Electra$someAddress$Yossi$0501234567" +
                            "$yossi@electra.com$500$4580123456781234$10/25$123";

            String regProduct = "RegisterProduct$123456$Electra" +
                    "$someProductName$someProductDescription$someProductTechInfo";

            String regIoT =
                    "RegisterIoT$123456$Electra$someProductName$00D0B063C226$192168101137$Shmulik$Kipod$shmulik@gmail.com$0501234685$someUserAddress$203123456";


            String updateIoT =
                    "Update$123456$Electra$00D0B063C226$someTypeOfUpdate$someUpdate";

            // Write data to the ByteBuffer object
            byteBuffer.put(updateIoT.getBytes());
            byteBuffer.flip();

            // Write (send) the ByteBuffer to the socket
            socketChannel.write(byteBuffer);

            // Create ByteBuffer to store the response from the server
            ByteBuffer responseBuffer = ByteBuffer.allocateDirect(1024);

            int bytesRead = socketChannel.read(responseBuffer);
            responseBuffer.flip();

            // check how many bytes were read (in case the server
            // disconnected)
            if (bytesRead > 0) {
                byte[] responseMessage = new byte[bytesRead];
                responseBuffer.get(responseMessage);
                String response = new String(responseMessage);
                System.out.println("Response from server: " + response);
            } else {
                System.out.println("No response from server");
            }


            try {
                socketChannel.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {

        new Thread(new GatewayTCPClient()).start();

    }
}


