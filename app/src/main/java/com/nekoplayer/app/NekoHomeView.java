package com.nekoplayer.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** The reference reconstructed as native controls, with independently animated artwork. */
final class NekoHomeView extends FrameLayout {
    private final float u;
    private final Runnable openPlayer;
    private final List<View> entrances=new ArrayList<>();
    private ValueAnimator ambient;
    private ImageView mascot;
    private HeroArt heroArt;
    private FooterArt footerArt;
    private TextView playbackTitle, playbackSubtitle;
    private boolean resumed=true;
    private float phase;

    NekoHomeView(Context context, Runnable audio, Runnable video, Runnable player, Runnable settings) {
        super(context);
        u=getResources().getDisplayMetrics().widthPixels/698f; openPlayer=player;
        setBackgroundColor(NekoStyle.BG);
        LinearLayout screen=new LinearLayout(context); screen.setOrientation(LinearLayout.VERTICAL);
        addView(screen,new LayoutParams(-1,-1));
        ScrollView scroll=new ScrollView(context); scroll.setFillViewport(true);
        scroll.setClipToPadding(false); scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content=new LinearLayout(context); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(px(18),0,px(18),0); content.setClipChildren(false);
        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        add(content,header(settings),110,18);
        add(content,hero(),390,22);
        add(content,action(true,audio),138,18);
        add(content,action(false,video),138,22);
        add(content,playback(),216,22);
        add(content,sources(),168,0);
        footerArt=new FooterArt(context); footerArt.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        content.addView(footerArt,new LinearLayout.LayoutParams(-1,px(114)));
        screen.addView(new NekoNavigation(context,0,new Runnable[]{()->{},player,settings}),
                new LinearLayout.LayoutParams(-1,-2));
    }

    private int px(float value) { return Math.round(value*u); }

    private TextView text(String value,float size,int color,boolean bold) {
        TextView view=new TextView(getContext()); view.setText(value); view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX,size*u*getResources().getConfiguration().fontScale);
        view.setTypeface(bold ? NekoStyle.BOLD : NekoStyle.REGULAR);
        view.setIncludeFontPadding(false); view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private void position(FrameLayout parent,View child,float width,float height,float left,float top) {
        LayoutParams lp=new LayoutParams(width<0 ? (int)width : px(width),height<0 ? (int)height : px(height));
        lp.leftMargin=px(left); lp.topMargin=px(top); parent.addView(child,lp);
    }

