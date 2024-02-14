package genericiotinfrastructure.crud;

import com.mongodb.client.*;
import com.mongodb.client.result.InsertOneResult;
import org.bson.Document;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class MongoCRUD {

    private final String CONNECTION_STRING = "mongodb://localhost:27017";
    private final String COMPANY_NAME_FIELD = "companyName";
    private final String USERS_COLLECTION = "Users";
    private final String PRODUCT_NAME_FIELD = "productName";
    private final String UPDATES_COLLECTION = "Updates";
    private final String STATUS_FIELD = "Operation Status";
    private final String IOT_JSON_FIELD = "iotData";
    private final String UPDATE_JSON_FIELD = "iotUpdate";

    //instances of CRUD implementations
    private final RegisterCompanyCRUDImp registerCompanyCRUDImp;
    private final RegisterProductCRUDImp registerProductCRUDImp;
    private final IotCRUDImp iotCRUDImp;
    private final UpdateCRUDImp updateCRUDImp;

    public MongoCRUD() {
        this.registerCompanyCRUDImp = new RegisterCompanyCRUDImp();
        this.registerProductCRUDImp = new RegisterProductCRUDImp();
        this.iotCRUDImp = new IotCRUDImp();
        this.updateCRUDImp = new UpdateCRUDImp();
    }

    private JSONObject addPairsToJson(JSONObject jsonObject, Map<String,
            String> pairsToAdd) {

        // Create a new JSONObject from the existing JSONObject
        JSONObject updatedJsonObject = new JSONObject(jsonObject.toString());

        // Add key-value pairs from the map
        for (Map.Entry<String, String> entry : pairsToAdd.entrySet()) {
            updatedJsonObject.put(entry.getKey(), entry.getValue());
        }

        // Return the updated JSONObject
        return updatedJsonObject;
    }

    private String getUsersCollectionName(String productName) {
        return productName + "_Users";
    }

    private String getUpdatesCollectionName(String productName) {
        return productName + "_Updates";
    }

    private JSONObject convertDocumentToJson(Document document) {
        JSONObject jsonObject = new JSONObject();

        for (Map.Entry<String, Object> entry : document.entrySet()) {
            // Convert each entry in the Document to JSON
            addJsonEntry(jsonObject, entry.getKey(), entry.getValue());
        }

        return jsonObject;
    }

    private void addJsonEntry(JSONObject jsonObject, String key,
                              Object value) {
        if (value instanceof Document) {
            // If the value is another Document, recursively convert it
            jsonObject.put(key, convertDocumentToJson((Document) value));
        } else {
            // Otherwise, add the value directly
            jsonObject.put(key, value.toString());
        }
    }

    public boolean isCompanyRegistered(JSONObject data) {
        try (MongoClient mc = MongoClients.create(CONNECTION_STRING)) {
            boolean exists = false;
            String companyName = data.getString(COMPANY_NAME_FIELD);

            for (String db : mc.listDatabaseNames()) {
                if (db.equals(companyName)) {
                    exists = true;
                    break;
                }
            }

            return exists;
        }
    }

    public boolean isProductRegistered(JSONObject data) {
        try (MongoClient mc = MongoClients.create(CONNECTION_STRING)) {
            boolean exists = false;
            String companyName = data.getString(COMPANY_NAME_FIELD);
            String productName = data.getString(PRODUCT_NAME_FIELD);

            MongoDatabase db = mc.getDatabase(companyName);

            for (String col : db.listCollectionNames()) {
                if (col.equals(productName + "_Users")) {
                    exists = true;
                    break;
                }
            }

            return exists;
        }
    }

    public boolean isIOTRegistered(JSONObject data) {
        //check if iot is in Users collection

        try (MongoClient mc = MongoClients.create(CONNECTION_STRING)) {
            boolean exists = false;

            String companyName = data.getString(COMPANY_NAME_FIELD);
            String productName = data.getString(PRODUCT_NAME_FIELD);

            MongoDatabase db = mc.getDatabase(companyName);
            MongoCollection<Document> col = db.getCollection(productName +
                    "_Users");

            //check if document is in the collection
            Document docToFind = Document.parse(data.toString());
            MongoCursor<Document> cursor = col.find(docToFind).iterator();
            if (cursor.hasNext()) {
                exists = true;
            }

            return exists;
        }
    }

    public JSONObject registerCompanyCRUD(JSONObject data) {

        return this.registerCompanyCRUDImp.createCRUD(data);
    }

    public JSONObject registerProductCRUD(JSONObject data) {


        return this.registerProductCRUDImp.createCRUD(data);
    }

    public JSONObject registerIotCRUD(JSONObject data) {
        return this.iotCRUDImp.createCRUD(data);
    }

    public JSONObject updateCRUD(JSONObject data) {
        return this.updateCRUDImp.createCRUD(data);
    }

    private class IotCRUDImp implements CRUD {

        @Override
        public JSONObject createCRUD(JSONObject data) {
            //the registerIOT request json would include company name (to
            // select database), product name (to select the collection) and
            // a nested json with the details about the iot itself. this
            // nested json will be converted to a mongodb document and
            // inserted into the relevant collection.

            try (MongoClient mc = MongoClients.create(CONNECTION_STRING)) {
                MongoDatabase db = mc.getDatabase(data.getString(
                        COMPANY_NAME_FIELD));

                //select users collection using product name
                MongoCollection<Document> usersCol =
                        db.getCollection(getUsersCollectionName(data.getString(PRODUCT_NAME_FIELD)));

                //extract iot data (nested json) from data
                JSONObject iotData = data.getJSONObject(IOT_JSON_FIELD);

                //create document from jsonobject
                Document iotDoc = Document.parse(iotData.toString());

                System.out.println(iotDoc);

                //add iot document to users collection
                InsertOneResult insertResult = usersCol.insertOne(iotDoc);

                //create a document that includes the given data as well
                // as the id of the document and return it

                Map<String, String> pairs = new HashMap<>();
                pairs.put("_id",
                        insertResult.getInsertedId().asObjectId().getValue().toString());

                return addPairsToJson(data, pairs);
            }
        }

        @Override
        public JSONObject readCRUD(JSONObject data) {
            return null;
            //read what?
        }

        @Override
        public JSONObject updateCRUD(JSONObject data) {
            return null;
            //not needed
        }

        @Override
        public JSONObject deleteCRUD(JSONObject data) {

            //used to remove device. remove document from users collection

            try (MongoClient mc = MongoClients.create(CONNECTION_STRING)) {
                MongoDatabase db = mc.getDatabase(data.getString(
                        COMPANY_NAME_FIELD));

                //select users collection using product name
                MongoCollection<Document> usersCol =
                        db.getCollection(getUsersCollectionName(data.getString(PRODUCT_NAME_FIELD)));

                //extract iot data (nested json) from data
                JSONObject iotData = data.getJSONObject(IOT_JSON_FIELD);

                //create document from jsonobject
                Document iotDoc = Document.parse(iotData.toString());

                //find and remove iot document from users collection
                usersCol.findOneAndDelete(iotDoc);

                return null;
            }
        }
    }

    private class UpdateCRUDImp implements CRUD {

        @Override
        public JSONObject createCRUD(JSONObject data) {
            //add update to database
            try (MongoClient mc = MongoClients.create(CONNECTION_STRING)) {
                MongoDatabase db = mc.getDatabase(data.getString(
                        COMPANY_NAME_FIELD));

                //select updates collection using product name
                MongoCollection<Document> updatesCol =
                        db.getCollection(getUpdatesCollectionName(data.getString(PRODUCT_NAME_FIELD)));

                //extract update data (nested json) from data
                JSONObject updateData = data.getJSONObject(UPDATE_JSON_FIELD);

                //create document from updateData
                Document updateDoc = Document.parse(updateData.toString());

                //add updateDoc to Updates collection
                InsertOneResult insertResult = updatesCol.insertOne(updateDoc);

                //create a document that includes the given data as well
                // as the id of the document and return it

                Map<String, String> pairs = new HashMap<>();
                pairs.put("_id",
                        insertResult.getInsertedId().asObjectId().getValue().toString());

                return addPairsToJson(data, pairs);
            }
        }

        @Override
        public JSONObject readCRUD(JSONObject data) {
            //read specific update

            try (MongoClient mc = MongoClients.create(CONNECTION_STRING)) {
                MongoDatabase db = mc.getDatabase(data.getString(
                        COMPANY_NAME_FIELD));

                //select updates collection using product name
                MongoCollection<Document> updatesCol =
                        db.getCollection(getUpdatesCollectionName(data.getString(PRODUCT_NAME_FIELD)));

                //extract iot data (nested json) from data
                JSONObject updateData = data.getJSONObject(UPDATE_JSON_FIELD);

                //create document from jsonobject
                Document updateDoc = Document.parse(updateData.toString());

                //find first document that matches search criteria
                Document res = updatesCol.find(updateDoc).first();

                //convert document to jsonobject and return it

                return convertDocumentToJson(res);
            }
        }

        @Override
        public JSONObject updateCRUD(JSONObject data) {
            return null;
            //not needed
        }

        @Override
        public JSONObject deleteCRUD(JSONObject data) {
            //delete update

            try (MongoClient mc = MongoClients.create(CONNECTION_STRING)) {
                MongoDatabase db = mc.getDatabase(data.getString(
                        COMPANY_NAME_FIELD));

                //select users collection using product name
                MongoCollection<Document> updatesCol =
                        db.getCollection(getUpdatesCollectionName(data.getString(PRODUCT_NAME_FIELD)));

                //extract update data (nested json) from data
                JSONObject iotData = data.getJSONObject(UPDATE_JSON_FIELD);

                //create document from jsonobject
                Document updateDoc = Document.parse(iotData.toString());

                //find and remove iot document from users collection
                updatesCol.findOneAndDelete(updateDoc);

                return null;
            }
        }
    }

    private class RegisterCompanyCRUDImp implements CRUD {

        @Override
        public JSONObject createCRUD(JSONObject data) {

            //create database for company
            try (MongoClient mc = MongoClients.create(CONNECTION_STRING)) {
                MongoDatabase db = mc.getDatabase(data.getString(
                        COMPANY_NAME_FIELD));

                //add dummy Collection with a dummy document, otherwise the
                // database is not created
                db.getCollection("dummy").insertOne(new Document().append(
                        "dummy", "dummy"));
            }

            Map<String, String> newPairs = new HashMap<>();
            newPairs.put(STATUS_FIELD, "Success");

            return addPairsToJson(data, newPairs);
        }

        @Override
        public JSONObject readCRUD(JSONObject data) {
            return null;
            //not needed
        }

        @Override
        public JSONObject updateCRUD(JSONObject data) {
            return null;
            //not needed
        }

        @Override
        public JSONObject deleteCRUD(JSONObject data) {
            //deletes the company db, in case the company de-registers
            try (MongoClient mc = MongoClients.create(CONNECTION_STRING)) {
                MongoDatabase db = mc.getDatabase(COMPANY_NAME_FIELD);
                db.drop();
            }

            Map<String, String> newPairs = new HashMap<>();
            newPairs.put(STATUS_FIELD, "Success");
            return addPairsToJson(data, newPairs);
        }
    }

    private class RegisterProductCRUDImp implements CRUD {

        @Override
        public JSONObject createCRUD(JSONObject data) {
            //create collections for the company - 'users' and 'updates'
            try (MongoClient mc = MongoClients.create(CONNECTION_STRING)) {
                MongoDatabase db = mc.getDatabase(data.getString(
                        COMPANY_NAME_FIELD));

                db.getCollection(getUsersCollectionName(data.getString(PRODUCT_NAME_FIELD))).insertOne(new Document().append("dummy", "dummy"));
                db.getCollection(getUpdatesCollectionName(data.getString(PRODUCT_NAME_FIELD))).insertOne(new Document().append("dummy", "dummy"));
            }

            Map<String, String> newPairs = new HashMap<>();
            newPairs.put(STATUS_FIELD, "Success");
            return addPairsToJson(data, newPairs);
        }

        @Override
        public JSONObject readCRUD(JSONObject data) {
            return null;
            //not needed
        }

        @Override
        public JSONObject updateCRUD(JSONObject data) {
            return null;
            //not needed
        }

        @Override
        public JSONObject deleteCRUD(JSONObject data) {

            //delete the collections for this product (remove product)
            try (MongoClient mc = MongoClients.create(CONNECTION_STRING)) {
                MongoDatabase db = mc.getDatabase(data.getString(
                        COMPANY_NAME_FIELD));

                String usersCollection =
                        data.getString(PRODUCT_NAME_FIELD) + "_" +
                                USERS_COLLECTION;
                String updatesCollection =
                        data.getString(PRODUCT_NAME_FIELD) + "_" +
                                UPDATES_COLLECTION;

                db.getCollection(usersCollection).drop();
                db.getCollection(updatesCollection).drop();
            }

            Map<String, String> newPairs = new HashMap<>();
            newPairs.put(STATUS_FIELD, "Success");
            return addPairsToJson(data, newPairs);
        }
    }
}
