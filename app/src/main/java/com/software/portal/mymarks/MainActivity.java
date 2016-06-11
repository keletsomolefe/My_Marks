package com.software.portal.mymarks;

import android.content.Intent;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private WebView mWeb;
    private String url = "https://upnet.up.ac.za/psp/pscsmpra/EMPLOYEE/HRMS/c/UP_SS_MENU.UP_SS_STUDENT.GBL";
    private String pwd;
    private String userid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();

        pwd = intent.getStringExtra("pwd");
        userid = intent.getStringExtra("userid");

        final String js = "javascript:" +
                "document.getElementById('pwd').value = '" + pwd + "';"       +
                "document.getElementById('userid').value = '" + userid + "';" +
                "document.getElementById('login').submit();";

        mWeb = (WebView) findViewById(R.id.webView);
        mWeb.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (view.getTitle().equals("Oracle PeopleSoft Sign-in")) {
                    //login page
                    if (Build.VERSION.SDK_INT >= 19) {
                        view.evaluateJavascript(js, new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String s) {

                            }
                        });
                    } else {
                        view.loadUrl(js);
                    }
                }
            }
        });

        WebSettings settings = mWeb.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setDisplayZoomControls(true);
        settings.setDomStorageEnabled(true);


        mWeb.loadUrl(url);


    }
}
