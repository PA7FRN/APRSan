package nl.pa7frn.aprsan;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private AprsService aprsService;
    private ServiceConnection mConnection;
    BroadcastReceiver receiver;
    private GoogleMap mMap;
    private Bitmap allSymbols;
    private AprsSymbols mSymbols;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mConnection = new ServiceConnection() {

            public void onServiceConnected(ComponentName className, IBinder binder) {
                AprsService.MyBinder b = (AprsService.MyBinder) binder;
                aprsService = b.getService();
                AprsRecord station = aprsService.getMyStation();
                float zoom = aprsService.loadZoom();
                LatLng latLng = station.getLatLng();
                if (latLng != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
                }
                aprsService.updateMap();
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
                    case "ADD_MY_STATION":
                        addMyStation();
                        break;
                    case "MOVE_MY_STATION":
                        moveMyStation(intent.getBooleanExtra("is_beacon", false));
                        break;
                    case "ADD_STATION":
                        addStation(intent.getIntExtra("index", -1));
                        break;
                    case "REMOVE_STATION":
                        removeStation(intent.getIntExtra("index", -1));
                        break;
                    case "MOVE_STATION":
                        moveStation(intent.getIntExtra("index", -1));
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter("ADD_MY_STATION");
        filter.addAction("MOVE_MY_STATION");
        filter.addAction("ADD_STATION");
        filter.addAction("REMOVE_STATION");
        filter.addAction("MOVE_STATION");
        LocalBroadcastManager.getInstance(this).registerReceiver(
                (receiver), filter
        );
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        allSymbols.recycle();
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
        CameraPosition mMyCam = mMap.getCameraPosition();
        aprsService.saveZoom(mMyCam.zoom);
        unbindService(mConnection);
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setTrafficEnabled(true);
        allSymbols = BitmapFactory.decodeResource(getResources(),
                R.drawable.allicons);
        mSymbols = new AprsSymbols(allSymbols);
    }

    private void addStation(int stationIndex) {
        AprsRecord station = aprsService.getStation(stationIndex);
        if (station != null) {
            station.setMarker(mMap, mSymbols);
        }
    }

    private void removeStation(int stationIndex) {
        AprsRecord station = aprsService.getStation(stationIndex);
        if (station != null) {
            station.removeMarker();
        }
    }

    private void moveStation(int stationIndex) {
        AprsRecord station = aprsService.getStation(stationIndex);
        if (station != null) {
            station.moveMarker(mMap, mSymbols, true);
        }
    }

     private void addMyStation() {
         AprsRecord station = aprsService.getMyStation();
         station.setMarker(mMap, mSymbols);
         LatLng latLng = station.getLatLng();
         if (latLng != null) {
             mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
         }
    }

    private void moveMyStation(boolean isBeacon) {
        AprsRecord station = aprsService.getMyStation();
        station.moveMarker(mMap, mSymbols, isBeacon);
        LatLng latLng = station.getLatLng();
        if (latLng != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

}