    private void add(LinearLayout parent,View child,int height,int gap) {
        // Let text grow with Android's accessibility font size; small screens remain scrollable.
        float fontScale=getResources().getConfiguration().fontScale;
        int actual=Math.round(height*Math.max(1f,fontScale*fontScale));
        if(child instanceof FrameLayout) child.setMinimumHeight(px(actual));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,px(actual));
        lp.bottomMargin=px(gap); parent.addView(child,lp); entrances.add(child);
    }

    private FrameLayout header(Runnable settings) {
        FrameLayout header=new FrameLayout(getContext());
        ImageView logo=new ImageView(getContext()); logo.setImageResource(R.drawable.neko_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER); logo.setContentDescription("Logo de Neko Player");
        position(header,logo,91,88,-3,10);
        TextView title=text("Neko Player",44,NekoStyle.INK,true);
        SpannableString brand=new SpannableString("Neko Player");
        brand.setSpan(new ForegroundColorSpan(NekoStyle.ACCENT),5,11,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(brand); position(header,title,475,58*getResources().getConfiguration().fontScale,100,11);
        TextView tagline=text("Música y videos, con un toque de ternura",20,NekoStyle.MUTED,false);
        float fs=getResources().getConfiguration().fontScale;
        position(header,tagline,485,40*fs,100,11+58*fs);
        FrameLayout button=new FrameLayout(getContext());
        button.setBackground(NekoStyle.shape(NekoStyle.SURFACE,40*u)); button.setElevation(3*u);
        NekoIcon icon=new NekoIcon(getContext(),NekoIcon.Kind.SETTINGS,NekoStyle.INK,false);
        LayoutParams iconLp=new LayoutParams(px(31),px(31),Gravity.CENTER); button.addView(icon,iconLp);
        LayoutParams buttonLp=new LayoutParams(px(66),px(66),Gravity.END|Gravity.TOP);
        buttonLp.topMargin=px(28); header.addView(button,buttonLp);
        NekoStyle.button(button,"Abrir ajustes",settings,40*u);
        return header;
    }

    private FrameLayout hero() {
        FrameLayout hero=new FrameLayout(getContext());
        hero.setBackground(NekoStyle.gradient(NekoStyle.SURFACE,NekoStyle.LILAC,34*u));
        hero.setClipToOutline(true); hero.setElevation(2*u);
        heroArt=new HeroArt(getContext()); heroArt.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        hero.addView(heroArt,new LayoutParams(-1,-1));
        TextView headline=text("Tu música,\na tu manera",49,NekoStyle.INK,true);
        SpannableString copy=new SpannableString("Tu música,\na tu manera");
        copy.setSpan(new ForegroundColorSpan(NekoStyle.ACCENT),12,copy.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        headline.setText(copy); headline.setLineSpacing(0,.98f); headline.setGravity(Gravity.TOP);
        float fs=getResources().getConfiguration().fontScale;
        boolean large=fs>1.15f;
        position(hero,headline,large ? 600 : 348,118*fs,30,large ? 32 : 72);
        TextView subtitle=text("Elige un archivo de tu teléfono\ny deja que el gatito baile contigo",21,NekoStyle.MUTED,false);
        subtitle.setGravity(Gravity.TOP); subtitle.setLineSpacing(px(4),1);
        position(hero,subtitle,large ? 600 : 337,85*fs,30,large ? 40+118*fs : 188);
        mascot=new ImageView(getContext()); mascot.setImageResource(R.drawable.neko_mascot);
        mascot.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mascot.setContentDescription("Gatito con audífonos bailando");
        position(hero,mascot,300,450,351,large ? 210*fs : 70);
        return hero;
    }

    private FrameLayout action(boolean music,Runnable onClick) {
        FrameLayout card=new FrameLayout(getContext());
        card.setBackground(music ? NekoStyle.gradient(NekoStyle.ACCENT_LIGHT,NekoStyle.ACCENT_DARK,31*u)
                : NekoStyle.shape(NekoStyle.SURFACE,31*u));
        card.setElevation(5*u);
        FrameLayout badge=new FrameLayout(getContext());
        badge.setBackground(music ? NekoStyle.gradient(0xFFD6B3FF,0xFFA04FF0,52*u)
                : NekoStyle.gradient(NekoStyle.LILAC,0xFFE6DDF9,52*u));
        badge.addView(new NekoIcon(getContext(),music ? NekoIcon.Kind.MUSIC : NekoIcon.Kind.VIDEO,
                music ? NekoStyle.ON_ACCENT : NekoStyle.ACCENT,true),
                new LayoutParams(px(50),px(50),Gravity.CENTER));
        LayoutParams badgeLp=new LayoutParams(px(100),px(100),Gravity.START|Gravity.CENTER_VERTICAL);
        badgeLp.leftMargin=px(28); card.addView(badge,badgeLp);
        LinearLayout copy=new LinearLayout(getContext()); copy.setOrientation(LinearLayout.VERTICAL);
        TextView title=text(music ? "Abrir música del teléfono" : "Abrir video del teléfono",28,
                music ? NekoStyle.ON_ACCENT : NekoStyle.INK,true);
        TextView subtitle=text(music ? "Reproduce tus canciones locales" : "Reproduce tus videos locales",22,
                music ? NekoStyle.ON_ACCENT_MUTED : NekoStyle.MUTED,false);
        copy.addView(title,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams subtitleLp=new LinearLayout.LayoutParams(-1,-2);
        subtitleLp.topMargin=px(7); copy.addView(subtitle,subtitleLp);
        LayoutParams copyLp=new LayoutParams(-1,-2,Gravity.CENTER_VERTICAL);
        copyLp.leftMargin=px(156); copyLp.rightMargin=px(78); card.addView(copy,copyLp);
        FrameLayout arrow=new FrameLayout(getContext());
        arrow.setBackground(NekoStyle.shape(music ? 0x22FFFFFF : NekoStyle.SURFACE,32*u));
        arrow.addView(new NekoIcon(getContext(),NekoIcon.Kind.CHEVRON,
                music ? NekoStyle.ON_ACCENT : NekoStyle.INK,false),
                new LayoutParams(px(24),px(24),Gravity.CENTER));
        LayoutParams arrowLp=new LayoutParams(px(60),px(60),Gravity.END|Gravity.CENTER_VERTICAL);
        arrowLp.rightMargin=px(23); card.addView(arrow,arrowLp);
        NekoStyle.button(card,music ? "Abrir música del teléfono" : "Abrir video del teléfono",onClick,31*u);
        hideChildrenFromAccessibility(card);
        return card;
    }

    private FrameLayout playback() {
        FrameLayout card=new FrameLayout(getContext());
        card.setBackground(NekoStyle.shape(NekoStyle.SURFACE,30*u));
        TextView eyebrow=text("REPRODUCCIÓN",19,NekoStyle.ACCENT,false);
        position(card,eyebrow,400,32,29,22);
        FrameLayout inner=new FrameLayout(getContext());
        inner.setBackground(NekoStyle.gradient(NekoStyle.LILAC,0xFFFAF7FF,24*u));
        float fs=getResources().getConfiguration().fontScale;
        LayoutParams innerLp=new LayoutParams(-1,px(130*Math.max(1,fs*fs))); innerLp.setMargins(px(24),px(64),px(24),0);
        card.addView(inner,innerLp);
        FrameLayout record=new FrameLayout(getContext());
        record.setBackground(NekoStyle.shape(NekoStyle.LILAC,24*u));
        record.addView(new NekoIcon(getContext(),NekoIcon.Kind.RECORD,0xFF987BCB,true),
                new LayoutParams(px(90),px(90),Gravity.CENTER));
        position(inner,record,159,130,0,0);
        LinearLayout copy=new LinearLayout(getContext()); copy.setOrientation(LinearLayout.VERTICAL);
        playbackTitle=text("Aún no hay nada reproduciéndose",24,NekoStyle.INK,true);
        playbackTitle.setMaxLines(2); playbackTitle.setEllipsize(TextUtils.TruncateAt.END);
        playbackSubtitle=text("Tus archivos permanecen en tu dispositivo",21,NekoStyle.MUTED,false);
        copy.addView(playbackTitle,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams subLp=new LinearLayout.LayoutParams(-1,-2);
        subLp.topMargin=px(6); copy.addView(playbackSubtitle,subLp);
        LayoutParams copyLp=new LayoutParams(-1,-2,Gravity.CENTER_VERTICAL);
        copyLp.leftMargin=px(183); copyLp.rightMargin=px(14); inner.addView(copy,copyLp);
        NekoStyle.button(inner,"Abrir reproductor. Aún no hay nada reproduciéndose",openPlayer,24*u);
        hideChildrenFromAccessibility(record);
        return card;
    }

    private FrameLayout sources() {
        FrameLayout card=new FrameLayout(getContext());
        card.setBackground(NekoStyle.gradient(0xFFE7DCFD,0xFFF2E6FC,30*u));
        card.setClipToOutline(true);
        SourceArt art=new SourceArt(getContext()); art.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        card.addView(art,new LayoutParams(-1,-1));
        LinearLayout copy=new LinearLayout(getContext()); copy.setOrientation(LinearLayout.VERTICAL);
        TextView title=text("Muy pronto más fuentes",21,NekoStyle.INK,true);
        TextView description=text("YouTube y Spotify podrán añadirse más adelante. Por ahora puedes reproducir archivos locales.",19,
                NekoStyle.MUTED,false); description.setLineSpacing(px(4),1);
        copy.addView(title,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams subLp=new LinearLayout.LayoutParams(-1,-2);
        subLp.topMargin=px(8); copy.addView(description,subLp);
        LayoutParams copyLp=new LayoutParams(-1,-2,Gravity.CENTER_VERTICAL);
        copyLp.leftMargin=px(310); copyLp.rightMargin=px(25); card.addView(copy,copyLp);
        return card;
    }

    private static void hideChildrenFromAccessibility(ViewGroup group) {
        for(int i=0;i<group.getChildCount();i++) {
            View child=group.getChildAt(i); child.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            if(child instanceof ViewGroup) hideChildrenFromAccessibility((ViewGroup)child);
        }
    }

    void setPlayback(String title,String subtitle,boolean hasSelection) {
        playbackTitle.setText(title); playbackSubtitle.setText(subtitle);
        View card=(View)playbackTitle.getParent().getParent();
        card.setContentDescription("Abrir reproductor. "+title+". "+subtitle);
        if(footerArt!=null) footerArt.playing=hasSelection;
    }

    void setResumed(boolean value) { resumed=value; updateMotion(); }

    private void updateMotion() {
        if(ambient!=null) { ambient.cancel(); ambient=null; }
        if(!resumed || !isAttachedToWindow() || getWindowVisibility()!=VISIBLE
                || !NekoStyle.motionEnabled(getContext())) {
            if(mascot!=null) { mascot.setTranslationY(0); mascot.setRotation(0); }
            return;
        }
        ambient=ValueAnimator.ofFloat(0,1); ambient.setDuration(4200);
        ambient.setRepeatCount(ValueAnimator.INFINITE);
        ambient.setInterpolator(new android.view.animation.LinearInterpolator());
        ambient.addUpdateListener(animation->{
            phase=(float)animation.getAnimatedValue();
            double wave=phase*Math.PI*2;
            mascot.setTranslationY((float)Math.sin(wave)*px(5));
            mascot.setRotation((float)Math.sin(wave)*1.7f);
            heroArt.phase=phase; heroArt.invalidate();
            footerArt.phase=phase; footerArt.invalidate();
        });
        ambient.start();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow(); updateMotion();
        if(NekoStyle.motionEnabled(getContext())) {
            for(int i=0;i<entrances.size();i++) {
                View view=entrances.get(i); view.setAlpha(0); view.setTranslationY(px(14));
                view.animate().alpha(1).translationY(0).setStartDelay(i*NekoStyle.STAGGER)
                        .setDuration(NekoStyle.ENTER).setInterpolator(new DecelerateInterpolator(1.5f)).start();
            }
        }
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility); if(mascot!=null) updateMotion();
    }

    @Override protected void onDetachedFromWindow() {
        if(ambient!=null) { ambient.cancel(); ambient=null; }
        for(View view:entrances) view.animate().cancel();
        super.onDetachedFromWindow();
    }

    private abstract class Art extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        float phase;
        Art(Context c) { super(c); }
        void fill(Canvas c,Path path,int color) {
            paint.setShader(null); paint.setStyle(Paint.Style.FILL); paint.setColor(color); c.drawPath(path,paint);
        }
        void sparkle(Canvas c,float x,float y,float size,int color) {
            Path p=new Path(); p.moveTo(x,y-size); p.quadTo(x+size*.18f,y-size*.2f,x+size,y);
            p.quadTo(x+size*.18f,y+size*.2f,x,y+size); p.quadTo(x-size*.18f,y+size*.2f,x-size,y);
            p.quadTo(x-size*.18f,y-size*.2f,x,y-size); p.close(); fill(c,p,color);
        }
        void note(Canvas c,float x,float y,float size,float rotation) {
            c.save(); c.translate(x,y); c.rotate(rotation); c.scale(size/32,size/32);
            NekoIcon.draw(c,NekoIcon.Kind.MUSIC,NekoStyle.NOTE,true,paint); c.restore();
        }
    }

    private final class HeroArt extends Art {
        HeroArt(Context c) { super(c); }
        @Override protected void onDraw(Canvas c) {
            c.save(); c.scale(getWidth()/662f,getHeight()/390f);
            Path p=new Path(); p.moveTo(380,76); p.cubicTo(441,-21,565,-1,662,0);
            p.lineTo(662,78); p.cubicTo(616,136,569,125,513,115);
            p.cubicTo(446,103,429,171,390,167); p.close();
            paint.setShader(new LinearGradient(390,0,662,150,0xFFE7E2FF,0xFFB6A9FD,Shader.TileMode.CLAMP));
            c.drawPath(p,paint); paint.setShader(null);
            p.reset(); p.moveTo(0,265); p.cubicTo(92,287,148,305,205,354);
            p.cubicTo(286,422,371,332,446,346); p.lineTo(662,390); p.lineTo(0,390); p.close();
            fill(c,p,0xFFCCC8FF);
            p.reset(); p.moveTo(0,334); p.cubicTo(114,204,171,359,267,388);
            p.cubicTo(354,418,395,305,464,343); p.cubicTo(568,411,581,273,662,319);
            p.lineTo(662,390); p.lineTo(0,390); p.close();
            paint.setShader(new LinearGradient(0,350,662,350,0xFFB9B0FF,0xFFEECDFF,Shader.TileMode.CLAMP));
            c.drawPath(p,paint); paint.setShader(null);
            p.reset(); p.moveTo(104,327); p.cubicTo(204,251,248,414,398,344);
            p.lineTo(440,390); p.lineTo(263,390); p.close(); fill(c,p,0x88FFC4F6);
            float drift=(float)Math.sin(phase*Math.PI*2)*5;
            note(c,327,44+drift,50,-12); note(c,604,147-drift,43,14);
            note(c,300,267-drift,47,-12); note(c,352,139+drift,22,-14);
            note(c,592,246+drift,22,15);
            sparkle(c,629,282,12,NekoStyle.NOTE); sparkle(c,367,326,11,NekoStyle.LAVENDER);
            sparkle(c,638,91,3,Color.WHITE); sparkle(c,73,367,2,Color.WHITE);
            paint.setColor(0x66FFFFFF); c.drawOval(new RectF(530,40,588,78),paint);
            paint.setColor(0xCCFFFFFF); c.drawCircle(534,72,9,paint);
            c.restore();
        }
    }

    private final class SourceArt extends Art {
        SourceArt(Context c) { super(c); }
        @Override protected void onDraw(Canvas c) {
            c.save(); c.scale(getWidth()/662f,getHeight()/168f);
            paint.setColor(0x55FFFFFF); paint.setStrokeWidth(5); c.drawLine(470,83,609,12,paint);
            paint.setStrokeWidth(1); c.drawLine(570,168,662,113,paint);
            tile(c,84,90,-16,NekoIcon.Kind.YOUTUBE,NekoStyle.RED);
            tile(c,214,87,16,NekoIcon.Kind.SPOTIFY,NekoStyle.GREEN);
            paint.setColor(Color.WHITE); c.drawCircle(151,94,18,paint);
            paint.setColor(NekoStyle.ACCENT); paint.setStrokeWidth(3); paint.setStrokeCap(Paint.Cap.ROUND);
            c.drawLine(145,94,157,94,paint); c.drawLine(151,88,151,100,paint);
            sparkle(c,54,36,9,NekoStyle.LAVENDER); sparkle(c,278,64,9,NekoStyle.LAVENDER);
            sparkle(c,38,125,9,NekoStyle.LAVENDER); c.restore();
        }
        private void tile(Canvas c,float x,float y,float angle,NekoIcon.Kind kind,int color) {
            c.save(); c.translate(x,y); c.rotate(angle);
            paint.setColor(Color.WHITE); c.drawRoundRect(new RectF(-40,-42,40,42),19,19,paint);
            c.translate(-23,-23); c.scale(1.45f,1.45f);
            NekoIcon.draw(c,kind,color,true,paint); c.restore();
        }
    }

    private final class FooterArt extends Art {
        boolean playing;
        FooterArt(Context c) { super(c); }
        @Override protected void onDraw(Canvas c) {
            c.save(); c.scale(getWidth()/662f,getHeight()/114f);
            Path p=new Path(); p.moveTo(-18,37); p.cubicTo(111,-7,167,110,268,69);
            p.cubicTo(359,31,390,126,488,94); p.cubicTo(602,59,627,17,680,51);
            p.lineTo(680,114); p.lineTo(-18,114); p.close(); fill(c,p,0xFFDFD6FC);
            p.reset(); p.moveTo(-18,70); p.cubicTo(121,162,191,31,326,100);
            p.cubicTo(467,153,544,77,680,109); p.lineTo(680,114); p.lineTo(-18,114);
            p.close(); fill(c,p,0xFFD0C3F8);
            for(int i=0;i<12;i++) {
                float height=12+(float)Math.abs(Math.sin(i*1.4+(playing ? phase*12 : 0)))*65;
                paint.setColor(0x99C7B1F8); c.drawRoundRect(new RectF(80+i*17,100-height,91+i*17,100),3,3,paint);
            }
            paint.setColor(0xFFBFB0F2);
            Path cat=new Path(); cat.moveTo(522,114); cat.lineTo(524,87);
            cat.cubicTo(513,70,520,43,525,31); cat.quadTo(527,25,542,41);
            cat.lineTo(559,39); cat.quadTo(574,18,578,31); cat.quadTo(585,51,587,64);
            cat.quadTo(610,86,589,106); cat.quadTo(639,62,655,95);
            cat.quadTo(662,113,661,114); cat.close(); c.drawPath(cat,paint);
            note(c,235,-7,35,17); note(c,410,38,28,15);
            sparkle(c,479,57,9,NekoStyle.LAVENDER);
            paint.setColor(0xFFDFD0FC); c.drawCircle(329,37,7,paint); c.restore();
        }
    }
}
