package app.cike.mobile;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.util.Base64;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView webView;
    private WebViewAssetLoader assetLoader;
    private static final String KEY_ALIAS = "cike_api_secret_key_v1";
    private static final String PREFS = "cike_secure_values";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(247,246,242));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(247,246,242));
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        createNotificationChannel();
        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/web/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.addJavascriptInterface(new NativeBridge(this), "CikeNative");
        webView.setWebViewClient(new WebViewClient() {
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !"appassets.androidplatform.net".equals(request.getUrl().getHost());
            }
        });
        setContentView(webView);
        webView.loadUrl("https://appassets.androidplatform.net/assets/web/index.html");
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel("cike_reminders", "任务提醒", android.app.NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("你主动设置的任务提醒");
            getSystemService(android.app.NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private static SecretKey secretKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if (store.containsAlias(KEY_ALIAS)) return (SecretKey) store.getKey(KEY_ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build());
        return generator.generateKey();
    }

    private static class NativeBridge {
        private final MainActivity activity;
        private final CikeDatabase database;
        NativeBridge(MainActivity activity) { this.activity = activity; this.database = new CikeDatabase(activity); }
        @JavascriptInterface public String loadState() {
            try { return database.readState(); } catch (Exception ex) { return ""; }
        }
        @JavascriptInterface public boolean saveState(String payload) {
            try { return database.writeState(payload); } catch (Exception ex) { return false; }
        }
        @JavascriptInterface public String getSecret() {
            try {
                String encoded = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("api_key", null);
                if (encoded == null) return "";
                byte[] packed = Base64.decode(encoded, Base64.NO_WRAP); ByteBuffer b = ByteBuffer.wrap(packed);
                byte[] iv = new byte[b.getInt()]; b.get(iv); byte[] ciphertext = new byte[b.remaining()]; b.get(ciphertext);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(128, iv));
                return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception ex) { return ""; }
        }
        @JavascriptInterface public boolean setSecret(String value) {
            try {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, secretKey());
                byte[] iv = cipher.getIV(), encrypted = cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                ByteBuffer b = ByteBuffer.allocate(4 + iv.length + encrypted.length); b.putInt(iv.length).put(iv).put(encrypted);
                return activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("api_key", Base64.encodeToString(b.array(), Base64.NO_WRAP)).commit();
            } catch (Exception ex) { return false; }
        }
        @JavascriptInterface public void requestNotificationPermission() {
            activity.runOnUiThread(() -> { if (Build.VERSION.SDK_INT >= 33 && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 501); });
        }
        @JavascriptInterface public void scheduleNotification(String id, String title, long atMillis) {
            if (atMillis <= System.currentTimeMillis()) return;
            Intent intent = new Intent(activity, ReminderReceiver.class).putExtra("title", title).putExtra("id", id);
            PendingIntent pending = PendingIntent.getBroadcast(activity, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager alarms = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending);
        }
        @JavascriptInterface public void cancelNotification(String id) {
            Intent intent = new Intent(activity, ReminderReceiver.class);
            PendingIntent pending = PendingIntent.getBroadcast(activity, id.hashCode(), intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pending != null) { ((AlarmManager) activity.getSystemService(Context.ALARM_SERVICE)).cancel(pending); pending.cancel(); }
        }
    }
}
