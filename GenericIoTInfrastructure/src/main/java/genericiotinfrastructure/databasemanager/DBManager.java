package genericiotinfrastructure.databasemanager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBManager {

    private final String url;
    private final String user;
    private final String password;
    private final String adminDBName;

    public DBManager(String url, String user,
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

        query = "ALTER TABLE\n" +
                "    Products ADD CONSTRAINT products_company_id_foreign " +
                "FOREIGN KEY(company_id) REFERENCES Companies(company_id);";

        statement.execute(query);

        query = "ALTER TABLE\n" +
                "    CreditCardDetails ADD CONSTRAINT " +
                "creditcarddetails_company_id_foreign FOREIGN KEY(company_id) " +
                "REFERENCES Companies(company_id);";

        statement.execute(query);
    }

    private void insertRecord(Statement statement, String tableName,
                              String attributes, String values) throws SQLException {
        String query =
                "INSERT INTO " + tableName + " (" + attributes + ")" +
                        " VALUES " + "(" + values + ");";
        System.out.println("query: " + query);

        statement.executeUpdate(query);
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
            String columns = "company_id, company_name, company_address, " +
                    "contact_name, " +
                    "contact_phone, contact_email, service_fee";
            String values =
                    companyDetails.getCompanyID() + ", " +
                            companyDetails.getCompanyName() + ", " +
                            companyDetails.getAddress()
                            + ", " + companyDetails.getContactName() + ", " +
                            companyDetails.getContactPhone() + ", " +
                            companyDetails.getContactEmail() + ", " +
                            companyDetails.getServiceFee();

            insertRecord(statement, "Companies", columns, values);

            //add payment info to CreditCardDetails table
            addPaymentInfo(statement, companyDetails);

            createCompanyDB(statement, companyDetails);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void addPaymentInfo(Statement statement, CompanyDetails companyDetails)
            throws SQLException {
        String attributes = "company_id, card_number, ex_date, CVV";
        String values =
                companyDetails.getCompanyID() + ", " + companyDetails.getCardNumber()
                        + ", " + companyDetails.getCardExpDate() + ", "
                        + companyDetails.getCardCVV();

        insertRecord(statement, "CreditCardDetails", attributes, values);
    }

    private void createCompanyDB(Statement statement, CompanyDetails companyDetails)
            throws SQLException {
        //create company - specific DB
        statement.execute("create database " + companyDetails.getCompanyID());

        //select company DB
        statement.execute("use " + companyDetails.getCompanyID());

        //add tables to company DB
        addCompanyDBTables(statement, companyDetails);
    }

    private void addCompanyDBTables(Statement statement,
                                    CompanyDetails companyDetails) throws SQLException {

        //create user info table
        String query = "CREATE TABLE UserInfo(ID_number BIGINT NOT NULL" +
                " " +
                "PRIMARY KEY, " +
                "first_name VARCHAR(255) NOT NULL, " +
                "last_name VARCHAR(255) NOT NULL, " +
                "address VARCHAR(255) NOT NULL, " +
                "email VARCHAR(255) NOT NULL, " +
                "phone VARCHAR(255) NOT NULL";

        statement.execute(query);

        //create Products table. this table includes all the products that
        // this company supports
        query = "CREATE TABLE Products(" +
                "    product_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    product_name VARCHAR(255) NOT NULL," +
                "    product_tech_info VARCHAR(255) NOT NULL," +
                "    registration_time TIMESTAMP NOT NULL" +
                ");";

        statement.execute(query);

        //create UserProducts table. this table is used to find the products
        // belonging to each registered user, as it includes a userID field.
        // it's one table for all the users.

        query = "CREATE TABLE UserProduct(" +
                "    UserProduct_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    ID_number BIGINT NOT NULL," +
                "    product_id BIGINT NOT NULL," +
                "    MAC_address BIGINT NOT NULL" +
                ");";

        statement.execute(query);

        //add foreign key
        query = "ALTER TABLE UserProduct ADD CONSTRAINT " +
                "userproduct_ID_number_foreign " +
                "FOREIGN KEY(ID_number) REFERENCES Userinfo(ID_number);";

        statement.execute(query);
    }

    public void registerProduct(ProductDetails productDetails) {

        try (Connection connection = DriverManager.getConnection(url, user,
                password);
             Statement statement = connection.createStatement()
        ) {

            // access admin DB
            statement.execute("USE " + adminDBName);

            // add product to Products table of adminDB
            addProductToAdminDB(statement, productDetails);

            // access company DB
            statement.execute("USE " + productDetails.getCompanyID());

            // add product to Products table of company - specific DB
            addProductToCompanyDB(statement, productDetails);

            // create table 'IoTs'
            createTablesForNewProduct(statement, productDetails);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createTablesForNewProduct(Statement statement, ProductDetails productDetails) throws SQLException {

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
        query = "ALTER TABLE\n" +
                "    `IoTs` ADD CONSTRAINT `iots_product_id_foreign` FOREIGN " +
                "KEY(`product_id`) REFERENCES `Products`(`product_id`)";

        statement.execute(query);

        query = "ALTER TABLE\n" +
                "    `Updates` ADD CONSTRAINT `updates_mac_address_foreign` " +
                "FOREIGN KEY(`MAC_address`) REFERENCES `IoTs`(`MAC_address`)";

        statement.execute(query);
    }

    private void addProductToAdminDB(Statement statement,
                                     ProductDetails productDetails)
            throws SQLException {

        String attributes = "company_id, product_name, product_description";
        String values =
                productDetails.getCompanyID() + ", "
                        + productDetails.getProductName() + ", "
                        + productDetails.getProductDescription();

        insertRecord(statement, "Products", attributes, values);
    }

    private void addProductToCompanyDB(Statement statement, ProductDetails productDetails) throws SQLException {

        String attributes = "product_name, product_tech_info, " +
                "registration_time";
        String values =
                productDetails.getProductName() + ", "
                        + productDetails.getProductTechInfo() + ", " + "NOW";

        insertRecord(statement, "Products", attributes, values);
    }

    public void registerIoT(DeviceDetails deviceDetails) {


        try (Connection connection = DriverManager.getConnection(url, user,
                password);
             Statement statement = connection.createStatement()
        ) {

            // access company DB
            statement.execute("USE " + deviceDetails.getCompanyID());

            // add user info to UserInfo table
            addUserInfoToCompanyDB(statement, deviceDetails);

            // add device to UserProduct table
            addDeviceToUserProductTable(statement, deviceDetails);

            // add info about device to IoTs table
            addDeviceToIOTsTable(statement, deviceDetails);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void addDeviceToUserProductTable(Statement statement,
                                             DeviceDetails deviceDetails) throws SQLException {

        String productID = statement.executeQuery("SELECT product_id FROM " +
                "Products WHERE product_name = " +
                deviceDetails.getProductName()).getString("product_it");


        String attributes = "ID_number, product_id, MAC_address";
        String values = deviceDetails.getUserIDNumber() + ", " + productID +
                ", " + deviceDetails.getMacAddress();

        insertRecord(statement, "UserProduct", attributes, values);
    }

    private void addUserInfoToCompanyDB(Statement statement,
                                        DeviceDetails deviceDetails) throws SQLException {

        String attributes = "ID_number, first_name, last_name, address, " +
                "email, phone";
        String values =
                deviceDetails.getUserIDNumber() + ", " +
                        deviceDetails.getFirstName() + ", " +
                        deviceDetails.getLastName() + ", " +
                        deviceDetails.getUserAddress() + ", " +
                        deviceDetails.getUserEmail() + ", " +
                        deviceDetails.getUserPhone();

        insertRecord(statement, "UserInfo", attributes, values);
    }

    private void addDeviceToIOTsTable(Statement statement, DeviceDetails deviceDetails) throws SQLException {

        // get product_id from UserProduct table
        String productID =
                statement.executeQuery("SELECT * FROM UserProduct WHERE " +
                        "MAC_address = " + deviceDetails.getMacAddress()).getString(
                        "product_id");

        String attributes = "MAC_address, registration_time, product_id, " +
                "IP_address";
        String values =
                deviceDetails.getMacAddress() + ", NOW, " + productID + ", " + deviceDetails.getIPAddress();

        insertRecord(statement, "IoTs", attributes, values);
    }

    public void updateIOT(UpdateDetails updateDetails) {
        try (Connection connection = DriverManager.getConnection(url, user,
                password);
             Statement statement = connection.createStatement()
        ) {

            // use company db
            statement.execute("USE " + updateDetails.getCompanyID());

            // add record to Updates table
            addUpdateToUpdatesTable(statement, updateDetails);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addUpdateToUpdatesTable(Statement statement, UpdateDetails updateDetails) throws SQLException {

        String attributes = "time, type, MAC_address, content";
        String values =
                "NOW, " + updateDetails.getType() + ", " +
                        updateDetails.getMacAddress() + ", " +
                        updateDetails.getContent();

        insertRecord(statement, "Updates", attributes, values);
    }


    public static void main(String[] args) {
        //tests

        DBManager dbManager = new DBManager("jdbc:mysql" +
                "://localhost:3306/", "root", "root");

        dbManager.createAdminDB();

        dbManager.registerCompany(new CompanyDetails(123456, "Electra",
                "tel-aviv", "Yossi", "0505050505", "yossi@electra.com", 500
                , "4580123456781234", "10/25", 123));

    }
}
