package err.sfp.Database;

import android.provider.BaseColumns;

import err.sfp.Utils;

/**
 * Created by Err on 17-1-6.
 */

public class DBContract implements BaseColumns {

    public static String SONGS_TABLE = "songtable";
    public static String SONG_IMAGE = "SONG_IMAGE";
    public static String SONG_TITLE = "SONGTITLE";
    public static String SONG_SENDER_NAME = "SONG_SENDER_NAME";
    public static String SONG_MESSAGE = "SONG_MESSAGE";
    public static String SONG_DATE = "SONG_DATE";
    public static String SONG_SERVER_DATABASE_ID = "SSDI";
    public static String SONG_SENDER_ID = "SONG_SENDER_ID";
    public static String SONG_URL = "SONG_URL";

    public static String PEERS_TABLE = "peerstable";
    public static String PEER_IMAGE = "PEER_IMAGE";
    public static String PEER_NAME =  "PEER_NAME";
    public static String PEER_EMAIL = "PEER_EMAIL";
    public static String PEER_SENDER_ID = "PEER_ID";
    public static String PEER = "peer";

    public static String CREATE_SONGS_TABLE = new StringBuilder("CREATE TABLE ")
            .append(SONGS_TABLE).append(" ( ")
            .append(SONG_IMAGE).append(" BLOB, ")
            .append(SONG_TITLE).append(" TEXT, ")
            .append(SONG_SENDER_NAME).append(" TEXT, ")
            .append(SONG_MESSAGE).append(" TEXT, ")
            .append(SONG_DATE).append(" INT, ")
            .append(SONG_SERVER_DATABASE_ID).append(" INT, ")
            .append(SONG_SENDER_ID).append(" Text, ")
            .append(SONG_URL).append(" TEXT);").toString();

    public static String CREATE_PEERS_TABLE = new StringBuilder("CREATE TABLE ")
            .append(PEERS_TABLE).append(" ( ")
            .append(PEER_IMAGE).append(" BLOB, ")
            .append(PEER_NAME).append(" TEXT, ")
            .append(PEER_EMAIL).append(" TEXT, ")
            .append(PEER_SENDER_ID).append(" Text, ")
            .append(PEER).append(" BOOLEAN DEFAULT 0);").toString();

    public static String DELETE_PEERS_TABLE = "DROP TABLE IF EXISTS "+PEERS_TABLE;
    public static String DELETE_SONGS_TABLE = "DROP TABLE IF EXISTS "+SONGS_TABLE;
}
