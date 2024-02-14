package genericiotinfrastructure.databasemanager;

import java.util.List;
import java.util.Map;

public interface CRUD {
    // insert record to table
    void create(String key, String values);

    // read data from table
    List<Map<String, Object>> read();

    // modify record
    void update(String values);

    // delete record
    void delete(String recordID);
}
