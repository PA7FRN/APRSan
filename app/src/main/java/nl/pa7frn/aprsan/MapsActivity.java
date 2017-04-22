package nl.pa7frn.aprsan;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
                LatLng latLng = new LatLng(station.getLocation().getLatitude(), station.getLocation().getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
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
                        moveMyStation(intent.getBooleanExtra("gps", false));
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
                    case "ZOOM_MAP":
                        zoomMap();
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter("ADD_MY_STATION");
        filter.addAction("MOVE_MY_STATION");
        filter.addAction("ADD_STATION");
        filter.addAction("REMOVE_STATION");
        filter.addAction("MOVE_STATION");
        filter.addAction("ZOOM_MAP");
        LocalBroadcastManager.getInstance(this).registerReceiver(
                (receiver), filter
        );
    }

    @Override
    public void onDestroy() {
        CameraPosition mMyCam = mMap.getCameraPosition();
        float zoom = mMyCam.zoom;
        aprsService.saveZoom(zoom);
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
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setTrafficEnabled(true);
    }

    private void addStation(int stationIndex) {
        AprsRecord station = aprsService.getStation(stationIndex);
        if (station != null) {
            station.setMarker(mMap);
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
            station.moveMarker(mMap, false);
        }
    }

     private void addMyStation() {
        AprsRecord station = aprsService.getMyStation();
        station.setMarker(mMap);
        LatLng latLng = new LatLng(station.getLocation().getLatitude(), station.getLocation().getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    private void moveMyStation(boolean gps) {
        AprsRecord station = aprsService.getMyStation();
        station.moveMarker(mMap, gps);
        LatLng latLng = new LatLng(station.getLocation().getLatitude(), station.getLocation().getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    private void zoomMap() {
        mMap.animateCamera(CameraUpdateFactory.zoomTo(10.0f));
     }

}
