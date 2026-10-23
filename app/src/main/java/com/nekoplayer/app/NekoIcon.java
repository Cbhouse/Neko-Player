package com.nekoplayer.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Resolution-independent icons: no emoji, font glyphs, or screenshot hit areas. */
final class NekoIcon extends View {
    enum Kind { HOME, PLAY, SETTINGS, MUSIC, VIDEO, CHEVRON, RECORD, YOUTUBE, SPOTIFY }
    private final Kind kind;
    private final int color;
    private final boolean filled;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    NekoIcon(Context context, Kind kind, int color, boolean filled) {
        super(context); this.kind = kind; this.color = color; this.filled = filled;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        float scale = Math.min(getWidth(), getHeight()) / 32f;
        canvas.translate((getWidth()-32*scale)/2, (getHeight()-32*scale)/2);
        canvas.scale(scale, scale); draw(canvas, kind, color, filled, p); canvas.restore();
    }

    static void draw(Canvas c, Kind kind, int color, boolean filled, Paint p) {
        p.reset(); p.setAntiAlias(true); p.setColor(color); p.setStrokeWidth(2.6f);
        p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
        p.setStyle(Paint.Style.STROKE);
        Path path = new Path();
        switch (kind) {
            case HOME:
                path.moveTo(5,14); path.lineTo(16,4); path.lineTo(27,14);
                path.lineTo(27,27); path.quadTo(27,29,25,29); path.lineTo(20,29);
                path.lineTo(20,20); path.quadTo(16,17,12,20); path.lineTo(12,29);
                path.lineTo(7,29); path.quadTo(5,29,5,27); path.close();
                p.setStyle(filled ? Paint.Style.FILL : Paint.Style.STROKE); c.drawPath(path,p); break;
            case PLAY:
                path.moveTo(10,5); path.quadTo(8,4,8,7); path.lineTo(8,25);
                path.quadTo(8,28,11,26); path.lineTo(26,17);
                path.quadTo(28,16,25,14); path.close();
                p.setStyle(filled ? Paint.Style.FILL : Paint.Style.STROKE); c.drawPath(path,p); break;
            case CHEVRON:
                path.moveTo(12,7); path.lineTo(21,16); path.lineTo(12,25); c.drawPath(path,p); break;
            case SETTINGS:
                for (int i=0;i<64;i++) {
                    double a = Math.PI*2*i/64 - Math.PI/2;
                    float r = (i%8 < 3) ? 13 : 10.6f;
                    float x=16+(float)Math.cos(a)*r, y=16+(float)Math.sin(a)*r;
                    if(i==0) path.moveTo(x,y); else path.lineTo(x,y);
                }
                path.close(); c.drawPath(path,p); c.drawCircle(16,16,4.1f,p); break;
            case MUSIC:
                p.setStyle(Paint.Style.FILL);
                c.drawOval(new RectF(2,21,12,29),p); c.drawOval(new RectF(20,17,30,25),p);
                c.drawRoundRect(new RectF(10,5,13,25),1,1,p);
                c.drawRoundRect(new RectF(27,2,30,21),1,1,p);
                path.moveTo(10,5); path.lineTo(30,2); path.lineTo(30,9); path.lineTo(10,12);
                path.close(); c.drawPath(path,p); break;
            case VIDEO:
            case YOUTUBE:
                p.setStyle(Paint.Style.FILL); c.drawRoundRect(new RectF(1,5,31,27),6,6,p);
                p.setColor(NekoStyle.SURFACE); path.moveTo(13,10); path.lineTo(23,16);
                path.lineTo(13,22); path.close(); c.drawPath(path,p); break;
            case SPOTIFY:
                p.setStyle(Paint.Style.FILL); c.drawCircle(16,16,15,p);
                p.setColor(NekoStyle.SURFACE); p.setStyle(Paint.Style.STROKE);
                for(int i=0;i<3;i++) {
                    p.setStrokeWidth(2.4f-i*.35f); path.reset(); path.moveTo(7+i,11+i*5);
                    path.quadTo(16,7+i*5,26-i,13+i*5); c.drawPath(path,p);
                } break;
            case RECORD:
                p.setColor(NekoStyle.LAVENDER); p.setStrokeWidth(2.4f);
                c.drawOval(new RectF(1,18,31,30),p); c.drawOval(new RectF(5,20,27,28),p);
                p.setStyle(Paint.Style.FILL); c.drawOval(new RectF(12,22,20,26),p);
                p.setColor(color); c.drawOval(new RectF(13,10,19,15),p);
                c.drawRoundRect(new RectF(17,2,19,13),1,1,p);
                path.moveTo(17,2); path.lineTo(23,5); path.lineTo(22,7); path.lineTo(17,5);
                path.close(); c.drawPath(path,p); break;
        }
    }
}
