package com.nekoplayer.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_AUDIO = 41;
    private LinearLayout root;
    private TextView trackTitle, statusText, playButton;
    private MediaPlayer player;
    private Uri selectedAudio;
    private boolean isPlaying = false;
    private final ExecutorService updateExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private File pendingUpdateApk;
    private boolean openingInstallSettings;

    private static final String LATEST_RELEASE_URL =
        "https://api.github.com/repos/Cbhouse/Neko-Player/releases/latest";

    private final int bg = Color.rgb(249, 245, 255);
    private final int purple = Color.rgb(125, 88, 190);
    private final int ink = Color.rgb(48, 35, 68);

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildHome();
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER); t.setPadding(10, 8, 10, 8);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this); b.setText(value); b.setTextColor(ink);
        b.setAllCaps(false); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        return b;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(22, 14, 22, 18); l.setBackgroundColor(bg); return l;
    }

    private void buildHome() {
        root = column(); root.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = text("Neko Player  ♡", 29, ink); title.setPadding(8, 18, 8, 4); root.addView(title);
        root.addView(text("Tu música, en compañía de gatitos", 14, purple));
        TextView cat = text("ฅ^•ﻌ•^ฅ", 56, purple); cat.setPadding(8, 25, 8, 18); root.addView(cat);
        root.addView(text("Un reproductor sencillo y sin distracciones", 15, ink));

        Button choose = button("♫  Elegir archivo de audio");
        choose.setOnClickListener(v -> pickAudio()); root.addView(choose, fullWidth());

        trackTitle = text("Todavía no has elegido una canción", 16, ink); trackTitle.setPadding(8, 18, 8, 4); root.addView(trackTitle);
        statusText = text("Elige un archivo de audio de tu dispositivo para comenzar.", 13, purple); root.addView(statusText);
        SeekBar seek = new SeekBar(this); seek.setMax(100); seek.setProgress(0); seek.setEnabled(false); root.addView(seek, fullWidth());

        LinearLayout controls = new LinearLayout(this); controls.setGravity(Gravity.CENTER); controls.setOrientation(LinearLayout.HORIZONTAL);
        Button stop = button("■"); stop.setOnClickListener(v -> stopAudio()); controls.addView(stop);
        playButton = text("▶", 24, Color.WHITE); playButton.setBackgroundTintList(null); playButton.setBackgroundColor(purple);
        playButton.setGravity(Gravity.CENTER); playButton.setPadding(25, 12, 25, 12); playButton.setOnClickListener(v -> togglePlayback()); controls.addView(playButton);
        root.addView(controls);

        TextView note = text("YouTube y Spotify · preparación futura", 14, ink); note.setPadding(8, 25, 8, 0); root.addView(note);
        Button updates = button("⚙  Actualizaciones y versión"); updates.setOnClickListener(v -> showUpdates()); root.addView(updates, fullWidth());
        root.addView(text("Versión 0.1.0 · base inicial", 12, purple));
        setContentView(root);
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private void pickAudio() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("audio/*");
        startActivityForResult(intent, PICK_AUDIO);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedAudio = data.getData();
            if (player != null) { player.release(); player = null; }
            try {
                player = new MediaPlayer();
                player.setDataSource(this, selectedAudio);
                player.setOnPreparedListener(mp -> {
                    trackTitle.setText(selectedAudio.getLastPathSegment() == null ? "Audio seleccionado" : selectedAudio.getLastPathSegment());
                    statusText.setText("Listo para reproducir"); mp.start(); isPlaying = true; playButton.setText("Ⅱ");
                });
                player.setOnCompletionListener(mp -> { isPlaying = false; playButton.setText("▶"); statusText.setText("Reproducción terminada"); });
                player.prepareAsync(); statusText.setText("Cargando audio…");
            } catch (IOException e) {
                statusText.setText("No se pudo abrir ese archivo de audio.");
                Toast.makeText(this, "Prueba con otro archivo de audio", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void togglePlayback() {
        if (player == null) { pickAudio(); return; }
        if (isPlaying) { player.pause(); isPlaying = false; playButton.setText("▶"); statusText.setText("En pausa"); }
        else { player.start(); isPlaying = true; playButton.setText("Ⅱ"); statusText.setText("Reproduciendo"); }
    }

    private void stopAudio() {
        if (player != null) { player.pause(); player.seekTo(0); isPlaying = false; playButton.setText("▶"); statusText.setText("Detenido"); }
    }

    private void showUpdates() {
        final android.app.AlertDialog progress = new android.app.AlertDialog.Builder(this)
            .setTitle("Buscando actualizaciones")
            .setMessage("Conectando con las versiones publicadas de Neko Player…")
            .setCancelable(false)
            .create();
        progress.show();

        updateExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(LATEST_RELEASE_URL).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "Neko-Player-Android");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                int response = connection.getResponseCode();
                if (response == HttpURLConnection.HTTP_NOT_FOUND) {
                    throw new IOException("Todavía no hay una actualización publicada. Primero hay que publicar la versión inicial desde GitHub Actions.");
                }
                if (response != HttpURLConnection.HTTP_OK) {
                    throw new IOException("GitHub respondió con el código " + response);
                }

                String json = readText(connection.getInputStream());
                JSONObject release = new JSONObject(json);
                String latestVersion = release.optString("tag_name", "").replaceFirst("^[vV]", "");
                String apkUrl = findApkUrl(release.optJSONArray("assets"));
                if (latestVersion.isEmpty() || apkUrl.isEmpty()) {
                    throw new IOException("La publicación no contiene una versión o APK descargable.");
                }

                String currentVersion = getCurrentVersionName();
                int comparison = compareVersions(latestVersion, currentVersion);
                mainHandler.post(() -> {
                    if (isFinishing()) return;
                    progress.dismiss();
                    if (comparison <= 0) {
                        new android.app.AlertDialog.Builder(this)
                            .setTitle("Neko Player está actualizado")
                            .setMessage("Tienes la versión " + currentVersion + ".")
                            .setPositiveButton("Entendido", null)
                            .show();
                    } else {
                        new android.app.AlertDialog.Builder(this)
                            .setTitle("Nueva versión disponible")
                            .setMessage("Versión " + latestVersion + " lista para descargar.")
                            .setNegativeButton("Ahora no", null)
                            .setPositiveButton("Descargar", (dialog, which) -> downloadUpdate(apkUrl, latestVersion))
                            .show();
                    }
                });
            } catch (Exception e) {
                String message = e.getMessage() == null ? "Comprueba tu conexión e inténtalo de nuevo." : e.getMessage();
                mainHandler.post(() -> {
                    if (isFinishing()) return;
                    progress.dismiss();
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("No se pudo buscar")
                        .setMessage("No pude consultar las actualizaciones. " + message)
                        .setPositiveButton("Entendido", null)
                        .show();
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private String readText(InputStream input) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) content.append(line);
        }
        return content.toString();
    }

    private String findApkUrl(JSONArray assets) {
        if (assets == null) return "";
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.optJSONObject(i);
            if (asset != null && "app-release.apk".equals(asset.optString("name"))) {
                String url = asset.optString("browser_download_url", "");
                return url.startsWith("https://") ? url : "";
            }
        }
        return "";
    }

    private String getCurrentVersionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName == null ? "0.1.0" : info.versionName;
        } catch (Exception ignored) {
            return "0.1.0";
        }
    }

    private int compareVersions(String first, String second) {
        String[] a = first.split("\\.");
        String[] b = second.split("\\.");
        int length = Math.max(a.length, b.length);
        for (int i = 0; i < length; i++) {
            int x = i < a.length ? leadingNumber(a[i]) : 0;
            int y = i < b.length ? leadingNumber(b[i]) : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }

    private int leadingNumber(String part) {
        String digits = part.replaceFirst("^([^0-9]*)([0-9]+).*$", "$2");
        try { return Integer.parseInt(digits); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private void downloadUpdate(String apkUrl, String version) {
        final android.app.AlertDialog progress = new android.app.AlertDialog.Builder(this)
            .setTitle("Descargando actualización")
            .setMessage("Preparando la versión " + version + "…")
            .setCancelable(false)
            .create();
        progress.show();

        updateExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(apkUrl).openConnection();
                connection.setRequestProperty("User-Agent", "Neko-Player-Android");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(30000);
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("No se pudo descargar el APK.");
                }

                File apk = new File(getCacheDir(), "NekoPlayer-update.apk");
                try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(apk)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                    output.getFD().sync();
                }

                mainHandler.post(() -> {
                    if (isFinishing()) return;
                    progress.dismiss();
                    pendingUpdateApk = apk;
                    installDownloadedUpdate();
                });
            } catch (Exception e) {
                String message = e.getMessage() == null ? "Inténtalo de nuevo más tarde." : e.getMessage();
                mainHandler.post(() -> {
                    if (isFinishing()) return;
                    progress.dismiss();
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("No se pudo descargar")
                        .setMessage(message)
                        .setPositiveButton("Entendido", null)
                        .show();
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void installDownloadedUpdate() {
        if (pendingUpdateApk == null || !pendingUpdateApk.exists()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            openingInstallSettings = true;
            new android.app.AlertDialog.Builder(this)
                .setTitle("Permite la instalación")
                .setMessage("Android necesita que autorices a Neko Player a iniciar la instalación de su actualización.")
                .setNegativeButton("Cancelar", (dialog, which) -> pendingUpdateApk = null)
                .setPositiveButton("Abrir ajustes", (dialog, which) -> {
                    Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName()));
                    startActivity(settings);
                })
                .show();
            return;
        }

        try {
            Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pendingUpdateApk);
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(install);
            pendingUpdateApk = null;
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir el instalador de Android.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (openingInstallSettings) {
            openingInstallSettings = false;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || getPackageManager().canRequestPackageInstalls()) {
                installDownloadedUpdate();
            }
        }
    }

    @Override protected void onDestroy() {
        updateExecutor.shutdownNow();
        if (player != null) { player.release(); player = null; }
        super.onDestroy();
    }
}
