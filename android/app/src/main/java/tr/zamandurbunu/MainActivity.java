package tr.zamandurbunu;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;

// Sayfayı uygulamanın içinden güvenli (https) bir adresle açar; böylece kamera çalışır.
public class MainActivity extends Activity {
    private static final String ADRES = "https://appassets.androidplatform.net/assets/index.html";
    private WebView web;
    private PermissionRequest bekleyenIzin;
    private ValueCallback<Uri[]> dosyaGeri;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        final WebViewAssetLoader yukleyici = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return yukleyici.shouldInterceptRequest(r.getUrl());
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest r) {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    r.grant(r.getResources());
                } else {
                    bekleyenIzin = r;
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> geri, FileChooserParams p) {
                if (dosyaGeri != null) dosyaGeri.onReceiveValue(null);
                dosyaGeri = geri;
                try {
                    startActivityForResult(p.createIntent(), 2);
                } catch (Exception e) {
                    dosyaGeri = null;
                    return false;
                }
                return true;
            }
        });
        if (b == null) web.loadUrl(ADRES); else web.restoreState(b);
    }

    @Override
    public void onRequestPermissionsResult(int kod, String[] izinler, int[] sonuc) {
        if (kod == 1 && bekleyenIzin != null) {
            if (sonuc.length > 0 && sonuc[0] == PackageManager.PERMISSION_GRANTED) bekleyenIzin.grant(bekleyenIzin.getResources());
            else bekleyenIzin.deny();
            bekleyenIzin = null;
        }
    }

    @Override
    protected void onActivityResult(int kod, int sonuc, Intent veri) {
        if (kod == 2 && dosyaGeri != null) {
            dosyaGeri.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(sonuc, veri));
            dosyaGeri = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle b) { super.onSaveInstanceState(b); web.saveState(b); }

    @Override
    public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
