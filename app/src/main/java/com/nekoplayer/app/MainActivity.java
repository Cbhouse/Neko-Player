package com.nekoplayer.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final int PICK_AUDIO = 41;
    private LinearLayout root;
    private TextView trackTitle, statusText, playButton;
    private MediaPlayer player;
    private Uri selectedAudio;
    private boolean isPlaying = false;

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
        new android.app.AlertDialog.Builder(this)
            .setTitle("Actualizaciones")
            .setMessage("Neko Player 0.1.0\n\nEsta versión inicial todavía no está conectada a un servidor de actualizaciones. La arquitectura queda preparada para añadir comprobación de versiones cuando publiquemos un servidor seguro.")
            .setPositiveButton("Entendido", (d, w) -> {})
            .show();
    }

    @Override protected void onDestroy() {
        if (player != null) { player.release(); player = null; }
        super.onDestroy();
    }
}
