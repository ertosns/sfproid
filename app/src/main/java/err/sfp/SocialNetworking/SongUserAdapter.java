package err.sfp.SocialNetworking;

import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Date;

import err.sfp.Database.Database;
import err.sfp.R;
import err.sfp.Utils;

import static err.sfp.Consts.T;

/**
 * Created by Err on 17-1-2.
 */

public class SongUserAdapter extends BaseAdapter {
    public ItemInfo[] itemInfo;
    String sharedSongUrlId;
    LayoutInflater inflater;
    ListView list;
    View v;
    Button acceptBtn = null;
    Button refuseBtn = null;

    public int mode = 0;
    public static final int SONGS = 0;
    public static final int REQUESTS = 1;
    public static final int USERS = 2;
    public static final int SEARCH = 3;
    String sharedSongTitle;
    Context context;

    public SongUserAdapter(Context c, ItemInfo[] itemInfo, int mode, ListView list)
    {
        this.itemInfo = itemInfo;
        inflater = LayoutInflater.from(c);
        this.mode = mode;
        this.context = c;
        this.list = list;
    }

    public SongUserAdapter(Context c, ItemInfo[] itemInfo, int mode, String sharedSongUrlId,
                           String sharedSongTitle, ListView list)
    {
        this.itemInfo = itemInfo;
        inflater = LayoutInflater.from(c);
        this.mode = mode;
        this.context = c;
        this.sharedSongUrlId = sharedSongUrlId;
        this.sharedSongTitle = sharedSongTitle;
        this.list = list;
    }

    @Override
    public int getCount()
    {
        return itemInfo.length;
    }

    @Override
    public Object getItem(int i)
    {
        return null;
    }

    @Override
    public long getItemId(int i)
    {
        return itemInfo[i].hashCode();
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup)
    {

        if (view == null)
        {
            final ItemInfo ii = itemInfo[i];
            Log.i(T, "sender id in hex "+Utils.bytesToHex(ii.senderId));
            v = inflater.inflate(R.layout.iteminfo, null);
            if (ii.image != null)
            {
                Log.i(T, "image is set");
                ((ImageView) v.findViewById(R.id.itemimage)).setImageBitmap(ii.image);
            }
            ((TextView) v.findViewById(R.id.title)).setText(ii.title);
            Log.i(T, "title set with "+ii.title);
            if ((mode == SONGS) || (mode == USERS))
            {
                ((TextView) v.findViewById(R.id.subtitle)).setText(ii.subTitle);
                Log.i(T, "songs || users mode, setting subTitle "+ii.subTitle);
            }

            if ((mode == SONGS))
            {
                Log.i(T, "setting on Click listener in songs mode");
                if(ii.message != null)
                {
                    TextView message = ((TextView) v.findViewById(R.id.message));
                    message.setVisibility(View.VISIBLE);
                    message.setText(ii.message);
                }
                acceptBtn = (Button) v.findViewById(R.id.accept);
                refuseBtn = (Button) v.findViewById(R.id.refuse);
                acceptBtn.setText(context.getString(R.string.DOWNLOAD_SONG));
                refuseBtn.setText(context.getString(R.string.REFUSE));
                acceptBtn.setOnClickListener (new View.OnClickListener()
                {
                    @Override
                    public void onClick (View view)
                    {
                        Utils.markSongDownloadable(ii.databaseId); //remove song with senderId > 0
                        removeItem(i);
                    }
                });
                refuseBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Utils.removeSong(ii.databaseId);
                        removeItem(i);
                        Utils.removeSong(ii.databaseId);
                    }
                });

            }
            else if ((mode == REQUESTS))
            {
                Log.i(T, "setting on Click listener in request mode");
                acceptBtn = (Button) v.findViewById(R.id.accept);
                refuseBtn = (Button) v.findViewById(R.id.refuse);
                acceptBtn.setText(context.getString(R.string.ACCEPT_PEERSHIP));
                refuseBtn.setText(context.getString(R.string.REFUSE));
                acceptBtn.setOnClickListener (new View.OnClickListener() {
                    @Override
                    public void onClick (View view) {
                        Utils.acceptPeershipRequest(ii.senderId);
                        removeItem(i);

                    }
                });

                refuseBtn.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view) {
                        Utils.removePeershipRequest(ii.senderId);
                        removeItem(i);
                    }
                });

            }
            else if ((mode == USERS))
            {
                Log.i(T, "setting on Click listener in users mode");
                refuseBtn = (Button) v.findViewById(R.id.refuse);
                acceptBtn = (Button) v.findViewById(R.id.accept);
                final EditText messageET = (EditText) v.findViewById(R.id.messageinput);
                messageET.setVisibility(View.VISIBLE);
                refuseBtn.setVisibility(View.GONE);
                acceptBtn.setText(context.getString(R.string.SHARE_SONG_WITH_PEERS));
                acceptBtn.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        String messageStr = messageET.getText().toString();
                        Utils.shareSong (sharedSongUrlId, sharedSongTitle, messageStr, ii.senderId);
                        removeItem(i);
                    }
                });
            }
            else if ((mode == SEARCH))
            {
                Log.i(T, "setting on Click listener in search mode");
                refuseBtn = (Button) v.findViewById(R.id.refuse);
                acceptBtn = (Button) v.findViewById(R.id.accept);
                refuseBtn.setVisibility(View.GONE);
                acceptBtn.setText(context.getString(R.string.SEND_PEER_SHIP_REQUEST));
                acceptBtn.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Utils.sendPeershipRequest (ii.senderId);
                        removeItem(i);
                        for(int i = 0; i < itemInfo.length; i++)
                            Log.i(T, i+") current ii names "+itemInfo[i].title);

                    }
                });
            }
        } else v = view;

        return v;
    }

    public void removeItem (int removePos)
    {
        Log.i(T, "removing item with pos "+ removePos);
        int len = itemInfo.length;
        ItemInfo[] ii = ItemInfo.initArray(len-1);
        int passed =  0;

        for (int i = 0; i < len; i++)
        {
            if (i==removePos) passed = 1;
            else ii[i-passed] = itemInfo[i];
        }

        // doesn't work properly without that line.
        // must empty the current listview in order to work
        itemInfo = ii;
        list.setAdapter(new SongUserAdapter(context, ii, mode, list));
    }
}