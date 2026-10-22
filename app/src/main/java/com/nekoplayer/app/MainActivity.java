package com.nekoplayer.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
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
    private static final int PICK_VIDEO = 42;
    private LinearLayout root;
    private LinearLayout body;
    private TextView trackTitle, statusText, playButton;
    private TextView elapsedText, durationText;
    private SeekBar progressBar;
    private MediaPlayer player;
    private VideoView videoView;
    private Uri selectedAudio;
    private Uri selectedVideo;
    private boolean isSeeking;
    private boolean audioPrepared;
    private boolean isPlaying = false;
    private final ExecutorService updateExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable playbackTicker = new Runnable() {
        @Override public void run() {
            if (player == null || !audioPrepared || progressBar == null) return;
            if (!isSeeking) {
                progressBar.setProgress(player.getCurrentPosition());
                if (elapsedText != null) elapsedText.setText(formatTime(player.getCurrentPosition()));
            }
            if (isPlaying) mainHandler.postDelayed(this, 500);
        }
    };
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

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private LinearLayout makeScreen() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(12), dp(20), dp(20));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(bottomNavigation());
        return root;
    }

    private LinearLayout bottomNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setBackground(cardShape(Color.WHITE, 0xFFE8DDF5, 0));
        String[] labels = {"♫\nInicio", "▶\nReproductor", "⚙\nAjustes"};
        Runnable[] actions = {this::buildHome, this::buildPlayer, this::buildSettings};
        for (int i = 0; i < labels.length; i++) {
            final Runnable action = actions[i];
            TextView item = new TextView(this);
            item.setText(labels[i]); item.setTextSize(12); item.setTextColor(purple);
            item.setGravity(Gravity.CENTER); item.setPadding(4, dp(10), 4, dp(10));
            nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
            item.setOnClickListener(v -> action.run());
        }
        return nav;
    }

    private TextView label(String value, int size, int color, int gravity) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setGravity(gravity);
        t.setPadding(dp(4), dp(5), dp(4), dp(5));
        return t;
    }

    private GradientDrawable cardShape(int fill, int stroke, int strokeWidth) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill); shape.setCornerRadius(dp(22));
        if (strokeWidth > 0) shape.setStroke(dp(strokeWidth), stroke);
        return shape;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(cardShape(Color.WHITE, 0xFFE9DFF3, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(9), 0, dp(9));
        card.setLayoutParams(lp);
        return card;
    }

    private TextView actionButton(String value, int fill, int foreground, Runnable action) {
        TextView button = label(value, 15, foreground, Gravity.CENTER);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        button.setPadding(dp(16), dp(14), dp(16), dp(14));
        button.setBackground(cardShape(fill, fill == Color.WHITE ? 0xFFD8CBE7 : fill, 1));
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(6), 0, dp(6));
        button.setLayoutParams(lp);
        return button;
    }

    private void addHeader(String subtitle) {
        TextView title = label("Neko Player  🐾", 29, ink, Gravity.CENTER_VERTICAL);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, dp(10), 0, dp(2)); body.addView(title);
        body.addView(label(subtitle, 14, purple, Gravity.CENTER_VERTICAL));
    }

    private void buildHome() {
        makeScreen();
        addHeader("Música y videos, con un toque de ternura");

        LinearLayout welcome = card();
        welcome.setGravity(Gravity.CENTER);
        TextView heading = label("Tu música, a tu manera", 20, ink, Gravity.CENTER);
        heading.setTypeface(null, android.graphics.Typeface.BOLD); welcome.addView(heading);
        welcome.addView(label("Elige un archivo de tu teléfono y deja que el gatito baile contigo", 14, purple, Gravity.CENTER));
        welcome.addView(new DancingCatView(this), new LinearLayout.LayoutParams(-1, dp(170)));
        body.addView(welcome);

        body.addView(actionButton("♫   Abrir música del teléfono", purple, Color.WHITE, this::pickAudio));
        body.addView(actionButton("▣   Abrir video del teléfono", Color.WHITE, ink, this::pickVideo));

        LinearLayout now = card();
        now.addView(label("REPRODUCCIÓN", 12, purple, Gravity.START));
        String current = selectedVideo != null ? displayName(selectedVideo) : selectedAudio != null ? displayName(selectedAudio) : "Aún no hay nada reproduciéndose";
        now.addView(label(current, 16, ink, Gravity.START));
        now.addView(label(selectedVideo != null ? "Video local" : selectedAudio != null ? (isPlaying ? "Reproduciendo ahora" : "Audio listo") : "Tus archivos permanecen en tu dispositivo", 13, purple, Gravity.START));
        body.addView(now);

        body.addView(label("YouTube y Spotify podrán añadirse más adelante. Por ahora puedes reproducir archivos locales.", 12, ink, Gravity.CENTER));
        setContentView(root);
    }

    private void buildPlayer() {
        makeScreen();
        addHeader("Tu rincón para escuchar y mirar");
        body.addView(actionButton("♫   Elegir audio", purple, Color.WHITE, this::pickAudio));
        body.addView(actionButton("▣   Elegir video", Color.WHITE, ink, this::pickVideo));

        if (selectedVideo != null) {
            LinearLayout videoCard = card();
            videoCard.addView(label(displayName(selectedVideo), 15, ink, Gravity.START));
            videoView = new VideoView(this);
            videoView.setVideoURI(selectedVideo);
            android.widget.MediaController controls = new android.widget.MediaController(this);
            controls.setAnchorView(videoView); videoView.setMediaController(controls);
            videoCard.addView(videoView, new LinearLayout.LayoutParams(-1, dp(230)));
            body.addView(videoCard);
            videoView.setOnPreparedListener(mp -> videoView.start());
        } else {
            videoView = null;
            LinearLayout musicCard = card(); musicCard.setGravity(Gravity.CENTER);
            musicCard.addView(new DancingCatView(this), new LinearLayout.LayoutParams(-1, dp(180)));
            trackTitle = label(selectedAudio == null ? "Elige una canción para empezar" : displayName(selectedAudio), 17, ink, Gravity.CENTER);
            trackTitle.setTypeface(null, android.graphics.Typeface.BOLD); musicCard.addView(trackTitle);
            statusText = label(selectedAudio == null ? "El gatito está esperando música" : (isPlaying ? "Reproduciendo" : "En pausa"), 13, purple, Gravity.CENTER);
            musicCard.addView(statusText);
            progressBar = new SeekBar(this); progressBar.setMax(!audioPrepared ? 1000 : Math.max(player.getDuration(), 1));
            if (audioPrepared) progressBar.setProgress(player.getCurrentPosition());
            musicCard.addView(progressBar, new LinearLayout.LayoutParams(-1, -2));
            LinearLayout times = new LinearLayout(this); times.setGravity(Gravity.CENTER_VERTICAL);
            elapsedText = label(!audioPrepared ? "0:00" : formatTime(player.getCurrentPosition()), 12, purple, Gravity.START);
            durationText = label(!audioPrepared ? "0:00" : formatTime(player.getDuration()), 12, purple, Gravity.END);
            times.addView(elapsedText, new LinearLayout.LayoutParams(0, -2, 1));
            times.addView(durationText, new LinearLayout.LayoutParams(0, -2, 1)); musicCard.addView(times);
            progressBar.setEnabled(audioPrepared);
            progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { if (fromUser && elapsedText != null) elapsedText.setText(formatTime(progress)); }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { isSeeking = true; }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { isSeeking = false; if (player != null && audioPrepared) player.seekTo(seekBar.getProgress()); }
            });
            LinearLayout controls = new LinearLayout(this); controls.setGravity(Gravity.CENTER);
            TextView stop = actionButton("■", Color.WHITE, ink, this::stopAudio);
            playButton = actionButton(isPlaying ? "Ⅱ" : "▶", purple, Color.WHITE, this::togglePlayback);
            controls.addView(stop, new LinearLayout.LayoutParams(0, -2, 1));
            controls.addView(playButton, new LinearLayout.LayoutParams(0, -2, 1));
            musicCard.addView(controls);
            body.addView(musicCard);
        }
        setContentView(root);
        if (isPlaying) mainHandler.removeCallbacks(playbackTicker);
        if (isPlaying) mainHandler.post(playbackTicker);
    }

    private void buildSettings() {
        makeScreen();
        addHeader("Detalles de tu reproductor");
        LinearLayout appearance = card();
        appearance.addView(label("Un espacio suave y sin distracciones", 18, ink, Gravity.START));
        appearance.addView(label("Neko Player reproduce archivos de audio y video guardados en tu teléfono.", 14, purple, Gravity.START));
        body.addView(appearance);
        body.addView(actionButton("↻   Buscar actualizaciones", purple, Color.WHITE, this::showUpdates));
        body.addView(label("La conexión con YouTube y Spotify todavía no está disponible.", 13, ink, Gravity.CENTER));
        setContentView(root);
    }

    private String displayName(Uri uri) {
        if (uri == null) return "";
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (column >= 0) return cursor.getString(column);
            }
        } catch (Exception ignored) { }
        String last = uri.getLastPathSegment();
        return last == null ? "Archivo seleccionado" : last;
    }

    private String formatTime(int milliseconds) {
        int total = Math.max(0, milliseconds / 1000);
        return String.format(java.util.Locale.getDefault(), "%d:%02d", total / 60, total % 60);
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private class DancingCatView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int fur = Color.WHITE;
        private final int outline = 0xFF6F5793;
        private final int pink = 0xFFF1AFCB;
        private final int cheek = 0xFFFFC9DB;
        private boolean attached;
        DancingCatView(android.content.Context context) { super(context); }
        @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); attached = true; }
        @Override protected void onDetachedFromWindow() { attached = false; super.onDetachedFromWindow(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float scale = Math.min(getWidth() / 210f, getHeight() / 170f);
            if (scale <= 0) return;
            float dance = (float) Math.sin(System.currentTimeMillis() / 150.0) * 4f * scale;
            canvas.save(); canvas.translate(getWidth() / 2f, getHeight() / 2f + dance); canvas.scale(scale, scale);
            paint.setStrokeWidth(3); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
            // Bailando: brazos alternan y el cuerpo rebota suavemente.
            paint.setColor(fur); paint.setStyle(Paint.Style.FILL);
            canvas.drawOval(new RectF(-28, 20, 28, 82), paint);
            paint.setColor(outline); paint.setStyle(Paint.Style.STROKE); canvas.drawOval(new RectF(-28, 20, 28, 82), paint); paint.setStyle(Paint.Style.FILL);
            paint.setColor(fur);
            Path ear = new Path(); ear.moveTo(-34, -27); ear.lineTo(-29, -65); ear.lineTo(-2, -41); ear.close();
            canvas.drawPath(ear, paint); paint.setColor(outline); paint.setStyle(Paint.Style.STROKE); canvas.drawPath(ear, paint); paint.setStyle(Paint.Style.FILL);
            Path otherEar = new Path(); otherEar.moveTo(4, -41); otherEar.lineTo(31, -65); otherEar.lineTo(36, -25); otherEar.close();
            paint.setColor(fur); canvas.drawPath(otherEar, paint); paint.setColor(outline); paint.setStyle(Paint.Style.STROKE); canvas.drawPath(otherEar, paint); paint.setStyle(Paint.Style.FILL);
            paint.setColor(pink); Path inner1 = new Path(); inner1.moveTo(-29, -38); inner1.lineTo(-27, -55); inner1.lineTo(-14, -43); inner1.close(); canvas.drawPath(inner1, paint);
            Path inner2 = new Path(); inner2.moveTo(17, -43); inner2.lineTo(30, -55); inner2.lineTo(32, -37); inner2.close(); canvas.drawPath(inner2, paint);
            paint.setColor(fur); canvas.drawCircle(0, -18, 37, paint); paint.setColor(outline); paint.setStyle(Paint.Style.STROKE); canvas.drawCircle(0, -18, 37, paint); paint.setStyle(Paint.Style.FILL);
            paint.setColor(purple); paint.setStrokeWidth(5); paint.setStyle(Paint.Style.STROKE);
            canvas.drawArc(new RectF(-31, -60, 31, 22), 180, 180, false, paint);
            paint.setStyle(Paint.Style.FILL); canvas.drawRoundRect(new RectF(-40, -29, -32, -5), 5, 5, paint); canvas.drawRoundRect(new RectF(32, -29, 40, -5), 5, 5, paint);
            paint.setStrokeWidth(3);
            paint.setColor(outline); canvas.drawCircle(-12, -20, 2.7f, paint); canvas.drawCircle(12, -20, 2.7f, paint);
            paint.setColor(cheek); canvas.drawOval(new RectF(-28, -12, -17, -5), paint); canvas.drawOval(new RectF(17, -12, 28, -5), paint);
            paint.setColor(pink); Path nose = new Path(); nose.moveTo(-4, -12); nose.lineTo(4, -12); nose.lineTo(0, -8); nose.close(); canvas.drawPath(nose, paint);
            paint.setColor(outline); paint.setStyle(Paint.Style.STROKE); canvas.drawArc(new RectF(-5, -11, 0, -4), 0, 90, false, paint); canvas.drawArc(new RectF(0, -11, 5, -4), 90, 90, false, paint);
            canvas.drawLine(-27, -11, -44, -15, paint); canvas.drawLine(-26, -7, -43, -7, paint); canvas.drawLine(27, -11, 44, -15, paint); canvas.drawLine(26, -7, 43, -7, paint);
            float arm = (float) Math.sin(System.currentTimeMillis() / 120.0) * 10f;
            canvas.drawLine(-20, 31, -46, 9 + arm, paint); canvas.drawLine(-46, 9 + arm, -48, -2 + arm, paint);
            canvas.drawLine(20, 31, 46, 9 - arm, paint); canvas.drawLine(46, 9 - arm, 48, -2 - arm, paint);
            canvas.drawLine(-16, 76, -24, 91, paint); canvas.drawLine(16, 76, 24, 91, paint);
            paint.setStyle(Paint.Style.FILL); paint.setTextSize(22); paint.setColor(purple); canvas.drawText("♫", 53, -30 + arm, paint); canvas.drawText("♪", -64, 18 - arm, paint);
            canvas.restore(); if (attached) postInvalidateDelayed(45);
        }
    }

    private void pickAudio() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("audio/*");
        startActivityForResult(intent, PICK_AUDIO);
    }

    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("video/*");
        startActivityForResult(intent, PICK_VIDEO);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        if (requestCode == PICK_AUDIO) {
            selectedAudio = data.getData(); selectedVideo = null;
            if (videoView != null) { try { videoView.stopPlayback(); } catch (Exception ignored) { } videoView = null; }
            if (player != null) { player.release(); player = null; }
            isPlaying = false; audioPrepared = false;
            buildPlayer();
            try {
                player = new MediaPlayer();
                player.setDataSource(this, selectedAudio);
                player.setOnPreparedListener(mp -> {
                    audioPrepared = true;
                    if (trackTitle != null) trackTitle.setText(displayName(selectedAudio));
                    if (statusText != null) statusText.setText("Reproduciendo ahora");
                    if (progressBar != null) { progressBar.setMax(Math.max(mp.getDuration(), 1)); progressBar.setEnabled(true); }
                    if (durationText != null) durationText.setText(formatTime(mp.getDuration()));
                    mp.start(); isPlaying = true;
                    if (playButton != null) playButton.setText("Ⅱ");
                    mainHandler.removeCallbacks(playbackTicker); mainHandler.post(playbackTicker);
                });
                player.setOnCompletionListener(mp -> {
                    isPlaying = false;
                    if (playButton != null) playButton.setText("▶");
                    if (statusText != null) statusText.setText("Reproducción terminada");
                    mainHandler.removeCallbacks(playbackTicker);
                });
                player.setOnErrorListener((mp, what, extra) -> {
                    isPlaying = false;
                    if (statusText != null) statusText.setText("No se pudo reproducir este audio.");
                    return true;
                });
                if (statusText != null) statusText.setText("Cargando audio…");
                player.prepareAsync();
            } catch (IOException e) {
                if (statusText != null) statusText.setText("No se pudo abrir ese archivo de audio.");
                Toast.makeText(this, "Prueba con otro archivo de audio", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PICK_VIDEO) {
            selectedVideo = data.getData(); selectedAudio = null; isPlaying = false;
            audioPrepared = false;
            mainHandler.removeCallbacks(playbackTicker);
            if (player != null) { player.release(); player = null; }
            buildPlayer();
        }
    }

    private void togglePlayback() {
        if (player == null) { pickAudio(); return; }
        if (!audioPrepared) { Toast.makeText(this, "Espera a que termine de cargar la canción", Toast.LENGTH_SHORT).show(); return; }
        if (isPlaying) { player.pause(); isPlaying = false; if (playButton != null) playButton.setText("▶"); if (statusText != null) statusText.setText("En pausa"); mainHandler.removeCallbacks(playbackTicker); }
        else { player.start(); isPlaying = true; if (playButton != null) playButton.setText("Ⅱ"); if (statusText != null) statusText.setText("Reproduciendo"); mainHandler.post(playbackTicker); }
    }

    private void stopAudio() {
        if (player != null && audioPrepared) { player.pause(); player.seekTo(0); isPlaying = false; if (playButton != null) playButton.setText("▶"); if (statusText != null) statusText.setText("Detenido"); if (progressBar != null) progressBar.setProgress(0); if (elapsedText != null) elapsedText.setText("0:00"); mainHandler.removeCallbacks(playbackTicker); }
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
        mainHandler.removeCallbacks(playbackTicker);
        if (player != null) { player.release(); player = null; }
        super.onDestroy();
    }
}
