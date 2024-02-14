package genericiotinfrastructure.databasemanager;

import java.sql.*;

public class DBManagerForBothDBs {

    private final String url;
    private final String user;
    private final String password;
    private final String adminDBName;

    public DBManagerForBothDBs(String url, String user,
                               String password) {
        this.url = url;
        this.adminDBName = "AdminDB";
        this.user = user;
        this.password = password;
    }

    public void createAdminDB() {
        String query = "CREATE DATABASE IF NOT EXISTS " + adminDBName;

        try (Connection connection = DriverManager.getConnection(url, user,
                password);
             Statement statement = connection.createStatement()
        ) {
            statement.execute(query);

            createAdminDBTables(statement);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createAdminDBTables(Statement statement) throws SQLException {
        //connect to database

        statement.execute("USE " + adminDBName);

        String query = "CREATE TABLE IF NOT EXISTS CreditCardDetails(" +
                "    payment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    company_id INT UNSIGNED NOT NULL," +
                "    card_number VARCHAR(255) NOT NULL," +
                "    ex_date VARCHAR(255) NOT NULL," +
                "    CVV VARCHAR(3) NOT NULL" +
                ");";

        statement.execute(query);

        query = "CREATE TABLE IF NOT EXISTS Products(" +
                "    product_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    company_id INT UNSIGNED NOT NULL," +
                "    product_name VARCHAR(255) NOT NULL," +
                "    product_description TEXT NOT NULL" +
                ");";

        statement.execute(query);

        query = "CREATE TABLE IF NOT EXISTS Companies(" +
                "    company_id INT UNSIGNED NOT NULL PRIMARY KEY," +
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

    private void insertRecord(Connection connection,
                              String tableName,
                              String attributes, String values) throws SQLException {

        String query =
                "INSERT INTO " + tableName + " (" + attributes + ")" +
                        " VALUES " + "(" + values + ");";
        System.out.println("query: " + query);

        PreparedStatement preparedStatement = connection.prepareStatement(query);

        // Set the values of the placeholders in the SQL query.
        preparedStatement.setString(1, values);

        // Execute the PreparedStatement object.
        preparedStatement.executeUpdate();

        // Close the PreparedStatement object.
        preparedStatement.close();
    }

    public void registerCompany(CompanyDetails companyDetails) {

        // add record to Companies table of AdminDB;
        try (Connection connection = DriverManager.getConnection(url, user,
                password);
             Statement statement = connection.createStatement()
        ) {
            //select admin DB
            statement.execute("USE " + adminDBName);
            //add record to Companies table

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

            //create company DB
            createCompanyDB(statement, companyDetails);

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

    private void createCompanyDB(Statement statement, CompanyDetails companyDetails)
            throws SQLException {
        //create company - specific DB

        statement.execute("create database " + companyDetails.getCompanyDBName());
        System.out.println("after creating db named " + companyDetails.getCompanyDBName());
        //select company DB
        statement.execute("use " + companyDetails.getCompanyDBName());
        System.out.println("after selecting db named " + companyDetails.getCompanyDBName());
        //add tables to company DB
        addCompanyDBTables(statement);
    }

    private void addCompanyDBTables(Statement statement) throws SQLException {
        System.out.println("adding tables to company db");
        //create user info table
        String query = "CREATE TABLE Userinfo(ID_number INT NOT NULL " +
                "PRIMARY KEY, " +
                "first_name VARCHAR(255) NOT NULL, " +
                "last_name VARCHAR(255) NOT NULL, " +
                "address VARCHAR(255) NOT NULL, " +
                "email VARCHAR(255) NOT NULL, " +
                "phone VARCHAR(255) NOT NULL)";

        statement.execute(query);

        System.out.println("after creating table UserInfo");

        //create Products table. this table includes all the products that
        // this company supports
        query = "CREATE TABLE Products(" +
                "    product_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    product_name VARCHAR(255) NOT NULL," +
                "    product_tech_info VARCHAR(255) NOT NULL," +
                "    registration_time TIMESTAMP NOT NULL" +
                ")";

        statement.execute(query);

        System.out.println("after creating table Products");

        //create UserProducts table. this table is used to find the products
        // belonging to each registered user, as it includes a userID field.
        // it's one table for all the users.

        query = "CREATE TABLE UserProduct(" +
                "    UserProduct_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    ID_number INT NOT NULL," +
                "    product_id BIGINT NOT NULL," +
                "    MAC_address BIGINT NOT NULL" +
                ")";

        statement.execute(query);
        System.out.println("after creating table UserProduct");

        //add foreign key
        query = "ALTER TABLE UserProduct ADD CONSTRAINT " +
                "userproduct_ID_number_foreign " +
                "FOREIGN KEY(ID_number) REFERENCES Userinfo(ID_number)";

        statement.execute(query);
        System.out.println("after adding constraints");
    }

    public void registerProduct(ProductDetails productDetails) {

        try (Connection connection = DriverManager.getConnection(url, user,
                password);
             Statement statement = connection.createStatement()
        ) {

            // access admin DB
            statement.execute("USE " + adminDBName);

            // add product to Products table of adminDB
            addProductToAdminDB(connection, productDetails);

            // access company DB
            statement.execute("USE " + productDetails.getCompanyDBName());

            // add product to Products table of company - specific DB
            addProductToCompanyDB(connection, productDetails);

            // create table 'IoTs'
            createTablesForNewProduct(statement);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createTablesForNewProduct(Statement statement) throws SQLException {

        String query = "CREATE TABLE IoTs(" +
                "    MAC_address BIGINT UNSIGNED NOT NULL PRIMARY," +
                "    registration_time TIMESTAMP NOT NULL," +
                "    product_id BIGINT NOT NULL," +
                "    IP_address BIGINT NOT NULL" +
                ");";

        statement.execute(query);

        query = "CREATE TABLE Updates(" +
                "    update_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    time TIMESTAMP NOT NULL," +
                "    type VARCHAR(255) NOT NULL," +
                "    MAC_address BIGINT NOT NULL," +
                "    content VARCHAR(255) NOT NULL" +
                ");";

        statement.execute(query);

        // add relations
        query = "ALTER TABLE" +
                "    `IoTs` ADD CONSTRAINT `iots_product_id_foreign` FOREIGN " +
                "KEY(`product_id`) REFERENCES `Products`(`product_id`)";

        statement.execute(query);

        query = "ALTER TABLE" +
                "    `Updates` ADD CONSTRAINT `updates_mac_address_foreign` " +
                "FOREIGN KEY(`MAC_address`) REFERENCES `IoTs`(`MAC_address`)";

        statement.execute(query);
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

    private void addProductToCompanyDB(Connection connection, ProductDetails productDetails) throws SQLException {

        String query = "INSERT INTO Products (product_name, product_tech_info, " +
                "registration_time) VALUES (?, ?, ?)";
        // string, string, timestamp
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, productDetails.getProductName());
        statement.setString(2, productDetails.getProductTechInfo());
        statement.setString(3, "NOW");

        statement.executeUpdate();
        statement.close();
    }

    public void registerIoT(DeviceDetails deviceDetails) {


        try (Connection connection = DriverManager.getConnection(url, user,
                password);
             Statement statement = connection.createStatement()
        ) {

            // access company DB
            statement.execute("USE " + deviceDetails.getCompanyDBName());

            // add user info to UserInfo table
            addUserInfoToCompanyDB(connection, deviceDetails);

            // add device to UserProduct table
            addDeviceToUserProductTable(connection,
                    deviceDetails);

            // add info about device to IoTs table
            addDeviceToIOTsTable(connection, deviceDetails);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void addDeviceToUserProductTable(Connection connection,
                                             DeviceDetails deviceDetails) throws SQLException {

        String query = "SELECT product_id FROM Products WHERE product_name = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, deviceDetails.getProductName());

        int productID = statement.executeQuery().getInt(1);

        statement.close();

        query = "INSERT INTO UserProduct (ID_number, product_id, MAC_address)" +
                " VALUES (?, ?, ?)";
        // int, int, string
        statement = connection.prepareStatement(query);
        statement.setLong(1, deviceDetails.getUserIDNumber());
        statement.setInt(2, productID);
        statement.setString(3, deviceDetails.getMacAddress());

        statement.executeUpdate();
        statement.close();
    }

    private void addUserInfoToCompanyDB(Connection connection,
                                        DeviceDetails deviceDetails) throws SQLException {


        String query = "INSERT INTO UserInfo (ID_number, first_name, last_name, " +
                "address, " + "email, phone) VALUES (?, ?, ?, ?, ?, ?)";
        // int, string, string, string, string, string
        PreparedStatement statement = connection.prepareStatement(query);

        statement.setLong(1, deviceDetails.getUserIDNumber());
        statement.setString(2, deviceDetails.getFirstName());
        statement.setString(3, deviceDetails.getLastName());
        statement.setString(4, deviceDetails.getUserAddress());
        statement.setString(5, deviceDetails.getUserEmail());
        statement.setString(6, deviceDetails.getUserPhone());

        statement.executeUpdate();
        statement.close();
    }

    private void addDeviceToIOTsTable(Connection connection,
                                      DeviceDetails deviceDetails) throws SQLException {
        String query = "SELECT product_id FROM UserProduct WHERE " +
                "MAC_address = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, deviceDetails.getMacAddress());

        ResultSet resultSet = statement.executeQuery();
        int productID = resultSet.getInt(1);

        resultSet.close();
        statement.close();

        query = "INSERT INTO IoTs (MAC_address, registration_time, product_id, " +
                "IP_address) VALUES (?, ?, ?, ?)";
        // string, timestamp, int, int
        statement = connection.prepareStatement(query);
        statement.setString(1, deviceDetails.getMacAddress());
        statement.setString(2, "NOW"); //TODO use set TimeStamp?
        statement.setInt(3, productID);
        statement.setLong(4, deviceDetails.getIPAddress());

        statement.executeUpdate();
        statement.close();
    }

    public void updateIOT(UpdateDetails updateDetails) {
        try (Connection connection = DriverManager.getConnection(url, user,
                password);
             Statement statement = connection.createStatement()
        ) {

            // use company db
            statement.execute("USE " + updateDetails.getCompanyDBName());

            // add record to Updates table
            addUpdateToUpdatesTable(connection, updateDetails);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addUpdateToUpdatesTable(Connection connection,
                                        UpdateDetails updateDetails) throws SQLException {

        String query = "INSERT INTO Updates (time, type, MAC_address, " +
                "content) VALUES (?, ?, ?, ?)";
        // timestamp, string, string, string
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, "NOW");
        statement.setString(2, updateDetails.getType());
        statement.setString(3, updateDetails.getMacAddress());
        statement.setString(4, updateDetails.getContent());

        statement.executeUpdate();
        statement.close();
    }

    public static void main(String[] args) {
        //tests

        DBManagerForBothDBs dbManagerForBothDBs = new DBManagerForBothDBs("jdbc:mysql" +
                "://localhost:3306/", "root", "root");

        dbManagerForBothDBs.createAdminDB();

        dbManagerForBothDBs.registerCompany(new CompanyDetails(123456, "Electra",
                "someAddress", "Yossi", "0505050505", "yossi@electra" +
                ".com", 500
                , "4580123456781234", "10/25", 123));
    }
}
