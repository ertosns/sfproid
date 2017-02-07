package err.sfp;

import android.Manifest;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;
import butterknife.BindView;
import butterknife.ButterKnife;
import err.sfp.Authentication.Login;
import err.sfp.Authentication.Signup;
import err.sfp.SocialNetworking.Requests;
import err.sfp.SocialNetworking.Search;
import err.sfp.SocialNetworking.SongInfoActivity;
import err.sfp.SocialNetworking.Songs;

//TODO menu, floating btn, intents flags
//TODO if givin search string is song url view download btn as the only option
//TODO enable relationships between users, and sending songs with messages.
//TODO setup inputs limit warning android, web.
//TODO authentication should be done in background, frames are skipped!

public class Download extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, Consts {

    @BindView(R.id.toggle_listener) SwitchCompat listening;
    @BindView(R.id.downloads) TextView downloads;
    @BindView(R.id.drawer_login_btn) Button login;
    @BindView(R.id.list_of_downloading) TextView listOfDownloadings;
    @BindView(R.id.drawer_signup_btn) Button signup;

    SharedPreferences sharedPreferences = null;
    SharedPreferences.Editor pref = null;
    Intent listenerService = null;
    GridView grid = null;
    TextView searchTv = null;
    Button searchBtn = null;
    LinearLayout searchLayout = null;
    boolean searching = false;
    YoutubeClient clientRef = null;
    String pastSearchText = null;
    String listOfDownloadingSongsFile = "lodsf";
    FileInputStream listOfDownloadingSongsIS = null;
    FileOutputStream listOfDownloadingSongsOS = null;
    Handler handler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        ButterKnife.bind(this);

