package genericiotinfrastructure.databasemanager;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminCRUD implements CRUD {
    Map<String, Create> creators;

    private final String url;
    private final String user;
    private final String password;
    private final String adminDBName;

    //create two implementations of create, one for RegisterCompany and
    // onother for RegisterProduct

    public AdminCRUD(String adminDBName, String url, String user,
                     String password) {
        this.adminDBName = adminDBName;
        this.url = url;
        this.user = user;
        this.password = password;

        creators = new HashMap<>();
        creators.put("RegisterCompany", new RegisterCompanyCreator());
        creators.put("RegisterProduct", new RegisterProductCreator());
    }

    public void createAdminDB() {

        //check if db exists
        if (checkIfDBExists(adminDBName)) {
            return;
        }

        try (Connection connection = DriverManager.getConnection(url, user,
                password);
             Statement statement = connection.createStatement()
        ) {

            //create db
            statement.execute("CREATE DATABASE " + adminDBName);

            //select db
            statement.execute("USE " + adminDBName);

            //create tables
            createAdminDBTables(statement);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkIfDBExists(String dbName) {
        try (Connection connection = DriverManager.getConnection(url, user,
                password)
        ) {

            // get db metadata
            DatabaseMetaData databaseMetaData = connection.getMetaData();

            // Get a list of all the databases on the database server.
            ResultSet catalogs = databaseMetaData.getCatalogs();

            // Check if the database name that you are looking for is in the list.
            boolean databaseExists = false;
            while (catalogs.next()) {
                if (catalogs.getString("TABLE_CAT").equals(dbName)) {
                    databaseExists = true;
                    break;
                }
            }

            return databaseExists;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createAdminDBTables(Statement statement) throws SQLException {
        //connect to database

        statement.execute("USE " + adminDBName);

        String query = "CREATE TABLE IF NOT EXISTS CreditCardDetails(" +
                "    payment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    company_id BIGINT UNSIGNED NOT NULL," +
                "    card_number VARCHAR(255) NOT NULL," +
                "    ex_date VARCHAR(255) NOT NULL," +
                "    CVV VARCHAR(3) NOT NULL" +
                ");";

        statement.execute(query);

        query = "CREATE TABLE IF NOT EXISTS Products(" +
                "    product_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    company_id BIGINT UNSIGNED NOT NULL," +
                "    product_name VARCHAR(255) NOT NULL," +
                "    product_description TEXT NOT NULL" +
                ");";

        statement.execute(query);

        query = "CREATE TABLE IF NOT EXISTS Companies(" +
                "    company_id BIGINT UNSIGNED NOT NULL PRIMARY KEY," +
                "    company_name VARCHAR(255) NOT NULL," +
                "    company_address VARCHAR(255) NOT NULL," +
                "    contact_name VARCHAR(255) NOT NULL," +
                "    contact_phone VARCHAR(255) NOT NULL," +
                "    contact_email VARCHAR(255) NOT NULL," +
                "    service_fee BIGINT NOT NULL" +
                ");";

        statement.execute(query);
        //TODO if table already exists with constraints, i get an error.
        // check if the foreign key can be included in the table creation
        // query instead. this applies to both ALTER TABLE statements that
        // follow.

        query = "ALTER TABLE" +
                "    Products ADD CONSTRAINT products_company_id_foreign " +
                "FOREIGN KEY(company_id) REFERENCES Companies(company_id);";

        statement.execute(query);

        query = "ALTER TABLE" +
                "    CreditCardDetails ADD CONSTRAINT " +
                "creditcarddetails_company_id_foreign FOREIGN KEY(company_id) " +
                "REFERENCES Companies(company_id);";

        statement.execute(query);
    }

    @Override
    public void create(String key, String values) {
        //used either for RegisterCompany or RegisterProduct
        creators.get(key).create(values);
    }

    @Override
    public List<Map<String, Object>> read() {
        return null;
    }

    @Override
    public void update(String values) {

    }

    @Override
    public void delete(String recordID) {

    }

    private class RegisterCompanyCreator implements Create {

        @Override
        public void create(String values) {
            //perform all actions needed to register company
            //connect to database;
            try (Connection connection = DriverManager.getConnection(url, user,
                    password);
                 Statement statement = connection.createStatement()
            ) {
                //create CompanyDetails object
                CompanyDetails companyDetails = getCompanyDetails(values);

                // USE adminDB
                statement.execute("USE " + adminDBName);

                //add company to Companies table.
                String query = "INSERT INTO Companies (company_id, company_name, " +
                        "company_address, contact_name, contact_phone, " +
                        "contact_email, service_fee) VALUES (?, ?, ?, ?, ?, ?, ?)";
                //int, string, string, string, string, string, int
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setLong(1, companyDetails.getCompanyID());
                preparedStatement.setString(2, companyDetails.getCompanyName());
                preparedStatement.setString(3, companyDetails.getAddress());
                preparedStatement.setString(4, companyDetails.getContactName());
                preparedStatement.setString(5, companyDetails.getContactPhone());
                preparedStatement.setString(6, companyDetails.getContactEmail());
                preparedStatement.setInt(7, companyDetails.getServiceFee());

                preparedStatement.executeUpdate();
                preparedStatement.close();

                //add payment info to CreditCardDetails table
                addPaymentInfo(connection, companyDetails);

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        private void addPaymentInfo(Connection connection,
                                    CompanyDetails companyDetails)
                throws SQLException {

            String query = "INSERT INTO CreditCardDetails (company_id, " +
                    "card_number, ex_date, CVV) VALUES (?, ?, ?, ?)";

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setLong(1, companyDetails.getCompanyID());
            statement.setString(2, companyDetails.getCardNumber());
            statement.setString(3, companyDetails.getCardExpDate());
            statement.setInt(4, companyDetails.getCardCVV());

            statement.executeUpdate();
            statement.close();
        }

        private CompanyDetails getCompanyDetails(String values) {
            String[] strings = values.split("\\$");
            // example of 'values' string:
            //CompanyID$CompanyName$address$contactName$contactPhone$contactEmail$serviceFee$cardNumber$cardExpDate$cardCVV

            return new CompanyDetails(Integer.parseInt(strings[0]), strings[1], strings[2],
                    strings[3], strings[4], strings[5], Integer.parseInt(strings[6]),
                    strings[7], strings[8], Integer.parseInt(strings[9]));
        }

    }

    private class RegisterProductCreator implements Create {

        @Override
        public void create(String values) {
            //add product to Products table in the admin db
            try (Connection connection = DriverManager.getConnection(url, user
                    , password);
                 Statement statement = connection.createStatement()
            ) {
                //select admin db
                statement.execute("USE " + adminDBName);

                //create ProductDetails object
                ProductDetails productDetails = getProductDetails(values);

                // add product to Products table of adminDB
                addProductToAdminDB(connection, productDetails);

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private ProductDetails getProductDetails(String values) {
        String[] strings = values.split("\\$");
        // example of values: companyID$companyName$productName$productDescription$productTechInfo

        return new ProductDetails(strings[2], strings[1], Integer.parseInt(strings[0]),
                strings[3], strings[4]);
    }

    private void addProductToAdminDB(Connection connection,
                                     ProductDetails productDetails)
            throws SQLException {

        String query = "INSERT INTO Products (company_id, product_name, " +
                "product_description) VALUES (?, ?, ?)";
        // int, string, string
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setLong(1, productDetails.getCompanyID());
        statement.setString(2, productDetails.getProductName());
        statement.setString(3, productDetails.getProductDescription());

        statement.executeUpdate();
        statement.close();
    }
}
