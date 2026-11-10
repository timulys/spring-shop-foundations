package study.shop.catalog;

public class ProductResponse {
    private final String code;
    private final String name;
    private final long unitPrice;
    private final String category;

    public ProductResponse(Product product) {
        this.code = product.getCode();
        this.name = product.getName();
        this.unitPrice = product.getUnitPrice();
        this.category = product.getCategory();
    }

    public String getCategory() {
        return category;
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
