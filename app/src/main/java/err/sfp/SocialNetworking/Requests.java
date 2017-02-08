package err.sfp.SocialNetworking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ListView;
import err.sfp.Consts;
import err.sfp.Database.Database;
import err.sfp.Main;
import err.sfp.R;
import err.sfp.Utils;

/**
 * Created by Err on 17-1-2.
 */

public class Requests extends AppCompatActivity implements Consts
{
    ListView listView;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle onSaveInstance)
    {
        super.onCreate(onSaveInstance);
        setContentView(R.layout.crudelist);
        IntentFilter filter = new IntentFilter(REQUEST_ACTION);
        registerReceiver(notificationReceiver, filter);
        /*toolbar = (Toolbar) findViewById(R.id.songstoolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(Requests.this, Download.class);
                //TODO set flag
                startActivity(intent);
            }
        });*/
        listView  = (ListView) findViewById(R.id.listview);
        listView.setAdapter(new SongUserAdapter(this, Database.getDatabase().getPeersShip(false), SongUserAdapter.REQUESTS, listView));
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(notificationReceiver);
    }

    BroadcastReceiver notificationReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.i(T, "notification ation received");
            byte[] senderId = intent.getByteArrayExtra(PEER_REQUEST_SENDER_ID);
            boolean accept = (intent.getIntExtra(PEERSHIP_REQUEST_ACCPTED, TRUE) == TRUE)? true:false;
            if(accept) Utils.acceptPeershipRequest(senderId);
            else Utils.removePeershipRequest(senderId);
        }
    };

}
