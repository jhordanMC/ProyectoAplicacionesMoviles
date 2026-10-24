package com.example.saasvilcas.shared.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.example.saasvilcas.R;

/** Lightweight presentation of API values. Empty charts never fall back to illustrative data. */
public class DashboardChartView extends View {
    public static final int[] SEGMENT_COLORS = {0xFF13AF35, 0xFFFFBD37, 0xFFFF333F, 0xFF04CED4};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // Reused across frames so drawing never allocates.
    private final RectF bounds = new RectF();
    private final Path line = new Path(), fill = new Path();
    private LinearGradient fillShader;
    private int shaderHeight = -1;
    private final int grid, axis, label, income, baseline, center;
    private boolean donut, bars;
    private double[] incomes = new double[0];
    private double[] values = new double[0];
    private float[] heights = new float[0];
    private float progress = 1f;
    private ValueAnimator reveal;
    public DashboardChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setTypeface(ResourcesCompat.getFont(context, R.font.inter));
        grid = ContextCompat.getColor(context, R.color.vl_chart_grid);
        axis = ContextCompat.getColor(context, R.color.vl_chart_axis);
        label = ContextCompat.getColor(context, R.color.vl_chart_label);
        income = ContextCompat.getColor(context, R.color.vl_chart_income);
        baseline = ContextCompat.getColor(context, R.color.vl_chart_baseline);
        center = ContextCompat.getColor(context, R.color.vl_chart_center);
    }
    public void showDonut(double[] values) { donut=true; bars=false; this.values=values.clone(); animateIn(); }
    public void showLine(double[] values) { donut=false; bars=false; this.values=values.clone(); animateIn(); }
    public void showBars(double[] incomes,double[] profits) { donut=false; bars=true; this.incomes=incomes.clone(); this.values=profits.clone(); animateIn(); }
    public static int segmentColor(int index) { return SEGMENT_COLORS[index % SEGMENT_COLORS.length]; }
    /** Grows the new values in once; skipped when system animations are off. */
    private void animateIn() {
        if (reveal != null) reveal.cancel();
        if (android.os.Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled()) { progress = 1f; invalidate(); return; }
        reveal = ValueAnimator.ofFloat(0f, 1f);
        reveal.setDuration(520); reveal.setInterpolator(new DecelerateInterpolator(1.6f));
        reveal.addUpdateListener(animation -> { progress = (float) animation.getAnimatedValue(); invalidate(); });
        reveal.start();
    }
    @Override protected void onDetachedFromWindow() {
        if (reveal != null) { reveal.cancel(); reveal = null; progress = 1f; }
        super.onDetachedFromWindow();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (donut) drawDonut(canvas); else if(bars) drawBars(canvas); else drawLine(canvas);
    }
    private void drawDonut(Canvas canvas) {
        float size = Math.min(getWidth(), getHeight());
        float centerX = getWidth()/2f, centerY = getHeight()/2f;
        float radius = size * .39f, thickness = size * .24f;
        bounds.set(centerX-radius, centerY-radius, centerX+radius, centerY+radius);
        double total=0; for(double value:values) total+=Math.max(0,value);
        float start = -90;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(thickness);
        if(total<=0) {
            paint.setColor(grid); canvas.drawArc(bounds,0,360,false,paint); return;
        }
        for (int i = 0; i < values.length; i++) {
            float sweep = (float)(Math.max(0,values[i])/total*360) * progress;
            paint.setColor(segmentColor(i));
            if(sweep>0) canvas.drawArc(bounds, start+Math.min(1,sweep/4), Math.max(0,sweep-Math.min(2,sweep/2)), false, paint);
            start += sweep;
        }
        if(progress<1f) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(center);
        paint.setTextSize(size * .075f);
        paint.setTextAlign(Paint.Align.CENTER);
        start = -90;
        for (double value : values) {
            float percentage=(float)(Math.max(0,value)/total*100);
            double angle = Math.toRadians(start + percentage * 1.8f);
            if(percentage>=8) canvas.drawText(Math.round(percentage)+"%", centerX+(float)Math.cos(angle)*radius,
                    centerY+(float)Math.sin(angle)*radius+size*.025f, paint);
            start += percentage*3.6f;
        }
    }
    private void drawBars(Canvas canvas) {
        if(values.length==0) return;
        double rawMax=1,rawMin=0;
        for(int i=0;i<values.length;i++) { rawMax=Math.max(rawMax,Math.max(values[i],incomes[i])); rawMin=Math.min(rawMin,Math.min(values[i],incomes[i])); }
        double rough=(rawMax-rawMin)/4, magnitude=Math.pow(10,Math.floor(Math.log10(rough))), normalized=rough/magnitude;
        double step=(normalized<1.5?1:normalized<3?2:normalized<7?5:10)*magnitude;
        double max=Math.ceil(rawMax/step)*step,min=Math.floor(rawMin/step)*step,range=max-min;
        float density=getResources().getDisplayMetrics().density, left=64*density, top=12*density, bottom=getHeight()-12*density;
        float height=bottom-top, column=(getWidth()-left)/values.length;
        paint.setStyle(Paint.Style.FILL); paint.setTextSize(10*getResources().getDisplayMetrics().scaledDensity);
        paint.setTextAlign(Paint.Align.RIGHT);
        for(double tick=max;tick>=min-step*.01;tick-=step) {
            float y=top+(float)((max-tick)/range)*height;
            paint.setColor(grid); canvas.drawRect(left,y,getWidth(),y+density,paint);
            paint.setColor(label); canvas.drawText("S/"+String.format(java.util.Locale.US,"%.0f",tick),left-6*density,y+4*density,paint);
        }
        float zero=top+(float)(max/range)*height;
        paint.setColor(axis); canvas.drawRect(left,zero,getWidth(),zero+1.5f*density,paint);
        for(int i=0;i<values.length;i++) {
            for(int j=0;j<2;j++) {
                double value=(j==0?incomes[i]:values[i])*progress;
                float y=top+(float)((max-value)/range)*height;
                float x=left+column*(i+(j==0?.22f:.52f));
                paint.setColor(j==0?income:0xffDDA500);
                canvas.drawRoundRect(x,Math.min(zero,y),x+column*.26f,Math.max(zero,y),4*density,4*density,paint);
            }
        }
    }
    private void drawLine(Canvas canvas) {
        if(values.length==0) return;
        float w = getWidth(), h = getHeight(),inset=6*getResources().getDisplayMetrics().density;
        double min=0,max=0;
        for(double value:values) { min=Math.min(min,value); max=Math.max(max,value); }
        double range=max-min; if(range==0) range=1;
        if(heights.length!=values.length) heights=new float[values.length];
        float baselineY=inset+(float)(max/range)*(h-inset*2);
        for(int i=0;i<values.length;i++) {
            float target=inset+(float)((max-values[i])/range)*(h-inset*2);
            heights[i]=baselineY+(target-baselineY)*progress;
        }
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(getResources().getDisplayMetrics().density);
        paint.setColor(baseline); canvas.drawLine(inset,baselineY,w-inset,baselineY,paint);
        line.rewind();
        float x0=values.length==1?w/2:inset, y0=heights[0];
        line.moveTo(x0,y0);
        for (int i=1;i<values.length;i++) {
            float x=inset+(w-inset*2)*i/(values.length-1), y=heights[i];
            float previousX=inset+(w-inset*2)*(i-1)/(values.length-1);
            float previousY=heights[i-1];
            line.cubicTo((previousX+x)/2, previousY, (previousX+x)/2,y,x,y);
        }
        fill.set(line);
        fill.lineTo(values.length==1?x0:w-inset,baselineY); fill.lineTo(x0,baselineY); fill.close();
        if(fillShader==null || shaderHeight!=(int)h) {
            fillShader=new LinearGradient(0,0,0,h,0x9930BE77,0x0030BE77, Shader.TileMode.CLAMP); shaderHeight=(int)h;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(fillShader);
        canvas.drawPath(fill,paint);
        paint.setShader(null); paint.setColor(0xFF149557);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2*getResources().getDisplayMetrics().density);
        canvas.drawPath(line,paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x0,y0,3*getResources().getDisplayMetrics().density,paint);
        if(values.length>1) canvas.drawCircle(w-inset,heights[values.length-1],3*getResources().getDisplayMetrics().density,paint);
    }
}
