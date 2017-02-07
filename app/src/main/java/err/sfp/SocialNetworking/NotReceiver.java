package err.sfp.SocialNetworking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import err.sfp.Consts;
import err.sfp.Utils;

/**
 * Created by Err on 17-1-19.
 */

public class NotReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(Consts.T, "notification ation received");
        byte[] senderId = intent.getByteArrayExtra(Consts.PEER_REQUEST_SENDER_ID);
        boolean accept = (intent.getIntExtra(Consts.PEERSHIP_REQUEST_ACCPTED, Consts.TRUE) == Consts.TRUE)? true:false;
        if(accept) Utils.acceptPeershipRequest(senderId);
        else Utils.removePeershipRequest(senderId);
    }
}
