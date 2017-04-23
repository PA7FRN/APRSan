package nl.pa7frn.aprsan;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ToneGenerator;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class MsgData {
    String callsign;
    String message;
}

public class AprsService extends Service {
    private ArrayList<MsgData> msgList = new ArrayList<>();
    private boolean aprsActive = false;
    private boolean aprsAlive = true;
    private boolean doSendNextItem = true;
    private boolean serviceStarted = false;
    private boolean taskCheckItemSent = true;
    private int aprsApiVersion = 0;

    private AprsPermissions aprsPermissions;
    private AprsAudioManager aprsAudioManager;

    private AprsDecoder aprsDecoder;
    private AprsRecord myStation;
    private AprsData stationsData;
    private AprsData itemsData;
    private AprsDataLoader aprsDataLoader;
    private AprsLog aprsLog;

    private LocalBroadcastManager broadcaster;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private ScheduledExecutorService scheduleTaskExecutor;
    private ScheduledFuture<?> txNextItemScheduleHandle;
    private SharedPreferences settings;

    private final IBinder mBinder = new MyBinder();

    @Override
    public void onCreate() {
        broadcaster = LocalBroadcastManager.getInstance(this);
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        aprsPermissions = new AprsPermissions(this, broadcaster);
        aprsLog = new AprsLog(aprsPermissions, "aprs_log.txt");
        aprsAudioManager = new AprsAudioManager(this);

        aprsDecoder = new AprsDecoder();
        myStation = new AprsRecord(
                aprsDecoder, "", "", "", 0, false, false, false
        );
        stationsData = new AprsData(aprsDecoder);
        itemsData = new AprsData(aprsDecoder);
        aprsDataLoader = new AprsDataLoader(settings, aprsPermissions);
        loadData();

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (aprsPermissions.negotiatePermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Location newLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (newLocation != null) {
                myStation.setLocation(newLocation, "");
            }
        }

        locationListener = new LocationListener() {

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

            @Override
            public void onLocationChanged(Location location) {
                myStation.setLocation(location, "");
                toUiMyLocation(false);
                ownLocationChange();
            }
        };

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void performPeriodicAprsTask() {
        //This has to do 2 things:
        // - Send next item on timeout
        // - Check if APRSdroid is still alive
        // - send command for removing old stations and items from the map
        if (aprsActive) {
            if (taskCheckItemSent) {
                if (doSendNextItem) {
                    doSendNextItem = false;
                    sendNextItem();
                }
                doSendNextItem = true;
                taskCheckItemSent = false;
            }
            else {
                if (!aprsAlive) {
                    aprsActive = false;
                    aprsAudioManager.stopControlVolume();
                    toUiStatus();
                }
                aprsAlive = false;
                taskCheckItemSent = true;
            }
        }
    }

    private void removeOldStations() {
        int idx = 0;
        Long now = System.currentTimeMillis()/1000;

        while (idx < stationsData.size()) {
            Long lastHeard = stationsData.get(idx).getLastHeard();
            if ((now - lastHeard) > 2000) {
                stationsData.remove(idx);
            }
            else {
                if ((now - lastHeard) > 1800) {
                    Intent intent = new Intent("REMOVE_STATION");
                    intent.putExtra("index", idx);
                    broadcaster.sendBroadcast(intent);
                }
                idx++;
            }
        }
    }

    private void handleAprsEvent(Intent intent) {
        String action = intent.getAction().replace("org.aprsdroid.app.", "");
        switch (action) {
            case "SERVICE_STARTED":
                serviceStarted = true;
                aprsApiVersion = intent.getIntExtra("api_version", 0);
                String myCallsign = intent.getStringExtra("callsign");
                myStation.name = myCallsign;

                SharedPreferences.Editor editor = settings.edit();
                editor.putInt("aprsApiVersion", aprsApiVersion);
                editor.putString("myCallsign", myCallsign);
                editor.apply();

                toUiMyLocation(false);
                aprsAudioManager.playSound(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT);
                break;
            case "SERVICE_STOPPED":
                if (scheduleTaskExecutor != null) {
                    scheduleTaskExecutor.shutdown();
                }
                aprsActive = false;
                aprsAudioManager.stopControlVolume();
                if (serviceStarted) {
                    if (aprsPermissions.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        locationManager.removeUpdates(locationListener);
                    }
                    serviceStarted = false;
                }
                toUiStatus();
                aprsAudioManager.playSound(ToneGenerator.TONE_SUP_RADIO_ACK);
                break;
            case "MESSAGE":
                MsgData msgData = new MsgData();
                msgData.callsign = intent.getStringExtra("source");
                msgData.message = intent.getStringExtra("body");
                msgList.add(msgData);
                startActivityForMessage(true);
                break;
            case "POSITION":
                aprsAlive = true;
                String callsign = intent.getStringExtra("callsign");
                Location location = intent.getParcelableExtra("location");
                String packet = intent.getStringExtra("packet");
                if ( !myStation.name.equals("") ) {
                    if (callsign.equals(myStation.name)) {
                        myStation.setLocation(location, packet);
                        toUiMyLocation(true);
                        aprsAudioManager.delaySounds();
                        if (txNextItemScheduleHandle != null) {
                            if (!txNextItemScheduleHandle.isDone()) {
                                txNextItemScheduleHandle.cancel(true);
                                sendNextItemDelayed();
                            }
                        }
                        if (doSendNextItem) {
                            doSendNextItem = false;
                            sendNextItemDelayed();
                        }
                    } else {
                        toUiRxLocation(callsign, location, packet);
                        callsignLocationChange(callsign);
                    }
                }
                break;
            case "UPDATE":
                int logType = intent.getIntExtra("type", 0);
                String logText = intent.getStringExtra("status");
                switch (logType) {
                    case 0:
                        break;
                    case 1:
                        if (logText.indexOf("AFSK") > 0) {
                            aprsAudioManager.startControlVolume();
                        }
                        break;
                    case 3:
                        if (logText.indexOf(myStation.name) == 0) {
                            aprsAudioManager.playSound(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD);
                        }
                        break;
                }
                break;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().startsWith("org.aprsdroid.app")) {
            if (!aprsActive) {
                aprsActive = true;
                serviceStarted = false;
                taskCheckItemSent = true;
                loadData();
                if (aprsPermissions.negotiatePermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
                }
                scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
                scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
                    public void run() { performPeriodicAprsTask(); }
                }, 0, 3, TimeUnit.MINUTES);
                scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
                    public void run() { removeOldStations(); }
                }, 0, 60, TimeUnit.SECONDS);

