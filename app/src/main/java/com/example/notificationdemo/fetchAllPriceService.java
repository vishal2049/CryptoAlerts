package com.example.notificationdemo;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class fetchAllPriceService extends IntentService {
    RequestQueue mRequestQueue;
    String selectedItem;
    myDatabase mDB = new myDatabase(this);
    ArrayList<AlertTableData> tempArray;
    NotificationManager manager;

    public fetchAllPriceService() {
        super("fetchAllPriceService");
        setIntentRedelivery(true);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        selectedItem = intent.getStringExtra("selectedItem");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String url = intent.getStringExtra("url");
        mRequestQueue = Volley.newRequestQueue(this);

        //looping to get price at specific interval
        while (true) {
            fetchAllPriceOnService(url);

            try {
                tempArray = mDB.getAllDataList(); // dont put it inside above function otherwise it block UI
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private void fetchAllPriceOnService(String url) {
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject ob1 = response.getJSONObject(i);
                        String symbol = ob1.getString("symbol");
                        String price = ob1.getString("price");

                        //sending broadcast to UI for selected symbol
                        if(symbol.equals(selectedItem)) {
                            if (selectedItem.endsWith("USDT") || selectedItem.endsWith("USDC")) {
                                float f = Float.parseFloat(price);
                                String str = String.format("%.2f", f);
                                EventBus.getDefault().post(new priceMapBroadcast(str));
                            }else
                                EventBus.getDefault().post(new priceMapBroadcast(price));
                        }
                        //Checking for every alerts
                        checkForALerts(symbol,price);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d("myTAG", "ERROR: " + e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("myTAG", "onErrorResponse: ");
                Log.d("myTAG", "status code: " + error.getMessage());
//                Log.d("myTAG", "status code: " + error.networkResponse.statusCode);
            }
        });
        mRequestQueue.add(request);
    }

    private void checkForALerts(String symbol, String price) {
            for (AlertTableData data1 : tempArray) {
                if (symbol.equals(data1.getSymbol())){
                 if (data1.getUpDown().equals("UP")) {
                    if (Double.parseDouble(data1.getPrice()) <= Double.parseDouble(price)) {
                        //Show notification
                        String msg = data1.getSymbol() + ": " + "Price above " + data1.getPrice();
                         showNotification(msg, data1.getNote());

                         EventBus.getDefault().post(new priceMapBroadcast(true,data1.getPrice()));
                         mDB.deleteData(data1.getPrice());
                    }
                 }
                if (data1.getUpDown().equals("DOWN")) {
                    if (Double.parseDouble(data1.getPrice()) >= Double.parseDouble(price)) {
                         //Show notification
                        String msg = data1.getSymbol() + ": " + "Price below " + data1.getPrice();
                        showNotification(msg, data1.getNote());

                        EventBus.getDefault().post(new priceMapBroadcast(true,data1.getPrice()));
                        mDB.deleteData(data1.getPrice());
                    }
                }
            }
            }
    }

    private void showNotification(String updown, String note) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,myNotificationChannels.CHANNEL_ID_1)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // for lower versions
                .setSmallIcon(R.drawable.ic_sentiment_satisfied_black_24dp)
                .setColor(Color.YELLOW)
                .setContentTitle(updown)
                .setContentText(note)
                .setVibrate(new long[]{1000,1000,1000})
                .setOnlyAlertOnce(false);
        manager.notify(1, builder.build());
    }

}
