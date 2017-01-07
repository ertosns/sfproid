package err.sfp.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import err.sfp.SocialNetworking.ItemInfo;
import err.sfp.Utils;

/**
 * Created by Err on 17-1-3.
 */

public class Database implements DBContract {
    Context context;

    public Database(Context c) {
        this.context = c;
    }

    Helper helper = new Helper(context);
    SQLiteDatabase read = helper.getReadableDatabase();
    SQLiteDatabase write = helper.getWritableDatabase();

    public void insertSongsItemsInfo(ItemInfo[] ii) {
        ContentValues cv = new ContentValues();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (int i = 0; i < ii.length; i++) {
            ii[i].image.compress(Bitmap.CompressFormat.PNG, 100, stream);
            cv.put(SONG_IMAGE, stream.toByteArray());
            cv.put(SONG_TITLE, ii[i].subTitle);
            cv.put(SONG_SENDER_NAME, ii[i].subTitle);
            cv.put(SONG_MESSAGE, ii[i].message);
            cv.put(SONG_DATE, ii[i].dateMillis);
            cv.put(SONG_SERVER_DATABASE_ID, Utils.bytesToInt(ii[i].databaseId));
            cv.put(SONG_SENDER_ID, ii[i].senderId);
            cv.put(SONG_URL, ii[i].songUrl);

            write.insert(SONGS_TABLE, null, cv);
        }
    }

    public void insertRequestsItemsInfo(ItemInfo[] ii) {
        ContentValues cv = new ContentValues();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (int i = 0; i < ii.length; i++) {
            ii[i].image.compress(Bitmap.CompressFormat.PNG, 100, stream);
            cv.put(PEER_IMAGE, stream.toByteArray());
            cv.put(PEER_NAME, ii[i].title);
            cv.put(PEER_EMAIL, ii[i].title);
            cv.put(PEER_ID, ii[i].senderId);
            //peer has defult 0(false)

            write.insert(PEERS_TABLE, null, cv);
        }
    }

    public ItemInfo[] getSongsInfo() {
        ArrayList<ItemInfo> iiArray = new ArrayList<>();
        String[] columns = {SONG_IMAGE , SONG_TITLE, SONG_SENDER_NAME, SONG_MESSAGE
                , SONG_DATE, SONG_SERVER_DATABASE_ID, SONG_SENDER_ID, SONG_URL};
        Cursor c = read.query(SONGS_TABLE, columns, null, null, null, null, null);
        while(c.moveToNext()) {
            byte[] imageBytes = c.getBlob(0);
            iiArray.add(new ItemInfo(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length),
                    c.getString(1), c.getString(2), c.getString(3), c.getLong(4),
                    Utils.intToBytes(c.getInt(5)), c.getString(6).getBytes(), c.getString(7)));
        }
        return (ItemInfo[]) iiArray.toArray();
    }

    public ItemInfo[] getPeersShip(boolean peers) {
        ArrayList<ItemInfo> iiArray = new ArrayList<>();
        String[] columns = {PEER_IMAGE, PEER_NAME, PEER_EMAIL, PEER_ID};
        String selection = PEER + " = ? ";
        String[] args = {(peers?"1":"0")};
        Cursor c = read.query(PEERS_TABLE, columns, selection, args, null, null, null);
        while(c.moveToNext()) {
            byte[] imageBytes = c.getBlob(0);
            iiArray.add(new ItemInfo(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length),
                    c.getString(1), c.getString(2), null, 0, new byte[0], c.getString(3).getBytes(), null));
        }
        return (ItemInfo[]) iiArray.toArray();
    }

    public void removeLocalSong (byte[] idBytes) {
        String selection = SONG_SERVER_DATABASE_ID + " LIKE ? ";
        String[] args =  {Utils.bytesToInt(idBytes)+""};
        write.delete(SONGS_TABLE, selection, args);
    }

    public void removePeershipRequest(byte[] senderId) {
        String selection = PEER_ID + " LIKE ? ";
        String[] args = {new String(senderId)};
        write.delete(PEERS_TABLE, selection, args);
    }

    public void markAsPeer(byte[] senderId) {
        ContentValues cv = new ContentValues();
        cv.put(PEER, "1");
        String whereClause = PEER_ID + " = ? ";
        String[] args = {new String(senderId)};
        write.update(PEERS_TABLE, cv, whereClause, args);
    }

    public boolean hasTable(String table) {
        Cursor c = read.rawQuery("select name from sqlite_master where name = '"+table+"';", null);
        if(c.moveToNext()) return true;
        return false;
    }
}
