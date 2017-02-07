package err.sfp.SocialNetworking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ListView;
import err.sfp.Consts;
import err.sfp.Database.Database;
import err.sfp.R;
import err.sfp.Utils;

/**
 * Created by Err on 17-1-2.
 */

public class Songs extends AppCompatActivity implements Consts
{

    ListView listView;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle onSaveInstance)
    {
        super.onCreate(onSaveInstance);
        setContentView(R.layout.crudelist);
        IntentFilter filter = new IntentFilter(SONG_ACTION);
        registerReceiver(notificationReceiver, filter);
        /*toolbar = (Toolbar) findViewById(R.id.songstoolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(Songs.this, Download.class);
                //TODO set flag
                startActivity(intent);
            }
        });*/
        listView  = (ListView) findViewById(R.id.listview);
        listView.setAdapter(new SongUserAdapter(this, Database.getDatabase().getSongsInfo(),
                SongUserAdapter.SONGS, listView));
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(notificationReceiver);
    }

    BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] songDatabaseIdBytes = intent.getByteArrayExtra(SONG_DATABASE_ID);
            boolean accept = (intent.getIntExtra(DOWNLOAD_SONG, TRUE) == TRUE)? true:false;
            String songId = intent.getStringExtra(SONG_ID);
            String title = intent.getStringExtra(TITLE);
            if(accept) Utils.downloadSong(songId, title);
            else Utils.removeSong(songDatabaseIdBytes);
        }
    };
}
