package genericiotinfrastructure.databasemanager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class CompanyDBManager {
    private final Map<String, CRUD> companyCRUDs;
    private final String url;
    private final String user;
    private final String password;

    public CompanyDBManager(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;

        companyCRUDs = new HashMap<>();
    }

    public CRUD getCRUD(String companyName, int companyID) {
        System.out.println("in getCRUD");
        String companyDBName = getCompanyDBName(companyName, companyID);

        System.out.println("companyDB name: " + companyDBName);
        System.out.println(companyCRUDs.get(companyDBName));

        return companyCRUDs.get(companyDBName);
    }

    private String getCompanyDBName(String companyName, int companyID) {
        return companyName + companyID;
    }

    public void createCompanyDB(String companyDBName) {
        try (Connection connection = DriverManager.getConnection(url, user,
                password);
             Statement statement = connection.createStatement()
        ) {
            //create db
            statement.execute("create database " + companyDBName);

            //select db
            statement.execute("USE " + companyDBName);

            //create tables;
            addCompanyDBTables(statement);

            //create CRUD for companyDB and add it to map
            CompanyCRUD companyCRUD = new CompanyCRUD(url, user, password);
            companyCRUDs.put(companyDBName, companyCRUD);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void addCompanyDBTables(Statement statement) throws SQLException {
        System.out.println("adding tables to company db");
        //create user info table
        String query = "CREATE TABLE UserInfo(ID_number BIGINT NOT NULL " +
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
                "    ID_number BIGINT NOT NULL," +
                "    product_id BIGINT NOT NULL," +
                "    MAC_address VARCHAR(255) NOT NULL" +
                ")";

        statement.execute(query);
        System.out.println("after creating table UserProduct");

        //add foreign key
        query = "ALTER TABLE UserProduct ADD CONSTRAINT " +
                "userproduct_ID_number_foreign " +
                "FOREIGN KEY(ID_number) REFERENCES UserInfo(ID_number)";

        statement.execute(query);
        System.out.println("after adding constraints");
    }

}
