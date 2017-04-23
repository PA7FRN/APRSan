package nl.pa7frn.aprsan;

import android.Manifest;
import android.graphics.Bitmap;
import android.os.Environment;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Xml;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class AprsSymbol {
    public int symbol;
    public int overlay;
}

class AprsDecoder {
    private static final int POSITION_SYMBOL_TABLE = 9;
    private static final int POSITION_SYMBOL_CODE = 19;
    private static final int COMPR_POSITION_SYMBOL_TABLE = 1;
    private static final int COMPR_POSITION_SYMBOL_CODE = 10;
    private static final int POSITION_TS_SYMBOL_TABLE = 16;
    private static final int POSITION_TS_SYMBOL_CODE = 26;
    private static final int COMPR_POSITION_TS_SYMBOL_TABLE = 8;
    private static final int COMPR_POSITION_TS_SYMBOL_CODE = 17;
    private static final int OBJECT_SYMBOL_TABLE = 26;
    private static final int OBJECT_SYMBOL_CODE = 36;
    private static final int COMPR_OBJECT_SYMBOL_TABLE = 18;
    private static final int COMPR_OBJECT_SYMBOL_CODE = 27;
    private static final int MICE_SYMBOL_TABLE = 8;
    private static final int MICE_SYMBOL_CODE = 7;

    private Bitmap aprsSymbols;

    AprsDecoder(Bitmap aSymbols) {
        aprsSymbols = aSymbols;
    }

    private String getInfoField(String packet) {
        String infoField = "";
        int fieldStart = packet.indexOf(":");
        if (fieldStart > -1) {
            fieldStart++;
            if (fieldStart < packet.length()) {
                infoField = packet.substring(fieldStart);
            }
        }
        return infoField;
    }

    private String getSymbol(String infoField, int posSymbolTable, int posSymbolCode) {
        if (infoField.length() < (posSymbolCode+1) ) {return ""; }
        String iconCode = "";
        iconCode += infoField.charAt(posSymbolTable);
        iconCode += infoField.charAt(posSymbolCode);
        return iconCode;
    }

    private String getCompressedSymbol(String infoField, int posSymbolTable, int posSymbolCode) {
        if (infoField.length() < (posSymbolCode+1) ) {return ""; }
        String iconCode = "";
        char chCheck = infoField.charAt(posSymbolTable);
        if ((chCheck < '0') || (chCheck > '9')) {
            iconCode += infoField.charAt(posSymbolTable);
            iconCode += infoField.charAt(posSymbolCode);
        }
        return iconCode;
    }

    private String getPositionSymbol(String infoField) {
        String iconCode = getCompressedSymbol(infoField, COMPR_POSITION_SYMBOL_TABLE, COMPR_POSITION_SYMBOL_CODE);
        if (iconCode.equals("")) {
            iconCode = getSymbol(infoField, POSITION_SYMBOL_TABLE, POSITION_SYMBOL_CODE);
        }
        return iconCode;
    }

    private String getPositionTimestampSymbol(String infoField) {
        String iconCode = getCompressedSymbol(infoField, COMPR_POSITION_TS_SYMBOL_TABLE, COMPR_POSITION_TS_SYMBOL_CODE);
        if (iconCode.equals("")) {
            iconCode = getSymbol(infoField, POSITION_TS_SYMBOL_TABLE, POSITION_TS_SYMBOL_CODE);
        }
        return iconCode;
    }

    private String getObjectSymbol(String infoField) {
        String iconCode = getCompressedSymbol(infoField, COMPR_OBJECT_SYMBOL_TABLE, COMPR_OBJECT_SYMBOL_CODE);
        if (iconCode.equals("")) {
            iconCode = getSymbol(infoField, OBJECT_SYMBOL_TABLE, OBJECT_SYMBOL_CODE);
        }
        return iconCode;
    }

    private String getMicEIcon(String infoField) {
        if (infoField.length() < (MICE_SYMBOL_TABLE+1) ) {return ""; }
        String iconCode = "";
        iconCode += infoField.charAt(MICE_SYMBOL_TABLE);
        iconCode += infoField.charAt(MICE_SYMBOL_CODE);
        return iconCode;
    }

    private String getItemSymbol(String infoField) {
        int idx = infoField.indexOf("!");
        if (idx < 0) {
            idx = infoField.indexOf("_");
        }

        if (idx < 0) { return ""; }

        String itemField = infoField.substring(idx);
        String iconCode = getCompressedSymbol(itemField, COMPR_POSITION_SYMBOL_TABLE, COMPR_POSITION_SYMBOL_CODE);
        if (iconCode.equals("")) {
            iconCode = getSymbol(itemField, POSITION_SYMBOL_TABLE, POSITION_SYMBOL_CODE);
        }
        return iconCode;
    }

