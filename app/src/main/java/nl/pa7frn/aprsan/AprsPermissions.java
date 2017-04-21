package nl.pa7frn.aprsan;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by earts001 on 2/25/2017.
 * Generic way of negotiation of permissions
 */

class AprsPermissions {
    private Context context;
    private LocalBroadcastManager broadcaster;

    AprsPermissions(Context aContext, LocalBroadcastManager aBroadcaster) {
        context = aContext;
        broadcaster = aBroadcaster;
    }

    boolean negotiatePermission(String permission) {
        if (!checkPermission(permission)) {
            toUiGetPermission(permission);
        }
        return checkPermission(permission);
    }

    boolean checkPermission(String permission) {
        return (ActivityCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED);
    }

    private void toUiGetPermission(String permission) {
        Intent dialogIntent = new Intent(context, AprsAnActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(dialogIntent);

        Intent intent = new Intent("PERMISSION_REQUEST");
        intent.putExtra("permission", permission);
        intent.putExtra("requestCode", 12345);
        broadcaster.sendBroadcast(intent);
    }

}
