package backend;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

@WebServlet("/products")
public class ProductServlet extends HttpServlet{

	private SQLCRUD sqlCrud;

	@Override
	public void init(ServletConfig config) throws ServletException{
		ServletContext context = config.getServletContext();

		sqlCrud = (SQLCRUD) context.getAttribute("sqlCrud");

		if(sqlCrud == null) {
			throw new ServletException("could not load sqlCrud from context");
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{


		//Extract JsonObject from request
		JSONObject productData = getJsonObjectFromRequest(request);

		System.out.println(productData.toString());

		if(!sqlCrud.isCompanyRegistered(productData.getString("companyName"))){
			//send response to website

			sendResponse(response, "This company is not registered");

			return;
		}

		if(sqlCrud.isProductRegistered(productData.getString("companyName"), productData.getString("productName"))) {
			sendResponse(response, "This product is already registered");

			return;
		}

		//register product in admin database
		sqlCrud.registerProduct(productData);

		//send request to GatewayServer //TODO add aux func for product registration in gateway
		JSONObject requestData = new JSONObject();
		requestData.put("companyName", productData.getString("companyName"));
		requestData.put("productName", productData.getString("productName"));
		
		GatewayRequest.sendRequestToGateway("RegisterProduct", requestData);

		//send response to website
		sendResponse(response, "Product successfully registered");

	}

	private void sendResponse(HttpServletResponse response, String message) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("message", message);

		response.setContentType("application/json");
		try {
			response.getWriter().write(jsonObject.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private JSONObject getJsonObjectFromRequest(HttpServletRequest request) throws IOException {
		InputStream inputStream = request.getInputStream();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			StringBuilder stringBuilder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
			}

			return new JSONObject(stringBuilder.toString());
		}
	}
}
