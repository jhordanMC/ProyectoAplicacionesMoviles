package com.example.saasvilcas.shared.ui;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.animation.OvershootInterpolator;

/** Clipped mint liquid rises behind the digit, matching the web OTP fill. */
final class OtpLiquidDrawable extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private float level;
    private boolean filled;
    private ValueAnimator animator;
    OtpLiquidDrawable(float density) { this.density=density; }
    void setFilled(boolean value) {
        if(value==filled) return;
        filled=value; stop();
        if((android.os.Build.VERSION.SDK_INT>=26 && !ValueAnimator.areAnimatorsEnabled())) { level=value?1:0; invalidateSelf(); return; }
        animator=ValueAnimator.ofFloat(level,value?1:0);
        animator.setDuration(420); animator.setInterpolator(new OvershootInterpolator(.6f));
        animator.addUpdateListener(a->{level=(float)a.getAnimatedValue(); invalidateSelf();});
        animator.start();
    }
    void stop() { if(animator!=null) { animator.cancel(); animator=null; } }
    void settle() { stop(); level=filled?1:0; invalidateSelf(); }
    @Override public boolean isStateful() { return true; }
    @Override protected boolean onStateChange(int[] states) { invalidateSelf(); return true; }
    @Override public void draw(Canvas canvas) {
        RectF box=new RectF(getBounds()); box.inset(density,density);
        float radius=14*density;
        paint.setShader(null); paint.setStyle(Paint.Style.FILL); paint.setColor(0xffF8FDFB);
        canvas.drawRoundRect(box,radius,radius,paint);
        if(level>0) {
            Path clip=new Path(); clip.addRoundRect(box,radius,radius,Path.Direction.CW);
            int save=canvas.save(); canvas.clipPath(clip);
            float surface=box.bottom-box.height()*level*1.2f;
            Path wave=new Path(); wave.moveTo(box.left,surface);
            wave.cubicTo(box.left+box.width()*.3f,surface-8*density,box.left+box.width()*.7f,surface+8*density,box.right,surface);
            wave.lineTo(box.right,box.bottom); wave.lineTo(box.left,box.bottom); wave.close();
            paint.setShader(new LinearGradient(0,box.top,0,box.bottom,0xff64D59C,0xff3DB87A,Shader.TileMode.CLAMP));
            canvas.drawPath(wave,paint); canvas.restoreToCount(save);
        }
        boolean focused=false;
        for(int state:getState()) if(state==android.R.attr.state_focused) focused=true;
        paint.setShader(null); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth((focused?2:1.5f)*density);
        paint.setColor(filled?0xff3DB87A:focused?0xff64D59C:0xffADE4CF);
        canvas.drawRoundRect(box,radius,radius,paint); paint.setStyle(Paint.Style.FILL);
    }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}

