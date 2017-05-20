package com.akikoz.securechat;

import android.app.ProgressDialog;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.akikoz.securechat.util.RESTUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailEdit;
    private EditText passwordEdit;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailEdit = (EditText) findViewById(R.id.email);
        passwordEdit = (EditText) findViewById(R.id.password);

        Button register = (Button) findViewById(R.id.register);
        register.setOnClickListener(new View.OnClickListener() {
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
                showProgressDialog();
                RESTUtil.securePOST("register", body, new Callback() {
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            final JSONObject body = new JSONObject(RESTUtil.getPlainBody(response.body().string()));
                            if (response.code() == 200) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dismissProgressDialog();
                                        Toast.makeText(RegisterActivity.this, R.string.register_succeeded, Toast.LENGTH_LONG).show();
                                    }
                                });
                                finish();
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dismissProgressDialog();
                                        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                                        builder.setTitle(R.string.register_failed);
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
                                dismissProgressDialog();
                                Toast.makeText(RegisterActivity.this, R.string.network_error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Registering");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}
