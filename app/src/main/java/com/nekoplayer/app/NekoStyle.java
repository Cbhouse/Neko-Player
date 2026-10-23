package com.nekoplayer.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.OvershootInterpolator;

/** Shared native design and motion tokens measured from the supplied 698px reference. */
final class NekoStyle {
    static final int BG = 0xFFF8F4FF, SURFACE = Color.WHITE, INK = 0xFF100C29;
    static final int MUTED = 0xFF746498, ACCENT = 0xFF7539CB, ACCENT_DARK = 0xFF713BC5;
    static final int ACCENT_LIGHT = 0xFF9654DD, ON_ACCENT = Color.WHITE;
    static final int ON_ACCENT_MUTED = Color.WHITE, LILAC = 0xFFF2ECFC, BORDER = 0xFFEDE5F9;
    static final int LAVENDER = 0xFFC7BBFA, PINK = 0xFFF8C7F8, NOTE = 0xFFAD6AF0;
    static final int PLUM = 0xFF613A92, RED = 0xFFEF302E, GREEN = 0xFF1ED760;
    static final int QUICK = 130, ENTER = 340, STAGGER = 55;
    static final Typeface REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    static final Typeface BOLD = Typeface.create("sans-serif", Typeface.BOLD);

    static GradientDrawable shape(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(radius); return d;
    }

    static GradientDrawable gradient(int start, int end, float radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{start, end});
        d.setCornerRadius(radius); return d;
    }

    static boolean motionEnabled(Context c) {
        return c.getSharedPreferences("neko_preferences", Context.MODE_PRIVATE)
                .getBoolean("animations", true)
                && Settings.Global.getFloat(c.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0;
    }

    // Returning false preserves View's native click handling, including performClick and keyboard use.
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    static void button(View view, String description, Runnable action, float radius) {
        view.setContentDescription(description);
        view.setFocusable(true); view.setClickable(true);
        view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName("android.widget.Button");
            }
        });
        view.setForeground(new RippleDrawable(ColorStateList.valueOf(0x237539CB),
                null, shape(Color.WHITE, radius)));
        view.setOnTouchListener((v, e) -> {
            if (motionEnabled(v.getContext())) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(.975f).scaleY(.975f).setDuration(QUICK).start();
                } else if (e.getActionMasked() == MotionEvent.ACTION_UP
                        || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1).scaleY(1).setDuration(220)
                            .setInterpolator(new OvershootInterpolator(1.1f)).start();
                }
            }
            return false;
        });
        view.setOnClickListener(v -> action.run());
    }

    private NekoStyle() { }
}
