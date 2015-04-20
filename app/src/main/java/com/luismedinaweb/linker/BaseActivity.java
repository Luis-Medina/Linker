package com.luismedinaweb.linker;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

/**
 * Created by Luis on 1/29/2015.
 */
public class BaseActivity extends ActionBarActivity {

    private Toolbar toolbar;

    protected Toolbar activateToolbar(){
        if(toolbar == null){
            toolbar = (Toolbar) findViewById(R.id.app_toolbar);
            if(toolbar != null){
                setSupportActionBar(toolbar);
            }
        }
        return toolbar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
