package backend;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.json.JSONObject;

public class GatewayRequest {
	public static void sendRequestToGateway(String request, JSONObject data) throws IOException{
		System.out.println("in sendRequestToGateway");
		
		//create JSONObject for request
		JSONObject requestJsonObject = new JSONObject();

		requestJsonObject.put("request", request);
		requestJsonObject.put("data", data);
		
		System.out.println(requestJsonObject.toString());
		

		try {
			// Create a SocketChannel object.
			SocketChannel socketChannel = SocketChannel.open();

			// Connect the SocketChannel to the server.
			InetSocketAddress serverAddress = new InetSocketAddress("localhost", 8090);
			socketChannel.connect(serverAddress);

			// Create a ByteBuffer object to store the data you want to send.
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);

			// Write data to the ByteBuffer object
			byteBuffer.put(requestJsonObject.toString().getBytes());
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
}
