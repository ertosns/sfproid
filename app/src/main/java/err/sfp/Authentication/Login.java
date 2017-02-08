package err.sfp.Authentication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import err.sfp.Consts;
import err.sfp.Database.Database;
import err.sfp.Database.Helper;
import err.sfp.HttpConThread;
import err.sfp.Main;
import err.sfp.R;
import err.sfp.Utils;

/**
 * Created by Err on 16-12-7.
 */

public class Login extends AppCompatActivity implements Consts {

    @BindView(R.id.login_pass) EditText password;
    @BindView(R.id.login_email) EditText email;
    @BindView(R.id.login_btn) Button login;
    @BindView(R.id.signup) TextView signup;
    //@BindView(R.id.login_err) TextView err;
    SharedPreferences sharedPreferences = null;
    SharedPreferences.Editor edit = null;
    ProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle saveBundleInstance) {
        super.onCreate(saveBundleInstance);
        setContentView(R.layout.login);
        ButterKnife.bind(this);
        progressDialog = new ProgressDialog(Login.this);
        sharedPreferences = Main.sharedPreferences;
        edit = sharedPreferences.edit();
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!Utils.isOnline(Login.this)) {
                    Log.i(T, "no connectivity");
                    login.setError(getString(R.string.NO_CONNECTIVITY));
                    return;
                }

                String inputPassword = String.valueOf(password.getText());
                if(inputPassword == null) badPass();
                boolean validPass = Utils.validatePass(inputPassword);
                if(!validPass) {
                    Log.i(T, "not a valid pass "+inputPassword);
                    badPass();
                    return;
                }

                String inputEmail = String.valueOf(email.getText());
                if(inputEmail == null) badEmail();
                boolean validEmail = Utils.validateEmail(inputEmail);
                if(!validEmail) {
                    Log.i(T, "not a valid email "+inputEmail);
                    badEmail();
                    return;
                }
                String url = new StringBuilder(PROTOCOL).append(HOST).append(PATH).
                        append(getLoginUrl(inputPassword, inputEmail)).toString();
                Log.i(T, new StringBuilder("login url:").append(url).toString());
                int resCode = login(url);
                if (resCode == CONNECTIVITY_ERR_CODE) {
                    login.setError(getString(R.string.SERVER_ERROR));
                } else if (resCode == BAD_AUTH_CODE) {
                    login.setError(getString(R.string.USER_NOT_FOUND));
                } else if (resCode == CONNECTIVITY_ERR_CODE) {
                    login.setError(getString(R.string.CONNECTIVITY_ERROR));
                }
            }
        });

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Login.this, Signup.class);
                //TODO add flags
                startActivity(intent);
            }
        });
    }

    public void badPass() {
        password.setError(getString(R.string.BAD_PASS));
    }

    public void badEmail() {
        email.setError(getString(R.string.BAD_EMAIL));
    }

    public int login(String url)
    {
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Logging...");
        progressDialog.show();

        HttpConThread hct = new HttpConThread(url);
        synchronized (hct)
        {
            try
            {
                hct.wait();
            }
            catch (InterruptedException ie)
            {
                ie.printStackTrace();
            }
        }

        int code = hct.code;
        progressDialog.dismiss();
        if (code == SUCCESS_CODE)
        {
            // used in Database Helper class, should be set before initDatabase();
            String uniqueId = hct.getResponseString();
            if(uniqueId != null && !uniqueId.equals(""))
            {
                Log.i(T, "previous uniqueId "+Main.sharedPreferences.getString(UNIQUE_ID, null)
                        +"\ncurrent uniqueId "+uniqueId);
                Main.edit.putString(UNIQUE_ID, uniqueId);
                Main.edit.putBoolean(SIGNED, true);
                Main.edit.commit();
                Log.i(T, "unique id read");
                Utils.updateMainAndUtils();
                Utils.initPanel();
                if (!uniqueId.equals(Main.sharedPreferences.getString(UNIQUE_ID, "")))
                {
                    // change helper of Database, new helper for client database.
                    Utils.getRequestsFromServer(NON_NEW_REQUESTS, null, false, false, null);
                    Utils.getSongsFromServer(UNRESPONDED_SONGS, null, false, null);
                }
                Utils.mainActivityIntent(Login.this);
            }
        }
        return code;
    }

    public String getLoginUrl(String pass, String email)
    {
        return new StringBuilder("login=true&mobile=true&")
                .append(Utils.base64Url("pass")).append("=")
                .append(Utils.base64Url(pass)).append("&")
                .append(Utils.base64Url("email")).append("=")
                .append(Utils.base64Url(email)).toString();
    }
}
