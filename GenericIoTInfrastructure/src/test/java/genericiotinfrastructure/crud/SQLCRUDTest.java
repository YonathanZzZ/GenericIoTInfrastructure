package genericiotinfrastructure.crud;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

class SQLCRUDTest {

    private SQLCRUD crud;

    @BeforeEach
    public void init() throws SQLException {
        crud = new SQLCRUD("AdminDB", "jdbc:mysql://localhost:3306", "root",
                "root");
    }

    @Test
    void registerCompany() {
        //create json
        JSONObject request = new JSONObject();

        request.put("Company Name", "Electra");
        request.put("Company Address", "Tel Aviv");
        request.put("Contact Name", "Shmulik");
        request.put("Contact Phone", "031234567");
        request.put("Contact Email", "shmulik@electra.com");
        request.put("Service Fee", 500);
        request.put("Card Number", "458012345678");
        request.put("Card Holder Name", "Yossi");
        request.put("Expiration Data", "10/28");
        request.put("CVV", "123");


        crud.registerCompany(request);
    }

    @Test
    void registerProduct() {
        JSONObject request = new JSONObject();

        request.put("Company Name", "Electra");
        request.put("Product Name", "AC-130");
        request.put("Product Description", "this is a very nice ac");

        crud.registerProduct(request);
    }
}