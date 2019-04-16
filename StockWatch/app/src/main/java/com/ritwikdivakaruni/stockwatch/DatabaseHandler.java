package com.ritwikdivakaruni.stockwatch;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "StockAppDB";
    private static final String TABLE_NAME = "StockWatchTable";
    private static final String STOCKSYMBOL = "StockSymbol";
    private static final String COMPANYNAME = "CompanyName";
    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    STOCKSYMBOL + " TEXT not null unique," +
                    COMPANYNAME + " TEXT not null)";

    private SQLiteDatabase database;


    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        database = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public ArrayList<Stock> loadStocks() {
        ArrayList<Stock> stockList = new ArrayList<>();
        Cursor cursor = database.query(
                TABLE_NAME,  // The table to query
                new String[]{STOCKSYMBOL, COMPANYNAME}, // The columns to return
                null, // The columns for the WHERE clause
                null, // The values for the WHERE clause
                null, // don't group the rows
                null, // don't filter by row groups
                null); // The sort order

        if (cursor != null) {
            cursor.moveToFirst();

            for (int i = 0; i < cursor.getCount(); i++) {
                String symbol = cursor.getString(0);
                String name = cursor.getString(1);

                Stock s = new Stock();
                s.setSymbol(symbol);
                s.setName(name);
                s.setPrice(0.0);
                s.setPriceChange(0.0);
                s.setChangePercent(0.0);
                stockList.add(s);
                cursor.moveToNext();
            }
            cursor.close();
        }

        return stockList;
    }

    public void addStock(Stock newStock) {
        ContentValues values = new ContentValues();
        values.put(STOCKSYMBOL, newStock.getSymbol());
        values.put(COMPANYNAME, newStock.getName());
        long key = database.insert(TABLE_NAME, null, values);

    }

    public void deleteStock(String symbol) {
        int cnt = database.delete(TABLE_NAME, STOCKSYMBOL + " = ?", new String[]{symbol});
    }

    public void shutDown() {
        database.close();
    }
}
