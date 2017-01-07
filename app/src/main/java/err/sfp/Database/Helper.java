package err.sfp.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Err on 17-1-6.
 */

public class Helper extends SQLiteOpenHelper implements DBContract {

    public Helper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_PEERS_TABLE);
        sqLiteDatabase.execSQL(CREATE_SONGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(DELETE_PEERS_TABLE);
        sqLiteDatabase.execSQL(DELETE_SONGS_TABLE);
        onCreate(sqLiteDatabase);
    }
}
