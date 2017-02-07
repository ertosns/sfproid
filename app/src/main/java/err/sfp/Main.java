package err.sfp;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.Random;
import err.sfp.Database.Database;

/**
 * Created by Err on 17-1-3.
 */

public class Main extends Application {

    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor edit ;
    public static Database database;
    public static Random random;
    public static boolean logout;
    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences(Consts.APP_PREFERENCES, MODE_PRIVATE);
        edit = sharedPreferences.edit();
        context = getApplicationContext();
        random = new Random();
        logout = true;
    }

}
