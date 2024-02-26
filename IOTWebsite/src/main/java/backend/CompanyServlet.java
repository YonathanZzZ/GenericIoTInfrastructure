package backend;

import org.json.JSONObject;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@WebServlet("/companies")
public class CompanyServlet extends HttpServlet{

	private SQLCRUD sqlCrud;

	@Override
	public void init(ServletConfig config) throws ServletException{
		ServletContext context = config.getServletContext();

		sqlCrud = (SQLCRUD) context.getAttribute("sqlCrud");

		if(sqlCrud == null) {
			throw new ServletException("could not load sqlCrud from context");
		}
	}

	private JSONObject addServiceFee(JSONObject companyData) {

		//add arbitrary fee
		companyData.put("serviceFee", 500);

		return companyData;
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{


		//Extract JsonObject from request
		JSONObject companyData = getJsonObjectFromRequest(request);

		System.out.println(companyData.toString());

		//add arbitrary service fee to companyData
		companyData = addServiceFee(companyData);

		System.out.println(companyData.toString());

		if(sqlCrud.isCompanyRegistered(companyData.getString("companyName"))){
			//send response to website

			sendResponse(response, "A company by this name is already registered");

			return;
		}

		//register company in admin database
		sqlCrud.registerCompany(companyData);

		//send request to GatewayServer
		JSONObject requestData = new JSONObject();
		requestData.put("companyName", companyData.getString("companyName"));

		try{
			GatewayRequest.sendRequestToGateway("RegisterCompany", requestData);
		}catch (Exception e) {
			sendResponse(response, "Failed to register company due to internal error. Please try again later.");
			
			//TODO company was already registered in adminDB. remove it 
			
			return;
		}

		//send response to website
		sendResponse(response, "Company successfully registered");
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
