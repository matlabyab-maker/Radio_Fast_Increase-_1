package com.fast.radio;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

/** Opens station pages that are web-player pages rather than direct audio streams. */
public class WebStreamActivity extends Activity {
    private WebView web;
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        FrameLayout root=new FrameLayout(this);
        web=new WebView(this);
        root.addView(web,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        String url=getIntent().getStringExtra("url");
        if(url!=null) web.loadUrl(url);
    }
    @Override protected void onDestroy(){if(web!=null)web.destroy();super.onDestroy();}
}
