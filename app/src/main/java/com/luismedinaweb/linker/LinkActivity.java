package com.luismedinaweb.linker;

import android.app.FragmentManager;
import android.content.Intent;
import android.app.Activity;
import android.os.Bundle;
import android.os.Debug;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class LinkActivity extends Activity implements SendToServerTaskFragment.TaskCallbacks {

    private static final String TAG = "LinkActivity";
    private PrefHelper prefHelper;
    private static final String SEND_TASK_FRAGMENT = "send_task_fragment";
    private SendToServerTaskFragment mSendTaskFragment;
    private FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        prefHelper = PrefHelper.getInstance(this);
        TextView msgText = (TextView) findViewById(R.id.txt_message);
        Button setupButton = (Button) findViewById(R.id.btn_setup);

        fm = getFragmentManager();
        mSendTaskFragment = (SendToServerTaskFragment) fm.findFragmentByTag(SEND_TASK_FRAGMENT);
        if (mSendTaskFragment != null) {
            if(!mSendTaskFragment.isWorking()){
                fm.beginTransaction().remove(mSendTaskFragment).commit();
                mSendTaskFragment = null;
            }
        }

        final Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                if(prefHelper.haveValidServer()){
                    msgText.setText("Sending link to " + prefHelper.getServerName());
                    setupButton.setVisibility(Button.GONE);
                    handleText(intent);
                }
                else {
                    msgText.setText("Please setup a host computer");
                    setupButton.setVisibility(Button.VISIBLE);
                    setupButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class);
                            mainActivity.putExtra("url", intent.getStringExtra(Intent.EXTRA_TEXT));
                            startActivity(mainActivity);
                        }
                    });
                }
            }
        }

    }

    private void handleText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            if((mSendTaskFragment != null && !mSendTaskFragment.isWorking()) || mSendTaskFragment == null){
                mSendTaskFragment = new SendToServerTaskFragment();
                Bundle params = new Bundle();
                params.putString("url", sharedText);
                mSendTaskFragment.setArguments(params);
                fm.beginTransaction().add(mSendTaskFragment, SEND_TASK_FRAGMENT).commit();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }

    @Override
    public void onSendPostExecute(String result) {
        processResult(result);
    }

    private void processResult(String result){
        if(result != null){
            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
        }
        this.finish();
    }

}
