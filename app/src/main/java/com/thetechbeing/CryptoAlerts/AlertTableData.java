package com.thetechbeing.CryptoAlerts;


// This class is used to hold one row Data each time from DB
public class AlertTableData {
    private String symbol;
    private String price;
    private String note;
    private String upDown;

    public AlertTableData(String symbol, String price, String note, String upDown) {
        this.symbol = symbol;
        this.price = price;
        this.note = note;
        this.upDown = upDown;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getPrice() {
        return price;
    }

    public String getNote() {
        return note;
    }

    public String getUpDown() {
        return upDown;
    }

}
