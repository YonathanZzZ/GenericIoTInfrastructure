package genericiotinfrastructure.databasemanager;

public class ProductDetails {
    private final String productName;
    private final String companyName;
    private final long companyID;
    private final String productDescription;
    private final String productTechInfo;

    public ProductDetails(String productName, String companyName,
                          long companyID,
                          String productDescription, String productTechInfo) {
        this.productName = productName;
        this.companyID = companyID;
        this.productDescription = productDescription;
        this.productTechInfo = productTechInfo;
        this.companyName = companyName;
    }

    public String getProductName() {
        return productName;
    }

    public long getCompanyID() {
        return companyID;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public String getProductTechInfo() {
        return productTechInfo;
    }

    public String getCompanyDBName() {
        return companyName + companyID;
    }
}
