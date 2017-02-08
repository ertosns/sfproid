package err.sfp;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by Err on 16-12-6.
 */

//TODO SET FLAGS ALL OVER THE CODE
//TODO inform server you received successfully which song should be removed from server, (res) how several write will work?

public class Listener extends IntentService implements Consts
{
    SharedPreferences sharedPreferences  = null;
    SharedPreferences.Editor pref = null;
    String uniqueId = null;
    boolean breakFlag = false;
    int pastTotalDownloads = 0;
    String pastSongsInfo = "";
    boolean wasDownloading = false;
    String lastDownloadedSongId = "";

    public Listener() {
        super(Listener.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        breakFlag = false;
        breakFlag = false;
        IntentFilter filter = new IntentFilter(SHAREDPREFERENCES_ACTION_FILTER);
        filter.addAction(SHAREDPREFERENCES_ACTION_FILTER);
        registerReceiver(broadcastReceiver, filter);
        sharedPreferences = getSharedPreferences(APP_PREFERENCES_LISTENER, MODE_PRIVATE);
        pref = sharedPreferences.edit();
        uniqueId = intent.getStringExtra(UNIQUE_ID);
        Log.i(T, "connecting");
        connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    BroadcastReceiver broadcastReceiver = new  BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            //possible received intents key sorted by frequency {stop, online}
            //one pair received at time preceded by type flag (int)
            Log.i(T, "sharedPreferences broadcast received, to kill service");
            int preference = intent.getIntExtra(PREFERENCE_TYPE, STOP_FLAG);

            if (preference == STOP_FLAG)
            {
                if (intent.getBooleanExtra(STOP_PREF, false))
                {
                    Log.i(T, "kill service");
                    killService();
                }
            }
            else if (preference == ONLINE_FLAG)
            {
                Log.i(T, "update online state");
                pref.putBoolean(ONLINE, intent.getBooleanExtra(ONLINE, false));
                pref.commit();
            }
        }


    };

    public void killService()
    {
        Log.i(T, "stopping Listener service");
        stopSelf();
        breakFlag = true;
    }

    public void sendUpdateDrawerBroadcast(int x, String info) {
        Intent bc = new Intent(LISTENER_MESSAGES_INTNET_FILTER);
        bc.setAction(LISTENER_MESSAGES_INTNET_FILTER);
        if(info == null) { //update downloads
            bc.putExtra(LISTENER_FLAG_TYPE, LISTENER_INTEGER_FLAG);
            bc.putExtra(LISTENER_INTEGER_FLAG_KEY, x);
        } else { //update info
            bc.putExtra(LISTENER_FLAG_TYPE, LISTENER_STRING_FLAG);
            bc.putExtra(LISTENER_MESSAGE, info);
        }
        Log.i(T, "sending listener info broadcast");
        sendBroadcast(bc);
    }

