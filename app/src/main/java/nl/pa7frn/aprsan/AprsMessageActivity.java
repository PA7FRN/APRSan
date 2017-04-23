package nl.pa7frn.aprsan;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class AprsMessageActivity  extends AppCompatActivity {
    private AprsService aprsService;
    private ServiceConnection mConnection;
    private BroadcastReceiver receiver;

    TextView tvCallsign;
    TextView tvMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aprs_message);

        tvCallsign = (TextView)findViewById(R.id.tvCallsign);
        tvMessage  = (TextView)findViewById(R.id.tvMessage);

        mConnection = new ServiceConnection() {

            public void onServiceConnected(ComponentName className, IBinder binder) {
                AprsService.MyBinder b = (AprsService.MyBinder) binder;
                aprsService = b.getService();
                aprsService.updateMsg();
            }

            public void onServiceDisconnected(ComponentName className) {
                aprsService = null;
            }
        };

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case "MESSAGE":
                        tvCallsign.setText(intent.getStringExtra("callsign"));
                        tvMessage.setText(intent.getStringExtra("text"));
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter("MESSAGE");
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

    public void onCloseClick(View view) {
        if (aprsService != null) {
            aprsService.closeMessage();
            Toast.makeText(this, aprsService.getMsgCount() + " messages", Toast.LENGTH_SHORT).show();
        }
    }

    public void onReplyOkClick(View view) {
        if (aprsService != null) {
            aprsService.replyMessage("Yes OK");
            Toast.makeText(this, aprsService.getMsgCount() + " messages", Toast.LENGTH_SHORT).show();
        }
    }

    public void onReplyNotOkClick(View view) {
        if (aprsService != null) {
            aprsService.replyMessage("Negative");
            Toast.makeText(this, aprsService.getMsgCount() + " messages", Toast.LENGTH_SHORT).show();
        }
    }

    public void onReplyLaterClick(View view) {
        if (aprsService != null) {
            aprsService.replyMessage("Will reply later");
            Toast.makeText(this, aprsService.getMsgCount() + " messages", Toast.LENGTH_SHORT).show();
        }
    }

}
