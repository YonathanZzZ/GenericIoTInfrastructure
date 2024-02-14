package genericiotinfrastructure.databasemanager;

public class DeviceDetails {
    private final String macAddress;
    private final long ipAddress;
    private final long companyID;
    private final String companyName;
    private final String productName;
    private final String userEmail;
    private final String firstName;
    private final String lastName;
    private final String userAddress;
    private final String userPhone;
    private final long userIDNumber;

    public DeviceDetails(String macAddress, long ipAddress,
                         long companyID, String companyName,
                         String productName,
                         String userEmail, String firstName, String lastName,
                         String userAddress, String userPhone,
                         long userIDNumber) {
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.companyID = companyID;
        this.productName = productName;
        this.userEmail = userEmail;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userAddress = userAddress;
        this.userPhone = userPhone;
        this.userIDNumber = userIDNumber;
        this.companyName = companyName;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public long getIPAddress() {
        return ipAddress;
    }

    public long getCompanyID() {
        return companyID;
    }

    public String getProductName() {
        return productName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUserAddress() {
        return userAddress;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public long getUserIDNumber() {
        return userIDNumber;
    }

    public String getCompanyDBName() {
        return companyName + companyID;
    }
}