    boolean decodeAprsSymbol(String packet, AprsSymbol aprsSymbol) {

        if (packet.equals("")) { return false; }

        String infoField = getInfoField(packet);

        if (infoField.equals("")) { return false; }

        String iconCode;
        char dataType = infoField.charAt(0);
        switch (dataType) {
            case 0x1C:
                iconCode = getMicEIcon(infoField);
                break;
            case 0x1D:
                iconCode = getMicEIcon(infoField);
                break;
            case '!':
                iconCode = getPositionSymbol(infoField);
                break;
            case ')':
                iconCode = getItemSymbol(infoField);
                break;
            case '\'':
                iconCode = getMicEIcon(infoField);
                break;
            case '/':
                iconCode = getPositionTimestampSymbol(infoField);
                break;
            case ';':
                iconCode = getObjectSymbol(infoField);
                break;
            case '=':
                iconCode = getPositionSymbol(infoField);
                break;
            case '@':
                iconCode = getPositionTimestampSymbol(infoField);
                break;
            case 96: //'‘':
                iconCode = getMicEIcon(infoField);
                break;
            default:
                iconCode = "";
        }

        if (iconCode.equals("")) {
            int idx =  infoField.indexOf("!");
            if (idx > 0 ) {
                if (idx < 40) {
                    iconCode = getPositionSymbol(infoField.substring(idx));
                }
            }
        }

        if (iconCode.equals("")) { return false; }

        char symbolTable = iconCode.charAt(0);
        char symbolCode = iconCode.charAt(1);

        int bitmap = symbolCode-33;
        if (bitmap <  0) { return false; }
        if (bitmap > 93) { return false; }

        if (symbolTable != '/') {
            bitmap += 96;
        }

        boolean result = (aprsSymbol.symbol == bitmap);
        aprsSymbol.symbol = bitmap;

        return result;

    }

    Bitmap getSymbolBitmap(AprsSymbol aprsSymbol) {
        int symboleSize = 192; // 190 .. 195

        int idxX = aprsSymbol.symbol % 16;
        int idxY = aprsSymbol.symbol / 16;

        return Bitmap.createScaledBitmap(
                Bitmap.createBitmap(aprsSymbols, idxX*symboleSize, idxY*symboleSize, symboleSize, symboleSize),
                64,64,false
        );
    }
}

