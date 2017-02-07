package err.sfp;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by Err on 16-12-7.-
 */

public class HttpConThread extends Thread implements Consts{
    public int code = 0;
    public InputStream is = null;
    String url = null;
    HttpURLConnection con = null;
    byte[] postBytes = null;

    public HttpConThread(String url) {
        this.url = url.startsWith("http")?url:PROTOCOL+HOST+PATH+url;
        this.start();
    }

    public HttpConThread(String url, byte[] outputStreamBytes) {
        this.url = url.startsWith("http")?url:PROTOCOL+HOST+PATH+url;
        this.postBytes = outputStreamBytes;
        this.start();
    }

    @Override
    public void run() {
        try {
            Log.i(T, "connecting");
            con = (HttpURLConnection) new URL(url).openConnection();
            if (postBytes != null) {
                Log.i(T, "post method with body of len "+postBytes.length);
                con.setRequestMethod("POST");
                con.setRequestProperty("content-type", "application/octet-stream");
                con.getOutputStream().write(postBytes);
            }
            con.connect();
            code = con.getResponseCode();
            Log.i(T, "code read");
            if (code < 400)
                is = con.getInputStream();
            Log.i(T, "is, code read");
        } catch (Exception io) {
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
