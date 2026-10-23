package com.nekoplayer.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

final class NekoNavigation extends LinearLayout {
    NekoNavigation(Context context, int selected, Runnable[] actions) {
        super(context);
        float u = context.getResources().getDisplayMetrics().widthPixels / 698f;
        setOrientation(HORIZONTAL); setGravity(Gravity.CENTER);
        setPadding(Math.round(24*u),Math.round(10*u),Math.round(24*u),Math.round(4*u));
        GradientDrawable background=NekoStyle.shape(Color.WHITE,0);
        background.setCornerRadii(new float[]{36*u,36*u,36*u,36*u,0,0,0,0});
        setBackground(background); setElevation(4*u);
        String[] labels={"Inicio","Reproductor","Ajustes"};
        NekoIcon.Kind[] icons={NekoIcon.Kind.HOME,NekoIcon.Kind.PLAY,NekoIcon.Kind.SETTINGS};
        for(int i=0;i<3;i++) {
            final int index=i;
            int tint=i==selected ? NekoStyle.ACCENT : NekoStyle.MUTED;
            LinearLayout item=new LinearLayout(context); item.setOrientation(VERTICAL);
            item.setGravity(Gravity.CENTER); item.setMinimumHeight(Math.round(84*u));
            if(i==selected) item.setBackground(NekoStyle.shape(NekoStyle.LILAC,44*u));
            item.addView(new NekoIcon(context,icons[i],tint,i==selected),
                    new LayoutParams(Math.round(29*u),Math.round(29*u)));
            TextView label=new TextView(context); label.setText(labels[i]); label.setTextColor(tint);
            label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,20*u
                    *context.getResources().getConfiguration().fontScale);
            label.setTypeface(i==selected ? NekoStyle.MEDIUM : NekoStyle.REGULAR);
            label.setGravity(Gravity.CENTER); label.setPadding(0,Math.round(4*u),0,0);
            label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            item.addView(label,new LayoutParams(-1,-2)); item.setSelected(i==selected);
            NekoStyle.button(item,labels[i],()->{ if(index!=selected) actions[index].run(); },44*u);
            LayoutParams lp=new LayoutParams(0,-2,1); lp.setMargins(0,0,Math.round(10*u),0);
            addView(item,lp);
        }
    }
}
