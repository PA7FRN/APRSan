package nl.pa7frn.aprsan;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by earts001 on 2/26/2017.
 * Show log
 */

public class AprsLogActivity extends ListActivity {
    private AprsService s;
    private ServiceConnection mConnection;
    private BroadcastReceiver receiver;
    private ArrayAdapter<String> adapter;
    private List<String> wordList;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aprs_log);

        wordList = new ArrayList<>();

        adapter = new ArrayAdapter<>(
                this,
                R.layout.rowlayout, R.id.logitem,
                wordList
        );
        setListAdapter(adapter);

        mConnection = new ServiceConnection() {

            public void onServiceConnected(ComponentName className, IBinder binder) {
                AprsService.MyBinder b = (AprsService.MyBinder) binder;
                s = b.getService();
                wordList.clear();
                s.update();
            }

            public void onServiceDisconnected(ComponentName className) {
                s = null;
            }
        };

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                switch (action) {
                    case "LOG":
                        wordList.add(intent.getStringExtra("logline"));
                        adapter.notifyDataSetChanged();
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter("MESSAGE");
        filter.addAction("LOG");
        LocalBroadcastManager.getInstance(this).registerReceiver(
                (receiver), filter
        );
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent= new Intent(this, AprsService.class);
        bindService(
                intent, mConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    @Override
    protected void onStop() {
        unbindService(mConnection);
        super.onStop();
    }

}
