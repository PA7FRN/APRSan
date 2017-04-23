package nl.pa7frn.aprsan;

import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

class AprsRecord {
    private AprsDecoder aprsDecoder;
    private boolean  log;
    private boolean  visible;
    private boolean  reached;
    private int      radius;
    private Location location;
    private boolean  locationKnown = false;
    String  name = "";
    private String   txData;
    private Long     lastHeard;
    private AprsSymbol aprsSymbol;
    private boolean  symbolChanged = false;
    private Marker   marker;

    AprsRecord(
            AprsDecoder aAprsDecoder,
            String aName,
            String packet,
            String aTxData,
            int eventRadius,
            boolean doLog,
            boolean doTx,
            boolean isReached
    ) {
        lastHeard = System.currentTimeMillis()/1000;
        aprsDecoder = aAprsDecoder;
        txData = aTxData;
        aprsSymbol = new AprsSymbol();
        aprsSymbol.symbol = 14;
        symbolChanged = true;
        aprsDecoder.decodeAprsSymbol(packet, aprsSymbol);
        log = doLog;
        visible = doTx;
        reached = isReached;
        radius = eventRadius;
        name = aName;
        location = new Location("");
    }

    AprsRecord(
            AprsDecoder aAprsDecoder,
            String aName,
            String packet,
            String aTxData,
            int eventRadius,
            boolean doLog,
            boolean doTx,
            boolean isReached,
            Double aLat,
            Double aLon
    ) {
        lastHeard = System.currentTimeMillis()/1000;
        aprsDecoder = aAprsDecoder;
        txData = aTxData;
        aprsSymbol = new AprsSymbol();
        aprsSymbol.symbol = 14;
        symbolChanged = true;
        aprsDecoder.decodeAprsSymbol(packet, aprsSymbol);
        log = doLog;
        visible = doTx;
        reached = isReached;
        radius = eventRadius;
        name = aName;
        location = new Location("");
        location.setLatitude(aLat);
        location.setLongitude(aLon);
        locationKnown = true;
    }

    boolean  getLog() {
        return log;
    }
    boolean  getReached() {
        return reached;
    }
    Long     getLastHeard() { return lastHeard; }
    String   getName()  {
        return name;
    }
    boolean  getVisible() {
        return visible;
    }
    String   getTxData() {
        return txData;
    }

    void setLocation(Location aLocation, String packet) {
        location.set(aLocation);
        locationKnown = true;
        symbolChanged = aprsDecoder.decodeAprsSymbol(packet, aprsSymbol);
        lastHeard = System.currentTimeMillis()/1000;
    }

    Location getLocation() { return location; }

    LatLng getLatLng() {
        if (locationKnown) {
            return new LatLng(location.getLatitude(), location.getLongitude());
        }
        else {
            return null;
        }
    }

    void setMarker(GoogleMap map, AprsSymbols aprsSymbols) {
        if (marker != null) {
            marker.remove();
        }
        if (locationKnown) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            marker = map.addMarker(new MarkerOptions().position(latLng).title(name));
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(aprsSymbols.getSymbolBitmap(aprsSymbol)));
            symbolChanged = false;
            marker.showInfoWindow();
        }
    }

    void removeMarker() {
        if (marker != null) {
            marker.remove();
            marker = null;
        }
        symbolChanged = false;
    }

    void moveMarker(GoogleMap map, AprsSymbols aprsSymbols, boolean doShowInfoWindow) {
        if (locationKnown) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (marker == null) {
                marker = map.addMarker(new MarkerOptions().position(latLng).title(name));
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(aprsSymbols.getSymbolBitmap(aprsSymbol)));
                symbolChanged = false;
            } else {
                marker.setPosition(latLng);
                if (symbolChanged) {
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(aprsSymbols.getSymbolBitmap(aprsSymbol)));
                    symbolChanged = false;
                }
            }
            if (doShowInfoWindow) {
                marker.showInfoWindow();
            }
        }
    }

    boolean checkDistance(Location aLocation) {
        return locationKnown && (location.distanceTo(aLocation) < radius);
    }
}

class AprsData extends ArrayList<AprsRecord> {
    private int nextItemToSend = 0;
    private AprsDecoder aprsDecoder;

    AprsData (AprsDecoder aAprsDecoder) {
        aprsDecoder = aAprsDecoder;
    }

    AprsRecord addRecord(
            String aName,
            String packet,
            String aTxData,
            int eventRadius,
            boolean doLog,
            boolean doTx,
            boolean isReached,
            Double aLat,
            Double aLon
    ) {
        AprsRecord aprsRecord = new AprsRecord(
                aprsDecoder,
                aName,
                packet,
                aTxData,
                eventRadius,
                doLog,
                doTx,
                isReached,
                aLat,
                aLon
        );
        add(aprsRecord);
        return aprsRecord;
    }

    int indexOf(String aName) {
        int itemCount = size();
        int idx = 0;
        int foundIdx = -1;

        while ((foundIdx < 0) && (idx < itemCount)) {
            if (aName.equals(get(idx).getName())) {
                foundIdx = idx;
            }
            idx++;
        }

        return foundIdx;
    }

    String getNextPacketString() {
        int itemCount = size();
        Boolean found = false;
        String strPacket = "";

        if (nextItemToSend >= itemCount) {
            nextItemToSend = 0;
        }
        while ((!found) && (nextItemToSend < itemCount)) {
            AprsRecord aprsRecord = get(nextItemToSend);
            if (aprsRecord.getVisible()) {
                strPacket = aprsRecord.getTxData();
                found = true;
            }
            nextItemToSend++;
        }
        if (!found) {
            nextItemToSend = 0;
            while ((!found) && (nextItemToSend < itemCount)) {
                AprsRecord aprsRecord = get(nextItemToSend);
                if (aprsRecord.getVisible()) {
                    strPacket = aprsRecord.getTxData();
                    found = true;
                }
                nextItemToSend++;
            }
        }

        return strPacket;
    }
}