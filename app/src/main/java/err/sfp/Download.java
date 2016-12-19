package err.sfp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import err.sfp.Authentication.Login;
import err.sfp.Authentication.Signup;

import static android.R.attr.animationDuration;
import static android.R.attr.visible;

//TODO menu, floating btn, intents flags
//TODO drawer should be updated each time actionbar humberger icon clicked
//TODO while downloading in case of listening=false task killing service after download is done.
//TODO listener doens't work because no broadcast works
//TODO if givin search string is song url view download btn as the only option
//TODO enable relationships between users, and sending songs with messages.
public class Download extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, Consts {

    @BindView(R.id.toggle_listener) SwitchCompat listening;
    @BindView(R.id.downloads) TextView downloads;
    @BindView(R.id.drawer_login_btn) Button login;
    @BindView(R.id.drawer_signup_btn) Button signup;
    SharedPreferences sharedPreferences = null;
    SharedPreferences.Editor pref = null;
    Intent listenerService = null;
    GridView grid = null;
    TextView searchTv = null;
    Button searchBtn = null;
    LinearLayout searchLayout = null;
    boolean searching = false;
    Handler handler = null;
    YoutubeClient clientRef = null;
    String pastSearchText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        ButterKnife.bind(this);

        sharedPreferences = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);
        pref = sharedPreferences.edit();
        listenerService = new Intent(this, Listener.class);
        searchTv = (TextView) findViewById(R.id.search_tv);
        searchBtn = (Button) findViewById(R.id.search_btn);
        grid = (GridView) findViewById(R.id.gridview);
        searchLayout = (LinearLayout) findViewById(R.id.search_layout);
        //make btn click uneffective if it's already searching and text didn't change.
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                assert clientRef != null : "clientRef = null";
                populateGridThread(clientRef);
            }
        };
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
                //Log.i(T, "looper is out")
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
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
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

    public void initDrawerViews() {
        final boolean signed = signedUp();
        if(signed) {
            Log.i(T, "signed");
            login.setText(getString(R.string.LOG_OUT));
            login.setVisibility(View.VISIBLE);
            signup.setVisibility(View.INVISIBLE);
            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Download.this, Login.class);
                    //TODO set flags
                    startActivity(intent);
                }
            });
        } else {
            signup.setVisibility(View.VISIBLE);
            login.setText(getString(R.string.LOG_IN));
            login.setVisibility(View.VISIBLE);
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

        downloads.setText("Downloads: "+getNumOfDownloads());
        listening.setChecked(isListeningToggleOn());
        listening.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (signed) {
                    boolean checked = listening.isChecked();
                    if (checked) {
                        if (!rwPermissionsGranted()) {
                            Log.i(T, "any of rw permissions isn't/aren't granted");
                            askRWPermissions();
                            synchronized (this) {
                                //TODO make sure that work properly
                                try {
                                    Log.i(T, "w8 main thread for  User rw permission");
                                    this.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        Toast.makeText(Download.this, getString(R.string.LISTENER_ACTIVATED), Toast.LENGTH_LONG).show();
                        Log.i(T, "starting service");
                        pref.putBoolean(LISTENING, true);
                        pref.commit();
                        openListener();
                    } else {
                        Toast.makeText(Download.this, getString(R.string.LISTENER_DEACTIVATED),
                                Toast.LENGTH_LONG).show();
                        Log.i(T, "sending kill broadcast");
                        stopServiceBroadcast();
                        Log.i(T, "stopping service");
                        pref.putBoolean(LISTENING, false);
                        pref.commit();
                    }
                }
            }
        });
    }

    public void openListener() {
        if(signedUp() && rwPermissionsGranted() && isListeningToggleOn()) {
            Log.i(T, "start Listener Service");
            listenerService.putExtra(UNIQUE_ID, sharedPreferences.getString(UNIQUE_ID, null));
            if(isListeningToggleOn()) stopService(listenerService); //TODO diff between boths wasy of killing.
            startService(listenerService);
        }
    }

    public void stopServiceBroadcast() {
        Intent intent = new Intent(SHAREDPREFERENCES_ACTION_FILTER);
        intent.setAction(SHAREDPREFERENCES_ACTION_FILTER);
        intent.putExtra(PREFERENCE_TYPE, STOP_FLAG);
        intent.putExtra(STOP_PREF, true);
        sendBroadcast(intent);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.download, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
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

    public int getNumOfDownloads() {
        return sharedPreferences.getInt(DOWNLOADS, 0);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] result) {
        synchronized (this){
            notify();
            Log.i(T, "main thread notified");
        }

        if (code == RW_PERMISSION) {
            if (result[0] == PackageManager.PERMISSION_GRANTED &&
                    result[1] == PackageManager.PERMISSION_GRANTED) {
                Log.i(T, "rw permission granted");
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
                    if (!sharedPreferences.getBoolean(LISTENING, false)) {
                        Log.i(T, "listener is off");
                        Toast.makeText(Download.this, getString(R.string.LISTENER_IS_OFF), Toast.LENGTH_LONG).show();
                    }
                    downloadSong(client.items.get(i).videoId);
                }
        });
        Log.i(T, "populatingGridThread method finished");
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

    public void downloadSong(String songId) {
        HttpConThread con = new HttpConThread(getDownloadSongUrl(songId));
        con.start();
        int res = 0;
        synchronized (con) {
            try {
                con.wait();
                res =  con.code;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Log.i(T, "downloadSongCode = "+res);
        Toast.makeText(Download.this, (res==200)?getString(R.string.WILL_BE_DOWNLOADED_SOON):getString(R.string.SERVER_ERROR_TRY_AGAIN), Toast.LENGTH_LONG).show();
    }

    public String getDownloadSongUrl(String songId) {
        return new StringBuilder(PROTOCOL).append(HOST).append(PATH)
                .append("mobilesdownload=true&id=")
                .append(sharedPreferences.getString(UNIQUE_ID, "null"))
                .append("&songId=").append(songId).toString();
    }
}
