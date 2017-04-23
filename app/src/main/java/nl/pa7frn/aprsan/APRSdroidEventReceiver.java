package nl.pa7frn.aprsan;

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
