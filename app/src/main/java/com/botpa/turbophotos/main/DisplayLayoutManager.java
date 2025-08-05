package com.botpa.turbophotos.main;

import android.content.Context;

import androidx.recyclerview.widget.LinearLayoutManager;

public class DisplayLayoutManager extends LinearLayoutManager {

    private boolean isScrollEnabled = true;


    //Layout manager
    public DisplayLayoutManager(Context context) {
        super(context);
    }

    //Scrolling
    public void setScrollEnabled(boolean flag) {
        this.isScrollEnabled = flag;
    }

    @Override
    public boolean canScrollHorizontally() {
        return isScrollEnabled && super.canScrollHorizontally();
    }

}