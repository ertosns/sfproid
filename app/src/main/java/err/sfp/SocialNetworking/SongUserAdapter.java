package err.sfp.SocialNetworking;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import err.sfp.R;
import err.sfp.Utils;

/**
 * Created by Err on 17-1-2.
 */

public class SongUserAdapter extends BaseAdapter {
    public ItemInfo[] itemInfo;
    String sharedSongUrlId;
    LayoutInflater inflater;
    View v;
    Button acceptBtn = null;
    Button refuseBtn = null;

    public int mode = 0;
    public static final int SONGS = 0;
    public static final int REQUESTS = 1;
    public static final int USERS = 2;
    public static final int SEARCH = 3;
    Context context;

    public SongUserAdapter(Context c, ItemInfo[] itemInfo, int mode) {
        this.itemInfo = itemInfo;
        inflater = LayoutInflater.from(c);
        this.mode = mode;
        this.context = c;
    }

    public SongUserAdapter(Context c, ItemInfo[] itemInfo, int mode, String sharedSongUrlId) {
        this.itemInfo = itemInfo;
        inflater = LayoutInflater.from(c);
        this.mode = mode;
        this.context = c;
        this.sharedSongUrlId = sharedSongUrlId;
    }

    @Override
    public int getCount() {
        return itemInfo.length;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {

        if (view == null) {
            final ItemInfo ii = itemInfo[i];
            v = inflater.inflate(R.layout.songuser.xml, null);
            ((ImageView) v.findViewById(R.id.songuseritemimage)).setImageBitmap(ii.image);
            ((TextView) v.findViewById(R.id.songusertitle)).setText(ii.title);
            if ((mode == SONGS) || (mode == USERS))
                ((TextView) v.findViewById(R.id.songusersubtitle)).setText(ii.subTitle);

            if ((mode == SONGS)) {
                ((TextView) v.findViewById(R.id.songusermessage)).getText(ii.message);
                acceptBtn = (Button) v.findViewById(R.id.songuseraccept);
                refuseBtn = (Button) v.findViewById(R.id.songuserrefuse);
                acceptBtn.setText(context.getString(R.string.DOWNLOAD_SONG));
                refuseBtn.setText(context.getString(R.string.REFUSE));
                //TODO test does onitem listener could be handled hear?
                acceptBtn.setOnClickListener (new View.OnClickListener() {
                    @Override
                    public void onClick (View view) {
                        Utils.updateSharedSong(ii.databaseId, true, ii.songUrl, ii.title); // true(download);
                        removeItem(i);
                        SongUserAdapter.this.notifyDataSetChanged();
                    }
                });
                refuseBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Utils.updateSharedSong(ii.databaseId, false, null, null); // false(don't download);
                        removeItem(i);
                        SongUserAdapter.this.notifyDataSetChanged();
                    }
                });

            }  else if ((mode == REQUESTS)) {
                acceptBtn = (Button) v.findViewById(R.id.songuseraccept);
                refuseBtn = (Button) v.findViewById(R.id.songuserrefuse);
                acceptBtn.setText(context.getString(R.string.ACCEPT_PEER_SHIP));
                refuseBtn.setText(context.getString(R.string.REFUSE));
                acceptBtn.setOnClickListener (new View.OnClickListener() {
                    @Override
                    public void onClick (View view) {
                        Utils.acceptPeershipRequest(ii.senderId);
                        removeItem(i);
                        SongUserAdapter.this.notifyDataSetChanged();
                    }
                });

                refuseBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Utils.removePeershipRequest(ii.senderId);
                        removeItem(i);
                        SongUserAdapter.this.notifyDataSetChanged();
                    }
                });

            } else if ((mode == USERS)) {
                acceptBtn = (Button) v.findViewById(R.id.songuseraccept);
                acceptBtn.setText(context.getString(R.string.SHARE_SONG_WITH_PEERS));
                acceptBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Utils.downloadSong (sharedSongUrlId, ii.title);
                        removeItem(i);
                        SongUserAdapter.this.notifyDataSetChanged();
                    }
                });
            } else if ((mode == SEARCH)) {
                acceptBtn = (Button) v.findViewById(R.id.songuseraccept);
                acceptBtn.setText(context.getString(R.string.SEND_PEER_SHIP_REQUEST));
                acceptBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Utils.sendPeershipRequest (ii.senderId);
                        removeItem(i);
                        SongUserAdapter.this.notifyDataSetChanged();
                    }
                });
            }
        } else v = view;

        return v;
    }

    public void removeItem (int removePos) {
        // fast than updating array
        int len = itemInfo.length;
        ItemInfo[] ii =new ItemInfo[len--];
        int passed = 0;
        for(int i = 0; i < len; i++) {
            if (i==removePos) passed = 1;
            ii[i] = itemInfo[i+passed];
        }
        itemInfo = ii;
    }
}