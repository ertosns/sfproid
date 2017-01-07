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
    SharedPreferences.Editor pref = null;
    ProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle saveBundleInstance) {
        super.onCreate(saveBundleInstance);
        setContentView(R.layout.login);
        ButterKnife.bind(this);
        progressDialog = new ProgressDialog(Login.this);
        sharedPreferences = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);
        pref = sharedPreferences.edit();
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
                    Log.i(T, "not a valid pass");
                    badPass();
                    return;
                }

                String inputEmail = String.valueOf(email.getText());
                if(inputEmail == null) badEmail();
                boolean validEmail = Utils.validateEmail(inputEmail);
                if(!validEmail) {
                    Log.i(T, "not a valid email");
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

    public int login(String url) {
        HttpConThread hct = new HttpConThread(url);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Logging...");
        progressDialog.show();
        hct.start();
        //TODO SET STYLE
        synchronized (hct) {
            try {
                hct.wait();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        progressDialog.dismiss();

        int code = hct.code;
        if(code == SUCCESS_CODE) {
            pref.putString(UNIQUE_ID,  hct.getResponseString());
            pref.putBoolean(SIGNED, true);
            pref.commit();
            Log.i(T, "unique id read");
            Main.update();
            Utils.mainActivityIntent(Login.this);
        }
        return code;
    }

    public String getLoginUrl(String pass, String email) {
        return new StringBuilder("login=true&")
                .append(Utils.base64Url("pass")).append("=")
                .append(Utils.base64(pass)).append("&")
                .append(Utils.base64Url("email")).append("=")
                .append(Utils.base64(email)).toString();
    }
}