    public void connect() {
        while(true) {
            if(breakFlag) break;
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

    public void updateDownloads(int num) {
        Log.i(T, "preparing updateDownloads broadcast");
        sendUpdateDrawerBroadcast(num, null);
    }

    public void updateDownloadList(String songsInfo) {
        Log.i(T, "preparing updateList broadcast, with info: "+songsInfo);
        sendUpdateDrawerBroadcast(0, songsInfo);
    }

    public String removeDownloadedSongAndClean(String id) {
        Log.i(T, "removing Downloaded song from list, with id: "+id);
        return pastSongsInfo.substring(pastSongsInfo.indexOf("\n"), pastSongsInfo.length());
    }

    class ListenerThread extends Thread
    {
        boolean done = false;
        public void run()
        {
            Socket sock = null;
            try
            {
                Thread t;
                sock = new Socket(HOST, SOCK_PORT);
                sock.setSoTimeout(0);
                // \n used to terminate scanner on the other side.
                byte[] uniqueIdBytes =uniqueId.getBytes();
                Log.i(T, "sending id = "+new String(uniqueIdBytes));
                InputStream is = sock.getInputStream();
                OutputStream os = sock.getOutputStream();
                os.write(Utils.mergeBytes(Utils.intToBytes(uniqueIdBytes.length), uniqueIdBytes));
                Log.i(T, "id sent, blocking for inputStream reading interval");

                byte[] intBytes = new byte[4];
                is.read(intBytes);
                int interval = Utils.bytesToInt(intBytes);
                pref.putInt(W8_INTERVAL, interval);
                Log.i(T, "received interval = "+interval);
                pref.commit();

                //if user authenticated continue
                is.read(intBytes);
                boolean hasUnackRequests = (Utils.bytesToInt(intBytes) == TRUE);
                Log.i(T, "hasUnackRequests "+hasUnackRequests);
                if (hasUnackRequests)
                {
                    new Thread() {
                        @Override
                        public void run() {
                            Utils.getRequestsFromServer(ACCEPTED_REQUESTS, null, false, true, uniqueId);
                        }
                    }.start();
                }

                is.read(intBytes);
                boolean hasNewSharedSongs = (Utils.bytesToInt(intBytes) == TRUE);
                Log.i(T, "hasSongs "+hasNewSharedSongs);
                if(hasNewSharedSongs)
                {
                    t = new Thread() {
                        @Override
                        public void run() {
                            Utils.getSongsFromServer(NEW_SONGS, null, true, uniqueId);
                        }
                    };
                    t.start();
                }

                is.read(intBytes);
                boolean hasRequests = (Utils.bytesToInt(intBytes) == TRUE);
                Log.i(T, "hasRequests "+hasRequests);
                if(hasRequests)
                {
                    new Thread() {
                        @Override
                        public void run() {
                            Utils.getRequestsFromServer(NEW_REQUESTS, null, false, false, uniqueId); //un-responded requests
                        }
                    }.start();
                }

                is.read(intBytes);
                int numOfSongsToDownload = Utils.bytesToInt(intBytes);
                Log.i(T, "num of totalDownloads " + numOfSongsToDownload);

                if(numOfSongsToDownload > 0)
                {

                    is.read(intBytes);
                    int totalDownloads = Utils.bytesToInt(intBytes);
                    Log.i(T, "total Downloads " + totalDownloads);
                    if (totalDownloads > 0 && totalDownloads != pastTotalDownloads) {
                        Log.i(T, "updating drawer panel with server num of total downloads");
                        updateDownloads(totalDownloads);
                        pastTotalDownloads = totalDownloads;
                    }

                    wasDownloading = true; // will be true in a few secs

                    is.read(intBytes);
                    int songsListBytesSize = Utils.bytesToInt(intBytes);

                    byte[] songsNameBytes = new byte[songsListBytesSize];
                    is.read(songsNameBytes);
                    String songsInfo = new String(songsNameBytes);
                    Log.i(T, "songs info in ISO " + new String(songsNameBytes));
                    if (!pastSongsInfo.equals(songsInfo)) {
                        Log.i(T, "pastSongsInfo != serverSongsInfo, using server value");
                        Log.i(T, "pastSongsInfo "+pastSongsInfo);
                        Log.i(T, "songsInfo "+songsInfo);

                        updateDownloadList(songsInfo);
                        pastSongsInfo = songsInfo;
                    }
                    readSong(is, os, songsInfo.split("\n")[0]);
                }

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

        public void readSong(InputStream is, OutputStream os, String urlAndName) throws IOException {

            String[] urlNameInfo = urlAndName.split("/");
            byte[] songSizeBytes = new byte[4];
            is.read(songSizeBytes);
            int songSize = Utils.bytesToInt(songSizeBytes);
            Log.i(T, " songSize = "+songSize+"b");

            byte[] songByte = new byte[songSize];
            DownloadSong downloadSong = new DownloadSong(Listener.this, is, songByte, new String(urlNameInfo[1].getBytes(), ISO_8859_1)+".webm", urlNameInfo[0]);

            synchronized (songByte) {
                try {
                    songByte.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            os.write(downloadSong.successed()?DOWNLOAD_SUCCESSED:DOWNLOAD_FAILED);

            Log.i(T, "song read, with success "+downloadSong.successed());
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath();
            path = path+ "/" + urlNameInfo[1];
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

    class DownloadSong  extends Thread {
        private InputStream is;
        private byte[] bytes;
        private NotificationCompat.Builder notBuilder;
        private int size;
        private int updatePoint;
        private NotificationManager notMan;
        private boolean successed = true;
        private String songId;
        public DownloadSong(Context context, InputStream inputStream, byte[] sb, String songName, String songId) {
            is = inputStream;
            bytes = sb;
            size = bytes.length;
            updatePoint = size/50; // update notification every updatePoint of bytes;
            this.songId = songId;
            try {
                notBuilder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_menu_camera)
                        .setContentTitle(getString(R.string.DOWNLOADING_NOTIFY_TITLE)+"  "+String.format("%.2f", (float) size / (1024 * 1024))+" MB")
                        .setContentText(songName)
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
                    Log.i(T, "song downloaded, update totalDownloads to "+(pastTotalDownloads+1)
                            + " remove downloaded song");
                    notBuilder.setContentText(getString(R.string.DOWNLOAD_DONE));
                    notBuilder.setProgress(0, 0, false);
                    notMan.notify(0, notBuilder.build());
                    lastDownloadedSongId = songId;
                    Log.i(T, "update Downloads with passTotalDownloads "+pastTotalDownloads+" +1");
                    updateDownloads(++pastTotalDownloads);
                    Log.i(T, "update Panel with songsInfo without song with id "+songId);
                    updateDownloadList(removeDownloadedSongAndClean(songId));

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
;