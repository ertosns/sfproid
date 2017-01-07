package err.sfp;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

import err.sfp.Database.Database;
import err.sfp.SocialNetworking.ItemInfo;
import err.sfp.SocialNetworking.Requests;
import err.sfp.SocialNetworking.Songs;

/**
 * Created by Err on 16-12-6.
 */

//TODO SET FLAGS ALL OVER THE CODE
//TODO inform server you received successfully which song should be removed from server, (res) how several write will work?

public class Listener extends IntentService implements Consts {
    SharedPreferences sharedPreferences  = null;
    SharedPreferences.Editor pref = null;
    String uniqueId = null;
    boolean breakFlag = false;
    int pastTotalDownloads = 0;
    String pastSongsInfo = "";
    boolean wasDownloading = false;
    String lastDownloadedSongId = "";
    Database database;
    NotificationManager manger = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    public Listener() {
        super(Listener.class.getName());
        database = Main.database;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
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
        public void onReceive(Context context, Intent intent) {
            //possible received intents key sorted by frequency {stop, online}
            //one pair received at time preceded by type flag (int)
            Log.i(T, "sharedPreferences broadcast received, to stop service(!online), kill service");
            int preference = intent.getIntExtra(PREFERENCE_TYPE, STOP_FLAG);

            if (preference == STOP_FLAG) {
                if (intent.getBooleanExtra(STOP_PREF, false)) killService();
            } else if (preference == ONLINE_FLAG) {
                pref.putBoolean(ONLINE, intent.getBooleanExtra(ONLINE, false));
                pref.commit();
            }
        }


    };

