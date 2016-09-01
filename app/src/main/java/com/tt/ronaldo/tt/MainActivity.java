package com.tt.ronaldo.tt;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sensedia.rrn.tt.R;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.tls.OkHostnameVerifier;

import org.json.JSONObject;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URLEncoder;

import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity {

    public static final String USER = "user";
    public static final String PASS = "pass";
    public static final String ERROR_MESSAGE = "Error ao efetuar a marcação no TT";
    final String deviceInfoUrl =
        "https://tt.ciandt.com/.net/index.ashx/GetClockDeviceInfo?deviceID=2";
        //"https://demo7898229.mockable.io/GetClockDeviceInfo?deviceID=2";
    final String registerTime =
        "https://tt.ciandt.com/.net/index.ashx/SaveTimmingEvent?deviceID=2&eventType=1";
        //"https://demo7898229.mockable.io/SaveTimmingEvent?deviceID=2&eventType=1";
    public final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private AlertDialog msgDialog;
    private ProgressDialog loadingDialog;
    private OkHttpClient client = null;
    private Handler handler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getViewUser().setText(getValue(USER));
        getViewPass().setText(getValue(PASS));
        getMessageCheckView().setText(getValue("messageCheck"));
        if (getValue("messageCheck") != null && !"".equals(getValue("messageCheck"))) {
            getMessageCheckView().setVisibility(View.VISIBLE);
        } else {
            getMessageCheckView().setVisibility(View.GONE);
        }

        Button button = (Button) findViewById(R.id.btn_send);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    onclickTaks();
                }
            });
        }

        msgDialog = new AlertDialog.Builder(this).setTitle("Aviso")
            .setIcon(android.R.drawable.ic_dialog_info)
            .setNeutralButton("Fechar", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {}
            })
            .create();

    }

    private TextView getMessageCheckView() {
        return (TextView)findViewById(R.id.lastCheck);
    }

    private void onclickTaks() {
        EditText edit = getViewUser();
        final String user = edit.getText().toString();
        saveValue(USER, user);

        edit = getViewPass();
        final String pass = edit.getText().toString();
        saveValue(PASS, pass);

        if (!"".equals(user.trim()) && !"".equals(pass.trim())) {
            registerTimeTask(user, pass);
        }
    }

    private void registerTimeTask(final String user, final String pass) {

        loadingDialog = ProgressDialog.show(this, "Aguarde", "Processando...", true);

        new Thread(new Runnable() {
                @Override
                public void run() {
                    String resultMessage = "";

                    try {
                        OkHttpClient client = getUnsafeOkHttpClient();

                        Request request = new Request.Builder().url(deviceInfoUrl).build();
                        //cookie token call
                        Response response = client.newCall(request).execute();

                        if (response.code() < 300) {

                            RequestBody body = RequestBody.create(JSON, "");

                            request = new Request.Builder().url(registerTime
                                    + "&userName=" + URLEncoder.encode(user, "UTF-8")
                                    + "&password=" + URLEncoder.encode(pass, "UTF-8")
                            ).post(body).build();

                            //register time call
                            response = client.newCall(request).execute();

                            if (response.code() >= 300) {
                                throw new Exception(ERROR_MESSAGE);
                            }

                            JSONObject jsonObj = new JSONObject(response.body().string());
                            resultMessage = jsonObj.getJSONObject("msg").getString("msg");

                            final String messageCheck = "Your last check in/out here was on: \n"
                                    + SimpleDateFormat.getDateTimeInstance().format(new Date());
                            saveValue("messageCheck", messageCheck);

                            handler.post(new Runnable() {
                                public void run() {
                                    getMessageCheckView().setText(messageCheck);
                                    getMessageCheckView().setVisibility(View.VISIBLE);
                                }
                            });

                        } else {
                            throw new Exception(ERROR_MESSAGE);
                        }

                    } catch (Exception e) {

                        e.printStackTrace();
                        resultMessage = e.getMessage();

                    } finally {

                        loadingDialog.dismiss();
                        final String m = resultMessage;
                        runOnUiThread(new Runnable() {
                            public void run() {
                                msgDialog.setTitle("Message");
                                msgDialog.setMessage(m);
                                msgDialog.show();
                            }
                        });

                    }
                }
        }).start();

    }

    private EditText getViewPass() {
        return (EditText) findViewById(R.id.pass);
    }

    private EditText getViewUser() {
        return (EditText) findViewById(R.id.user);
    }

    private void saveValue(String key, String value) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private String getValue(String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getString(key, "");
    }

    private OkHttpClient getUnsafeOkHttpClient() {

        if (client == null) {

            try {
                // Create a trust manager that does not validate certificate chains
                final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
                };

                // Install the all-trusting trust manager
                final SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                // Create an ssl socket factory with our all-trusting manager
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                client = new OkHttpClient();
                client.setHostnameVerifier(OkHostnameVerifier.INSTANCE);
                client.setSslSocketFactory(sslSocketFactory);

                CookieManager cookieManager = new CookieManager();
                cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
                client.setCookieHandler(cookieManager);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return client;
    }
}
