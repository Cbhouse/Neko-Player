package com.nekoplayer.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
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
    private int activeTab = 0;
    private NekoHomeView homeScreen;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildHome();
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private LinearLayout makeScreen() {
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFFF8F4FF, 0xFFF2EAFE, 0xFFF9F6FF}));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(10), dp(8), dp(10), dp(4));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(bottomNavigation());
        if (Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                        android.view.WindowInsets.Type.systemBars()
                        | android.view.WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
            root.requestApplyInsets();
        }
        return root;
    }

    private LinearLayout bottomNavigation() {
        return new NekoNavigation(this, activeTab,
                new Runnable[]{this::buildHome, this::buildPlayer, this::buildSettings});
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
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, dp(2), 0, dp(12));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.neko_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("Logo de Neko Player");
        header.addView(logo, new LinearLayout.LayoutParams(dp(54), dp(58)));
        LinearLayout words = new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL);
        TextView title = label("Neko Player", 28, ink, Gravity.CENTER_VERTICAL);
        title.setTypeface(null, android.graphics.Typeface.BOLD); title.setPadding(0, 0, 0, 0);
        words.addView(title);
        TextView sub = label(subtitle, 13, 0xFF777198, Gravity.CENTER_VERTICAL); sub.setPadding(0, 0, 0, 0);
        words.addView(sub);
        header.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        TextView settings = new TextView(this); settings.setGravity(Gravity.CENTER); settings.setText("⚙"); settings.setTextSize(21); settings.setTextColor(ink);
        settings.setBackground(cardShape(Color.WHITE, 0xFFF0EAF8, 0));
        header.addView(settings, new LinearLayout.LayoutParams(dp(54), dp(54)));
        settings.setOnClickListener(v -> buildSettings());
        body.addView(header);
    }

    private void buildHome() {
        activeTab = 0;
        mainHandler.removeCallbacks(playbackTicker);
        progressBar = null; elapsedText = null; durationText = null;
        trackTitle = null; statusText = null; playButton = null;
        if (videoView != null) { videoView.pause(); }
        getWindow().setStatusBarColor(NekoStyle.BG);
        getWindow().setNavigationBarColor(NekoStyle.BG);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        int systemUi = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) systemUi |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(systemUi);
        homeScreen = new NekoHomeView(this, this::pickAudio, this::pickVideo,
                this::buildPlayer, this::buildSettings);
        setContentView(homeScreen);
        // Android 15 enforces edge-to-edge for target 35: keep controls clear of system bars.
        if (Build.VERSION.SDK_INT >= 35) {
            homeScreen.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                        android.view.WindowInsets.Type.systemBars()
                        | android.view.WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
            homeScreen.requestApplyInsets();
        }
        refreshHomePlayback();
    }

    private void refreshHomePlayback() {
        if (homeScreen == null || activeTab != 0) return;
        if (selectedAudio != null) {
            homeScreen.setPlayback(displayName(selectedAudio), !audioPrepared ? "Preparando tu canción…"
                    : isPlaying ? "Reproduciendo ahora · En tu dispositivo" : "En pausa · En tu dispositivo", isPlaying);
        } else if (selectedVideo != null) {
            homeScreen.setPlayback(displayName(selectedVideo), "Video seleccionado · Abrir reproductor", false);
        } else {
            homeScreen.setPlayback("Aún no hay nada reproduciéndose",
                    "Tus archivos permanecen en tu dispositivo", false);
        }
    }

    private void buildPlayer() {
        homeScreen = null;
        activeTab = 1;
        makeScreen();
        addHeader("Tu rincón para escuchar y mirar");
        body.addView(playerSourceSelector());

        if (selectedVideo != null) {
            videoView = new VideoView(this);
            videoView.setVideoURI(selectedVideo);
            android.widget.MediaController videoControls = new android.widget.MediaController(this);
            videoControls.setAnchorView(videoView); videoView.setMediaController(videoControls);
            LinearLayout videoCard = card();
            videoCard.setPadding(dp(10), dp(10), dp(10), dp(12));
            videoCard.addView(videoView, new LinearLayout.LayoutParams(-1, dp(270)));
            videoCard.addView(label(displayName(selectedVideo), 15, ink, Gravity.START));
            videoCard.addView(label("Video de tu dispositivo", 13, 0xFF777198, Gravity.START));
            body.addView(videoCard);
            videoView.setOnPreparedListener(mp -> videoView.start());
            progressBar = null; elapsedText = null; durationText = null;
            trackTitle = null; statusText = null; playButton = null;
        } else {
            videoView = null;
            LinearLayout artworkCard = card();
            artworkCard.setPadding(dp(12), dp(12), dp(12), dp(14));
            FrameLayout artwork = new FrameLayout(this);
            artwork.setBackground(cardShape(0xFFF0E9FF, 0xFFF0E9FF, 0));
            artwork.addView(new DancingCatView(this), new FrameLayout.LayoutParams(-1, -1));
            artworkCard.addView(artwork, new LinearLayout.LayoutParams(-1, dp(250)));

            LinearLayout titleRow = new LinearLayout(this);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            titleRow.setPadding(dp(4), dp(12), dp(4), dp(2));
            LinearLayout trackInfo = new LinearLayout(this); trackInfo.setOrientation(LinearLayout.VERTICAL);
            trackTitle = label(selectedAudio == null ? "Tu próxima canción" : displayName(selectedAudio), 18, ink, Gravity.START);
            trackTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            trackTitle.setMaxLines(1); trackTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
            trackInfo.addView(trackTitle);
            statusText = label(selectedAudio == null ? "El gatito está esperando música" : (isPlaying ? "Reproduciendo ahora" : audioPrepared ? "En pausa" : "Preparando tu canción…"), 13, 0xFF777198, Gravity.START);
            trackInfo.addView(statusText);
            titleRow.addView(trackInfo, new LinearLayout.LayoutParams(0, -2, 1));
            artworkCard.addView(titleRow);

            progressBar = new SeekBar(this);
            progressBar.setMax(!audioPrepared ? 1000 : Math.max(player.getDuration(), 1));
            if (audioPrepared) progressBar.setProgress(player.getCurrentPosition());
            if (Build.VERSION.SDK_INT >= 21) {
                progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF8150D2));
                progressBar.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFF8150D2));
                progressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE6DFF1));
            }
            artworkCard.addView(progressBar, new LinearLayout.LayoutParams(-1, dp(34)));
            LinearLayout times = new LinearLayout(this); times.setGravity(Gravity.CENTER_VERTICAL);
            elapsedText = label(!audioPrepared ? "0:00" : formatTime(player.getCurrentPosition()), 12, 0xFF777198, Gravity.START);
            durationText = label(!audioPrepared ? "0:00" : formatTime(player.getDuration()), 12, 0xFF777198, Gravity.END);
            times.addView(elapsedText, new LinearLayout.LayoutParams(0, -2, 1));
            times.addView(durationText, new LinearLayout.LayoutParams(0, -2, 1)); artworkCard.addView(times);
            progressBar.setEnabled(audioPrepared);
            progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { if (fromUser && elapsedText != null) elapsedText.setText(formatTime(progress)); }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { isSeeking = true; }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { isSeeking = false; if (player != null && audioPrepared) player.seekTo(seekBar.getProgress()); }
            });
            LinearLayout controls = new LinearLayout(this); controls.setGravity(Gravity.CENTER); controls.setPadding(0, dp(8), 0, dp(2));
            TextView stop = playerRoundControl("■", Color.WHITE, ink, dp(52), this::stopAudio);
            playButton = playerRoundControl(isPlaying ? "Ⅱ" : "▶", purple, Color.WHITE, dp(68), this::togglePlayback);
            TextView queue = playerRoundControl("+", Color.WHITE, purple, dp(52), this::pickAudio);
            controls.addView(stop, new LinearLayout.LayoutParams(0, dp(62), 1));
            controls.addView(playButton, new LinearLayout.LayoutParams(0, dp(72), 1));
            controls.addView(queue, new LinearLayout.LayoutParams(0, dp(62), 1));
            artworkCard.addView(controls);
            body.addView(artworkCard);
        }

        LinearLayout library = new LinearLayout(this);
        library.setGravity(Gravity.CENTER_VERTICAL); library.setPadding(dp(14), dp(8), dp(14), dp(8));
        library.setBackground(cardShape(Color.WHITE, 0xFFEDE5F9, 1));
        TextView libraryIcon = label(selectedVideo == null ? "♫" : "▶", 22, purple, Gravity.CENTER);
        libraryIcon.setBackground(cardShape(0xFFF2ECFC, 0xFFF2ECFC, 0));
        library.addView(libraryIcon, new LinearLayout.LayoutParams(dp(46), dp(46)));
        LinearLayout libraryText = new LinearLayout(this); libraryText.setOrientation(LinearLayout.VERTICAL); libraryText.setPadding(dp(12), 0, 0, 0);
        TextView libraryTitle = label(selectedAudio != null || selectedVideo != null ? "Elegir otro archivo" : "Reproduce desde tu teléfono", 14, ink, Gravity.START);
        libraryTitle.setTypeface(null, android.graphics.Typeface.BOLD); libraryText.addView(libraryTitle);
        libraryText.addView(label("Tu música y tus videos permanecen en tu dispositivo", 11, 0xFF777198, Gravity.START));
        library.addView(libraryText, new LinearLayout.LayoutParams(0, -2, 1));
        TextView browse = label("›", 26, purple, Gravity.CENTER); library.addView(browse, new LinearLayout.LayoutParams(dp(30), dp(44)));
        library.setOnClickListener(v -> { if (selectedVideo != null) pickVideo(); else pickAudio(); });
        LinearLayout.LayoutParams libraryLp = new LinearLayout.LayoutParams(-1, -2); libraryLp.setMargins(0, dp(6), 0, dp(8)); library.setLayoutParams(libraryLp);
        body.addView(library);
        setContentView(root);
        if (isPlaying) mainHandler.removeCallbacks(playbackTicker);
        if (isPlaying) mainHandler.post(playbackTicker);
    }

    private LinearLayout playerSourceSelector() {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(playerSourceButton("♫", "Música", selectedVideo == null ? purple : Color.WHITE,
                selectedVideo == null ? Color.WHITE : ink, this::pickAudio), new LinearLayout.LayoutParams(0, dp(58), 1));
        View spacer = new View(this); row.addView(spacer, new LinearLayout.LayoutParams(dp(10), 1));
        row.addView(playerSourceButton("▶", "Video", selectedVideo != null ? purple : Color.WHITE,
                selectedVideo != null ? Color.WHITE : ink, this::pickVideo), new LinearLayout.LayoutParams(0, dp(58), 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(2), 0, dp(5)); row.setLayoutParams(lp);
        return row;
    }

    private View playerSourceButton(String icon, String title, int fill, int foreground, Runnable action) {
        LinearLayout button = new LinearLayout(this); button.setGravity(Gravity.CENTER);
        button.setBackground(cardShape(fill, fill == Color.WHITE ? 0xFFEDE5F9 : fill, 1));
        PlayerGlyph glyph = new PlayerGlyph(this, icon, foreground);
        LinearLayout.LayoutParams glyphLp = new LinearLayout.LayoutParams(dp(24), dp(24)); glyphLp.setMargins(0, 0, dp(7), 0);
        TextView name = label(title, 14, foreground, Gravity.CENTER); name.setTypeface(null, android.graphics.Typeface.BOLD);
        button.addView(glyph, glyphLp); button.addView(name); button.setOnClickListener(v -> action.run());
        return button;
    }

    private TextView playerRoundControl(String symbol, int fill, int foreground, int size, Runnable action) {
        TextView control = new TextView(this); control.setText(symbol); control.setTextSize(size == dp(68) ? 25 : 19);
        control.setTextColor(foreground); control.setGravity(Gravity.CENTER);
        control.setBackground(cardShape(fill, fill == Color.WHITE ? 0xFFECE3F7 : fill, 1));
        control.setElevation(dp(2)); control.setOnClickListener(v -> action.run());
        return control;
    }

    private class PlayerGlyph extends View {
        private final String kind;
        private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        PlayerGlyph(android.content.Context context, String kind, int color) {
            super(context); this.kind = kind; iconPaint.setColor(color);
            iconPaint.setStrokeWidth(dp(2.4f)); iconPaint.setStrokeCap(Paint.Cap.ROUND); iconPaint.setStrokeJoin(Paint.Join.ROUND);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float scale = Math.min(getWidth(), getHeight()) / 32f;
            canvas.save(); canvas.translate((getWidth()-32*scale)/2f, (getHeight()-32*scale)/2f); canvas.scale(scale, scale);
            iconPaint.setStyle(Paint.Style.STROKE);
            boolean active = kind.endsWith("_ACTIVE");
            String shape = active ? kind.substring(0, kind.length()-7) : kind;
            if ("♫".equals(kind)) {
                iconPaint.setStyle(Paint.Style.FILL);
                canvas.drawOval(new RectF(3,22,13,29),iconPaint); canvas.drawOval(new RectF(18,18,28,25),iconPaint);
                iconPaint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(12,24,12,5,iconPaint); canvas.drawLine(12,5,28,2,iconPaint); canvas.drawLine(28,2,28,21,iconPaint);
            } else if ("HOME".equals(shape)) {
                Path home = new Path(); home.moveTo(3,14); home.lineTo(16,3); home.lineTo(29,14); home.lineTo(26,17); home.lineTo(26,28); home.lineTo(19,28); home.lineTo(19,21); home.lineTo(13,21); home.lineTo(13,28); home.lineTo(6,28); home.lineTo(6,17); home.close();
                iconPaint.setStyle(active ? Paint.Style.FILL : Paint.Style.STROKE); canvas.drawPath(home,iconPaint);
            } else if ("PLAY".equals(shape)) {
                Path play = new Path(); play.moveTo(10,4); play.lineTo(27,16); play.lineTo(10,28); play.close();
                iconPaint.setStyle(active ? Paint.Style.FILL : Paint.Style.STROKE); canvas.drawPath(play,iconPaint);
            } else if ("SETTINGS".equals(shape)) {
                iconPaint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(16,16,10,iconPaint); canvas.drawCircle(16,16,3.4f,iconPaint);
                for(int i=0;i<8;i++){ double a=Math.PI*i/4; float x=(float)Math.cos(a), y=(float)Math.sin(a); canvas.drawLine(16+x*11,16+y*11,16+x*14,16+y*14,iconPaint); }
            } else {
                canvas.drawRoundRect(new RectF(3,6,29,26),5,5,iconPaint);
                iconPaint.setStyle(Paint.Style.FILL);
                Path triangle = new Path(); triangle.moveTo(13,10); triangle.lineTo(13,22); triangle.lineTo(22,16); triangle.close(); canvas.drawPath(triangle,iconPaint);
            }
            canvas.restore();
        }
    }

    private void buildSettings() {
        homeScreen = null;
        activeTab = 2;
        makeScreen();
        addHeader("Detalles de tu reproductor");
        LinearLayout appearance = card();
        appearance.addView(label("Un espacio suave y sin distracciones", 18, ink, Gravity.START));
        appearance.addView(label("Neko Player reproduce archivos de audio y video guardados en tu teléfono.", 14, purple, Gravity.START));
        body.addView(appearance);
        android.widget.Switch motion = new android.widget.Switch(this);
        motion.setText("Animaciones del gatito");
        motion.setTextColor(ink); motion.setTextSize(15);
        motion.setPadding(dp(16), dp(16), dp(16), dp(16));
        motion.setChecked(getSharedPreferences("neko_preferences", MODE_PRIVATE).getBoolean("animations", true));
        motion.setOnCheckedChangeListener((button, enabled) ->
                getSharedPreferences("neko_preferences", MODE_PRIVATE).edit().putBoolean("animations", enabled).apply());
        body.addView(motion, new LinearLayout.LayoutParams(-1, -2));
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
        private final int outline = 0xFF573478;
        private final int pink = 0xFFF1AFCB;
        private final int cheek = 0xFFFFC9DB;
        private boolean attached;
        DancingCatView(android.content.Context context) { super(context); }
        @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); attached = true; }
        @Override public void onWindowFocusChanged(boolean focused) {
            super.onWindowFocusChanged(focused); if (focused) invalidate();
        }
        @Override protected void onDetachedFromWindow() { attached = false; super.onDetachedFromWindow(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (getWidth() <= 0 || getHeight() <= 0) return;
            canvas.save();
            Path clip = new Path(); clip.addRoundRect(new RectF(0, 0, getWidth(), getHeight()), dp(26), dp(26), Path.Direction.CW);
            canvas.clipPath(clip);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(0, 0, getWidth(), getHeight(), new int[]{0xFFE6DDFF, 0xFFF7DCFA, 0xFFD9D8FF}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setShader(null);
            paint.setColor(0x66FFFFFF); canvas.drawCircle(getWidth() * .88f, getHeight() * .08f, getWidth() * .36f, paint);
            paint.setColor(0x55C4A6F7); Path hill = new Path(); hill.moveTo(0, getHeight() * .72f); hill.cubicTo(getWidth() * .22f, getHeight() * .80f, getWidth() * .25f, getHeight() * .45f, getWidth() * .50f, getHeight() * .61f); hill.cubicTo(getWidth() * .70f, getHeight() * .72f, getWidth() * .78f, getHeight() * .88f, getWidth(), getHeight() * .49f); hill.lineTo(getWidth(), getHeight()); hill.lineTo(0, getHeight()); hill.close(); canvas.drawPath(hill, paint);
            paint.setColor(0x66EBC8F5); Path lowerWave = new Path(); lowerWave.moveTo(0, getHeight() * .87f); lowerWave.cubicTo(getWidth() * .27f, getHeight() * .75f, getWidth() * .48f, getHeight() * .93f, getWidth() * .68f, getHeight() * .83f); lowerWave.cubicTo(getWidth() * .83f, getHeight() * .77f, getWidth() * .93f, getHeight() * .94f, getWidth(), getHeight() * .80f); lowerWave.lineTo(getWidth(), getHeight()); lowerWave.lineTo(0, getHeight()); lowerWave.close(); canvas.drawPath(lowerWave, paint);
            paint.setColor(0xFFB45FE3); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(5));
            drawNote(canvas, getWidth() * .15f, getHeight() * .22f, dp(30)); drawNote(canvas, getWidth() * .83f, getHeight() * .64f, dp(25)); drawNote(canvas, getWidth() * .30f, getHeight() * .51f, dp(18));
            paint.setStyle(Paint.Style.FILL); paint.setColor(0xFFA47BE5); drawSparkle(canvas, getWidth() * .72f, getHeight() * .84f, dp(10)); drawSparkle(canvas, getWidth() * .13f, getHeight() * .91f, dp(7)); drawSparkle(canvas, getWidth() * .92f, getHeight() * .78f, dp(6));
            float scale = Math.min(getWidth() / 148f, getHeight() / 202f);
            if (scale <= 0) return;
            float dance = (float) Math.sin(System.currentTimeMillis() / 150.0) * 3f * scale;
            canvas.save(); canvas.translate(getWidth() * .56f, getHeight() * .69f + dance); canvas.scale(scale, scale);
            paint.setStrokeWidth(4); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
            // Brazos elevados, vestido/cuerpo redondeado y piernas abiertas para una postura alegre.
            paint.setColor(fur); paint.setStyle(Paint.Style.FILL);
            canvas.drawOval(new RectF(-34, 18, 35, 96), paint);
            paint.setColor(outline); paint.setStyle(Paint.Style.STROKE); canvas.drawOval(new RectF(-34, 18, 35, 96), paint); paint.setStyle(Paint.Style.FILL);
            paint.setColor(fur); canvas.drawOval(new RectF(-20, 84, 4, 106), paint); canvas.drawOval(new RectF(14, 83, 38, 105), paint);
            paint.setColor(outline); paint.setStyle(Paint.Style.STROKE); canvas.drawOval(new RectF(-20, 84, 4, 106), paint); canvas.drawOval(new RectF(14, 83, 38, 105), paint); paint.setStyle(Paint.Style.FILL);
            paint.setColor(fur);
            Path ear = new Path(); ear.moveTo(-38, -23); ear.lineTo(-31, -82); ear.lineTo(3, -48); ear.close();
            canvas.drawPath(ear, paint); paint.setColor(outline); paint.setStyle(Paint.Style.STROKE); canvas.drawPath(ear, paint); paint.setStyle(Paint.Style.FILL);
            Path otherEar = new Path(); otherEar.moveTo(-2, -48); otherEar.lineTo(38, -82); otherEar.lineTo(40, -21); otherEar.close();
            paint.setColor(fur); canvas.drawPath(otherEar, paint); paint.setColor(outline); paint.setStyle(Paint.Style.STROKE); canvas.drawPath(otherEar, paint); paint.setStyle(Paint.Style.FILL);
            paint.setColor(pink); Path inner1 = new Path(); inner1.moveTo(-30, -40); inner1.lineTo(-27, -66); inner1.lineTo(-11, -49); inner1.close(); canvas.drawPath(inner1, paint);
            Path inner2 = new Path(); inner2.moveTo(13, -49); inner2.lineTo(34, -67); inner2.lineTo(35, -38); inner2.close(); canvas.drawPath(inner2, paint);
            paint.setColor(fur); canvas.drawCircle(0, -15, 43, paint); paint.setColor(outline); paint.setStyle(Paint.Style.STROKE); canvas.drawCircle(0, -15, 43, paint); paint.setStyle(Paint.Style.FILL);
            paint.setColor(purple); paint.setStrokeWidth(5); paint.setStyle(Paint.Style.STROKE);
            canvas.drawArc(new RectF(-39, -69, 39, 30), 180, 180, false, paint);
            paint.setStyle(Paint.Style.FILL); canvas.drawRoundRect(new RectF(-49, -27, -39, 2), 5, 5, paint); canvas.drawRoundRect(new RectF(39, -27, 49, 2), 5, 5, paint);
            paint.setStrokeWidth(3);
            paint.setColor(outline); canvas.drawArc(new RectF(-29, -31, -8, -9), 205, 130, false, paint); canvas.drawArc(new RectF(8, -31, 29, -9), 205, 130, false, paint);
            paint.setColor(cheek); canvas.drawOval(new RectF(-34, -12, -17, -3), paint); canvas.drawOval(new RectF(17, -12, 34, -3), paint);
            paint.setColor(pink); Path nose = new Path(); nose.moveTo(-5, -13); nose.lineTo(5, -13); nose.lineTo(0, -7); nose.close(); canvas.drawPath(nose, paint);
            paint.setColor(outline); paint.setStyle(Paint.Style.STROKE); canvas.drawArc(new RectF(-5, -11, 0, -4), 0, 90, false, paint); canvas.drawArc(new RectF(0, -11, 5, -4), 90, 90, false, paint);
            canvas.drawLine(-30, -10, -48, -15, paint); canvas.drawLine(-30, -5, -48, -4, paint); canvas.drawLine(30, -10, 48, -15, paint); canvas.drawLine(30, -5, 48, -4, paint);
            float arm = (float) Math.sin(System.currentTimeMillis() / 120.0) * 10f;
            canvas.drawLine(-27, 34, -54, 13 + arm, paint); canvas.drawLine(-54, 13 + arm, -62, -5 + arm, paint);
            canvas.drawLine(27, 34, 53, 10 - arm, paint); canvas.drawLine(53, 10 - arm, 63, -7 - arm, paint);
            paint.setColor(fur); canvas.drawCircle(-63, -7 + arm, 9, paint); canvas.drawCircle(64, -8 - arm, 9, paint);
            paint.setColor(outline); canvas.drawCircle(-63, -7 + arm, 9, paint); canvas.drawCircle(64, -8 - arm, 9, paint);
            paint.setColor(cheek); canvas.drawCircle(-63, -7 + arm, 3, paint); canvas.drawCircle(64, -8 - arm, 3, paint);
            canvas.drawLine(-18, 87, -27, 101, paint); canvas.drawLine(18, 87, 27, 101, paint);
            canvas.restore(); if (attached && hasWindowFocus() && NekoStyle.motionEnabled(MainActivity.this)) postInvalidateDelayed(45);
        }

        private void drawNote(Canvas canvas, float x, float y, float size) {
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(Math.max(dp(2), size * .12f));
            canvas.drawLine(x + size * .48f, y - size, x + size * .48f, y + size * .45f, paint);
            canvas.drawLine(x + size * .48f, y - size, x + size, y - size * 1.15f, paint);
            canvas.drawLine(x + size, y - size * 1.15f, x + size, y + size * .1f, paint);
            paint.setStyle(Paint.Style.FILL); canvas.drawOval(x, y + size * .32f, x + size * .52f, y + size * .65f, paint); canvas.drawOval(x + size * .52f, y, x + size * 1.02f, y + size * .32f, paint);
        }

        private void drawSparkle(Canvas canvas, float x, float y, float size) {
            Path star = new Path(); star.moveTo(x, y-size); star.quadTo(x+size*.18f,y-size*.2f,x+size,y); star.quadTo(x+size*.18f,y+size*.2f,x,y+size); star.quadTo(x-size*.18f,y+size*.2f,x-size,y); star.quadTo(x-size*.18f,y-size*.2f,x,y-size); star.close(); canvas.drawPath(star, paint);
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
                    mp.start(); isPlaying = true; refreshHomePlayback();
                    if (playButton != null) playButton.setText("Ⅱ");
                    mainHandler.removeCallbacks(playbackTicker); mainHandler.post(playbackTicker);
                });
                player.setOnCompletionListener(mp -> {
                    isPlaying = false; refreshHomePlayback();
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
        if (homeScreen != null) homeScreen.setResumed(true);
        if (openingInstallSettings) {
            openingInstallSettings = false;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || getPackageManager().canRequestPackageInstalls()) {
                installDownloadedUpdate();
            }
        }
    }

    @Override protected void onPause() {
        if (homeScreen != null) homeScreen.setResumed(false);
        super.onPause();
    }

    @Override protected void onDestroy() {
        updateExecutor.shutdownNow();
        mainHandler.removeCallbacks(playbackTicker);
        if (player != null) { player.release(); player = null; }
        super.onDestroy();
    }
}

