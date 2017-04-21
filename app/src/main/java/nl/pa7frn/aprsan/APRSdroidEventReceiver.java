package nl.pa7frn.aprsan;

/**
 * Created by earts001 on 2/19/2017.
 * Starts the APRSan receiver is APRSdroid  is active.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class APRSdroidEventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent)
    {
        intent.setClass(context, AprsService.class);
        context.startService(intent);
    }
}
