package err.sfp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by root on 16-12-10.
 */

//NOT important, but works unlike dynamically defined receiver in service listener doesn't work.
public class ListenerPreferencesReceiver extends BroadcastReceiver implements Consts{
    SharedPreferences sharedPreferences = null;
    SharedPreferences.Editor pref = null;
    @Override
    public void onReceive(Context context, Intent intent) {
        //possible received intents key sorted by frequency {stop, online}
        //one pair received at time preceded by type flag (int)
        sharedPreferences = context.getSharedPreferences(APP_PREFERENCES_LISTENER, Context.MODE_PRIVATE);
        pref = sharedPreferences.edit();
        Log.i(T, "sharedPreferences broadcast received");
        int preference = intent.getIntExtra(PREFERENCE_TYPE, STOP_FLAG);

        /*if (preference == STOP_FLAG) {
            /if (intent.getBooleanExtra(STOP_PREF, false)) new Listener().killService();
        } else if (preference == ONLINE_FLAG) {
            pref.putBoolean(ONLINE, intent.getBooleanExtra(ONLINE, false));
            pref.commit();
        }*/
    }
}
