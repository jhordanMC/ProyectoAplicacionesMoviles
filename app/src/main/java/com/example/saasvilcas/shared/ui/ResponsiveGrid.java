package com.example.saasvilcas.shared.ui;
import android.view.View;
import com.example.saasvilcas.R;
/** Use available container width, including split screen, rather than a device category. */
public final class ResponsiveGrid {
    private ResponsiveGrid() { }
    public static int columns(View grid,int gapDp) {
        float density=grid.getResources().getDisplayMetrics().density;
        int width=grid.getWidth();
        if(width==0) width=grid.getResources().getDisplayMetrics().widthPixels-2*grid.getResources().getDimensionPixelSize(R.dimen.page_gutter);
        width-=grid.getPaddingLeft()+grid.getPaddingRight();
        int gap=Math.round(gapDp*density),minimum=grid.getResources().getDimensionPixelSize(R.dimen.product_min_width);
        return Math.max(1,Math.min(4,(width+gap)/(minimum+gap)));
    }
}
