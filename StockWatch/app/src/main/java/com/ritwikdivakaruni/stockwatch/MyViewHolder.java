package com.ritwikdivakaruni.stockwatch;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class MyViewHolder extends RecyclerView.ViewHolder {

    public TextView companySymbol;
    public TextView companyName;
    public TextView price;
    public TextView priceChange;
    public TextView changePercentage;
    public ImageView caret;

    public MyViewHolder(View view) {
        super(view);
        companySymbol = view.findViewById(R.id.symbol);
        companyName = view.findViewById(R.id.companyName);
        price = view.findViewById(R.id.price);
        priceChange = view.findViewById(R.id.priceChange);
        changePercentage = view.findViewById(R.id.changePercentage);
        caret = view.findViewById(R.id.caret);
    }
}