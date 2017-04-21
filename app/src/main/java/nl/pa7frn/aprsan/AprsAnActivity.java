package nl.pa7frn.aprsan;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class AprsAnActivity extends AppCompatActivity {
    private AprsService aprsService;
    private ServiceConnection mConnection;
    private BroadcastReceiver receiver;

    TextView tvStatus;
    TextView tvOwnCallsign;
    TextView tvOwnPosition;
    TextView tvPosCallsign;
    TextView tvPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aprs_an);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        tvStatus = (TextView)findViewById(R.id.tvStatus);

        tvOwnCallsign = (TextView)findViewById(R.id.tvOwnCallsign);
        tvOwnPosition = (TextView)findViewById(R.id.tvOwnPosition);

        tvPosCallsign = (TextView)findViewById(R.id.tvPosCallsign);
        tvPosition    = (TextView)findViewById(R.id.tvPosition);

        mConnection = new ServiceConnection() {

            public void onServiceConnected(ComponentName className, IBinder binder) {
                AprsService.MyBinder b = (AprsService.MyBinder) binder;
                aprsService = b.getService();
                if (aprsService.getAprsActive()) {
                    Toast.makeText(AprsAnActivity.this, aprsService.getMyCallsign() + " connected V. " + Integer.toString(aprsService.getApiVersion()), Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(AprsAnActivity.this, aprsService.getMyCallsign() + " connected APRS not active", Toast.LENGTH_SHORT).show();
                }
                aprsService.update();
            }

            public void onServiceDisconnected(ComponentName className) {
                aprsService = null;
            }
        };

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Location location;

                String action = intent.getAction();
                switch (action) {
                    case "STATUS":
                        if (intent.getBooleanExtra("aprsActive", false)) {
                            int version = intent.getIntExtra("version", 0);
                            tvStatus.setText(
                                    getString(R.string.aprsdroid_active) + " " + Integer.toString(version)
                            );
                        }
                        else {
                            tvStatus.setText(R.string.aprsdroid_inactive);
                        }
                        break;
                    case "MY_POSITION":
                        tvOwnCallsign.setText(intent.getStringExtra("callsign"));
                        location = intent.getParcelableExtra("location");
                        if (location != null) {
                            tvOwnPosition.setText(formatPositionString(location));
                        }
                        break;
                    case "RX_POSITION":
                        tvPosCallsign.setText(intent.getStringExtra("callsign"));
                        location = intent.getParcelableExtra("location");
                        if (location != null) {
                            tvPosition.setText(formatPositionString(location));
                        }
                        break;
                    case "PERMISSION_REQUEST":
                        ActivityCompat.requestPermissions(
                                AprsAnActivity.this ,
                                new String[]{intent.getStringExtra("permission")},
                                intent.getIntExtra("requestCode", 0)
                        );
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter("STATUS");
        filter.addAction("MY_POSITION");
        filter.addAction("RX_POSITION");
        filter.addAction("PERMISSION_REQUEST");
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_aprs_an, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_map:
                intent = new Intent(this, MapsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_log:
                intent = new Intent(this, AprsLogActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private String formatPositionString(Location aLocation) {
        Double dblLat = aLocation.getLatitude();
        String strNS = "N";
        if (dblLat < 0) {
            strNS = "S";
            dblLat = -dblLat;
        }
        String strLat = String.format("%.5f", dblLat);
        while (strLat.length() < 8) {
            strLat = "0" + strLat;
        }

        Double dblLon = aLocation.getLongitude();
        String strEW = "E";
        if (dblLon < 0) {
            strEW = "W";
            dblLon = -dblLon;
        }
        String strLon = String.format("%.5f", dblLon);
        while (strLon.length() < 9) {
            strLon = "0" + strLon;
        }
        return strLat + strNS + " " + strLon + strEW;
    }

}
