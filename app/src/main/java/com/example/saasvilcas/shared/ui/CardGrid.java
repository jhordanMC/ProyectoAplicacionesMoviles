package com.example.saasvilcas.shared.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import java.util.ArrayList;
import java.util.List;

/** Re-binds pooled cards instead of inflating the whole grid on every update; only new cards animate in. */
public final class CardGrid {
    public interface Binder<T> { void bind(View card,T item,int index); }
    private final GridLayout grid;
    private final int layout;
    private final List<View> pool=new ArrayList<>();
    private int shown;
    public CardGrid(GridLayout grid,int layout) { this.grid=grid; this.layout=layout; }
    public <T> void render(List<T> items,int columns,int sideGap,int bottomGap,boolean animate,Binder<T> binder) {
        int previous=shown;
        grid.removeAllViews(); grid.setColumnCount(columns);
        LayoutInflater inflater=LayoutInflater.from(grid.getContext());
        List<View> added=new ArrayList<>();
        for(int i=0;i<items.size();i++) {
            View card;
            if(i<pool.size()) card=pool.get(i);
            else { card=inflater.inflate(layout,grid,false); pool.add(card); }
            // A re-render can arrive before a pending entrance runs; never leave a pooled card hidden.
            card.animate().cancel(); card.setAlpha(1f); card.setTranslationY(0f);
            binder.bind(card,items.get(i),i);
            GridLayout.LayoutParams params=new GridLayout.LayoutParams();
            params.width=0; params.height=GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec=GridLayout.spec(i%columns,1f); params.rowSpec=GridLayout.spec(i/columns);
            params.setMargins(i%columns==0?0:sideGap,0,i%columns==columns-1?0:sideGap,bottomGap);
            grid.addView(card,params);
            if(i>=previous) added.add(card);
        }
        shown=items.size();
        if(animate) ViewEntranceAnimator.playItems(added);
    }
}
