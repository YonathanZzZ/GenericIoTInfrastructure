package genericiotinfrastructure.crud;

import org.json.JSONObject;

public interface CRUD {

    JSONObject createCRUD(JSONObject data);

    JSONObject readCRUD(JSONObject data);

    JSONObject updateCRUD(JSONObject data);

    JSONObject deleteCRUD(JSONObject data);

}
