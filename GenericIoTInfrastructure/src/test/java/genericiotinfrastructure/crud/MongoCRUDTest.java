package genericiotinfrastructure.crud;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MongoCRUDTest {

    private MongoCRUD crud;

    @BeforeEach
    public void init() {
        crud = new MongoCRUD();
    }

    @Test
    void registerCompanyCRUD() {
        //create json
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("companyName", "Tadiran");

        crud.registerCompanyCRUD(jsonObject);
    }

    @Test
    void registerProductCRUD() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("companyName", "Tadiran");
        jsonObject.put("productName", "AC-130");

        crud.registerProductCRUD(jsonObject);
    }

    @Test
    void registerIotCRUD() {
        //create the inner (nested) json for the iot
        JSONObject iotJson = new JSONObject();
        iotJson.put("Serial Number", "12345");
        iotJson.put("User name", "Shmulik");
        iotJson.put("Email", "potato@gmail.com");

        //create json
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("companyName", "Tadiran");
        jsonObject.put("productName", "AC-130");
        jsonObject.put("iotData", iotJson);

        crud.registerIotCRUD(jsonObject);
    }

    @Test
    void updateCRUD() {
        //create inner json for the update
        JSONObject updateJson = new JSONObject();
        updateJson.put("someUpdateField", "someData");
        updateJson.put("Turned on", System.currentTimeMillis());

        //create outer json
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("companyName", "Tadiran");
        jsonObject.put("productName", "AC-130");
        jsonObject.put("iotUpdate", updateJson);

        crud.updateCRUD(jsonObject);
    }
}