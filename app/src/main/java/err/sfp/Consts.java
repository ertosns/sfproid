package err.sfp;

/**
 * Created by Err on 16-12-7.
 */

public interface Consts {

    String T = "("+BuildConfig.VERSION_CODE+")##SFP##: ";
    String APP_PREFERENCES = "app_pref";
    String APP_PREFERENCES_LISTENER = "APPPREFLIST";
    String PROTOCOL ="http://";
    String HOST = "192.168.1.21";
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
    String STOP_PREF = "stopPref";
    String DEFULT_ID = "defId";
    byte[] DOWNLOAD_SUCCESSED = {1};
    byte[] DOWNLOAD_FAILED = {0};
    String YOUTUBE_CLIENT_ID = "AIzaSyA-B96SVHNxg1VgQzrt9glANGo_W91b_rs";
    int NUMBER_OF_VIDEOS_RETURNED = 25;

}
