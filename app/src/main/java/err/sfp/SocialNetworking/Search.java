package err.sfp.SocialNetworking;

import android.app.SearchManager;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;
import java.io.InputStream;
import java.util.Random;
import err.sfp.Consts;
import err.sfp.HttpConThread;
import err.sfp.Main;
import err.sfp.R;
import err.sfp.Utils;

/**
 * Created by Err on 17-1-4.
 */

public class Search extends AppCompatActivity implements Consts
{

    ListView listView;
    Random random;
    ItemInfo[] ii;
    int lastLimitEnd = 0;
    private final static int SEARCH_LIMIT = 10;

    @Override
    protected void onCreate (Bundle onSaveInstance)
    {
        super.onCreate(onSaveInstance);

        setContentView(R.layout.crudelist);
        random = Main.random;
        listView  = (ListView) findViewById(R.id.listview);

        handleIntent(getIntent());
    }

    public void handleIntent (Intent intent)
    {
        if (Intent.ACTION_SEARCH.equals(intent.getAction()))
        {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.i(T, "search query "+query);
            new Search.InitList(Search.this, listView, query).init();
        }
    }

    class InitList
    {   //TODO can't notify adapter not from main thread
        Search search;
        ListView list;
        String query;
        SongUserAdapter sua;

        public InitList(Search search, ListView list, String query)
        {
            this.search = search;
            this.list = list;
            this.query = query;
            ii = ItemInfo.initArray(0);
        }


        public void init()
        {
            if (sua == null)
            {
                sua =  new SongUserAdapter(search, ii, SongUserAdapter.SEARCH, listView);
                listView.setAdapter(sua);
                Log.i(T, "init adapter with ItemsInfo with len "+ii.length);
            }

            int iiLen = ii.length;
            ItemInfo[] searchII = searchUsers(query, lastLimitEnd, SEARCH_LIMIT);
            Log.i(T, "searched results "+searchII.length);
            ItemInfo[] newII = ItemInfo.initArray(searchII.length+iiLen);

            for(int i = 0; i < iiLen; i++)
                newII[i] = ii[i];
            for(int i = 0; i < searchII.length; i++)
                newII[i+iiLen] = searchII[i];

            ii = newII;
            sua.itemInfo = ii;
            Log.i(T, "ItemsInfo updated with new results, total ItemsInfo with len = "+ii.length);
            sua.notifyDataSetChanged();
            Log.i(T, "notify adapter with new itemInfos");
        }
    }

    public ItemInfo[] searchUsers(String query, int limitstart, int limit) {
        // new app install.
        // TODO (fun) instant search|query((general) through app) {server, database}?!
        // TODO store last searched index
        // prevent queries with url-banned chars
        if(!(query.matches("[a-zA-Z0-9]+") || Utils.validateEmail(query)))  return ItemInfo.initArray(0);
        Log.i(T, "perform search Query "+query+" with limitStart = "+limitstart+" and limit = "+limit);
        String alphabet = "abcdefghigklmnopqrstuvwxyz";
        String emailLikeRegex = ".*@.*\\..*";
        StringBuilder searchQueryUrl = new StringBuilder(PROTOCOL).append(HOST).append(PATH);
        boolean searchByEmail = false;
        if(query.matches(emailLikeRegex)) {
            searchQueryUrl.append("searchbyemail");
            searchByEmail = true;
        }
        else searchQueryUrl.append("searchbyname");
        searchQueryUrl.append("=true&mobile=true&query=")
                .append((query.equals(""))?alphabet.charAt((int)(random.nextFloat()*25)):query)
                .append("&id=").append(Utils.getUserUniqueId())
                .append("&limitstart=").append(limitstart)
                .append("&limit=").append(limit);
        Log.i(T, "searchQueryUrl: "+searchQueryUrl);
        //TODO INCREMENT LASTlIMIT WITH RETURNED SEA;RCH NUM
        ItemInfo[] ii = null;
        try {
            HttpConThread con = new HttpConThread(searchQueryUrl.toString());
            synchronized (con) {
                Log.i(T, "waiting for connection-response");
                con.wait();
                Log.i(T, "connection done");
            }

            InputStream is = con.is;
            int queryNum = 0;
            if (searchByEmail) queryNum = 1;
            else {
                byte[] intBytes = new byte[4];
                is.read(intBytes);
                queryNum = Utils.bytesToInt(intBytes);
            }
            Log.i(T, "numOfQueries " + queryNum);
            ii = ItemInfo.initArray(queryNum);
            Log.i(T, "reading queries");
            for (int i = 0; i < queryNum; i++) {

                int size = Utils.readInt(is); //name
                Log.i(T, "title size "+Utils.formatBytes(size));
                byte[] bytes = new byte[size];
                is.read(bytes);
                ii[i].title = new String(bytes);

                size = Utils.readInt(is);
                Log.i(T, "image size "+Utils.formatBytes(size));
                bytes = new byte[size];
                is.read(bytes);
                ii[i].image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                size = Utils.readInt(is);
                Log.i(T, "senderId size "+Utils.formatBytes(size));
                bytes = new byte[size];
                is.read(bytes);
                ii[i].senderId = bytes; // (no need to retrieve senderIds from give parameter ids) protocol will change send flag instead of ids
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(T, "done reading queries");
        return ii;
    }

}