    public void killService() {
        Log.i(T, "stopping Listener service");
        stopSelf();
        breakFlag = true;
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

    public void sendUpdateDrawerBroadcast(boolean integer, int x, String msg) {
        Intent bc = new Intent(LISTENER_MESSAGES_INTNET_FILTER);
        bc.setAction(LISTENER_MESSAGES_INTNET_FILTER);
        if(integer) {
            bc.putExtra(LISTENER_FLAG_TYPE, LISTENER_INTEGER_FLAG);
            bc.putExtra(LISTENER_INTEGER_FLAG_KEY, x);
        }
        else {
            bc.putExtra(LISTENER_FLAG_TYPE, LISTENER_STRING_FLAG);
            bc.putExtra(LISTENER_MESSAGE, msg);
        }
        Log.i(T, "sending listener info broadcast");
        sendBroadcast(bc);
    }

    public void updateDownloads(int num) {
        Log.i(T, "preparing updateDownloads broadcast");
        sendUpdateDrawerBroadcast(true, num, null);
    }

    public void updateDownloadList(String songsInfo) {
        Log.i(T, "preparing updateList broadcast, with info: "+songsInfo);
        sendUpdateDrawerBroadcast(false, 0, songsInfo);
    }

    public String removeDownloadedSongAndClean(String id) {
        Log.i(T, "removing Downloaded song from list, with id: "+id);
        return pastSongsInfo.replaceAll(id+"/[\\w\\W]+\\n", ""); //does \n is part of \\w
    }

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

                byte[] intBytes = new byte[4];
                is.read(intBytes);
                int interval = Utils.bytesToInt(intBytes);
                pref.putInt(W8_INTERVAL, interval);
                Log.i(T, "received interval = "+interval);
                pref.commit();

                Thread t;

                is.read(intBytes);
                int hasSongs = Utils.bytesToInt(intBytes);
                if(hasSongs == TRUE) {
                    t = new Thread() {
                        @Override
                        public void run() {
                            String songsInfoQuery = "listofnewsharedsongs=true&id="+Utils.getUserUniqueId();
                            try {
                                HttpConThread con = new HttpConThread(songsInfoQuery);
                                con.connect();
                                InputStream is = con.is;

                                Intent downloadSongIntent = new Intent(Listener.this, Songs.class);
                                downloadSongIntent.putExtra(DOWNLOAD_SONG, TRUE);
                                PendingIntent downloadSongPending = PendingIntent.getBroadcast(getApplicationContext(), 0, downloadSongIntent, flag);

                                Intent refuseSongIntent = new Intent(Listener.this, Songs.class);
                                refuseSongIntent.putExtra(DOWNLOAD_SONG, TRUE);
                                PendingIntent refuseSongPending = PendingIntent.getBroadcast(getApplicationContext(), 0, refuseSongIntent, flag);

                                byte[] intBytes = new byte[4];
                                is.read(intBytes);
                                int songs = Utils.bytesToInt(intBytes);
                                ItemInfo[] ii = new ItemInfo[songs];
                                for (int i = 0; i < songs; i++) {

                                    byte[] bytes = new byte[4];
                                    is.read(bytes);
                                    ii[i].databaseId = bytes;

                                    int size = Utils.readInt(is);
                                    bytes = new byte[size];
                                    is.read(bytes);
                                    ii[i].songUrl = new String(bytes);

                                    size = Utils.readInt(is);
                                    bytes = new byte[size];
                                    is.read(bytes);
                                    ii[i].title = new String(bytes);

                                    bytes = new byte[8];
                                    is.read(bytes);
                                    ii[i].dateMillis = Utils.bytesToInt(bytes);

                                    size = Utils.readInt(is);
                                    bytes = new byte[size];
                                    is.read(bytes);
                                    ii[i].message = new String(bytes);

                                    size = Utils.readInt(is);
                                    bytes = new byte[size];
                                    is.read(bytes);
                                    ii[i].subTitle = new String(bytes);

                                    size = Utils.readInt(is);
                                    bytes = new byte[size];
                                    is.read(bytes);
                                    ii[i].senderId = bytes;

                                    downloadSongIntent.putExtra(SONG_DATABASE_ID, ii[i].databaseId);
                                    downloadSongIntent.putExtra(SONG_ID, ii[i].songUrl);
                                    downloadSongIntent.putExtra(TITLE, ii[i].title);
                                    refuseSongIntent.putExtra(SONG_DATABASE_ID, ii[i].databaseId);

                                    NotificationCompat.Builder builder = new NotificationCompat
                                            .Builder(getApplicationContext())
                                            .setLargeIcon(ii[i].image)
                                            .setContentTitle(ii[i].title)
                                            .setContentText(ii[i].subTitle)
                                            .setContentInfo(ii[i].message)//TODO note sure what is contentInfo
                                            //TODO add drawable if necessary
                                            .addAction(new NotificationCompat.Action(
                                                    0,
                                                    getString(R.string.DOWNLOAD_SONG),
                                                    downloadSongPending))
                                            .addAction(new NotificationCompat.Action(
                                                    0,
                                                    getString(R.string.REFUSE),
                                                    refuseSongPending));

                                    manger.notify(0, builder.build());
                                }

                                for (int i = 0; i < songs; i++) {
                                    ii[i].image = Utils.getThumbnailBitmap(ii[i].songUrl);
                                }
                                database.insertSongsItemsInfo(ii);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    t.start();
                }

                is.read(intBytes);
                int hasRequests = Utils.bytesToInt(intBytes);
                if(hasRequests == TRUE) {
                    t = new Thread() {
                        @Override
                        public void run() {
                            String songsInfoQuery = "listofnewrequests=true&id="+Utils.getUserUniqueId();
                            try {
                                HttpConThread con = new HttpConThread(songsInfoQuery);
                                con.connect();
                                InputStream is = con.is;

                                Intent requestAcceptedIntent = new Intent(Listener.this, Requests.class);
                                requestAcceptedIntent.putExtra(PEERSHIP_REQUEST_ACCPTED, TRUE);
                                PendingIntent requestAcceptedPending = PendingIntent.getBroadcast(getApplicationContext(), 0, requestAcceptedIntent, flag);

                                Intent requestRefusedIntent = new Intent(Listener.this, Requests.class);
                                requestRefusedIntent.putExtra(PEERSHIP_REQUEST_ACCPTED, FALSE);
                                PendingIntent requestRefusedPending = PendingIntent.getBroadcast(getApplicationContext(), 0, requestRefusedIntent, flag);

                                byte[] intBytes = new byte[4];
                                is.read(intBytes);
                                int requests = Utils.bytesToInt(intBytes);
                                ItemInfo[] ii = new ItemInfo[requests];
                                // note the same code repeated at Request
                                for (int i = 0; i < requests; i++) {

                                    int size = Utils.readInt(is); //name
                                    byte[] bytes = new byte[size];
                                    is.read(bytes);
                                    ii[i].title = new String(bytes);

                                    size = Utils.readInt(is); //email
                                    bytes = new byte[size];
                                    is.read(bytes);
                                    ii[i].subTitle = new String(bytes);

                                    size = Utils.readInt(is);
                                    bytes = new byte[size];
                                    is.read(bytes);
                                    ii[i].senderId = bytes; // (no need to retrieve senderIds from give parameter ids) protocol will change send flag instead of ids

                                    size = Utils.readInt(is);
                                    bytes = new byte[size];
                                    is.read(bytes);
                                    ii[i].image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                                    // send broadcast to Requests to either accept, refuse requests
                                    // accept with acknowledge the server, and update local database as will
                                    // accepted requests will only be used to send songs, and should have separate table.
                                    // .. update will remove local requests row, and insert new peers row
                                    // .. remove remove requests table row with givin senderId (which is unique)

                                    requestAcceptedIntent.putExtra(PEER_REQUEST_SENDER_ID, ii[i].senderId);
                                    requestRefusedIntent.putExtra(PEER_REQUEST_SENDER_ID, ii[i].senderId);

                                    //TODO notification must dismiss after any btn clicked
                                    NotificationCompat.Builder builder = new NotificationCompat
                                            .Builder(getApplicationContext())
                                            .setLargeIcon(ii[i].image)
                                            .setContentTitle(ii[i].title)
                                            .setContentText(ii[i].subTitle)
                                            //TODO add drawable if necessary
                                            .addAction(new NotificationCompat.Action(
                                                    0,
                                                    getString(R.string.ACCEPT_PEERSHIP),
                                                    requestAcceptedPending))
                                            .addAction(new NotificationCompat.Action(
                                                    0,
                                                    getString(R.string.REFUSE),
                                                    requestRefusedPending));

                                    manger.notify(0, builder.build());
                                }
                                database.insertRequestsItemsInfo(ii);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    t.start();
                }

                is.read(intBytes);
                int totalDownloads = Utils.bytesToInt(intBytes);
                Log.i(T, "total Downloads "+totalDownloads);
                if (totalDownloads > 0 && totalDownloads != pastTotalDownloads) {
                    Log.i(T, "updating drawer panel with server num of total downloads");
                    updateDownloads(totalDownloads);
                    pastTotalDownloads = totalDownloads;
                }

                is.read(intBytes);
                int numOfSongsToDownload = Utils.bytesToInt(intBytes);
                Log.i(T, "num of totalDownloads "+numOfSongsToDownload);
                if (numOfSongsToDownload > 0) {
                    wasDownloading = true; // will be true in a few secs

                    is.read(intBytes);
                    int songsListBytesSize = Utils.bytesToInt(intBytes);

                    byte[] songsNameBytes = new byte[songsListBytesSize];
                    is.read(songsNameBytes);
                    String songsInfo = new String(songsNameBytes);
                    Log.i(T, "songs info in ISO "+new String(songsNameBytes));
                    if (!pastSongsInfo.equals(songsInfo)) {
                        updateDownloadList(songsInfo);
                        pastSongsInfo = songsInfo;
                    }
                    readSong(is, os, songsInfo.split("\n")[0]);

                } else {
                    if(wasDownloading && lastDownloadedSongId != null) {
                        Log.i(T, "updating downloading ++num, list(to \"\") after download is done, num "+pastTotalDownloads);
                        updateDownloads(++pastTotalDownloads);
                        updateDownloadList("");
                    }
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
                    updateDownloads(++pastTotalDownloads);
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
