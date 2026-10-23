package com.example.saasvilcas.shared.ui;

import android.animation.ValueAnimator;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.ScrollView;
import androidx.core.view.OneShotPreDrawListener;
import java.util.ArrayList;
import java.util.List;

/** One entrance per screen creation; never delays rendering or waits for data. */
public final class ViewEntranceAnimator {
    private ViewEntranceAnimator() { }

    public static void play(View root) {
        if (Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled()) return;
        List<View> components = new ArrayList<>();
        collect(root, components, true);
        float distance = -8 * root.getResources().getDisplayMetrics().density;
        for (View component : components) {
            component.setAlpha(0f);
            component.setTranslationY(distance);
        }
        OneShotPreDrawListener.add(root, () -> {
            components.sort((first, second) -> {
                int[] a = new int[2], b = new int[2];
                first.getLocationOnScreen(a); second.getLocationOnScreen(b);
                int vertical = Integer.compare(a[1], b[1]);
                return vertical != 0 ? vertical : Integer.compare(a[0], b[0]);
            });
            int index = 0;
            for (View component : components) {
                component.addOnAttachStateChangeListener(DETACH_LISTENER);
                component.animate().alpha(1f).translationY(0f)
                        .setStartDelay(Math.min(index++ * 35L, 175L))
                        .setDuration(240L).setInterpolator(new DecelerateInterpolator())
                        .withEndAction(() -> component.removeOnAttachStateChangeListener(DETACH_LISTENER))
                        .start();
            }
        });
    }

    private static void collect(View view, List<View> result, boolean root) {
        if (view.getVisibility() != View.VISIBLE) return;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            if (root || view instanceof ScrollView || view instanceof GridLayout || (group.getChildCount() == 1 && view.getBackground() == null)
                    || (view instanceof android.widget.LinearLayout && view.getLayoutParams() != null
                    && view.getLayoutParams().height == ViewGroup.LayoutParams.MATCH_PARENT)
                    || (view.getBackground() == null && (view instanceof android.widget.FrameLayout
                    || view instanceof android.widget.LinearLayout))) {
                for (int i = 0; i < group.getChildCount(); i++)
                    collect(group.getChildAt(i), result, view instanceof ScrollView);
                return;
            }
        }
        result.add(view);
    }

    private static final View.OnAttachStateChangeListener DETACH_LISTENER = new View.OnAttachStateChangeListener() {
        @Override public void onViewAttachedToWindow(View view) { }
        @Override public void onViewDetachedFromWindow(View view) {
            view.animate().cancel();
            view.setAlpha(1f); view.setTranslationY(0f);
            view.removeOnAttachStateChangeListener(this);
        }
    };
}
