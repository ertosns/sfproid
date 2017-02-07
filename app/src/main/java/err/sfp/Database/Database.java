package err.sfp.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;

import err.sfp.Consts;
import err.sfp.Main;
import err.sfp.SocialNetworking.ItemInfo;
import err.sfp.Utils;

import static err.sfp.Consts.T;

/**
 * Created by Err on 17-1-3.
 */

public class Database extends DBContract
{
    Context context;
    public Helper helper;
    public SQLiteDatabase read;
    public SQLiteDatabase write;

    public Database(Context c)
    {
        this.context = c;
        helper= new Helper(c);
        read = helper.getReadableDatabase();
        write = helper.getWritableDatabase();
    }

    public SQLiteDatabase getWritable()
    {

        if(write.isOpen()) return write;
        else return helper.getWritableDatabase();
    }

    public SQLiteDatabase getReadable()
    {
        if(read.isOpen()) return read;
        else return helper.getReadableDatabase();
    }

    public boolean hasTable(String table) {
        Log.i(T, "check has table "+table);
        Cursor c = getReadable().rawQuery("select name from sqlite_master where name = '"+table+"';", null);
        if(c.moveToNext()) return true;
        return false;
    }

    public static Database getDatabase() {
        if(Main.database == null) {
            Main.database = new Database(Main.context);
            return Main.database;
        }
        return Main.database;
    }

    public void insertRequestsItemsInfo(ItemInfo[] ii, boolean peer)
    {
        Log.i(T, "insert peers Items Info with len "+ii.length+" as peer "+peer);
        ContentValues cv = new ContentValues();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (int i = 0; i < ii.length; i++)
        {
            if (ii[i].image != null)
            {
                Log.i(T, "image found");
                ii[i].image.compress(Bitmap.CompressFormat.PNG, 100, stream);
                cv.put(PEER_IMAGE, stream.toByteArray());
            }
            Log.i(T, i+") inserting title "+ii[i].title+", subtitle "+ii[i].subTitle+", senderIdLen "+ii[i].senderId.length);
            cv.put(PEER_NAME, ii[i].title);
            cv.put(PEER_EMAIL, ii[i].subTitle);
            cv.put(PEER_SENDER_ID, Utils.bytesToHex(ii[i].senderId));
            if(peer) cv.put(PEER, 1); //peer has default 0(false)

            getWritable().insert(PEERS_TABLE, null, cv);
        }
    }

    public ItemInfo[] getPeersShip(boolean peers)
    {
        ArrayList<ItemInfo> iiArray = new ArrayList<>();
        String[] columns = {PEER_IMAGE, PEER_NAME, PEER_EMAIL, PEER_SENDER_ID};
        String selection = PEER + " = ? ";
        String[] args = {(peers?"1":"0")};
        Log.i(T, "get Requests for peers "+peers);
        Cursor c = getReadable().query(PEERS_TABLE, columns, selection, args, null, null, null);
        while(c.moveToNext())
        {
            Log.i(T, "reading peers from database");
            byte[] imageBytes = c.getBlob(0);
            iiArray.add(new ItemInfo((imageBytes==null)?null:BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length),
                    c.getString(1), c.getString(2), null, 0, new byte[0], Utils.hexToBytes(c.getString(3)), null));
        }
        return iiArray.toArray(new ItemInfo[iiArray.size()]);
    }

    public void removePeershipRequest(byte[] senderId) {
        Log.i(T, "removing peership request with senderId of len "+senderId.length);
        getWritable().execSQL("DELETE FROM "+PEERS_TABLE+" WHERE "+PEER_SENDER_ID+" = '"+Utils.bytesToHex(senderId)+"';");
    }

    public void markAsPeer(byte[] senderId) {
        Log.i(T, "marking sender as peer with senderId of len "+senderId.length);
        getWritable().execSQL("UPDATE "+PEERS_TABLE+" SET "+PEER+" = 1 WHERE "+PEER_SENDER_ID+" = '"+Utils.bytesToHex(senderId)+"';");
    }

    //TODO upon-insertion check records aren't already inserted, protocol should assure that won't happend
    public void insertSongsItemsInfo(ItemInfo[] ii)
    {
        Log.i(T, "insert songs item info with len "+ii.length);
        ContentValues cv = new ContentValues();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (int i = 0; i < ii.length; i++)
        {
            if(ii[i].image != null)
            {
                Log.i(T, "image found");
                ii[i].image.compress(Bitmap.CompressFormat.PNG, 100, stream);
                cv.put(SONG_IMAGE, stream.toByteArray());
            }
            Log.i(T, "inserting title "+ii[i].title+", subtitle "+ii[i].subTitle+", senderIdLen "+ii[i].senderId.length);
            cv.put(SONG_TITLE, ii[i].title);
            cv.put(SONG_SENDER_NAME, ii[i].subTitle);
            cv.put(SONG_MESSAGE, ii[i].message);
            cv.put(SONG_DATE, ii[i].dateMillis);
            cv.put(SONG_SERVER_DATABASE_ID, Utils.bytesToInt(ii[i].databaseId));
            cv.put(SONG_SENDER_ID, Utils.bytesToHex(ii[i].senderId));
            cv.put(SONG_URL, ii[i].songUrl);

            getWritable().insert(SONGS_TABLE, null, cv);
        }
    }

    public ItemInfo[] getSongsInfo()
    {
        ArrayList<ItemInfo> iiArray = new ArrayList<>();
        String[] columns = {SONG_IMAGE , SONG_TITLE, SONG_SENDER_NAME, SONG_MESSAGE
                , SONG_DATE, SONG_SERVER_DATABASE_ID, SONG_SENDER_ID, SONG_URL};
        Cursor c = getReadable().query(SONGS_TABLE, columns, null, null, null, null, null);
        while(c.moveToNext())
        {
            Log.i(T, "reading songs from database");
            byte[] imageBytes = c.getBlob(0);
            iiArray.add(new ItemInfo((imageBytes==null)?null:BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length),
                    c.getString(1), c.getString(2), c.getString(3), c.getLong(4),
                    Utils.intToBytes(c.getInt(5)), Utils.hexToBytes(c.getString(6)), c.getString(7)));
        }
        return iiArray.toArray(new ItemInfo[iiArray.size()]);
    }

    public void removeLocalSong (byte[] databaseId)
    {
        Log.i(T, "remove local song with givin database id");
        getWritable().execSQL("DELETE FROM "+SONGS_TABLE+" WHERE "+SONG_SERVER_DATABASE_ID
                +" = "+Utils.bytesToInt(databaseId)+";");
    }
}
