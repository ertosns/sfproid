package err.sfp.SocialNetworking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import err.sfp.Main;
import err.sfp.R;
import err.sfp.Utils;


/**
 * Created by Err on 17-1-2.
 */

public class Songs extends AppCompatActivity implements Consts {
    ListView listView;
    //gridview is init in one shot, you can't load GridView while it's downloading|reading_database, assume listView is the same since no doc about that
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
                Intent intent = new Intent(Songs.this, Download.class);
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
    //TODO FINSIH UTILS NOW
    BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] songIdBytes = intent.getByteArrayExtra(SONG_DATABASE_ID);
            boolean accept = (intent.getIntExtra(DOWNLOAD_SONG, TRUE) == TRUE)? true:false;
            String songId = intent.getStringExtra(SONG_ID);
            String title = intent.getStringExtra(TITLE);
            if(accept) Utils.updateSharedSong(songIdBytes, true, songId, title);
            else Utils.updateSharedSong(songIdBytes, false, null, null);
        }
    };

    class InitList implements  Runnable {
        Songs songs;
        ListView list;

        public InitList(Songs songs, ListView list) {
            this.songs = songs;
            this.list = list;
        }

        @Override
        public void run() {
            listView.setAdapter(new SongUserAdapter(songs, songs.getSongsItemsInfo(), SongUserAdapter.SONGS));
        }
    }

    public ItemInfo[] getSongsItemsInfo() {
        // check if data is present in database, which is always true unless table isn't created, in that case
        // either user never received song from peer, or reInstalled the app
        ItemInfo[] ii = null;
        if(!database.hasTable(DBContract.SONGS_TABLE)) {
            ii = getSongsFromServer(ii);
            database.insertSongsItemsInfo(ii);
        } else ii =  database.getSongsInfo();
        return ii;
    }

    public ItemInfo[] getSongsFromServer(ItemInfo[] ii) {
        // new app install.
        String getSongsInfoQuery = PROTOCOL+HOST+PATH+"listofsharedsongs=true&id="+ Utils.getUserUniqueId();
        try {
            HttpConThread con = new HttpConThread(getSongsInfoQuery);
            InputStream is = con.is;
            byte[] bytes;
            int songs = Utils.readInt(is);
            int size;
            for (int i = 0; i < songs; i++) {

                bytes = new byte[4];
                is.read(bytes);
                ii[i].databaseId = bytes;

                size = Utils.readInt(is);
                bytes = new byte[size];
                is.read(bytes);
                ii[i].songUrl = new String(bytes);

                size = Utils.readInt(is);
                bytes = new byte[size];
                is.read(bytes);
                ii[i].title = new String(bytes);

                bytes = new byte[8];
                is.read(bytes);
                ii[i].dateMillis = Utils.bytesToLong(bytes);

                size = Utils.readInt(is);
                bytes = new byte[size];
                is.read(bytes);
                ii[i].message = new String(bytes);

                size = Utils.readInt(is);
                bytes = new byte[size];
                ii[i].subTitle = new String(bytes);

                size = Utils.readInt(is);
                bytes = new byte[size];
                is.read(bytes);
                ii[i].senderId = bytes;
            }

            for (int i = 0; i < songs; i++) {
                ii[i].image = Utils.getThumbnailBitmap(ii[i].songUrl);//TODO update with full url
                // DOWNLOAD INFO OF SERVER SONGS
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        return ii;
    }



}
