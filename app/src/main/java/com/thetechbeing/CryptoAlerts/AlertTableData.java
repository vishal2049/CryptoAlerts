package com.thetechbeing.CryptoAlerts;


// This class is used to hold one row Data each time from DB
public class AlertTableData {
    private String symbol;
    private String price;
    private String note;
    private String upDown;
    private int repeatInterval;
    private String count;

    public AlertTableData(String symbol, String price, String note, String upDown,int repeatInterval, String count) {
        this.symbol = symbol;
        this.price = price;
        this.note = note;
        this.upDown = upDown;
        this.repeatInterval = repeatInterval;
        this.count = count;
    }

    public int getRepeatInterval() {
        return repeatInterval;
    }

    public String getCount() {
        return count;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDesiredPrice() {
        return price;
    }

    public String getNote() {
        return note;
    }

    public String getUpDown() {
        return upDown;
    }

}
