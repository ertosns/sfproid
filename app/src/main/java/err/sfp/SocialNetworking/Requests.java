package err.sfp.SocialNetworking;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;
import java.io.IOException;
import java.io.InputStream;
import err.sfp.Consts;
import err.sfp.Database.DBContract;
import err.sfp.Database.Database;
import err.sfp.Download;
import err.sfp.HttpConThread;
import err.sfp.Listener;
import err.sfp.Main;
import err.sfp.R;
import err.sfp.Utils;

/**
 * Created by Err on 17-1-2.
 */

public class Requests extends AppCompatActivity implements Consts {
    // view all un-responded requests, and only new are passed in notification
    // GridView is init in one shot, you can't load GridView while it's downloading|reading_database, assume listView is the same since no doc about that
    ListView listView;
    Database database;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle onSaveInstance) {
        super.onCreate(onSaveInstance);
        setContentView(R.layout.songs);
        database = Main.database;
        registerReceiver(notificationReceiver, null);
        toolbar = (Toolbar) findViewById(R.id.songstoolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Requests.this, Download.class);
                //TODO set flag
                startActivity(intent);
            }
        });
        listView  = (ListView) findViewById(R.id.songslv);
        new Thread(new InitList(this, listView)).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(notificationReceiver);
    }

    BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] senderId = intent.getByteArrayExtra(PEER_REQUEST_SENDER_ID);
            boolean accept = (intent.getIntExtra(PEERSHIP_REQUEST_ACCPTED, TRUE) == TRUE)? true:false;
            if(accept) Utils.acceptPeershipRequest(senderId);
            else Utils.removePeershipRequest(senderId);
        }
    };

    class InitList implements  Runnable {
        Requests request;
        ListView list;

        public InitList(Requests request, ListView list) {
            this.request = request;
            this.list = list;
        }

        @Override
        public void run() {
            listView.setAdapter(new SongUserAdapter(request, getRequestsItemsInfo(), SongUserAdapter.REQUESTS));
        }
    }

    public ItemInfo[] getRequestsItemsInfo() {
        // check if data is present in database, which is always true unless table isn't created, in that case
        // either user never received song from peer, or reInstalled the app

        // listener receives requests as ids, and either store ids, and fetch rest of data when needed,
        // TODO or install all users upon receiving ids, which lead to faster response (better)

        ItemInfo[] ii = null;
        if(!database.hasTable(DBContract.PEERS_TABLE)) {
            Utils.getPeersFromServer(ii, false);
            database.insertRequestsItemsInfo(ii);
        } else ii = database.getPeersShip(false); //false(not peers, but requests)
        return ii;
    }



}
