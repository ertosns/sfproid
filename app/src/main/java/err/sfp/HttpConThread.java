package err.sfp;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by root on 16-12-7.
 */

public class HttpConThread extends Thread implements Consts{
    String url = null;
    public int code = 0;
    InputStream is = null;

    public HttpConThread(String url) {
        this.url = url;
    }

    @Override
    public void run() {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.connect();
            code = con.getResponseCode();
            if(code == 200) is = con.getInputStream();
        } catch (IOException io) {
            Log.i(T, "connection failed");
            io.printStackTrace();
            code = CONNECTIVITY_ERR_CODE;
        } finally {
            synchronized (this) {
                Log.i(T, "code = "+code);
                this.notify();
            }
        }
    }

    public String getResponseString() {
        if(is == null) return "-1";
        byte[] idSizeBytes = new byte[4];
        String uniqueId = null;
        try {
            is.read(idSizeBytes);
            int x = Utils.bytesToInt(idSizeBytes);
            byte[] idBytes = new byte[x];
            is.read(idBytes);
            uniqueId = new String(idBytes);
            Log.i(T, "uniqueId = "+uniqueId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return uniqueId;
    }
}
