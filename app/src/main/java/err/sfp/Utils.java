package err.sfp;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Base64DataException;
import android.util.Patterns;
import java.util.regex.Pattern;
import err.sfp.Authentication.Login;


/**
 * Created by Err on 16-12-7.
 */

//TODO global pref
public class Utils implements Consts{

    public static byte[] intToBytes(int x) {
        byte[] bytes = new byte[4];
        for(int i = 0; i < 4; i++)
            bytes[i] = (byte) ( x >> 8*i);
        return bytes;
    }

    public static int bytesToInt(byte[] intBytes) {
        int x = 0;
        for(int i = 0; i < 4; i++)
            x |= (intBytes[i] & 0xff) << 8*i;
        return x;
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

    public static void broadcastUniqueId(String id, Context context) {
        Intent intent = new Intent(context, Listener.class);
        intent.setAction(SHAREDPREFERENCES_ACTION_FILTER);
        intent.putExtra(PREFERENCE_TYPE, UNIQUE_ID_FLAG);
        intent.putExtra(UNIQUE_ID, id);
        context.sendBroadcast(intent);
    }

}
