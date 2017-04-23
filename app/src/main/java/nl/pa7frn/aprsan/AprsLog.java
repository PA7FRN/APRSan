package nl.pa7frn.aprsan;

import android.Manifest;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

class AprsLog {
    private ArrayList<String> logList;
    private AprsPermissions aprsPermissions;
    private String fileName;

    AprsLog(AprsPermissions aAprsPermissions, String aFileName) {
        logList = new ArrayList<>();
        aprsPermissions = aAprsPermissions;
        fileName = aFileName;
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    void loadFromFile() {
        if (!isExternalStorageWritable()) {
            return;
        }

        if (!aprsPermissions.negotiatePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return;
        }

        File sdcard = Environment.getExternalStorageDirectory();
        File dir = new File(sdcard, "aprs");
        dir.mkdirs();
        File file = new File(dir, fileName);

        logList.clear();
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = r.readLine()) != null) {
                logList.add(line);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    String addToLog(String aLogString) {
        SimpleDateFormat simpDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateTimeAndMessage = simpDate.format(new Date())  + " " + aLogString;

        if (!isExternalStorageWritable()) {
            return dateTimeAndMessage;
        }

        if (!aprsPermissions.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return dateTimeAndMessage;
        }

        File sdcard = Environment.getExternalStorageDirectory();
        File dir = new File(sdcard, "aprs");
        dir.mkdirs();
        File file = new File(dir, fileName);

        FileOutputStream outputStream;
        OutputStreamWriter outputStreamWriter;

        try {
            outputStream = new FileOutputStream(file, true);
            outputStreamWriter = new OutputStreamWriter(outputStream);
            String nl = System.getProperty("line.separator");
            outputStreamWriter.write(dateTimeAndMessage + nl);
            outputStreamWriter.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        logList.add(dateTimeAndMessage);
        return dateTimeAndMessage;
    }

    int getCount() {
        return logList.size();
    }

    String getLogLine(int index) {
        return logList.get(index);
    }
}
