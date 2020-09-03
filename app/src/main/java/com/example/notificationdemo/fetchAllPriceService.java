package com.example.notificationdemo;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
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
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public class fetchAllPriceService extends IntentService {
    RequestQueue mRequestQueue;
    String selectedItem;
    myDatabase mDB = new myDatabase(this);
    ArrayList<AlertTableData> tempArray;
    NotificationManager manager;
    private static final String CHANNEL_FOREGROUND_ID = "foregroundNoti";

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

        // making app in foreground by foreground notification only
        createNotificationChannel();
        createNotification();

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
                        if (symbol.equals(selectedItem)) {
                            if (selectedItem.endsWith("USDT") || selectedItem.endsWith("USDC")) {
                                float f = Float.parseFloat(price);
                                String str = String.format("%.2f", f);
                                EventBus.getDefault().post(new priceMapBroadcast(str));
                            } else
                                EventBus.getDefault().post(new priceMapBroadcast(price));
                        }
                        //Checking for every alerts
                        checkForALerts(symbol, price);
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
            if (symbol.equals(data1.getSymbol())) {
                if (data1.getUpDown().equals("UP")) {
                    if (Double.parseDouble(data1.getPrice()) <= Double.parseDouble(price)) {
                        //Show notification
                        String msg = data1.getSymbol() + ": " + "Price above " + data1.getPrice();
                        showNotification(msg, data1.getNote());

                        EventBus.getDefault().post(new priceMapBroadcast(true, data1.getPrice()));
                        mDB.deleteData(data1.getPrice());
                    }
                }
                if (data1.getUpDown().equals("DOWN")) {
                    if (Double.parseDouble(data1.getPrice()) >= Double.parseDouble(price)) {
                        //Show notification
                        String msg = data1.getSymbol() + ": " + "Price below " + data1.getPrice();
                        showNotification(msg, data1.getNote());

                        EventBus.getDefault().post(new priceMapBroadcast(true, data1.getPrice()));
                        mDB.deleteData(data1.getPrice());
                    }
                }
            }
        }
    }
// alert notification
    private void showNotification(String updown, String note) {
        int m = new Random().nextInt(100) + 1;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, myNotificationChannels.CHANNEL_ID_1)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // for lower versions
                .setSmallIcon(R.drawable.bell2)
                .setContentTitle(updown)
                .setContentText(note)
                .setColor(Color.RED)
                .setVibrate(new long[]{1000, 1000, 1000})
                .setOnlyAlertOnce(false);
        manager.notify(m, builder.build());
    }
//foreground notification
    private void createNotification() {
        Intent contentIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, contentIntent, 0);

        NotificationCompat.Builder builder2 = new NotificationCompat.Builder(this, CHANNEL_FOREGROUND_ID)
                .setSmallIcon(R.drawable.not)
                .setContentTitle("Alerts are active")
//                .setContentText("click to Stop")
                .setColor(Color.GREEN)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        startForeground(111, builder2.build());
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)//app sdk ver & device ver
        {
            NotificationChannel channel1 = new NotificationChannel(CHANNEL_FOREGROUND_ID, "Foreground channel", NotificationManager.IMPORTANCE_HIGH);
            channel1.setDescription("This channel is for video.");

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel1);
        }

    }

}
