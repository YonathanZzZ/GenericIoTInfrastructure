package genericiotinfrastructure.databasemanager;

public class UpdateDetails {

    private final String type;
    private final String content;
    private final String macAddress;
    private final long companyID;
    private final String companyName;

    public UpdateDetails(String type, String content, String macAddress,
                         long companyID, String companyName) {
        this.type = type;
        this.content = content;
        this.macAddress = macAddress;
        this.companyID = companyID;
        this.companyName = companyName;
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public long getCompanyID() {
        return companyID;
    }

    public String getCompanyDBName() {
        return companyName + companyID;
    }
}
