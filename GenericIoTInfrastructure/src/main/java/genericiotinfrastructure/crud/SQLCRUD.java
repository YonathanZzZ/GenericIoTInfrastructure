package genericiotinfrastructure.crud;

import org.json.JSONObject;

import java.sql.*;

public class SQLCRUD {

    private final String URL;
    private final String user;
    private final String password;
    private final String dbName;

    //json fields
    private final String COMPANY_NAME_FIELD = "Company Name";
    private final String COMPANY_ADDRESS_FIELD = "Company Address";
    private final String CONTACT_NAME_FIELD = "Contact Name";
    private final String CONTACT_PHONE_FIELD = "Contact Phone";
    private final String CONTACT_EMAIL_FIELD = "Contact Email";
    private final String SERVICE_FEE_FIELD = "Service Fee";
    private final String CARD_NUMBER_FIELD = "Card Number";
    private final String CARD_HOLDER_FIELD = "Card Holder";
    private final String EXP_DATE_FIELD = "Expiration Date";
    private final String CVV_FIELD = "CVV";

    private final RegisterCompanyCRUDImpl registerCompanyCRUD;
    private final RegisterProductCRUDImpl registerProductCRUD;

    public SQLCRUD(String dbName, String URL, String user, String password) throws SQLException {
        this.dbName = dbName;
        this.URL = URL;
        this.user = user;
        this.password = password;

        this.registerCompanyCRUD = new RegisterCompanyCRUDImpl();
        this.registerProductCRUD = new RegisterProductCRUDImpl();

        //create AdminDB
        createAdminDB();
    }

    private void createAdminDBTables(Statement statement) throws SQLException {


        statement.execute("USE " + dbName);

        // Create Companies table
        String query = "CREATE TABLE IF NOT EXISTS Companies(" +
                "    company_name VARCHAR(255) NOT NULL PRIMARY KEY," +
                "    company_address VARCHAR(255) NOT NULL," +
                "    contact_name VARCHAR(255) NOT NULL," +
                "    contact_phone VARCHAR(255) NOT NULL," +
                "    contact_email VARCHAR(255) NOT NULL," +
                "    service_fee BIGINT NOT NULL" +
                ");";

        statement.execute(query);

        // Create CreditCardDetails table
        query = "CREATE TABLE IF NOT EXISTS CreditCardDetails(" +
                "    card_number VARCHAR(255) NOT NULL PRIMARY KEY," +
                "    company_name VARCHAR(255) NOT NULL," +
                "    card_holder_name VARCHAR(255)," +
                "    ex_date VARCHAR(255) NOT NULL," +
                "    CVV VARCHAR(3) NOT NULL," +
                "    CONSTRAINT creditcarddetails_company_name_foreign FOREIGN KEY (company_name) REFERENCES Companies(company_name)" +
                ");";

        statement.execute(query);

        // Create Products table
        query = "CREATE TABLE IF NOT EXISTS Products(" +
                "    product_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    company_name VARCHAR(255) NOT NULL," +
                "    product_name VARCHAR(255) NOT NULL," +
                "    product_description TEXT NOT NULL," +
                "    CONSTRAINT products_company_name_foreign FOREIGN KEY (company_name) REFERENCES Companies(company_name)" +
                ");";

        statement.execute(query);
    }

    private void createAdminDB() throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, user,
                password)) {
            Statement statement = connection.createStatement();

            String query = String.format("CREATE DATABASE IF NOT EXISTS %s",
                    dbName);

            statement.execute(query);

            createAdminDBTables(statement);

        } catch (SQLException e) {
            throw new SQLException("failed to create database");
        }
    }

    public void registerCompany(JSONObject data) {
        registerCompanyCRUD.createCRUD(data);
    }

    public void registerProduct(JSONObject data) {
        registerProductCRUD.createCRUD(data);
    }

    private class RegisterCompanyCRUDImpl implements CRUD {

        @Override
        public JSONObject createCRUD(JSONObject data) {
            //add company to Companies table and its payment details to
            // CreditCardDetails

            try (Connection connection = DriverManager.getConnection(URL, user
                    , password)) {

                Statement statement = connection.createStatement();

                statement.execute("use " + dbName);

                String query = "INSERT INTO Companies (company_name, " +
                        "company_address, contact_name, contact_phone, " +
                        "contact_email, service_fee) VALUES (?, ?, ?, ?, ?, ?)";

                PreparedStatement preparedStatement =
                        connection.prepareStatement(query);

                preparedStatement.setString(1, data.getString("Company Name"));

                preparedStatement.setString(2, data.getString("Company " +
                        "Address"));
                preparedStatement.setString(3, data.getString("Contact Name"));
                preparedStatement.setString(4,
                        data.getString("Contact Phone"));
                preparedStatement.setString(5,
                        data.getString("Contact Email"));
                preparedStatement.setInt(6, data.getInt("Service Fee"));

                preparedStatement.executeUpdate();
                preparedStatement.close();

                //add payment details to CreditCardDetails table
                addPaymentInfo(connection, data);

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return null; //TODO fix return value
        }

        private void addPaymentInfo(Connection connection,
                                    JSONObject data)
                throws SQLException {

            String query = "INSERT INTO CreditCardDetails (card_number, " +
                    "company_name, card_holder_name, ex_date, CVV) " +
                    "VALUES ( ?, ?, ?, ?, ?)";

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, data.getString("Card Number"));
            statement.setString(2, data.getString("Company Name"));
            statement.setString(3, data.getString("Card Holder Name"));
            statement.setString(4, data.getString("Expiration Data"));
            statement.setString(5, data.getString("CVV"));

            statement.executeUpdate();
            statement.close();
        }

        @Override
        public JSONObject readCRUD(JSONObject data) {
            return null;

        }

        @Override
        public JSONObject updateCRUD(JSONObject data) {
            return null;
        }

        @Override
        public JSONObject deleteCRUD(JSONObject data) {
            return null;
        }
    }

    private class RegisterProductCRUDImpl implements CRUD {

        @Override
        public JSONObject createCRUD(JSONObject data) {
            //add product to Products table
            try (Connection connection = DriverManager.getConnection(URL, user
                    , password);
                 Statement statement = connection.createStatement()
            ) {
                //select admin db
                statement.execute("USE " + dbName);

                // add product to Products table
                addProductToAdminDB(connection, data);

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return null; //TODO fix return value
        }

        private void addProductToAdminDB(Connection connection,
                                         JSONObject data)
                throws SQLException {

            String query = "INSERT INTO Products (company_name, product_name," +
                    " product_description) VALUES (?, ?, ?)";

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, data.getString("Company Name"));
            statement.setString(2, data.getString("Product Name"));
            statement.setString(3, data.getString("Product Description"));

            statement.executeUpdate();
            statement.close();
        }

        @Override
        public JSONObject readCRUD(JSONObject data) {
            return null;
        }

        @Override
        public JSONObject updateCRUD(JSONObject data) {
            return null;
        }

        @Override
        public JSONObject deleteCRUD(JSONObject data) {
            return null;
        }
    }
}
