package com.akikosoft.securechat;

import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.akikosoft.securechat.util.RESTUtil;
import com.akikosoft.securechat.util.RSAUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEdit;
    private EditText passwordEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final SecureChatApplication app = (SecureChatApplication) getApplication();
        emailEdit = (EditText) findViewById(R.id.email);
        passwordEdit = (EditText) findViewById(R.id.password);

        Button login = (Button) findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // validate
                final String email = emailEdit.getText().toString();
                final String password = passwordEdit.getText().toString();
                if (TextUtils.isEmpty(email)) {
                    emailEdit.setError("Email needed!");
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    passwordEdit.setError("Password needed!");
                    return;
                }
                // POST
                String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
                RESTUtil.securePOST("login", body, new Callback() {
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            final JSONObject body = new JSONObject(RESTUtil.getPlainBody(response.body().string()));
                            if (response.code() == 200) {
                                app.userEmail = email;
                                app.userPassword = password;
                                app.userPrivateKey = RSAUtil.getPrivateFromPEM(body.getString("privateKey"));
                                finish();
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                                        builder.setTitle(R.string.login_failed);
                                        builder.setMessage(body.optString("code"));
                                        builder.setNeutralButton(R.string.ok, null);
                                        builder.show();
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(LoginActivity.this, R.string.network_error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });

        Button register = (Button) findViewById(R.id.register);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

}
