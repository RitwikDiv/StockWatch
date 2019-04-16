package com.ritwikdivakaruni.stockwatch;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class NameDownloader extends AsyncTask<Void, Void, String> { //  <Parameter, Progress, Result>

    private MainActivity mainActivity;
    private static final String DATA_URL = "https://api.iextrading.com/1.0/ref-data/symbols";

    public NameDownloader(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected String doInBackground(Void... voids) {
        Uri dataUri = Uri.parse(DATA_URL);
        String urlToUse = dataUri.toString();
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URL(urlToUse);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader((new InputStreamReader(is)));
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
        } catch (Exception e) {
            return null;
        }

        return sb.toString();

    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        HashMap<String,String> symbolNameList = parseJSON(s);
        mainActivity.updateData(symbolNameList);
    }

    private HashMap<String,String> parseJSON(String s) {

        HashMap<String,String> nameSymbolMap = new HashMap<>();
        try {
            JSONArray jObjMain = new JSONArray(s);

            for (int i = 0; i < jObjMain.length(); i++) {
                JSONObject jSymbolName = (JSONObject) jObjMain.get(i);
                String symbol = jSymbolName.getString("symbol");
                String name = jSymbolName.getString("name");
                nameSymbolMap.put(symbol, name);
            }
            return nameSymbolMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
