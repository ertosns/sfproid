package err.sfp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.util.regex.Pattern;
import err.sfp.Database.Database;
import err.sfp.SocialNetworking.ItemInfo;
import err.sfp.SocialNetworking.Requests;
import err.sfp.SocialNetworking.Songs;

/**
 * Created by Err on 16-12-7.
 */

public class Utils implements Consts{

    static SharedPreferences sharedPreferences;
    static SharedPreferences.Editor edit;
    static Context context;
    static NotificationManager manger = (NotificationManager) Main.context.getSystemService(context.NOTIFICATION_SERVICE);

    static PendingIntent respondRequestPending;
    static PendingIntent respondSongPending;

    static
    {
        sharedPreferences = Main.sharedPreferences;
        edit = Main.edit;
        context = Main.context;
        // not context here in Main and not Listener's

        respondRequestPending = PendingIntent.getActivity (getApplicationContext(), 0,
                new Intent(context, Requests.class), PendingIntent.FLAG_UPDATE_CURRENT);
        respondSongPending = PendingIntent.getActivity (getApplicationContext(), 0,
                new Intent(context, Songs.class), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static String formatBytes(long len) {
        float m = 0;
        if(len < 1024) return len+"b";
        return len/1024+" K "+ (( (m = (((float)len)/(1024*1024)) ) > 0.1)?(String.format("%.2f", m)+" M "):"");
    }

    public static byte[] intToBytes(int x) {
        byte[] bytes = new byte[4];
        for(int i = 0; i < 4; i++)
            bytes[i] = (byte) ( x >> 8*i);
        return bytes;
    }

    public static int readInt(InputStream is)
    {
        byte[] bytes = new byte[4];
        int x = 0;
        try {
            is.read(bytes);
            Log.i(T, "int in hex "+bytesToHex(bytes));
            x = bytesToInt(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return x;
    }

    public static int bytesToInt(byte[] intBytes) {
        int x = 0;
        for(int i = 0; i < 4; i++)
            x |= (intBytes[i] & 0xff) << 8*i;
        return x;
    }

    public static String bytesToHex(byte[] bytes)
    {
        BigInteger intRep = new BigInteger(bytes);
        return intRep.toString(16);
    }

    public static byte[] hexToBytes(String hex) {
        BigInteger intRep = new BigInteger(hex, 16);
        return intRep.toByteArray();
    }

    public static long bytesToLong(byte[] intBytes)
    {
        long x = 0;
        for (int i = 0; i < 8; i++)
            x |= ((long)(intBytes[i] & 0xff)) << 8*i;
        return x;
    }

    public static String getUserUniqueId()
    {
        return Main.sharedPreferences.getString(Consts.UNIQUE_ID, null);
    }

    public static Bitmap getThumbnailBitmap(String url)
    {
        try
        {
            Log.i(T, "downloading bitmap of url: "+url);
            return BitmapFactory.decodeStream(new URL(url).openStream());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean validateWord(String word) {
        Pattern pattern = Pattern.compile("[\\w]{4,20}");
        return pattern.matcher(word).matches();
    }

    public static boolean validatePass(String pass) {
        Pattern pattern = Pattern.compile("[\\w]{8,20}");
        return pattern.matcher(pass).matches();
    }

    public static boolean validateEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static void mainActivityIntent(Context context) {
        Intent intent = new Intent(context, Download.class);
        //TODO set stack flags
        context.startActivity(intent);
    }

    public static boolean isOnline(Context context) {
        return ((ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE))
                .getActiveNetworkInfo() != null;
    }

    public static String base64Url(String m) {
        return Base64.encodeToString(m.getBytes(), Base64.URL_SAFE).replace("=", "");
    }

    public static byte[] mergeBytes(byte[] byte1, byte[] byte2) {
        int len1 = byte1.length;
        int len2 = byte2.length;
        byte[] newBytes = new byte[len1+len2];
        for(int i = 0; i < len1; i++)
            newBytes[i] = byte1[i];
        for(int i = 0; i < len2; i++)
            newBytes[i+len1] = byte2[i];
        return newBytes;
    }

    public static Object[] mergeObjectArrays(Object[] a1, Object[] a2) {
        int len1 = a1.length;
        int len2 = a2.length;
        Object[] newobjects = new Object[len1+len2];
        for(int i = 0; i < len1+len2; i++)
            newobjects[i] = a1[i];
        for(int i = 0; i < len2; i++)
            newobjects[i+len1] = a2[i];
        return newobjects;
    }

    public static Context getApplicationContext() {
        return Main.context;
    }

    public static void broadcastUniqueId(String id, Context context) {
        Intent intent = new Intent(context, Listener.class);
        intent.setAction(SHAREDPREFERENCES_ACTION_FILTER);
        intent.putExtra(PREFERENCE_TYPE, UNIQUE_ID_FLAG);
        intent.putExtra(UNIQUE_ID, id);
        context.sendBroadcast(intent);
    }

    public static void removePeershipRequest(byte[] senderId) {
        Log.i(T, "remove peership requer with senderId "+senderId.length);
        new HttpConThread("removerelation=true&mobile=true&id="+getUserUniqueId(), mergeBytes(intToBytes(senderId.length), senderId));
        Database.getDatabase().removePeershipRequest(senderId);
    }

    public static void acceptPeershipRequest(byte[] senderId) {
        Log.i(T, "accept peership reuest with sender id "+senderId.length);
        new HttpConThread("setrequestresponded=true&mobile=true&id="+getUserUniqueId(), mergeBytes(intToBytes(senderId.length), senderId));
        Database.getDatabase().markAsPeer(senderId);
    }

    public static void sendPeershipRequest(byte[] senderId) {
        Log.i(T, "send peership request with senderId len "+senderId.length);
        new HttpConThread("sendsharerequest=true&mobile=true&id="+getUserUniqueId(), mergeBytes(intToBytes(senderId.length), senderId));
    }

    public static void markRequestSent(byte[] senderId) {
        Log.i(T, "markRequestSent with senderId len "+senderId.length);
        String markRequestSentUrl = "markrequestsent=true&mobile=true&id="+getUserUniqueId();
        new HttpConThread(markRequestSentUrl, Utils.mergeBytes(Utils.intToBytes(senderId.length),senderId));
    }

    public static void getRequestsFromServer(String query, ItemInfo[] ii, boolean newRequests, boolean ack, String uniqueId)
    {
        // QUERY <- {ACK_REQUESTS, NEW_REQUESTS}
        //todo SET DATABASE VARAITION
        String songsInfoQuery = query+"=true&mobile=true&id="+(uniqueId == null?getUserUniqueId():uniqueId);
        Log.i(T, "songInfoQuery "+songsInfoQuery);
        try
        {
            HttpConThread con = new HttpConThread(songsInfoQuery);
            synchronized (con)
            {
                con.wait();
            }

            InputStream is = con.is;

            byte[] intBytes = new byte[4];
            is.read(intBytes);
            int requests = Utils.bytesToInt(intBytes);

            Log.i(T, "Requests "+requests);
            ii = ItemInfo.initArray(requests);
            // note the same code repeated at Request
            for (int i = 0; i < requests; i++)
            {
                int size = Utils.readInt(is); //name
                byte[] bytes = new byte[size];
                Log.i(T, "title size "+size);
                is.read(bytes);
                ii[i].title = new String(bytes);

                size = Utils.readInt(is); //email
                bytes = new byte[size];
                Log.i(T, "email size "+size);
                is.read(bytes);
                ii[i].subTitle = new String(bytes);

                size = Utils.readInt(is);
                bytes = new byte[size];
                Log.i(T, "image size "+size);
                is.read(bytes);
                ii[i].image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                size = Utils.readInt(is);
                bytes = new byte[size];
                Log.i(T, "senderId size "+size);
                is.read(bytes);
                ii[i].senderId = bytes;

                if (!newRequests)
                { //unAck_requests||accepted_requests

                    Log.i(T, "preparing request notification");

                    //TODO notification must dismiss after any btn clicked
                    NotificationCompat.Builder builder = new NotificationCompat
                            .Builder(getApplicationContext())
                            .setSmallIcon(R.drawable.ic_menu_gallery)
                            .setLargeIcon((ii[i].image == null)?BitmapFactory.decodeResource(Main.context.getResources(), android.R.drawable.gallery_thumb):ii[i].image)
                            .setContentTitle(ii[i].title+" "+context.getString(R.string.PEER_SEND_REQUEST))
                            .setContentText(ii[i].subTitle)
                            .addAction(new NotificationCompat.Action(0, context.getString(R.string.RESPOND), respondSongPending));


                    manger.notify(0, builder.build());
                }
            }
            Database.getDatabase().insertRequestsItemsInfo(ii, ack);
            for(int i = 0; i < ii.length; i++)
                if(query.equals(NEW_REQUESTS)) markRequestSent(ii[i].senderId);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void getSongsFromServer(String query, ItemInfo[] ii, boolean newShares, String uniqueId)
    {
        String songsInfoQuery = query+"=true&mobile=true&id="+(uniqueId == null?getUserUniqueId():uniqueId);
        Log.i(T, "songInfoQuery "+songsInfoQuery);
        try
        {
            HttpConThread con = new HttpConThread(songsInfoQuery);
            synchronized (con)
            {
                con.wait();
            }
            InputStream is = con.is;

            byte[] intBytes = new byte[4];
            is.read(intBytes);
            int songs = Utils.bytesToInt(intBytes);
            ii = ItemInfo.initArray(songs);
            Log.i(T, "songs "+songs);
            for (int i = 0; i < songs; i++)
            {

                byte[] bytes = new byte[4];
                is.read(bytes);
                Log.i(T, "toHex "+Utils.bytesToHex(bytes));
                ii[i].databaseId = bytes;

                int size = Utils.readInt(is);
                bytes = new byte[size];
                Log.i(T, "songUrl size "+size);
                is.read(bytes);
                Log.i(T, "toHex "+Utils.bytesToHex(bytes));
                ii[i].songUrl = new String(bytes);

                size = Utils.readInt(is);
                bytes = new byte[size];
                Log.i(T, "song title size "+size);
                is.read(bytes);
                Log.i(T, "toHex "+Utils.bytesToHex(bytes));
                ii[i].title = new String(bytes);

                is.read(new byte[4]); // length of date --> 8bytes, designed that way for easy testing
                bytes = new byte[8];
                is.read(bytes);
                Log.i(T, "toHex "+Utils.bytesToHex(bytes));
                ii[i].dateMillis = Utils.bytesToLong(bytes);

                size = Utils.readInt(is);
                bytes = new byte[size];
                Log.i(T, "message size "+size);
                is.read(bytes);
                Log.i(T, "toHex "+Utils.bytesToHex(bytes));
                ii[i].message = new String(bytes);

                size = Utils.readInt(is);
                bytes = new byte[size];
                Log.i(T, "subTitle size "+size);
                is.read(bytes);
                ii[i].subTitle = new String(bytes);

                size = Utils.readInt(is);
                bytes = new byte[size];
                Log.i(T, "senderId size "+size);
                Log.i(T, "toHex "+Utils.bytesToHex(bytes));
                is.read(bytes);
                ii[i].senderId = bytes;

                Log.i(T, "download song thumbnail");
                ii[i].image = Utils.getThumbnailBitmap(Utils.formYoutubeThumbnailUrl(ii[i].songUrl));

                if (newShares)
                {
                    Log.i(T, "preparing newShares|songs notification");

                    NotificationCompat.Builder builder = new NotificationCompat
                            .Builder(getApplicationContext())
                            .setSmallIcon(R.drawable.ic_menu_gallery)
                            .setLargeIcon((ii[i].image == null)?BitmapFactory.decodeResource(Main.context.getResources(), android.R.drawable.gallery_thumb):ii[i].image)
                            .setContentTitle(ii[i].title)
                            .setContentText(ii[i].subTitle)
                            .setContentInfo(ii[i].message)//TODO note sure what is contentInfo
                            //TODO add drawable if necessary
                            .addAction(new NotificationCompat.Action(0, context.getString(R.string.RESPOND), respondRequestPending));

                    manger.notify(0, builder.build());
                }
            }

            Database.getDatabase().insertSongsItemsInfo(ii);

            for (int i = 0; i < ii.length; i++)
            {
                Log.i(T, "mark song as sent");
                markSongSent(ii[i].songUrl, ii[i].senderId);
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static String formYoutubeThumbnailUrl(String id)
    {
        return new StringBuilder("https://i.ytimg.com/vi/").append(id).append("/default.jpg").toString();
    }

    public static void removeSong(byte[] idBytes)
    {
        Log.i(T, "removeSong");
        int databaseId = bytesToInt(idBytes);
        Database.getDatabase().removeLocalSong(idBytes);
        new HttpConThread("removesong=true&mobile=true&id="+getUserUniqueId()+"&songdatabaseid="+databaseId);
    }

    // if senderId = 0, then it's downloadable song, otherwise it's share record
    public static int downloadSong(String songUrl, String title)
    {
        String downloadSongQuery= new StringBuilder(PROTOCOL).append(HOST).append(PATH)
                .append("mobilesdownload=true&mobile=true&id=")
                .append(Main.sharedPreferences.getString(UNIQUE_ID, "null"))
                .append("&songId=").append(songUrl)
                .append("&title=").append(title)
                .toString();

        Log.i(T, "downloading song with title "+title+", songUrl "+songUrl);
        HttpConThread con = new HttpConThread(downloadSongQuery);
        int res = 0;
        synchronized (con) {
            try {
                con.wait();
                res =  con.code;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.i(T, "done! download Query request sent, downloadSongCode = "+res);
        return res;
    }

    public static int markSongDownloadable(byte[] idBytes)
    {
        int id = Utils.bytesToInt(idBytes);
        String downloadSongQuery= new StringBuilder(PROTOCOL).append(HOST).append(PATH)
                .append("marksongdownloadable=true&mobile=true&id=")
                .append(Main.sharedPreferences.getString(UNIQUE_ID, "null"))
                .append("&songdatabaseid=").append(id)
                .toString();

        Log.i(T, "mark song downloadable with databaseId "+id);
        HttpConThread con = new HttpConThread(downloadSongQuery);
        int res = 0;
        synchronized (con) {
            try {
                con.wait();
                res =  con.code;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Database.getDatabase().removeLocalSong(idBytes);
        Log.i(T, "done! download Query request sent, downloadSongCode = "+res);
        return res;
    }

    public static int shareSong(String songUrl, String title, String message, byte[] senderId)
    {
        String downloadSongQuery= new StringBuilder(PROTOCOL).append(HOST).append(PATH)
                .append("sharesong=true&mobile=true&id=")
                .append(Main.sharedPreferences.getString(UNIQUE_ID, "null"))
                .append("&songId=").append(songUrl)
                .append("&title=").append(title)
                .append("&message=").append(message)
                .toString();

        Log.i(T, "sharing song with title "+title+", message "+message+", songId "+songUrl+", senderId len "+senderId.length);
        HttpConThread con = new HttpConThread(downloadSongQuery, Utils.mergeBytes(Utils.intToBytes(senderId.length), senderId));
        int res = 0;
        synchronized (con)
        {
            try
            {
                con.wait();
                res =  con.code;
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        Log.i(T, "done! shared, shareSongCode = "+res);
        return res;
    }

    public static void markSongSent(String songUrl, byte[] senderId)
    {
        Log.i(T, "markSongSent for song url "+songUrl+" senderId len "+senderId.length);
        String markSongUrl = "marksongsent=true&mobile=true&id="+getUserUniqueId()+"&songId="+songUrl;
        new HttpConThread(markSongUrl, Utils.mergeBytes(Utils.intToBytes(senderId.length), senderId));
    }

    public static void initPanel()
    {
        Log.i(T, "init panel");
        String getDownloadsQuery = "numofdownloads=true&mobile=true&id="+getUserUniqueId();
        HttpConThread con = new HttpConThread(getDownloadsQuery);
        synchronized (con)
        {
            try
            {
                con.wait();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        int code = con.code;
        Log.i(T, "init panel http request code "+code);
        if(code == 200)
        {
            int downloads = readInt(con.is);
            edit.putInt(DOWNLOADS, downloads);
            edit.commit();
        }
    }

    public static void updateMainAndUtils() {
        if (Main.logout) {
            Log.i(T, "update database in Main, Utils, and mark Main_logout_flag = false");
            Main.database = new Database(Main.context);
            Main.logout = false;
        }
    }



}
