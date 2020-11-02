package com.thetechbeing.CryptoAlerts;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RecyclerViewClickListner {
    RequestQueue mRequestQueue;
    String url = "https://api.binance.com/api/v3/ticker/price";
    RecyclerView recyclerView;
    myAdapter mAdapter;
    List<model> alert_list = new ArrayList<>();
    AutoCompleteTextView symbol_atv;
    TextView variable_price;
    EditText alert_price, note;
    Button alert_btn, clear_all, menu_btn;
    CheckBox hide_o_pairs;
    ArrayList<String> symbolsList = new ArrayList<>();
    String selectedItem = "BTCUSDT"; //setting default symbol to AutoCompleteTV.
    String currentPrice = "0";
    myDatabase mDatabase;
    private static long back_pressed_time;
    private static long period = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initial settings for Views, recycler, Custom Adapter & Volley
        initConfigurations();

        //creating list by fetching data from DATABASE
        prepareAlertListFromDB();
        // request from API
        fetchAllSymbols();

        //set symbols in AutoCompleteTextView
        loadSymbols();

        //set menus
        menu_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(MainActivity.this,menu_btn);
                popupMenu.getMenuInflater().inflate(R.menu.menufile,popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch(item.getItemId())
                        {
                            case R.id.contact:
                                showmessage("Contact details","CONTACT@THETECHBEING.TK\n(Any feedback appreciated)");
                                return true;
                            case R.id.about:
                                showmessage("About app", "Crypto Alert can be used by those who are looking for an alternate platform to get free instant alerts from Binance exchange.\nThe unique feature that you will find here is the custom notes which will help you to track each alert.\n\nCRYPTO ALERTS\nversion 1.0");
                                return true;
                        }
                        return true;
                    }
                });
                popupMenu.show();
            }
        });

        // setting listner to AutoCompleteTV & sending its selection to service
        symbol_atv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);
                startIntentService(url, selectedItem);
            }
        });

        variable_price.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alert_price.setText(getCurrentPrice());
            }
        });

        //starting IntentService
        startIntentService(url, selectedItem);

    }

    private void refereshRecyclerView() {
        alert_list.clear();
        prepareAlertListFromDB();
        makeAdapter(alert_list);
    }

    private void prepareAlertListFromDB() {
        Cursor cursor1 = mDatabase.getAllData();
        if(cursor1.getCount() > 0) {
            while (cursor1.moveToNext())
                alert_list.add(new model(cursor1.getString(0), cursor1.getString(1), R.drawable.delete_alert_icon));
        }
        cursor1.close();
    }

    private void startIntentService(String url, String selectedItem) {
        Intent intent = new Intent(this, fetchAllPriceService.class);
        intent.putExtra("url", url);
        intent.putExtra("selectedItem", selectedItem);
        ContextCompat.startForegroundService(this,intent);
    }

    private void loadSymbols() {
        symbol_atv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1, symbolsList));

        symbol_atv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                symbol_atv.showDropDown();
                return false;
            }
        });
    }

    private void initConfigurations() {
        //Binding views
        symbol_atv = findViewById(R.id.select_symbol);
        variable_price = findViewById(R.id.variable_price);
        alert_price = findViewById(R.id.alert_price);
        note = findViewById(R.id.note);
        alert_btn = findViewById(R.id.alert_btn);
        hide_o_pairs = findViewById(R.id.hide_other_pairs);
        clear_all = findViewById(R.id.clear_all);
        menu_btn = findViewById(R.id.menu_btn);


        // creating volley for HTTP request
        mRequestQueue = Volley.newRequestQueue(this);

        //Binding Database to our activity
        mDatabase = new myDatabase(this);

        //for recycler
        recyclerView = findViewById(R.id.recyclerView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        //creating custom adapter & sending list which has all data that we want to show on recycler
        makeAdapter(alert_list);
    }

    private void makeAdapter(List<model> alert_list) {
        mAdapter = new myAdapter(alert_list, this);
        recyclerView.setAdapter(mAdapter);
    }

    private void fetchAllSymbols() {

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject ob1 = response.getJSONObject(i);
                        String symbol = ob1.getString("symbol");
                        symbolsList.add(symbol);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d("myTAG", "ERROR: " + e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("myTAG", "status code: " + error.getMessage());
//                Log.d("myTAG", "status code: " + error.networkResponse.statusCode);
            }
        });
        mRequestQueue.add(request);
    }

    @Override
    public void onImageClick(int position, String Rprice) {
        alert_list.remove(position);
        mAdapter.notifyItemRemoved(position);
        mDatabase.deleteData(Rprice);
    }

    // Recieving Broadcaste messages
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPriceMapBroadcast(priceMapBroadcast event) {
        variable_price.setText(event.price);
        setCurrentPrice(event.price);

    }

    // Recieving Broadcaste messages
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshRecyclerBroadcast(RefreshRecycler ref) {
        if (ref.isRefereshRecycler) {
            mDatabase.deleteData(ref.hitPrice);
            refereshRecyclerView();
        }

    }

    private void setCurrentPrice(String price) {
        currentPrice = price;
    }

    private String getCurrentPrice() {
        return currentPrice;
    }

    @Override
    protected void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public void create_alert(View view) {
        if (!alert_price.getText().toString().equals("")) {
            String textViewPrice = alert_price.getText().toString();
            alert_price.setTextColor(Color.parseColor("#FFFFFF"));
            String updown;
            if (Double.parseDouble(textViewPrice) > Double.parseDouble(getCurrentPrice()))
                updown = "UP";
            else if (Double.parseDouble(textViewPrice) < Double.parseDouble(getCurrentPrice()))
                updown = "DOWN";
            else {
                alert_price.setTextColor(Color.parseColor("#F44336"));
                return;
            }
            boolean isAdded = mDatabase.addData(symbol_atv.getText().toString(), textViewPrice, note.getText().toString(), updown);
            if (isAdded) {
                Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show();
                Cursor cursor2 = mDatabase.getAllData();
                if (cursor2.moveToLast()) {
                    alert_list.add(new model(cursor2.getString(0), cursor2.getString(1), R.drawable.delete_alert_icon));
                    makeAdapter(alert_list);
                }
                cursor2.close();
            } else
                Toast.makeText(this, "already added", Toast.LENGTH_SHORT).show();
        }
    }

    private void showmessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.create();
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

    public void clearAll(View view) {
        alert_list.clear();
        mAdapter.notifyDataSetChanged();
        if (mDatabase.deleteAllData())
            Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show();
    }

    public void hideOtherPairs(View view) {
        if (hide_o_pairs.isChecked()) {
            displaySimilarPairs(alert_list);
        } else {
            alert_list.clear();
            prepareAlertListFromDB();
            makeAdapter(alert_list);
        }
    }

    // important method- practice more
    private void displaySimilarPairs(List<model> alert_list) {
        Iterator<model> itr = alert_list.iterator();
        while (itr.hasNext()) {
            model mod = itr.next();
            if (!mod.getrsymbol().equals(symbol_atv.getText().toString())) {
                itr.remove();
            }
        }
        makeAdapter(alert_list); // displaying
    }

    @Override
    public void onBackPressed() {
        if (back_pressed_time + period > System.currentTimeMillis())
            super.onBackPressed();
        else {
            Toast.makeText(this, "Press again to Exit", Toast.LENGTH_SHORT).show();
            back_pressed_time = System.currentTimeMillis();
        }
    }
}