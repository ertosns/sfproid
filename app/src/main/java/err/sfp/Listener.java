package err.sfp;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.IBinder;
import android.renderscript.ScriptGroup;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerViewAccessibilityDelegate;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.PrivateKey;

/**
 * Created by Err on 16-12-6.
 */

//TODO prevent killing service|give high priority
//TODO inform server you received successfully which song should be removed from server, (res) how serveral write will work?
//TODO connectivity receiver doesn't seem to be working.
//TODO check if braodcast registered??

public class Listener extends IntentService implements Consts {
    SharedPreferences sharedPreferences  = null;
    SharedPreferences.Editor pref = null;
    String uniqueId = null;

    public Listener() {
        super(Listener.class.getName());
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        /*IntentFilter filter = new IntentFilter(SHAREDPREFERENCES_ACTION_FILTER);
        filter.addAction(SHAREDPREFERENCES_ACTION_FILTER);
        registerReceiver(broadcastReceiver, filter); */

        sharedPreferences = getSharedPreferences(APP_PREFERENCES_LISTENER, MODE_PRIVATE);
        pref = sharedPreferences.edit();
        uniqueId = intent.getStringExtra(UNIQUE_ID);

        Log.i(T, "connecting");
        connect();

    }


    public void killService() {
        //unregister receiver, not needed, broadcasts are local
        //selfStop
        Log.i(T, "stopping Listener service");
        //this.stopSelf(uniqueStartId);
    }

    public void connect() {
        while(true) {
            if(sharedPreferences.getBoolean(ONLINE, true) && uniqueId != null) {
                ListenerThread lt = new ListenerThread();

                synchronized (lt) {
                    try {
                        Log.i(T, "listening thread started");
                        lt.start();
                        Log.i(T, "just started");
                        lt.wait();
                        lt.done();
                        Log.i(T, "listening thread finished");
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }

            }

            try {
                int sleep = sharedPreferences.getInt(W8_INTERVAL, 60000);
                if (sleep == 0) sleep = 30000; //TODO ?!
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.i(T, "reconnect");
        }
    }

    //Doesn't work 4 no clear reason.
    BroadcastReceiver broadcastReceiver = new  BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            //possible received intents key sorted by frequency {stop, online}
            //one pair received at time preceded by type flag (int)
            Log.i(T, "sharedPreferences broadcast received");
            int preference = intent.getIntExtra(PREFERENCE_TYPE, STOP_FLAG);

            if (preference == STOP_FLAG) {
                if (intent.getBooleanExtra(STOP_PREF, false)) killService();
            } else if (preference == ONLINE_FLAG) {
                pref.putBoolean(ONLINE, intent.getBooleanExtra(ONLINE, false));
                pref.commit();
            }
        }


    };


    class ListenerThread extends Thread {
        boolean done = false;

        public void run() {
            Log.i(T, "stated and in first line in Thread run()");
            Socket sock = null;
            try {
                sock = new Socket(HOST, SOCK_PORT);
                sock.setSoTimeout(0);
                // \n used to terminate scanner on the other side.
                byte[] uniqueIdBytes =uniqueId.getBytes();
                Log.i(T, "sending id = "+new String(uniqueIdBytes));
                InputStream is = sock.getInputStream();
                OutputStream os = sock.getOutputStream();
                os.write(Utils.mergeBytes(Utils.intToBytes(uniqueIdBytes.length), uniqueIdBytes));
                Log.i(T, "id sent, blocking for inputStream reading interval");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                byte[] intervalBytes = new byte[4];
                is.read(intervalBytes);

                int interval = Utils.bytesToInt(intervalBytes);
                pref.putInt(W8_INTERVAL, interval);
                Log.i(T, "interval = "+interval);
                pref.commit();
                boolean hasSong = (is.read()==1)?true:false;
                Log.i(T, "has song: "+hasSong);
                if(hasSong) {
                    readSong(is, os);
                }
                //app_level pro
            } catch (IOException io) {
                io.printStackTrace();
            } finally {
                done = true;
                try {
                    if(sock != null) sock.close();
                } catch (IOException i) {
                    i.printStackTrace();
                }
                synchronized (this) {
                    this.notify();
                }
            }
        }

        public void readSong(InputStream is, OutputStream os) throws IOException {
            byte[] songNameSizeBytes = new byte[4];
            is.read(songNameSizeBytes);
            int songNameSize = Utils.bytesToInt(songNameSizeBytes);
            Log.i(T, "#1# songNameSize read = "+songNameSize+"b");

            byte[] songNameBytes = new byte[songNameSize];
            is.read(songNameBytes);
            String songName = new String(songNameBytes);
            Log.i(T, "#2# songName = "+songName);

            byte[] songSizeBytes = new byte[4];
            is.read(songSizeBytes);
            int songSize = Utils.bytesToInt(songSizeBytes);
            Log.i(T, "#3# songSize = "+songSize+"b");

            byte[] songByte = new byte[songSize];
            DownloadSong downloadSong = new DownloadSong(Listener.this, is, songByte, songName);

            synchronized (songByte) {
                try {
                    songByte.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            os.write(downloadSong.successed()?DOWNLOAD_SUCCESSED:DOWNLOAD_FAILED);

            Log.i(T, "#4# song read");
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath();
            path = path+ "/" + SONG_PREFIX + songName;
            File f = new File(path);
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(f);
            Log.i(T, "writing song to external dir with path = "+path);
            fos.write(songByte);
            Log.i(T, "done, song is wrote to file successfully");
        }

        public boolean done () {
            return done;
        }
    }

    class DownloadSong extends Thread {
        private InputStream is;
        private byte[] bytes;
        private NotificationCompat.Builder notBuilder;
        private int size;
        private int updatePoint;
        private NotificationManager notMan;
        private boolean successed = true;
        public DownloadSong(Context context, InputStream inputStream, byte[] sb, String songName) {
            is = inputStream;
            bytes = sb;
            size = bytes.length;
            updatePoint = size/50; // update notification every updatePoint of bytes;
            try {
                notBuilder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_menu_camera)
                        .setContentTitle(getString(R.string.DOWNLOADING_NOTIFY_TITLE))
                        .setContentText(new StringBuilder(songName).append("  ")
                                .append(String.format("%.2f", (float) size / (1024 * 1024))).append(" MB").toString())
                        .setProgress(size, 0, false)
                        .setAutoCancel(true);

                notMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notMan.notify(0, notBuilder.build());
            } catch (Exception e) {
                e.printStackTrace();
                successed = false;
            }
            this.start();
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < bytes.length; i++) {
                    try {
                        bytes[i] = (byte) is.read();
                        if (i == updatePoint) {
                            notBuilder.setProgress(size, i, false);
                            notMan.notify(0, notBuilder.build());
                            updatePoint += updatePoint;
                            Log.i(T, "updating notification");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (size > 0) {
                    Log.i(T, "song downloaded");
                    notBuilder.setContentText(getString(R.string.DOWNLOAD_DONE));
                    notBuilder.setProgress(0, 0, false);
                    notMan.notify(0, notBuilder.build());
                }
            } catch (Exception e ) {
                e.printStackTrace();
                successed = false;
            }
            synchronized (bytes) {
                bytes.notify();
            }
        }

        public boolean successed () {
            Log.i(T, "song download success state = "+successed);
            return successed;
        }
    }



}
