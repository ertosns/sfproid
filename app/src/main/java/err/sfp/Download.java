package err.sfp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import err.sfp.Authentication.Login;
import err.sfp.Authentication.Signup;

//TODO menu, floating btn, intents flags
//TODO drawer should be updated each time actionbar humberger icon clicked
//TODO while downloading in case of listening=false task killing service after download is done.
//TODO listener doens't work because no broadcast works

public class Download extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, Consts {

    @BindView(R.id.toggle_listener) SwitchCompat listening;
    @BindView(R.id.downloads) TextView downloads;
    @BindView(R.id.drawer_login_btn) Button login;
    @BindView(R.id.drawer_signup_btn) Button signup;
    SharedPreferences sharedPreferences = null;
    SharedPreferences.Editor pref = null;
    Intent listenerService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        ButterKnife.bind(this);

        sharedPreferences = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);
        pref = sharedPreferences.edit();
        listenerService = new Intent(this, Listener.class);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

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
                        Log.i(T, "toggle check to ON");
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
                        pref.putBoolean(LISTENING, true);
                        pref.commit();
                        openListener();
                    } else {
                        //kill broadCast
                        /* doesn't work
                        Log.i(T, "killing Listening service");
                        Intent intent = new Intent();
                        intent.setAction(SHAREDPREFERENCES_ACTION_FILTER);
                        intent.putExtra(PREFERENCE_TYPE, STOP_FLAG);
                        intent.putExtra(STOP_PREF, true);
                        sendBroadcast(intent);*/
                        //stopService(listenerService);

                        pref.putBoolean(LISTENING, false);
                        pref.commit();
                    }
                }
            }
        });
    }

    public void openListener(){
        if(signedUp() && rwPermissionsGranted() && isListeningToggleOn()) {
            Log.i(T, "start Listener Service");

            listenerService.putExtra(UNIQUE_ID, sharedPreferences.getString(UNIQUE_ID, null));
            if(isListeningToggleOn()) stopService(listenerService); //TODO diff between boths wasy of killing.
            startService(listenerService);
        }
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
}
