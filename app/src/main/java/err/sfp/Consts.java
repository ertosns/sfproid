package err.sfp;

/**
 * Created by Err on 16-12-7.
 */

public interface Consts {

    String T = "("+BuildConfig.VERSION_CODE+")##SFP##: ";
    String APP_PREFERENCES = "app_pref";
    String APP_PREFERENCES_LISTENER = "APPPREFLIST";
    String PROTOCOL ="http://";
    String HOST = "192.168.1.3";
    String PATH = ":8080/sfp/getsong?";
    String ONLINE = "online";
    int SOCK_PORT = 65123;
    int USER_ALREADY_FOUND_CODE = 201;
    int BAD_AUTH_CODE = 202;
    int CONNECTIVITY_ERR_CODE = 203;
    int SERVER_ERR_CODE = 500;
    int SUCCESS_CODE = 200;
    int RW_PERMISSION = 19;
    String UNIQUE_ID = "unique_id";
    String W8_INTERVAL = "w8interval";
    String SONG_PREFIX = "SFP_";
    String LISTENING = "listening";
    String SIGNED = "signed";
    String DOWNLOADS = "downloads";
    String PREFERENCE_TYPE = "preftype";
    int STOP_FLAG = 10;
    int UNIQUE_ID_FLAG = 11;
    int ONLINE_FLAG = 12;
    String SHAREDPREFERENCES_ACTION_FILTER = "err.sfp.lpr";
    String LISTENER_MESSAGES_INTNET_FILTER = "lmif";
    String LISTENER_FLAG_TYPE = "lft";
    int LISTENER_INTEGER_FLAG = 1;
    int LISTENER_STRING_FLAG = 2;
    String LISTENER_INTEGER_FLAG_KEY = "lifk";
    String LISTENER_MESSAGE = "lm";

    String STOP_PREF = "stopPref";
    String DEFULT_ID = "defId";
    byte[] DOWNLOAD_SUCCESSED = {1};
    byte[] DOWNLOAD_FAILED = {0};
    String YOUTUBE_CLIENT_ID = "AIzaSyA-B96SVHNxg1VgQzrt9glANGo_W91b_rs";
    int NUMBER_OF_VIDEOS_RETURNED = 25;
    String ADD_SONG = "aS";
    String REMOVE_SONG = "rS";
    String PROPERTIES_ACTION = "pA";
    String SONG_NAME = "sn";
    String ISO_8859_1 = "ISO-8859-1";
    String UTF8 = "UTF-8";
    int TRUE = 1;
    int FALSE = 0;
    String PEER_REQUEST_SENDER_ID = "RI";
    String SHARED_SONG_INDEX = "SSI";
    String PEERSHIP_REQUEST_ACCPTED = "ra";
    String DOWNLOAD_SONG = "ds";
    String SONG_DATABASE_ID = "sdi";
    String SONG_ID = "SI";
    String TITLE = "ti";
    String SONG_INFO_ACTIVITY_BITMAP = "siab";
    String SONG_INFO_ACTIVITY_TITLE = "siat";
    String SONG_INFO_ACTIVITY_URL = "siau";
    String NEW_DATABASE = "dbi";

    //server query parameters
    String ACCEPTED_REQUESTS= "getacceptedrequests";
    String UNRESPONDED_REQUESTS = "listofunrespondedrequests";
    String NEW_REQUESTS = "listofnewrequests";
    String NEW_SONGS = "listofnewsharedsongs";
    String UNRESPONDED_SONGS = "listofsharedsongs";
    String PEERS_REQUESTS = "listofpeersinfo";
    String NON_NEW_REQUESTS = "listofnonnewrequests";
    String REQUEST_ACTION = "qa";
    String SONG_ACTION = "sa";


}
