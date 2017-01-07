package err.sfp.SocialNetworking;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import err.sfp.Consts;
import err.sfp.Database.Database;
import err.sfp.Download;
import err.sfp.HttpConThread;
import err.sfp.Main;
import err.sfp.R;
import err.sfp.Utils;

/**
 * Created by Err on 17-1-4.
 */

public class Search extends AppCompatActivity implements Consts {

    ListView listView;
    Database database;
    Toolbar toolbar;
    Button search;
    TextView searchQuery;
    Random random;
    ItemInfo[] ii;
    int lastLimitEnd = 0;
    private final static int SEARCH_LIMIT = 10;

    @Override
    protected void onCreate(Bundle onSaveInstance) {
        super.onCreate(onSaveInstance);

        setContentView(R.layout.search);
        random = Main.random;
        listView  = (ListView) findViewById(R.id.songslv);
        toolbar = (Toolbar) findViewById(R.id.songstoolbar);
        searchQuery = (TextView) findViewById(R.id.searchquery);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Search.this, Download.class);
                //TODO set flag
                startActivity(intent);
            }
        });

        database = Main.database;
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Search.InitList(Search.this, listView, searchQuery.getText().toString())).start();
            }
         });

    }

    class InitList implements  Runnable {
        Search search;
        ListView list;
        String query;
        SongUserAdapter sua;

        public InitList(Search search, ListView list, String query) {
            this.search = search;
            this.list = list;
            this.query = query;
            ii = new ItemInfo[0];
        };

        @Override
        public void run() {
            if (sua == null) {
                sua =  new SongUserAdapter(search, ii, SongUserAdapter.REQUESTS);
                listView.setAdapter(sua);
            }

            ii = (ItemInfo[]) Utils.mergeObjectArrays(ii, searchUsers(query, lastLimitEnd, SEARCH_LIMIT));
            sua.itemInfo = ii;
            sua.notifyDataSetChanged();

        }
    }

    public ItemInfo[] searchUsers(String query, int limitstart, int limit) {
        // new app install.
        // TODO (fun) instant search|query((general) through app) {server, database}?!
        // TODO store last searched index

        String randomStr = "abcdefghigklmnopqrstuvwxyz";
        String emailLikeRegex = ".*@.*\\..*";
        StringBuilder searchQueryUrl = new StringBuilder(PROTOCOL).append(HOST).append(PATH);
        if(query.matches(emailLikeRegex)) searchQueryUrl.append("searchbyemail");
        else searchQueryUrl.append("searchbyname");
        searchQueryUrl.append("=true&query=")
                .append((query=="")?randomStr.charAt((int)(random.nextFloat()*25)):query)
                .append("&id=").append(Utils.getUserUniqueId())
                .append("&limitstart=").append(limitstart)
                .append("&limit=").append(limit);

        //TODO set limit, continue
        //TODO INCREMENT LASTlIMIT WITH RETURNED SEA;RCH NUM

        try {
            HttpConThread con = new HttpConThread(searchQueryUrl.toString());
            InputStream is = con.is;
            byte[] intBytes = new byte[4];
            is.read(intBytes);
            int queryNum = Utils.bytesToInt(intBytes);

            ii = new ItemInfo[queryNum];
            // note the same code repeated at Request
            for (int i = 0; i < queryNum; i++) {

                int size = Utils.readInt(is); //name
                byte[] bytes = new byte[size];
                is.read(bytes);
                ii[i].title = new String(bytes);

                size = Utils.readInt(is);
                bytes = new byte[size];
                is.read(bytes);
                ii[i].image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                size = Utils.readInt(is);
                bytes = new byte[size];
                is.read(bytes);
                ii[i].senderId = bytes; // (no need to retrieve senderIds from give parameter ids) protocol will change send flag instead of ids
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return ii;
    }

}
