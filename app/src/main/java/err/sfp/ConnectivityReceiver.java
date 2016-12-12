package err.sfp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by Err on 16-12-7.
 */

public class ConnectivityReceiver extends BroadcastReceiver implements Consts {

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean online = Utils.isOnline(context);
        Log.i(T, "connectivity state changed to "+online);
        context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
                .edit().putBoolean(ONLINE, online);
        broadcastConState(online, context);
    }

    public void broadcastConState(boolean state, Context context) {
        Intent intent = new Intent();
        intent.setAction(SHAREDPREFERENCES_ACTION_FILTER);
        intent.putExtra(PREFERENCE_TYPE, ONLINE_FLAG);
        intent.putExtra(ONLINE, state);
        context.sendBroadcast(intent);
    }
}
