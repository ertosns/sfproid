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
import err.sfp.Download;
import err.sfp.HttpConThread;
import err.sfp.R;
import err.sfp.Utils;

/**
 * Created by root on 16-12-7.
 */

public class Signup extends AppCompatActivity implements Consts{

    @BindView(R.id.signup_name) EditText name;
    @BindView(R.id.signup_pass) EditText password;
    @BindView(R.id.signup_email) EditText email;
    @BindView(R.id.signup_btn) Button signup;
    @BindView(R.id.login) TextView login;
    //@BindView(R.id.signup_err) TextView err;
    SharedPreferences sharedPreferences = null;
    ProgressDialog progressDialog = null;
    SharedPreferences.Editor pref = null;


    @Override
    protected void onCreate(Bundle saveBundleInstance) {
        super.onCreate(saveBundleInstance);
        setContentView(R.layout.signup);
        ButterKnife.bind(this);
        sharedPreferences = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);
        pref = sharedPreferences.edit();
        progressDialog = new ProgressDialog(Signup.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Signing up...");
        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO SET style

                if(!Utils.isOnline(Signup.this)) {
                    Log.i(T, "no connectivity");
                    signup.setError(getString(R.string.NO_CONNECTIVITY));
                    Log.i(T, "should return");
                    return;
                }
                
                String inputName = String.valueOf(name.getText());
                if(inputName == null) badName();
                boolean validName = Utils.validateWord(inputName);
                if(!validName) {
                    Log.i(T, "not a valid name");
                    badName();
                    return;
                }

                String inputPassword = String.valueOf(password.getText());
                if(inputPassword == null) badPass();
                boolean validPass = Utils.validatePass(inputPassword);
                if(!validPass) {
                    Log.i(T, "not a valid Pass");
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

                String url = new StringBuilder(PROTOCOL).append(HOST).append(PATH)
                        .append(getSignupUrl(inputName, inputPassword, inputEmail)).toString();
                Log.i(T, new StringBuilder("singup url:").append(url).toString());
                int resCode = signup(url);
                if (resCode == SERVER_ERR_CODE) {
                    signup.setError(getString(R.string.SERVER_ERROR));
                } else if (resCode == USER_ALREADY_FOUND_CODE) {
                    signup.setError(getString(R.string.USER_ALREADY_FOUND));
                } else if (resCode == CONNECTIVITY_ERR_CODE) {
                    signup.setError(getString(R.string.CONNECTIVITY_ERROR));
                }
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Signup.this, Login.class);
                //TODO set flags
                startActivity(intent);
            }
        });
    }

    public void badName(){
        name.setError(getString(R.string.BAD_NAME));
    }

    public void badPass() {
        password.setError(getString(R.string.BAD_PASS));
    }

    public void badEmail() {
        email.setError(getString(R.string.BAD_EMAIL));
    }

    public int signup(String url) {
        progressDialog.show();
        HttpConThread hct = new HttpConThread(url);
        hct.start();

        synchronized (hct) {
            try {
                hct.wait();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        int code = hct.code;
        if(code == SUCCESS_CODE) {
            pref.putString(UNIQUE_ID,  hct.getResponseString());
            pref.putBoolean(SIGNED, true);
            pref.commit();
            Log.i(T, "unique id read");
            Utils.mainActivityIntent(Signup.this);
        }
        progressDialog.dismiss();
        return code;
    }

    public String getSignupUrl(String name, String pass, String email) {
            return new StringBuilder("signup=true&")
                    .append(Utils.base64Url("name")).append("=")
                    .append(Utils.base64(name)).append("&")
                    .append(Utils.base64Url("pass")).append("=")
                    .append(Utils.base64(pass)).append("&")
                    .append(Utils.base64Url("email")).append("=")
                    .append(Utils.base64(email)).toString();
    }
}
