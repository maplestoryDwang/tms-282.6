package net.swordie.ms.loaders.Etc.Commodity;

public class CommodityInfo {

    private int id;
    private int SN;
    private int ItemId;
    private int Count = 0;
    private int Price = 0;
    private int Period = 0;
    private String gameWorld = "";
    private int originalPrice = 0;
    private int Gender = 0;
    private int OnSale = 0;
    private int discount = 0;
    private int term = 0;
    private int limitMax;
    private boolean monthlyLimited;
    private String termStart, termEnd;
    private boolean onlyMileage;

    public int getLimitMax() { return limitMax; }
    public void setLimitMax(int value) { limitMax = value; }
    public boolean isMonthlyLimited() { return monthlyLimited; }
    public void setMonthlyLimited(boolean value) { monthlyLimited = value; }
    public boolean isOnlyMileage() { return onlyMileage; }
    public void setOnlyMileage(boolean value) { onlyMileage=value; }
    public void setTermStart(String value) { termStart=value;term=1; }
    public void setTermEnd(String value) { termEnd=value;term=1; }
    public boolean isSaleActive(java.time.LocalDateTime now) {
        try {
            var format=java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd/HHmmss");
            return (termStart==null || !now.isBefore(java.time.LocalDateTime.parse(termStart,format)))
                    && (termEnd==null || !now.isAfter(java.time.LocalDateTime.parse(termEnd,format)));
        } catch(java.time.format.DateTimeParseException e) { return false; }
    }

    public CommodityInfo() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSN() {
        return SN;
    }

    public void setSN(int SN) {
        this.SN = SN;
    }

    public int getItemId() {
        return ItemId;
    }

    public void setItemId(int itemId) {
        ItemId = itemId;
    }

    public int getCount() {
        return Count;
    }

    public void setCount(int count) {
        Count = count;
    }

    public int getPrice() {
        return Price;
    }

    public void setPrice(int price) {
        Price = price;
    }

    public int getPeriod() {
        return Period;
    }

    public void setPeriod(int period) {
        Period = period;
    }

    public String getGameWorld() {
        return gameWorld;
    }

    public void setGameWorld(String gameWorld) {
        this.gameWorld = gameWorld;
    }

    public int getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(int originalPrice) {
        this.originalPrice = originalPrice;
    }

    public int getGender() {
        return Gender;
    }

    public void setGender(int gender) {
        Gender = gender;
    }

    public int getOnSale() {
        return OnSale;
    }

    public void setOnSale(int onSale) {
        OnSale = onSale;
    }

    public int getDiscount() {
        return discount;
    }

    public void setDiscount(int discount) {
        this.discount = discount;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }
}
