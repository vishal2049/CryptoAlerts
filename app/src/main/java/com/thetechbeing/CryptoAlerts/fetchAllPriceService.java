package com.thetechbeing.CryptoAlerts;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class fetchAllPriceService extends IntentService {
    RequestQueue mRequestQueue;
    String selectedItem;
    myDatabase mDB = new myDatabase(this);
    ArrayList<AlertTableData> tempArray;
    NotificationManager manager;
    private static final String CHANNEL_FOREGROUND_ID = "foregroundNoti";
    String fprice;

    public fetchAllPriceService() {
        super("fetchAllPriceService");
        setIntentRedelivery(true);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        assert intent != null;
        selectedItem = intent.getStringExtra("selectedItem");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        assert intent != null;
        String url = intent.getStringExtra("url");
        mRequestQueue = Volley.newRequestQueue(this);

        // making app in foreground by foreground notification only
        createNotificationChannel();
        createNotification();

        //looping to get price at specific interval
        while (true) {
            fetchAllPriceOnService(url);
//                    for(AlertTableData data : mDB.getAllDataList()){
//                        long currentTimePlusInterval = Calendar.getInstance().getTimeInMillis() + data.getRepeatInterval()*60*1000;
//                        Log.d("myTAG", "UP-currentTimePlusinterval: "+currentTimePlusInterval);
//                        mDB.updateCount(data.getSymbol(),data.getDesiredPrice(),String.valueOf(currentTimePlusInterval));
//                        for(AlertTableData data2 : mDB.getAllDataList()){
//                        Log.d("myTAG", "UP-now: "+data2.getCount());}
//
//                    }
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
        // if service restart then we need to make all count to zero otherwise it'll skip alert once
//        for(AlertTableData data : mDB.getAllDataList()){
//            mDB.updateCount(data.getSymbol(),data.getDesiredPrice(),"0");
//        }
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
                            if (selectedItem.endsWith("USDT")) {
                                float f = Float.parseFloat(price);
                                if ((selectedItem.equals("BTCUSDT") || selectedItem.equals("ETHUSDT")))
                                    fprice = String.format("%.2f", f);
                                else
                                    fprice = String.format("%.5f", f);
                                EventBus.getDefault().post(new priceMapBroadcast(fprice));
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
//                Log.d("myTAG", "onErrorResponse: ");
                Log.d("myTAG", "status code: " + error.getMessage());
//                Log.d("myTAG", "status code: " + error.networkResponse.statusCode);
            }
        });
        mRequestQueue.add(request);
    }

    private void checkForALerts(String symbol, String currentPrice) {
        for (AlertTableData data1 : tempArray) {
            if (symbol.equals(data1.getSymbol())) {
                if (data1.getUpDown().equals("UP")) {
                    if (Double.parseDouble(data1.getDesiredPrice()) <= Double.parseDouble(currentPrice)) {
                        // repeatAlert or not
                        if (data1.getRepeatInterval() != 0) { // zero means repeat once
                            if (data1.getCount().equals("0")) {
                                long currentTimePlusInterval = Calendar.getInstance().getTimeInMillis() + data1.getRepeatInterval() * 60 * 1000;
                                mDB.updateCount(symbol, data1.getDesiredPrice(), String.valueOf(currentTimePlusInterval));
                                //Show notification
                                String msg = data1.getSymbol() + ": " + "Price above " + data1.getDesiredPrice();
                                showNotification(msg, data1.getNote());
                            } else {
                                if (Calendar.getInstance().getTimeInMillis() >= Long.parseLong(data1.getCount())) {
                                    //make time zero
                                    mDB.updateCount(symbol, data1.getDesiredPrice(), "0");

                                    //dont invoke alert if price is very far after timer get zero
                                    if (Double.parseDouble(data1.getDesiredPrice()) >= Double.parseDouble(currentPrice))
                                        mDB.updateUpDown(symbol, data1.getDesiredPrice(), "UP");
                                    else
                                        mDB.updateUpDown(symbol, data1.getDesiredPrice(), "DOWN");

                                }
                            }
                        } else {
                            //Show notification
                            String msg = data1.getSymbol() + ": " + "Price above " + data1.getDesiredPrice();
                            showNotification(msg, data1.getNote());

                            EventBus.getDefault().post(new RefreshRecycler(true, data1.getDesiredPrice(), data1.getSymbol()));
                            mDB.deleteData(data1.getSymbol(), data1.getDesiredPrice());
                        }
                    }
                }
                if (data1.getUpDown().equals("DOWN")) {
                    if (Double.parseDouble(data1.getDesiredPrice()) >= Double.parseDouble(currentPrice)) {
                        // repeatAlert or not
                        if (data1.getRepeatInterval() != 0) { // zero means repeat once
                            if (data1.getCount().equals("0")) {
                                long currentTimePlusInterval = Calendar.getInstance().getTimeInMillis() + data1.getRepeatInterval() * 60 * 1000;
                                mDB.updateCount(symbol, data1.getDesiredPrice(), String.valueOf(currentTimePlusInterval));

                                //Show notification
                                String msg = data1.getSymbol() + ": " + "Price below " + data1.getDesiredPrice();
                                showNotification(msg, data1.getNote());
                            } else {
                                if (Calendar.getInstance().getTimeInMillis() >= Long.parseLong(data1.getCount())) {
                                    //make time zero
                                    mDB.updateCount(symbol, data1.getDesiredPrice(), "0");

                                    //dont invoke alert if price is very far after timer get zero
                                    if (Double.parseDouble(data1.getDesiredPrice()) >= Double.parseDouble(currentPrice))
                                        mDB.updateUpDown(symbol, data1.getDesiredPrice(), "UP");
                                    else
                                        mDB.updateUpDown(symbol, data1.getDesiredPrice(), "DOWN");

                                }
                            }
                        } else {
                            //Show notification
                            String msg = data1.getSymbol() + ": " + "Price below " + data1.getDesiredPrice();
                            showNotification(msg, data1.getNote());

                            EventBus.getDefault().post(new RefreshRecycler(true, data1.getDesiredPrice(), data1.getSymbol()));
                            mDB.deleteData(data1.getSymbol(), data1.getDesiredPrice());
                        }
                    }
                }
            }
        }
    }

    // alert notification
    private void showNotification(String updown, String note) {
        int m = new Random().nextInt(100) + 1;
        int n = new Random().nextInt(100) + 2;
//        Uri alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, myNotificationChannels.CHANNEL_ID_1)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // for lower versions
                .setSmallIcon(R.drawable.bell2)
                .setContentTitle(updown)
                .setContentText(note)
                .setColor(Color.RED)
                .setVibrate(new long[]{1000, 1000, 1000})
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setOnlyAlertOnce(false);
        manager.notify(m + n, builder.build());
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
                .setPriority(NotificationCompat.PRIORITY_LOW);
        startForeground(150, builder2.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)//app sdk ver & device ver
        {
            NotificationChannel channel1 = new NotificationChannel(CHANNEL_FOREGROUND_ID, "Foreground channel", NotificationManager.IMPORTANCE_HIGH);
            channel1.setDescription("standard channel");

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(channel1);
        }

    }

}
