package err.sfp.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import android.util.Log;

import err.sfp.Consts;
import err.sfp.Main;
import err.sfp.Utils;

/**
 * Created by Err on 17-1-6.
 */

public class Helper extends SQLiteOpenHelper {

    static int DATABASE_VERSION = 1;
    private static final String ID_SPLITER = ",";

    public Helper(Context context) {
        super(context, subtractNonce(Main.sharedPreferences.getString(Consts.UNIQUE_ID, null))
                , null, DATABASE_VERSION);
        Log.i(Consts.T, "SQLite helper init");
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(DBContract.CREATE_PEERS_TABLE);
        sqLiteDatabase.execSQL(DBContract.CREATE_SONGS_TABLE);
        Log.i(Consts.T, "database tables created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        sqLiteDatabase.execSQL(DBContract.DELETE_PEERS_TABLE);
        sqLiteDatabase.execSQL(DBContract.DELETE_SONGS_TABLE);
        onCreate(sqLiteDatabase);
        Log.i(Consts.T, "database tables upgraded");
    }

    public static String subtractNonce(String ui) {
        String[] id = new String(Base64.decode(ui, Base64.DEFAULT)).split(ID_SPLITER);
        return Base64.encodeToString(new String(id[0]+id[1]).getBytes(), Base64.DEFAULT);
    }
}
