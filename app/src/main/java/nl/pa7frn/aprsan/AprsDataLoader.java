package nl.pa7frn.aprsan;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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