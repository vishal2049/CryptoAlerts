package com.example.notificationdemo;

class priceMapBroadcast {
    String selectedPrice;
    String hitPrice;
    boolean isRefereshRecycler;

    public priceMapBroadcast(String selectedPrice) {
        this.selectedPrice = selectedPrice;
    }

    public priceMapBroadcast(boolean isRefereshRecycler, String hitPrice) {
        this.isRefereshRecycler = isRefereshRecycler;
        this.hitPrice = hitPrice;
    }

}