                toUiStatus();
            }
            handleAprsEvent(intent);
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    class MyBinder extends Binder {
        AprsService getService() {
            return AprsService.this;
        }
    }

    private void loadData() {
        aprsApiVersion = settings.getInt("aprsApiVersion", 0);
        myStation.name = settings.getString("myCallsign", "");
        aprsDataLoader.loadData(itemsData);
        aprsLog.loadFromFile();
    }

    private void sendPacket(String aPacket) {
        aprsAudioManager.delaySounds();
        Intent i = new Intent("org.aprsdroid.app.SEND_PACKET").setPackage("org.aprsdroid.app");
        i.putExtra("data", aPacket);
        startService(i);
    }

    private void sendNextItem() {
        String txPacket = itemsData.getNextPacketString();
        if (!txPacket.equals("")) {
            sendPacket(txPacket);
        }
    }

    private void sendNextItemDelayed() {
        txNextItemScheduleHandle = scheduleTaskExecutor.schedule(new Runnable() {
            public void run() { sendNextItem(); }
        }, 6, TimeUnit.SECONDS);
    }

    private void ownLocationChange() {
        for (int i=0; i < itemsData.size(); i++) {
            AprsRecord itemData = itemsData.get(i);
            checkDistance(itemData);
        }
    }

    private void callsignLocationChange(String aCallsign) {
        int idx;

        idx = itemsData.indexOf(aCallsign);
        if  (idx >-1) {
            checkDistance(itemsData.get(idx));
        }
    }

    private void checkDistance(AprsRecord aprsRecord) {
        boolean reached = aprsRecord.getReached();
        if (aprsRecord.checkDistance(myStation.getLocationX())) {
            if (!reached) {
                reportItemReached(aprsRecord, true);
            }
        }
        else {
            if (reached) {
                reportItemReached(aprsRecord, false);
            }
        }
    }

    private void reportItemReached(AprsRecord aprsRecord, boolean isReached) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(aprsRecord.getName(), isReached);
        editor.apply();

        if (aprsRecord.getLog()) {
            if (isReached) {
                toUiLogLine(aprsLog.addToLog("Reached " + aprsRecord.getName()));
            }
            else {
                toUiLogLine(aprsLog.addToLog("Leaving " + aprsRecord.getName()));
            }
        }
    }

    private void startActivityForMessage(boolean doPlaySound) {
        if (doPlaySound) {
            aprsAudioManager.playSound(ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_INTERGROUP);
        }

        Intent dialogIntent = new Intent(this, AprsMessageActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);
        toUiMessage();
    }

    private void startAprsMap() {
        if (msgList.size() > 0) {
            startActivityForMessage(false);
        }
        else {
            Intent dialogIntent = new Intent(this, MapsActivity.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dialogIntent);
        }
    }

    private void toUiStatus() {
        Intent intent = new Intent("STATUS");
        intent.putExtra("aprsActive", aprsActive);
        intent.putExtra("version", aprsApiVersion);
        broadcaster.sendBroadcast(intent);
    }

    private void toUiMyLocation(boolean isBeacon) {
        Intent intent = new Intent("MY_POSITION");
        intent.putExtra("callsign",  myStation.name);
        intent.putExtra("location", myStation.getLocationX());
        broadcaster.sendBroadcast(intent);

        intent = new Intent("MOVE_MY_STATION");
        intent.putExtra("is_beacon", isBeacon);
        broadcaster.sendBroadcast(intent);
    }

    private void toUiRxLocation(String callsign, Location location, String packet) {
        Intent intent = new Intent("RX_POSITION");
        intent.putExtra("callsign", callsign);
        intent.putExtra("location", location);
        intent.putExtra("packet", packet);
        broadcaster.sendBroadcast(intent);

        int idx = stationsData.indexOf(callsign);
        if  (idx < 0) {
            idx = stationsData.size();
            stationsData.addRecord(
                    callsign, packet, "", 0, false, false, false, location.getLatitude(), location.getLongitude()
            );
            intent = new Intent("ADD_STATION");
        }
        else {
            stationsData.get(idx).setLocation(location, packet);
            intent = new Intent("MOVE_STATION");
        }
        intent.putExtra("index", idx);
        broadcaster.sendBroadcast(intent);
    }

    private void toUiMessage() {
        Intent intent = new Intent("MESSAGE");
        if (msgList.size() > 0) {
            MsgData msgData = msgList.get(0);
            intent.putExtra("callsign", msgData.callsign);
            intent.putExtra("text", msgData.message);
        }
        else {
            intent.putExtra("callsign", "");
            intent.putExtra("text", "");
        }
        broadcaster.sendBroadcast(intent);
    }

    private void toUiLogLine(String logLine) {
        Intent intent = new Intent("LOG");
        intent.putExtra("logline", logLine);
        broadcaster.sendBroadcast(intent);
    }

    public void update() {
        toUiStatus();

        Intent intent = new Intent("MY_POSITION");
        intent.putExtra("callsign",  myStation.name);
        intent.putExtra("location", myStation.getLocationX());
        broadcaster.sendBroadcast(intent);

        toUiMessage();

        int logCount = aprsLog.getCount();
        for (int i=0; i<logCount; i++) {
            toUiLogLine(aprsLog.getLogLine(i));
        }
    }

    public void updateMap() {
        Intent intent;
        for (int idx = 0; idx < stationsData.size(); idx++) {
            intent = new Intent("ADD_STATION");
            intent.putExtra("index", idx);
            broadcaster.sendBroadcast(intent);
        }
        intent = new Intent("ADD_MY_STATION");
        broadcaster.sendBroadcast(intent);
    }

    public void saveZoom(float zoom) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("zoom", zoom);
        editor.apply();
    }

    public float loadZoom() {
        return  settings.getFloat("zoom", 12.0f);
    }

    public boolean getAprsActive() {
        return aprsActive;
    }

    public int getApiVersion() {
        return aprsApiVersion;
    }

    public String getMyCallsign() {
        return  myStation.name;
    }

    public int getMsgCount() {
        return msgList.size();
    }

    public void closeMessage() {
        int msgCount = msgList.size();
        if (msgCount > 0) {
            msgList.remove(0);
            toUiMessage();
        }
        if (msgCount < 2) {
            startAprsMap();
        }
    }

    public void replyMessage(String replyText) {
        int msgCount = msgList.size();
        if (msgCount > 0) {
            MsgData msgData = msgList.get(0);
            String callsignTo = msgData.callsign;
            while (callsignTo.length() < 9) {
                callsignTo = callsignTo + " ";
            }
            sendPacket(":" + callsignTo + ":" + replyText + "{8");

            msgList.remove(0);
            toUiMessage();
        }
        if (msgCount < 2) {
            startAprsMap();
        }
    }

    public AprsRecord getStation(int stationIndex) {
        AprsRecord station = null;
        if (stationIndex > -1 ) {
            if (stationIndex < stationsData.size() ) {
                station = stationsData.get(stationIndex);
            }
        }
        return station;
    }

    public AprsRecord getMyStation() {
        return myStation;
    }
}
