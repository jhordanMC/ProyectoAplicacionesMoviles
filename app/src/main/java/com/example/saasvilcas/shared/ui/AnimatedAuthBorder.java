package com.example.saasvilcas.shared.ui;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

/** Rotates the conic color field, leaving the card and its contents still. */
public final class AnimatedAuthBorder extends Drawable implements android.graphics.drawable.Animatable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix rotation=new Matrix();
    private final float radius;
    private ValueAnimator animator;
    private float angle;
    public AnimatedAuthBorder(float density) { radius=28*density; }
    @Override protected void onBoundsChange(Rect bounds) {
        paint.setShader(new SweepGradient(bounds.exactCenterX(),bounds.exactCenterY(),
                new int[]{0xff64D59C,0xff3DB87A,0xffDA9E0B,0xff64D59C},null));
    }
    @Override public void draw(Canvas canvas) {
        Rect bounds=getBounds();
        rotation.setRotate(angle,bounds.exactCenterX(),bounds.exactCenterY());
        if(paint.getShader()!=null) paint.getShader().setLocalMatrix(rotation);
        canvas.drawRoundRect(new RectF(bounds),radius,radius,paint);
    }
    @Override public void start() {
        if(isRunning() || (android.os.Build.VERSION.SDK_INT>=26 && !ValueAnimator.areAnimatorsEnabled())) return;
        animator=ValueAnimator.ofFloat(angle,angle+360);
        animator.setDuration(4000); animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(value->{ angle=(float)value.getAnimatedValue(); invalidateSelf(); });
        animator.start();
    }
    @Override public void stop() { if(animator!=null) { animator.cancel(); animator=null; } }
    @Override public boolean isRunning() { return animator!=null && animator.isRunning(); }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}

