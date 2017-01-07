package err.sfp.Database;

import android.provider.BaseColumns;

import err.sfp.Utils;

/**
 * Created by root on 17-1-6.
 */

public interface DBContract extends BaseColumns {
    String CREATE_SONGS_TABLE = "";
    String CREATE_PEERS_TABLE = "";
    String DELETE_PEERS_TABLE = "";
    String DELETE_SONGS_TABLE = "";

    int DATABASE_VERSION = 1;
    String DATABASE_NAME = Utils.getUserUniqueId().replace("\\W", "");

    String SONGS_TABLE = "songtable";
    String SONG_SERVER_DATABASE_ID = "SSDI";
    String SONG_TITLE = "SONGTITLE";
    String SONG_MESSAGE = "SONG_MESSAGE";
    String SONG_IMAGE = "SONG_IMAGE";
    String SONG_DATE = "SONG_DATE";
    String SONG_SENDER_ID = "SONG_SENDER_ID";
    String SONG_SENDER_NAME = "SONG_SENDER_ID";
    String SONG_URL = "SONG_URL";

    String PEERS_TABLE = "peerstable";
    String PEER_NAME =  "PEER_NAME";
    String PEER_EMAIL = "PEER_EMAIL";
    String PEER_ID = "PEER_ID";
    String PEER_IMAGE = "PEER_IMAGE";
    String PEER = "peer";
}
