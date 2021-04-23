package com.thetechbeing.CryptoAlerts;

public class RefreshRecycler {
    String matchedPrice;
    String matchedSymbol;
    boolean isRefereshRecycler;

    public RefreshRecycler(boolean isRefereshRecycler, String matchedPrice, String matchedSymbol) {
        this.isRefereshRecycler = isRefereshRecycler;
        this.matchedPrice = matchedPrice;
        this.matchedSymbol = matchedSymbol;
    }

}
