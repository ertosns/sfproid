package err.sfp.SocialNetworking;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import err.sfp.Consts;
import err.sfp.Database.Database;
import err.sfp.Main;
import err.sfp.R;
import err.sfp.Utils;

import static err.sfp.SocialNetworking.SongUserAdapter.USERS;

/**
 * Created by root on 17-1-5.
 */

public class SongInfoActivity extends AppCompatActivity implements Consts {

    Database database;
    ImageView songImage;
    TextView songTitle;
    Button watchOnYoutube;
    Button download;
    Button share;
    Intent intent;
    ItemInfo[] ii;

    @Override
    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.songinfo);
        database = Main.database;
        intent = this.getIntent();
        byte[] bitmapBytes = intent.getByteArrayExtra(SONG_INFO_ACTIVITY_BITMAP);
        songImage.setImageBitmap(BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length));
        final String title = intent.getStringExtra(SONG_INFO_ACTIVITY_TITLE);
        final String url = intent.getStringExtra(SONG_INFO_ACTIVITY_URL);
        songTitle.setText(title);
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.downloadSong(intent.getStringExtra(SONG_INFO_ACTIVITY_URL), title);
            }
        });

        watchOnYoutube.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //check if youtube app is installed from packageManager
                //learn youtubeApp braodcast, and broadcast songurl
                //if no present, or no way to broadcast songurl. open youtube via browser
                // make sure browser opens within the same stack for user to automatically return to main previous activity
                // if backBtn pressed, of him to download if like.
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ii == null) {
                    if (database.hasTable(Database.PEERS_TABLE)) ii = database.getPeersShip(false);
                    else Utils.getPeersFromServer(ii, true);
                }
                ShareSongDialog ssd = new ShareSongDialog();
                ssd.init(SongInfoActivity.this, url, ii, USERS);
                ssd.show(getSupportFragmentManager(), "shareSongDialog");
            }
        });


    }

    public static class ShareSongDialog extends DialogFragment {
        String songUrlId;
        ItemInfo[] ii;
        int mode;
        Context context;

        public void init (Context context, String url, ItemInfo[] ii, int mode) {
            this.songUrlId = url;
            this.ii = ii;
            this.mode = mode;
            this.context = context;
        }

        @Override
        public Dialog onCreateDialog(Bundle s) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View v = inflater.inflate(R.layout.sharesongdialog);
            ListView lv = v.findViewById(R.id.sharesonglv);
            lv.setAdapter(new SongUserAdapter(context, ii, mode, songUrlId));

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(v).setMessage(getString(R.string.SHARE_SONGS_DIALOG))
                   .setCancelable(true);

            return builder.create();
        }
    }

}
