package study.shop;

public class ProductResponse {
    private final String code;
    private final String name;
    private final long unitPrice;

    public ProductResponse(Product product) {
        this.code = product.getCode();
        this.name = product.getName();
        this.unitPrice = product.getUnitPrice();
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public long getUnitPrice() {
        return unitPrice;
    }
}
