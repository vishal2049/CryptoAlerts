package com.example.notificationdemo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class myDatabase extends SQLiteOpenHelper {
    public static final String ALERT_PRICE_DATABASE = "AlertPriceDB";
    public static final String ALERT_PRICE_TABLE = "AlertPriceTable";
    public static final String PRICE_INDEX = "price_index";
    public static final String COL1 = "SYMBOL";
    public static final String COL2 = "PRICE";
    public static final String COL3 = "NOTE";
    public static final String COL4 = "UPDOWN";

    SQLiteDatabase mdb;

    public myDatabase(@Nullable Context context) {
        super(context, ALERT_PRICE_DATABASE, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + ALERT_PRICE_TABLE + " (SYMBOL TEXT NOT NULL," + " PRICE TEXT PRIMARY KEY NOT NULL," + " NOTE TEXT," + " UPDOWN TEXT NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX "+PRICE_INDEX+" ON "+ALERT_PRICE_TABLE+" (PRICE);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ALERT_PRICE_TABLE);
        onCreate(db);
    }

    public boolean addData(String symbol, String price, String note, String upDown) {
        mdb = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL1, symbol);
        values.put(COL2, price);
        values.put(COL3, note);
        values.put(COL4, upDown);
        long result = mdb.insert(ALERT_PRICE_TABLE, null, values);
        //id++;
        return result != -1; // return true in case result is positive
    }

    public Cursor getAllData() {
        mdb = this.getReadableDatabase();
        String query = "SELECT * FROM " + ALERT_PRICE_TABLE;
        return mdb.rawQuery(query, null);
    }

    public ArrayList<AlertTableData> getAllDataList() {
        mdb = this.getReadableDatabase();
        ArrayList<AlertTableData> list = new ArrayList<>();
        String query = "SELECT * FROM " + ALERT_PRICE_TABLE;
        Cursor c = mdb.rawQuery(query, null);
        if(c != null && c.getCount() > 0)
            while (c.moveToNext())
              list.add(new AlertTableData(c.getString(0), c.getString(1), c.getString(2), c.getString(3)));
        assert c != null;
        c.close();
        return list;
    }

    public boolean deleteData(String price) {
        mdb = this.getWritableDatabase();
        int result = mdb.delete(ALERT_PRICE_TABLE, "PRICE=?", new String[]{price});
        return result > 0; // return true i.e successfull
    }

    public boolean deleteAllData() {
        mdb = this.getWritableDatabase();
        int result = mdb.delete(ALERT_PRICE_TABLE, null, null);
        mdb.execSQL("vacuum");
        return result > 0;
    }
}
