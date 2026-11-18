package study.shop.catalog.domain;

/** Temporary browser read model. Current product data comes from catalog-service. */
public record Product(String code, String name, long unitPrice, String category) {
    public String getCode() { return code; }
    public String getName() { return name; }
    public long getUnitPrice() { return unitPrice; }
    public String getCategory() { return category; }
}
