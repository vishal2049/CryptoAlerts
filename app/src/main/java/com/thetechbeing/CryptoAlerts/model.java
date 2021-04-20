package com.thetechbeing.CryptoAlerts;

public class model {
    private String rsymbol;
    private String rprice;
    private int rdelete;

    public model(String rsymbol, String rprice, int rdelete) {
        this.rsymbol = rsymbol;
        this.rprice = rprice;
        this.rdelete = rdelete;
    }

    public String getrsymbol() {
        return rsymbol;
    }

    public String getrprice() {
        return rprice;
    }

    public int getrdelete() {
        return rdelete;
    }

}
