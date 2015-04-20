package com.luismedinaweb.linker;

import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;


public class MainActivity extends BaseActivity implements SearchForServerTaskFragment.TaskCallbacks, VerifyConnectionTaskFragment.TaskCallbacks {

    private PrefHelper prefHelper;
    private ProgressBar progressBar;
    private Button searchButton;
    private TextView progressText;
    private TextView serverNameText;
    private TextView ipText;
    private TextView statusText;
    private String linkToSend = null;

    private static final String SEARCH_TASK_FRAGMENT = "search_task_fragment";
    private SearchForServerTaskFragment mSearchTaskFragment;
    private static final String VERIFY_TASK_FRAGMENT = "verify_task_fragment";
    private VerifyConnectionTaskFragment mVerifyTaskFragment;
    private FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefHelper = PrefHelper.getInstance(this);

        fm = getFragmentManager();
        mSearchTaskFragment = (SearchForServerTaskFragment) fm.findFragmentByTag(SEARCH_TASK_FRAGMENT);
        mVerifyTaskFragment = (VerifyConnectionTaskFragment) fm.findFragmentByTag(VERIFY_TASK_FRAGMENT);
        initWidgets();
        setConfigTextInView();

        initFragments();

        setupListeners();

        activateToolbar();
    }

    private void initFragments() {
        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (mSearchTaskFragment != null) {
            if (mSearchTaskFragment.isWorking()) {
                setScreenIsWorking(mSearchTaskFragment.getLastMessage());
            } else {
                fm.beginTransaction().remove(mSearchTaskFragment).commit();
                mSearchTaskFragment = null;
            }
        }
        if (mVerifyTaskFragment != null) {
            if (mVerifyTaskFragment.isWorking()) {
                setScreenIsWorking("Verifying server connection");
            } else {
                serverNameText.setText(prefHelper.getServerName());
                statusText.setText("Connected");
            }
        } else {
            mVerifyTaskFragment = new VerifyConnectionTaskFragment();
            if (prefHelper.haveValidServer()) {
                setScreenIsWorking("Verifying server connection");
                fm.beginTransaction().add(mVerifyTaskFragment, VERIFY_TASK_FRAGMENT).commit();
                searchButton.setEnabled(false);
            }
        }
    }

    private void initWidgets() {
        searchButton = (Button) findViewById(R.id.btn_search);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressText = (TextView) findViewById(R.id.progressText);
        serverNameText = (TextView) findViewById(R.id.host_name);
        statusText = (TextView) findViewById(R.id.lbl_connection_status);
        ipText = (TextView) findViewById(R.id.ipText);
    }

    private void setScreenIsWorking(String message) {
        searchButton.setText(R.string.btn_search_cancel);
        progressText.setText(message);
        progressText.setVisibility(TextView.VISIBLE);
        progressBar.setVisibility(ProgressBar.VISIBLE);
    }

    private void setupListeners() {
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((mSearchTaskFragment != null && !mSearchTaskFragment.isWorking()) || mSearchTaskFragment == null) {
                    setScreenIsWorking("");
                    mSearchTaskFragment = new SearchForServerTaskFragment();
                    fm.beginTransaction().add(mSearchTaskFragment, SEARCH_TASK_FRAGMENT).commit();
                }else if(mSearchTaskFragment.isWorking()){
                    searchButton.setEnabled(false);
                    progressText.setText("Cancelling...");
                    mSearchTaskFragment.cancel();
                }
            }
        });
    }

    private void setConfigTextInView(){
        ipText.setText(prefHelper.getIpAddress());
        serverNameText.setText(prefHelper.getServerName());
        if(prefHelper.getServerPort() == PrefHelper.defaultPort){
            ipText.setVisibility(TextView.INVISIBLE);
            statusText.setText(R.string.lbl_disconnected_status);
            statusText.setTextColor(Color.RED);
        }else{
            statusText.setText(R.string.lbl_connected_status);
            statusText.setTextColor(Color.rgb(4,141,4));
        }
        if(prefHelper.getIpAddress().equals(prefHelper.getServerName())){
            ipText.setVisibility(TextView.INVISIBLE);
        }else{
            ipText.setVisibility(TextView.VISIBLE);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        //int id = item.getItemId();



        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSearchProgressUpdate(String message) {
        progressText.setText(message);
    }

    @Override
    public void onSearchPostExecute(String[] result) {
        searchButton.setText(R.string.btn_search);
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        progressText.setVisibility(ProgressBar.INVISIBLE);
        if (result != null) {
            prefHelper.setIpAddress(result[0]);
            prefHelper.setServerPort(Integer.parseInt(result[1]));
            prefHelper.setServerName(result[2]);

            setConfigTextInView();

            if (getIntent().getStringExtra("url") != null) {
                Intent linkIntent = new Intent(getApplicationContext(), LinkActivity.class);
                linkIntent.setAction(Intent.ACTION_SEND);
                linkIntent.setType("text/plain");
                linkIntent.putExtra(Intent.EXTRA_TEXT, getIntent().getStringExtra("url"));
                startActivity(linkIntent);
                this.finish();
            }
        } else {
            prefHelper.setServerName(PrefHelper.defaultServer);
            prefHelper.setServerPort(PrefHelper.defaultPort);
            prefHelper.setIpAddress(PrefHelper.defaultIP);

            setConfigTextInView();
        }

    }

    @Override
    public void onSearchCancelled() {
        searchButton.setText(R.string.btn_search);
        searchButton.setEnabled(true);
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        progressText.setVisibility(ProgressBar.INVISIBLE);
    }


    @Override
    public void onVerifyPostExecute(Boolean connected) {
        if(!connected){
            prefHelper.setServerPort(PrefHelper.defaultPort);
            prefHelper.setServerName(PrefHelper.defaultServer);
            prefHelper.setIpAddress(PrefHelper.defaultIP);
        }
        setConfigTextInView();
        searchButton.setEnabled(true);
        searchButton.setText(R.string.btn_search);
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        progressText.setVisibility(ProgressBar.INVISIBLE);
    }
}
