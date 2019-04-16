package com.ritwikdivakaruni.stockwatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private List<Stock> stockList = new ArrayList<>();
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swiper;
    private StockAdapter sAdapter;
    private HashMap<String,String> symbolNameMap;
    private DatabaseHandler databaseHandler;

    /*onClick methods*/
    @Override
    public void onClick(View v) {

        final int pos = recyclerView.getChildLayoutPosition(v);
        String marketPlace = "http://www.marketwatch.com/investing/stock/";
        String symbol = stockList.get(pos).getSymbol();

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(marketPlace+symbol));
        startActivity(i);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_stock, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        databaseHandler.shutDown();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        stockList.size();
        super.onResume();
        sAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onLongClick(View v) {

        final int pos = recyclerView.getChildLayoutPosition(v);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.baseline_delete_24);
        builder.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                databaseHandler.deleteStock(stockList.get(pos).getSymbol());
                stockList.remove(pos);
                sAdapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        TextView tv = v.findViewById(R.id.symbol);
        String symbol = tv.getText().toString();
        builder.setMessage("Delete Stock Symbol "+symbol+"?");
        builder.setTitle("Delete Stock");
        AlertDialog dialog = builder.create();
        dialog.show();
        return false;
    }

    /* We need to check the connectivity and creating dialog boxes*/
    public void networkDialog(String text){
        String message = null;
        if(text.equals("add")){
            message = "Stocks Cannot Be Added Without A Network Connection";
        }
        else if(text.equals("update")){
            message = "Stocks Cannot Be Updated Without A Network Connection";
        }
        else{
            message = "Please Check Your Network";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setTitle("No Network Connection");
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private boolean doNetCheck() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (cm == null) {
            Toast.makeText(this, "Cannot access ConnectivityManager", Toast.LENGTH_SHORT).show();
            return false;
        }
        boolean status = netInfo.isConnected();
        if (netInfo != null && status) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recycler);
        sAdapter = new StockAdapter(stockList, this);
        recyclerView.setAdapter(sAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        swiper = findViewById(R.id.swiper);
        swiper.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                if(!doNetCheck()){
                    //show network error dialog
                    swiper.setRefreshing(false);
                    networkDialog("update");
                }else {
                    doRefresh();
                }
            }
        });
        databaseHandler = new DatabaseHandler(this);
        new NameDownloader(this).execute();
        ArrayList<Stock> tempList = databaseHandler.loadStocks();
        if(!doNetCheck()){
            networkDialog("");
            for(int i=0; i<tempList.size(); i++){
                stockList.add(tempList.get(i));
            }
            Collections.sort(stockList, new Comparator<Stock>() {
                @Override
                public int compare(Stock lhs, Stock rhs) {
                    return lhs.getSymbol().compareTo(rhs.getSymbol());
                }
            });
            sAdapter.notifyDataSetChanged();
        }else{
            for(int i=0; i<tempList.size(); i++){
                String symbol = tempList.get(i).getSymbol();
                new StockDownloader(this).execute(symbol);
            }
        }
    }

    public void updateData(HashMap<String,String> symbolNameMap){
        if(symbolNameMap!=null && !symbolNameMap.isEmpty()) {
            this.symbolNameMap = symbolNameMap;
        }
        else{
            Toast.makeText(this, "Problem loading Name and Symbol", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(!doNetCheck()){
            networkDialog("add");
            return false;
        }
        else{
            switch (item.getItemId()) {
                case R.id.addStock:
                    if(symbolNameMap == null)
                        new NameDownloader(MainActivity.this).execute();
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    final EditText et = new EditText(this);
                    et.setText("");
                    et.setInputType(InputType.TYPE_CLASS_TEXT);
                    InputFilter[] editFilters = et.getFilters();
                    InputFilter[] newFilters = new InputFilter[editFilters.length + 1];
                    System.arraycopy(editFilters, 0, newFilters, 0, editFilters.length);
                    newFilters[editFilters.length] = new InputFilter.AllCaps();
                    et.setFilters(newFilters);
                    editFilters = et.getFilters();
                    newFilters = new InputFilter[editFilters.length + 1];
                    System.arraycopy(editFilters, 0, newFilters, 0, editFilters.length);
                    newFilters[editFilters.length] =  new InputFilter() {
                        @Override
                        public CharSequence filter(CharSequence charSequence, int i, int i1, Spanned spanned, int i2, int i3) {
                            if(charSequence.equals("")){ // for backspace
                                return charSequence;
                            }
                            if(charSequence.toString().matches("[a-zA-Z 0-9]+")){
                                return charSequence;
                            }
                            return "";
                        }
                    };

                    et.setFilters(newFilters);


                    et.setGravity(Gravity.CENTER_HORIZONTAL);
                    builder.setView(et);

                    builder.setPositiveButton("ADD", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if(!doNetCheck()){
                                networkDialog("add");
                            }
                            else{

                                if(et.getText().toString().length() > 0){
                                    final ArrayList<String> stockOption = new ArrayList<>();
                                    ArrayList<String> tempList = searchForStock(et.getText().toString());
                                    if(!tempList.isEmpty()){
                                        stockOption.addAll(tempList);
                                        if(stockOption.size() == 1){
                                            if(duplicateStock(stockOption.get(0))){
                                                duplicateDialog(et.getText().toString());
                                            }
                                            else{
                                                saveStock(stockOption.get(0));
                                            }
                                        }
                                        else{
                                            AlertDialog.Builder builders = new AlertDialog.Builder(MainActivity.this);
                                            builders.setTitle("Make a selection");
                                            final String [] arr = new String[stockOption.size()];
                                            for(int i=0; i< arr.length; i++)
                                                arr[i] = stockOption.get(i);
                                            builders.setItems(arr, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    if(duplicateStock(arr[which])){
                                                        duplicateDialog(et.getText().toString());
                                                    }
                                                    else{
                                                        saveStock(arr[which]);
                                                    }
                                                }
                                            });

                                            builders.setNegativeButton("Nevermind", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                }
                                            });
                                            AlertDialog dialogs = builders.create();

                                            dialogs.show();
                                        }

                                    }
                                    else{
                                        dataNotFoundDialog(et.getText().toString());
                                    }
                                }
                                else{
                                    Toast.makeText(MainActivity.this, "Please Enter a valid Stock Symbol!", Toast.LENGTH_SHORT).show();
                                }
                            }

                        }
                    });

                    builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });

                    builder.setMessage("Please enter a Stock Symbol:");
                    builder.setTitle("Stock Selection");

                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }


    }

    private boolean duplicateStock(String item){

        String symbol = item.split("-")[0].trim();
        System.out.println("Symbol: "+ symbol);
        Stock temp = new Stock();
        temp.setSymbol(symbol);
        stockList.contains(temp);
        return stockList.contains(temp);
    }

    private void saveStock(String s) {

        //Use an async function to get the details and store it to the main list
        String symbol = s.split("-")[0].trim();
        System.out.println("Symbol: "+ symbol);

        new StockDownloader(this).execute(symbol);

        //Store the stock in the database (Only company symbol and name)
        Stock ts = new Stock();
        ts.setSymbol(symbol);
        ts.setName(symbolNameMap.get(symbol));
        databaseHandler.addStock(ts);

        return;
    }

    private ArrayList<String> searchForStock(String text) {
        ArrayList<String> stockOption = new ArrayList<>();

        if(symbolNameMap != null && !symbolNameMap.isEmpty()) {

            Iterator<String> it = symbolNameMap.keySet().iterator();
            while (it.hasNext()) {
                String symbol = it.next();
                String name = symbolNameMap.get(symbol);
                //pattern matching
                if (symbol.toUpperCase().contains(text.toUpperCase())) {
                    stockOption.add(symbol + " - " + name);
                } else if (name.toUpperCase().contains(text.toUpperCase())) {
                    stockOption.add(symbol + " - " + name);
                }

            }
        }
        return stockOption;
    }

    private void doRefresh() {

        swiper.setRefreshing(false);

        ArrayList<Stock> tempList = databaseHandler.loadStocks();

        for(int i=0; i<tempList.size(); i++){
            String symbol = tempList.get(i).getSymbol();
            new StockDownloader(this).execute(symbol);
        }

        Toast.makeText(this, "Data Refreshed...!", Toast.LENGTH_SHORT).show();

    }


    public void updateStock(Stock newStock) {

        if(newStock != null){
            int index;
            if((index = stockList.indexOf(newStock)) > -1){
                stockList.remove(index);
            }
            stockList.add(newStock);
            Collections.sort(stockList, new Comparator<Stock>() {
                @Override
                public int compare(Stock lhs, Stock rhs) {
                    return lhs.getSymbol().compareTo(rhs.getSymbol());
                }
            });
            sAdapter.notifyDataSetChanged();
        }
        else{

        }
    }

    /* Dialog boxes to be displayed*/
    public void dataNotFoundDialog(String symbol){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Data for stock symbol");
        builder.setTitle("Symbol Not Found: "+symbol);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void duplicateDialog(String symbol){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.baseline_report_problem_24);
        builder.setMessage("Stock Symbol " + symbol + " is already displayed");
        builder.setTitle("Duplicate Stock");
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