        sharedPreferences = Main.sharedPreferences;
        pref = sharedPreferences.edit();
        listenerService = new Intent(this, Listener.class);
        searchTv = (TextView) findViewById(R.id.search_tv);
        searchBtn = (Button) findViewById(R.id.search_btn);
        grid = (GridView) findViewById(R.id.gridview);
        searchLayout = (LinearLayout) findViewById(R.id.search_layout);
        try {
            Log.i(T, "listOfDownloadsFile will be created if it's not exists");
            new File(getFilesDir(), listOfDownloadingSongsFile).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        IntentFilter filter = new IntentFilter(LISTENER_MESSAGES_INTNET_FILTER);
        filter.addAction(LISTENER_MESSAGES_INTNET_FILTER);
        registerReceiver(listenerBroadCast, filter);
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.i(T, "handling YoutubeClient messages");
                int what = msg.what;
                if(what == 0) populateGridThread(clientRef);
                else if(what == -1) searchBtn.setError(getString(R.string.NO_RESULT_FOUND));
            }
        };
        //make btn click uneffective if it's already searching and text didn't change.
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO backup bitmaps for stack back btn.

                if(Utils.isOnline(Download.this)) {
                    if(searching) {
                        Log.i(T, "search requests while searching isn't done");
                        return;
                    }
                    String queryText = searchTv.getText().toString().trim();

                    Log.i(T, "search btn clicked WITH queryText = " + queryText);
                    if (queryText != "") {
                        if(pastSearchText!=null) {
                            if (pastSearchText.equals(queryText)) {
                                Log.i(T, "searching the same Text");
                                return;
                            }
                        }
                        if(clientRef != null) {
                            Log.i(T, "clearing items");
                            clientRef.items.clear();
                        }
                        pastSearchText = queryText;
                        final YoutubeClient client = new YoutubeClient(Download.this, queryText);
                        clientRef = client;
                        client.start();
                        searching = true;
                    }
                } else searchBtn.setError(getString(R.string.CONNECTIVITY_ERROR));
            }
        });
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        initDrawerViews();

    }

    @Override
    public void onResume(){
        super.onResume();
        openListener();
    }

    @Override
    public void onDestroy() {
        Log.i(T, "un-registering listener broadcastReceiver");
        unregisterReceiver(listenerBroadCast);
    }

    BroadcastReceiver listenerBroadCast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int type = intent.getIntExtra(LISTENER_FLAG_TYPE, -1);
            if (type == LISTENER_INTEGER_FLAG) {
                Log.i(T, "updating downloads");
                int intFlag = intent.getIntExtra(LISTENER_INTEGER_FLAG_KEY, -1);
                updateDownloads(intFlag);
            } else if (type == LISTENER_STRING_FLAG){
                Log.i(T, "updating listofDownloads");
                String songInfo = intent.getStringExtra(LISTENER_MESSAGE);
                updateSongsList(songInfo);
            }
        }
    };

    public void initDrawerViews() {
        final boolean signed = signedUp();
        if(signed) {
            loginMode();
            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.i(T, "logout");
                    logout();
                }
            });
        } else {
            Log.i(T, "logout Mode");
            logoutMode();
            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Download.this, Login.class);
                    //TODO set flags
                    startActivity(intent);
                }
            });
            signup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Download.this, Signup.class);
                    //TODO set flags
                    startActivity(intent);
                }
            });
        }

        listening.setChecked(isListeningToggleOn());
        listening.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = listening.isChecked();
                if (signed) {
                    if (checked) {
                        if (!rwPermissionsGranted()) {
                            Log.i(T, "any of rw permissions isn't/aren't granted");
                            listening.setChecked(false);
                            askRWPermissions();
                            return;
                        }
                        initListener();
                    } else {
                        Toast.makeText(Download.this, getString(R.string.LISTENER_DEACTIVATED),
                                Toast.LENGTH_LONG).show();
                        stopServiceBroadcast();
                    }
                } else if(!checked) {
                    listening.setChecked(false);
                }
            }
        });
    }


    public void initListener() {
        Toast.makeText(Download.this, getString(R.string.LISTENER_ACTIVATED), Toast.LENGTH_LONG).show();
        Log.i(T, "starting service");
        pref.putBoolean(LISTENING, true);
        pref.commit();
        listening.setChecked(true);
        openListener();
    }

    public void openListener()
    {
        if (signedUp() && rwPermissionsGranted() && isListeningToggleOn())
        {
            Log.i(T, "start Listener Service");
            listenerService.putExtra(UNIQUE_ID, sharedPreferences.getString(UNIQUE_ID, null));
            if (isListeningToggleOn())
            {
                Log.i(T, "stopping previous service");
                stopServiceBroadcast();
                stopService(listenerService); //TODO diff between both ways of killing.
            }
            startService(listenerService);
        }
    }


    public void stopServiceBroadcast()
    {
        Log.i(T, "stopping service");
        Intent intent = new Intent(SHAREDPREFERENCES_ACTION_FILTER);
        intent.setAction(SHAREDPREFERENCES_ACTION_FILTER);
        intent.putExtra(PREFERENCE_TYPE, STOP_FLAG);
        intent.putExtra(STOP_PREF, true);
        sendBroadcast(intent);
        pref.putBoolean(LISTENING, false);
        pref.commit();
    }

    public boolean isListeningToggleOn() {
        return sharedPreferences.getBoolean(LISTENING, false);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.download, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        ComponentName comName = new ComponentName(this, Search.class);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(comName));
        searchView.setIconifiedByDefault(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.sharedsongs) {
            Intent intent = new Intent(this, Songs.class);
            //TODO set flag
            startActivity(intent);
        } else if (id == R.id.sharedrequests) {
            Intent intent = new Intent(this, Requests.class);
            //TODO set flag
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public boolean signedUp() {
        return sharedPreferences.getBoolean(SIGNED, false);
    }

    public void setDownloadsText(int downloadsNum) {
        downloads.setText(getString(R.string.DOWNLOADS_NUM)+downloadsNum);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] result) {

        if (code == RW_PERMISSION) {
            if (result[0] == PackageManager.PERMISSION_GRANTED &&
                    result[1] == PackageManager.PERMISSION_GRANTED) {
                Log.i(T, "rw permission granted");
                initListener();
            }
        }
    }

    public boolean rwPermissionsGranted () {
        return  ContextCompat.checkSelfPermission(Download.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(Download.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;
    }

    public void askRWPermissions() {
        ActivityCompat.requestPermissions(Download.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE}, RW_PERMISSION);
    }

    public void populateGridThread (final YoutubeClient client) {

        if(grid.getVisibility() != View.VISIBLE) grid.setVisibility(View.VISIBLE);
        Log.i(T, "grid setAdapter");
        grid.setAdapter(new GridAdapter(Download.this, client));
            //TODO implement on double click
        Log.i(T, "grid onItemClickListener");
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    //Intent downloadActivity = new Intent(Download.this, DownloadSong.class);
                    //TODO set flags
                    //downloadActivity.putExtra("url", client.getUrl(i));
                    //startActivity(downloadActivity);
                    Log.i(T, "download request made");
                    if(!signedUp()) {
                        Log.i(T, "user isn't signed up");
                        Toast.makeText(Download.this, getString(R.string.YOU_NEED_TO_LOGIN_FIRST), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (!sharedPreferences.getBoolean(LISTENING, false)) {
                        Log.i(T, "listener is off");
                        Toast.makeText(Download.this, getString(R.string.LISTENER_IS_OFF), Toast.LENGTH_LONG).show();
                    }
                    //downloadSong(client.items.get(i).videoId, client.items.get(i).title);
                    Intent songInfoActivity = new Intent(Download.this, SongInfoActivity.class);
                    songInfoActivity.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    songInfoActivity.putExtra(SONG_INFO_ACTIVITY_BITMAP, client.items.get(i).thumbnail);
                    songInfoActivity.putExtra(SONG_INFO_ACTIVITY_TITLE, client.items.get(i).title);
                    songInfoActivity.putExtra(SONG_INFO_ACTIVITY_URL, client.items.get(i).videoId);
                    startActivity(songInfoActivity);
                    //TODO setSongIinfoFlag

                }
        });
        Log.i(T, "populatingGridThread method finished");
    }

    public void updateSongsList(String songsInfo) {
        Log.i(T, "updating Songs List with songsInfo :"+songsInfo);
        try {
            listOfDownloadingSongsOS = openFileOutput(listOfDownloadingSongsFile, MODE_PRIVATE);
            listOfDownloadingSongsOS.write(songsInfo.toString().getBytes());
            StringBuilder names = new StringBuilder();
            for(String s: songsInfo.split("\n")) {
                Log.i(T, "one line song info "+s);
                if (s.trim().length() > 0) names.append(s.split("/")[1] + "\n");
            }
            listOfDownloadings.setText(names.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                listOfDownloadingSongsOS.close();
                listOfDownloadingSongsIS.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateListOfDownloads() {
        Log.i(T, "updating List of downloads");
        try {
            StringBuilder listOfNames = new StringBuilder();
            listOfDownloadingSongsIS = openFileInput(listOfDownloadingSongsFile);
            Scanner scan  = new Scanner(listOfDownloadingSongsIS);
            while(scan.hasNext()) {

                String[] songInfo = scan.nextLine().split("/");
                //TODO (FIX) RETURN OUT OF BOUNDS
                Log.i(T, "songInfo[0] "+songInfo[0]+" songInfo[1] "+songInfo[1]);
                String nameSeg = songInfo[1]+"\n";
                listOfNames.append(nameSeg);
            }
            listOfDownloadings.setText(listOfNames.toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(listOfDownloadingSongsIS != null)
                listOfDownloadingSongsIS.close();
            } catch (IOException io) {
                io.printStackTrace();
            }
        }
    }

    public void updateDownloads(int downloads) {
        Log.i(T, "update downloads num : "+downloads);
        pref.putInt(DOWNLOADS, downloads);
        pref.commit();
        setDownloadsText(downloads);
    }

    public void updateDownloads() {
        Log.i(T, "update downloads from sp");
        setDownloadsText(sharedPreferences.getInt(DOWNLOADS, 0));
    }

    class GridAdapter extends BaseAdapter {
        Context mContext;
        YoutubeClient client;
        LayoutInflater inflater = getLayoutInflater();
        View v = null;
        ImageView videoThumbnail;
        TextView  videoTitle;

        public GridAdapter(Context context, YoutubeClient client) {
            Log.i(T, "GridAdapter initialized");
            mContext = context;
            this.client = client;
        }

        @Override
        public int getCount() {
            Log.i(T, "getCount");
            return NUMBER_OF_VIDEOS_RETURNED;
        }

        @Override
        public Object getItem(int i) {
            Log.i(T, "getItem");
            return null;
        }

        @Override
        public long getItemId(int i) {
            Log.i(T, "getItemId");
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            Log.i(T, "in getView()");
            if (view == null) {
                v = inflater.inflate(R.layout.grid_item, null);
                videoThumbnail = (ImageView) v.findViewById(R.id.grid_iv);
                videoTitle = (TextView) v.findViewById(R.id.grid_tv);
                videoThumbnail.setImageBitmap(client.items.get(i).thumbnail);
                videoTitle.setText(client.items.get(i).title);
                Log.i(T, "item "+i+" added to grid");
            } else {
                v =  view;
            }
            if(client.done) searching = false;
            return v;
        }
    }

    public void downloadSong(String songId, String title)
    {
        int res = Utils.downloadSong(songId, title);
        Toast.makeText(Download.this, (res==200)?getString(R.string.WILL_BE_DOWNLOADED_SOON):getString(R.string.SERVER_ERROR_TRY_AGAIN), Toast.LENGTH_LONG).show();
    }

    public void logout()
    {
        Log.i(T, "logging out");
        stopServiceBroadcast();
        pref.putBoolean(SIGNED, false);
        pref.commit();
        Main.logout = true;
        logoutMode();
        Intent intent = new Intent(Download.this, Login.class);
        //TODO set flags
        startActivity(intent);
    }

    public  void logoutMode()
    {
        Log.i(T, "logout mode");
        Main.logout = true;

        downloads.setVisibility(View.INVISIBLE);
        listening.setVisibility(View.INVISIBLE);
        listOfDownloadings.setVisibility(View.INVISIBLE);
        signup.setVisibility(View.VISIBLE);

        login.setText(getString(R.string.LOG_IN));
    }

    public void loginMode()
    {
        Log.i(T, "login mode");


        updateListOfDownloads();
        updateDownloads();

        downloads.setVisibility(View.VISIBLE);
        listening.setVisibility(View.VISIBLE);
        listOfDownloadings.setVisibility(View.VISIBLE);
        signup.setVisibility(View.INVISIBLE);

        login.setText(R.string.LOG_OUT);

    }
}