class AprsRecord {
    private AprsDecoder aprsDecoder;
    private boolean  log;
    private boolean  visible;
    private boolean  reached;
    private int      radius;
    private Location location;
    private String   name;
    private String   txData;
    private Long     lastHeard;
    private AprsSymbol aprsSymbol;
    private boolean  iconChanged = false;
    private Marker   marker;

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
        iconChanged = true;
        aprsDecoder.decodeAprsSymbol(packet, aprsSymbol);
        log = doLog;
        visible = doTx;
        reached = isReached;
        radius = eventRadius;
        name = aName;
        location = new Location("");
        location.setLatitude(aLat);
        location.setLongitude(aLon);
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
        iconChanged = aprsDecoder.decodeAprsSymbol(packet, aprsSymbol);
        location.set(aLocation);
        lastHeard = System.currentTimeMillis()/1000;
    }

    Location getLocation() { return location; }

    void setMarker(GoogleMap map) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (marker != null) {
            marker.remove();
        }
        marker = map.addMarker(new MarkerOptions().position(latLng).title(name));
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(aprsDecoder.getSymbolBitmap(aprsSymbol)));
        iconChanged = false;
        marker.showInfoWindow();
    }

    void removeMarker() {
        if (marker != null) {
            marker.remove();
            marker = null;
        }
    }

    void moveMarker(GoogleMap map, boolean gps) {
        /*old code*/
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (marker == null) {
            marker = map.addMarker(new MarkerOptions().position(latLng).title(name));
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(aprsDecoder.getSymbolBitmap(aprsSymbol)));
            iconChanged = false;
        }
        else {
            marker.setPosition(latLng);
            if (iconChanged) {
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(aprsDecoder.getSymbolBitmap(aprsSymbol)));
                iconChanged = false;
            }
        }
        if (!gps) {
            marker.showInfoWindow();
        }
    }

    boolean checkDistance(Location aLocation) {
        reached = (location.distanceTo(aLocation) < radius);
        return reached;
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

 //   @Override
 //   public AprsRecord remove(int index) {
 //       get(index).removeMarker();
 //       return super.remove(index);
 //   }

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

class AprsDataLoader {
    private SharedPreferences aprsSettings;
    private AprsPermissions aprsPermissions;

    AprsDataLoader(SharedPreferences settings, AprsPermissions aAprsPermissions) {
        aprsSettings = settings;
        aprsPermissions = aAprsPermissions;
    }

    void loadData(AprsData aprsData) {
        aprsData.clear();

        try {
            try {
                parseXML(aprsData);
            } catch (XmlPullParserException ppe){
                //	addToLog(ppe.getMessage());
            }
        } catch (IOException ioe) {
            //	addToLog(ioe.getMessage());
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void parseXML(AprsData aprsData) throws XmlPullParserException, IOException {
        if (!isExternalStorageWritable()) {
            return;
        }

        if (!aprsPermissions.negotiatePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return;
        }

        File sdcard = Environment.getExternalStorageDirectory();
        File dir = new File(sdcard, "aprs");
        dir.mkdirs();

        File file;
        file = new File(dir, "items.xml");
        InputStream inputStream = new FileInputStream(file);
        try {
            XmlPullParser xmlParser = Xml.newPullParser();
            xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            xmlParser.setInput(inputStream, null);
            xmlParser.nextTag();
            parseXMLAndStoreIt(xmlParser, aprsData);
        } finally {
            inputStream.close();
        }
    }

    private void parseXMLAndStoreIt(XmlPullParser aParser, AprsData aprsData) throws XmlPullParserException, IOException {
        String strTagName;
        aParser.require(XmlPullParser.START_TAG, null, "items");
        strTagName = "item";
        while (aParser.nextTag() != XmlPullParser.END_DOCUMENT) {
            if (aParser.getEventType() == XmlPullParser.START_TAG) {
                if (aParser.getName().equals(strTagName)) {
                    addAprsRecord(aParser, aprsData);
                }
            }
        }
    }

    private void addAprsRecord(XmlPullParser aParser, AprsData aprsData) {
        String  strXmlName   = aParser.getAttributeValue(null, "name");
        int     intXmlRadius = getXmlInt( aParser,  "radius");
        Boolean blnXmlLog    = getXmlBool(aParser,  "log"   );
        Boolean blnReached   = aprsSettings.getBoolean(strXmlName, false);

        String strXmlLat = aParser.getAttributeValue(null, "lat");
        String strXmlLon = aParser.getAttributeValue(null, "lon");
        Double dblXmlLat = Double.valueOf(strXmlLat);
        Double dblXmlLon = Double.valueOf(strXmlLon);
        Double dblDegLat = dblXmlLat / 3600000;
        Double dblDegLon = dblXmlLon / 3600000;

        String strXmlSymbol = aParser.getAttributeValue(null,  "symbol");
        String strXmlSymbol1 = "\\";
        String strXmlSymbol2 = "o";
        if (strXmlSymbol != null) {
            if(strXmlSymbol.length() > 1) {
                strXmlSymbol1 = strXmlSymbol.substring(0, 1);
                strXmlSymbol2 = strXmlSymbol.substring(1, 2);
            }
        }

        String txData = ")" + strXmlName + "!" + convertToAprsFormat(true, dblDegLat) + strXmlSymbol1 + convertToAprsFormat(false, dblDegLon) + strXmlSymbol2;
        Boolean blnVisible = getXmlBool(aParser,  "visible");

        aprsData.addRecord(
                strXmlName,
                "",
                txData,
                intXmlRadius,
                blnXmlLog,
                blnVisible,
                blnReached,
                dblDegLat,
                dblDegLon
        );
    }

    private int getXmlInt(XmlPullParser aParser, String attr) {
        String strVal  = aParser.getAttributeValue(null,  attr);
        if ( strVal == null) {
            return 0;
        }
        else {
            return Integer.valueOf(strVal);
        }
    }

    private Boolean getXmlBool(XmlPullParser aParser, String attr) {
        String strVal  = aParser.getAttributeValue(null,  attr);
        if ( strVal == null) {
            strVal = "false";
        }
        return strVal.equals("true");
    }

    private String convertToAprsFormat(boolean LatNotLon, Double dblDeg) {
        Double dblAprsDeg = Math.floor(dblDeg);
        Double dblAprsMin = Math.floor((dblDeg - dblAprsDeg) * 6000);

        int intAprsDeg = dblAprsDeg.intValue();
        int intAprsMin = dblAprsMin.intValue();

        String strAprsDeg = Integer.toString(intAprsDeg);  // d..
        String strAprsMin = Integer.toString(intAprsMin);  // mm..
        String strAprsDir;

        while (strAprsMin.length() < 4) {
            strAprsMin = "0" + strAprsMin;
        } // mmmm
        strAprsMin = strAprsMin.substring(0, 2) + "." + strAprsMin.substring(2, strAprsMin.length());   //     mm.mm

        if (LatNotLon) {
            while (strAprsDeg.length() < 2) {
                strAprsDeg = "0" + strAprsDeg;
            } // dd
            if (dblAprsDeg > 0) {
                strAprsDir = "N";
            }
            else {
                strAprsDir = "S";
            }
        }
        else {
            while (strAprsDeg.length() < 3) {
                strAprsDeg = "0" + strAprsDeg;
            } // ddd
            if (dblAprsDeg > 0) {
                strAprsDir = "E";
            }
            else {
                strAprsDir = "W";
            }
        }
        // lat:  ddmm.mmY -> 8
        // lon: dddmm.mmX -> 9
        return strAprsDeg + strAprsMin + strAprsDir;
    }

}
