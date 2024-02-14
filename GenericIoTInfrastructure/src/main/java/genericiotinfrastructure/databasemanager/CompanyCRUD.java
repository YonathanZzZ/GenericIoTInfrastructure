package genericiotinfrastructure.databasemanager;

import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompanyCRUD implements CRUD {

    Map<String, Create> creators;

    private final String url;
    private final String user;
    private final String password;

    public CompanyCRUD(String url, String user, String password) {

        this.url = url;
        this.user = user;
        this.password = password;

        this.creators = new HashMap<>();
        creators.put("RegisterProduct", new RegisterProductCreator());
        creators.put("RegisterIoT", new RegisterIOTCreator());
        creators.put("UpdateIoT", new UpdateIOTCreator());
    }

    @Override
    public void create(String key, String values) {
        System.out.println("getting creator for companyCRUD. key: " + key);
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

    private class RegisterProductCreator implements Create {

        @Override
        public void create(String values) {

            try (Connection connection = DriverManager.getConnection(url, user
                    , password);
                 Statement statement = connection.createStatement()) {
                System.out.println("in RegisterProductCreator of companyCRUD");
                //create ProductDetails object
                ProductDetails productDetails = getProductDetails(values);
                System.out.println("created productDetails object: " + productDetails);
                // access company DB
                statement.execute("USE " + productDetails.getCompanyDBName());
                System.out.println("after accessing companyDB. name of db: " + productDetails.getCompanyDBName());
                // add product to Products table of company - specific DB
                addProductToCompanyDB(connection, productDetails);

                //create IoTs and Updates tables for db
                createTablesForNewProduct(statement);

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        private void createTablesForNewProduct(Statement statement) throws SQLException {

            String query = "CREATE TABLE IoTs(" +
                    "    MAC_address VARCHAR(255) NOT NULL PRIMARY KEY," +
                    "    registration_time TIMESTAMP NOT NULL," +
                    "    product_id BIGINT UNSIGNED NOT NULL," +
                    "    IP_address BIGINT NOT NULL" +
                    ")";

            statement.execute(query);
            System.out.println("executed query to create table IoTs");

            query = "CREATE TABLE Updates(" +
                    "    update_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                    "    time TIMESTAMP NOT NULL," +
                    "    type VARCHAR(255) NOT NULL," +
                    "    MAC_address VARCHAR(255) NOT NULL," +
                    "    content VARCHAR(255) NOT NULL" +
                    ")";

            statement.execute(query);

            // add relations
            query = "ALTER TABLE" +
                    "    IoTs ADD CONSTRAINT iots_product_id_foreign FOREIGN " +
                    "KEY(product_id) REFERENCES Products(product_id)";

            statement.execute(query);

            System.out.println("executed ALTER TABLE IoTs");

            query = "ALTER TABLE" +
                    "    Updates ADD CONSTRAINT updates_mac_address_foreign " +
                    "FOREIGN KEY(MAC_address) REFERENCES IoTs(MAC_address)";

            statement.execute(query);

            System.out.println("executed ALTER TABLE Updates");
        }
    }

    private class RegisterIOTCreator implements Create {

        @Override
        public void create(String values) {
            try (Connection connection = DriverManager.getConnection(url, user,
                    password);
                 Statement statement = connection.createStatement()
            ) {
                //Request string example:
                //
                // RegisterIoT$companyID$companyName$productName$macAddress
                // $ipAddress$userFirstName$userLastName$userEmail$userPhone
                // $userAddress$userIDNumber
                System.out.println("in RegisterIoTCreator");
                //create DeviceDetails object
                DeviceDetails deviceDetails = getDeviceDetails(values);
                System.out.println("after getting DeviceDetails");
                // access company DB
                statement.execute("USE " + deviceDetails.getCompanyDBName());
                System.out.println("after selecting companyDB");
                // add user info to UserInfo table
                addUserInfoToCompanyDB(connection, deviceDetails);
                System.out.println("after adding UserInfo to companyDB");
                // add device to UserProduct table
                addDeviceToUserProductTable(connection,
                        deviceDetails);
                System.out.println("after adding device to UserProduct table");
                // add info about device to IoTs table
                addDeviceToIOTsTable(connection, deviceDetails);
                System.out.println("after adding device to IoTs table");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        private DeviceDetails getDeviceDetails(String values) {
            String[] strings = values.split("\\$");
            System.out.println("in getDeviceDetails. strings array: " + Arrays.toString(strings));
            return new DeviceDetails(strings[3], Long.parseLong(strings[4])
                    , Integer.parseInt(strings[0]),
                    strings[1], strings[2], strings[7], strings[5],
                    strings[6], strings[9], strings[8], Integer.parseInt(strings[10]));

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

        private void addDeviceToUserProductTable(Connection connection,
                                                 DeviceDetails deviceDetails) throws SQLException {

            String query = "SELECT product_id FROM Products WHERE product_name = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, deviceDetails.getProductName());

            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            long productID = resultSet.getLong(1);

            statement.close();

            query = "INSERT INTO UserProduct (ID_number, product_id, MAC_address)" +
                    " VALUES (?, ?, ?)";
            // int, int, string
            statement = connection.prepareStatement(query);
            statement.setLong(1, deviceDetails.getUserIDNumber());
            statement.setLong(2, productID);
            statement.setString(3, deviceDetails.getMacAddress());

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
            resultSet.next();
            long productID = resultSet.getLong(1);

            resultSet.close();
            statement.close();

            query = "INSERT INTO IoTs (MAC_address, registration_time, product_id, " +
                    "IP_address) VALUES (?, ?, ?, ?)";
            // string, timestamp, int, int
            statement = connection.prepareStatement(query);
            statement.setString(1, deviceDetails.getMacAddress());
            statement.setTimestamp(2,
                    new java.sql.Timestamp(System.currentTimeMillis()));
            statement.setLong(3, productID);
            statement.setLong(4, deviceDetails.getIPAddress());

            statement.executeUpdate();
            statement.close();
        }
    }

    private void addProductToCompanyDB(Connection connection, ProductDetails productDetails) throws SQLException {
        String query = "INSERT INTO Products (product_name, product_tech_info, " +
                "registration_time) VALUES (?, ?, ?)";
        // string, string, timestamp
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, productDetails.getProductName());
        statement.setString(2, productDetails.getProductTechInfo());
        statement.setTimestamp(3,
                new java.sql.Timestamp(System.currentTimeMillis()));

        statement.executeUpdate();

        statement.close();
    }

    private class UpdateIOTCreator implements Create {

        @Override
        public void create(String values) {
            try (Connection connection = DriverManager.getConnection(url, user,
                    password);
                 Statement statement = connection.createStatement()
            ) {

                //create DeviceUpdate object
                UpdateDetails updateDetails = getDeviceUpdate(values);

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
            statement.setTimestamp(1,
                    new java.sql.Timestamp(System.currentTimeMillis()));
            statement.setString(2, updateDetails.getType());
            statement.setString(3, updateDetails.getMacAddress());
            statement.setString(4, updateDetails.getContent());

            statement.executeUpdate();
            statement.close();
        }

        private UpdateDetails getDeviceUpdate(String values) {
            String[] strings = values.split("\\$");

            return new UpdateDetails(strings[3], strings[4], strings[2],
                    Integer.parseInt(strings[0]), strings[1]);
        }
    }

    private ProductDetails getProductDetails(String values) {
        String[] strings = values.split("\\$");
        // example of values: companyID$companyName$productName$productDescription$productTechInfo

        return new ProductDetails(strings[2], strings[1], Integer.parseInt(strings[0]),
                strings[3], strings[4]);
    }

}
