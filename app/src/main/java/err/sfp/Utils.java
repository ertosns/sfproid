package err.sfp;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Base64DataException;
import android.util.Log;
import android.util.Patterns;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Pattern;
import err.sfp.Authentication.Login;
import err.sfp.Database.Database;
import err.sfp.SocialNetworking.ItemInfo;

import static android.R.attr.dayOfWeekBackground;
import static android.R.attr.id;


/**
 * Created by Err on 16-12-7.
 */

//TODO global pref
public class Utils implements Consts{
    static SharedPreferences sharedPreferences;
    static SharedPreferences.Editor edit;
    static Database database;

    static {
        sharedPreferences = Main.sharedPreferences;
        edit = Main.edit;
        database = Main.database;
    }

    public static byte[] intToBytes(int x) {
        byte[] bytes = new byte[4];
        for(int i = 0; i < 4; i++)
            bytes[i] = (byte) ( x >> 8*i);
        return bytes;
    }

    public static int readInt(InputStream is) {
        byte[] bytes = new byte[4];
        int x = 0;
        try {
            is.read(bytes);
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

    public static long bytesToLong(byte[] intBytes) {
        long x = 0;
        for(int i = 0; i < 8; i++)
            x |= (intBytes[i] & 0xff) << 8*i;
        return x;
    }

    public static String getUserUniqueId() {
        return Main.sharedPreferences.getString(Consts.UNIQUE_ID, null);
    }

    public static Bitmap getThumbnailBitmap(String url) {
        try {
            Log.i(T, "downloading bitmap of url: "+url);
            return BitmapFactory.decodeStream(new URL(url).openStream());
        } catch (IOException e) {
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
        return ((ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo() != null;
    }

    public static String base64(String m) {
        return Base64.encodeToString(m.getBytes(), Base64.DEFAULT);
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
        return getApplicationContext();
    }

    public static void broadcastUniqueId(String id, Context context) {
        Intent intent = new Intent(context, Listener.class);
        intent.setAction(SHAREDPREFERENCES_ACTION_FILTER);
        intent.putExtra(PREFERENCE_TYPE, UNIQUE_ID_FLAG);
        intent.putExtra(UNIQUE_ID, id);
        context.sendBroadcast(intent);
    }

    public static void removePeershipRequest(byte[] senderId) {
        new HttpConThread("removerelation=true&id="+getUserUniqueId()+"peerid="+new String(senderId)).connect();
        database.removePeershipRequest(senderId);
    }

    public static void acceptPeershipRequest(byte[] senderId) {
        new HttpConThread("setrequestresponded=true&id="+getUserUniqueId()+"peerid="+new String(senderId)).connect();
        database.markAsPeer(senderId);
    }

    public static void sendPeershipRequest(byte[] senderId) {
        new HttpConThread("sendsharerequest=true&id="+getUserUniqueId()+"peerid"+new String(senderId));
        //TODO in tcp send accepted requests by users id. when senderId = givin id
        // receive requests at Client side
        // insert in db.
    }

    public static void getPeersFromServer(ItemInfo[] ii, boolean newRequests) {
        // new app install.

        String songsInfoQuery = ((newRequests)?"listofunrespondedrequests":"listofnewrequests"+"=true&id=")+Utils.getUserUniqueId();
        try {
            HttpConThread con = new HttpConThread(songsInfoQuery);
            con.connect();
            InputStream is = con.is;

            byte[] intBytes = new byte[4];
            is.read(intBytes);
            int requests = Utils.bytesToInt(intBytes);
            ii = new ItemInfo[requests];
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

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateSharedSong(byte[] idBytes, boolean download, String songId, String title) {
        int databaseId = bytesToInt(idBytes);
        database.removeLocalSong(idBytes);
        new HttpConThread("removesong=true&id="+getUserUniqueId()+"&songdatabaseid="+id).connect();
        if(download) downloadSong(songId, title);
    }


    public static int  downloadSong(String songId, String title) {
        String downloadSongQuery= new StringBuilder(PROTOCOL).append(HOST).append(PATH)
                .append("mobilesdownload=true&id=")
                .append(Main.sharedPreferences.getString(UNIQUE_ID, "null"))
                .append("&songId=").append(songId)
                .append("&title=").append(title).toString();

        HttpConThread con = new HttpConThread(downloadSongQuery);
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
        return res;
    }



}
