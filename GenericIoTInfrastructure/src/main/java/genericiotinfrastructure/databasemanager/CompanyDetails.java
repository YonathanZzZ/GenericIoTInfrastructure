package genericiotinfrastructure.databasemanager;

public class CompanyDetails {

    private final long companyID;
    private final String companyName;
    private final String address;
    private final String contactName;
    private final String contactPhone;
    private final String contactEmail;

    //payment info
    private final int serviceFee;
    private final String cardNumber;
    private final String cardExpDate;
    private final int cardCVV;

    public CompanyDetails(long companyID, String companyName, String address,
                          String contactName,
                          String contactPhone, String contactEmail,
                          int serviceFee, String cardNumber,
                          String cardExpDate, int cardCVV) {
        this.companyID = companyID;
        this.companyName = companyName;
        this.address = address;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.serviceFee = serviceFee;
        this.cardNumber = cardNumber;
        this.cardExpDate = cardExpDate;
        this.cardCVV = cardCVV;
    }

    public long getCompanyID() {
        return companyID;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getAddress() {
        return address;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public int getServiceFee() {
        return serviceFee;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getCardExpDate() {
        return cardExpDate;
    }

    public int getCardCVV() {
        return cardCVV;
    }

    public String getCompanyDBName() {
        return companyName + companyID;
    }
}
