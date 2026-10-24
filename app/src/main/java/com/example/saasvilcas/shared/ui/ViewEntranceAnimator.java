package com.example.saasvilcas.shared.ui;

import android.animation.ValueAnimator;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.GridLayout;
import android.widget.ScrollView;
import androidx.core.view.OneShotPreDrawListener;
import java.util.ArrayList;
import java.util.List;

/** One entrance per screen creation; never delays rendering or waits for data. */
public final class ViewEntranceAnimator {
    private static final Interpolator EASE = new DecelerateInterpolator(1.4f);
    private ViewEntranceAnimator() { }

    public static void play(View root) {
        if (!enabled()) return;
        List<View> components = new ArrayList<>();
        collect(root, components, true);
        hide(components, root);
        OneShotPreDrawListener.add(root, () -> {
            // Read each position once instead of on every comparison.
            int count = components.size();
            long[] keys = new long[count];
            int[] location = new int[2];
            for (int i = 0; i < count; i++) {
                components.get(i).getLocationOnScreen(location);
                keys[i] = ((long) location[1] << 32) | (location[0] & 0xffffffffL);
            }
            Integer[] order = new Integer[count];
            for (int i = 0; i < count; i++) order[i] = i;
            java.util.Arrays.sort(order, (a, b) -> Long.compare(keys[a], keys[b]));
            for (int i = 0; i < count; i++) reveal(components.get(order[i]), i);
        });
    }

    /** Staggers views that were just added to an existing screen, in the order given. */
    public static void playItems(List<View> views) {
        if (!enabled() || views.isEmpty()) return;
        hide(views, views.get(0));
        View anchor = views.get(0);
        OneShotPreDrawListener.add(anchor, () -> {
            for (int i = 0; i < views.size(); i++) reveal(views.get(i), i);
        });
    }

    private static boolean enabled() {
        return Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled();
    }

    private static void hide(List<View> views, View reference) {
        float distance = -8 * reference.getResources().getDisplayMetrics().density;
        for (View view : views) {
            view.animate().cancel();
            view.setAlpha(0f);
            view.setTranslationY(distance);
        }
    }

    private static void reveal(View component, int index) {
        component.addOnAttachStateChangeListener(DETACH_LISTENER);
        // A hardware layer keeps the fade from redrawing every child of a card on each frame.
        component.animate().alpha(1f).translationY(0f)
                .setStartDelay(Math.min(index * 35L, 175L))
                .setDuration(240L).setInterpolator(EASE).withLayer()
                .withEndAction(() -> component.removeOnAttachStateChangeListener(DETACH_LISTENER))
                .start();
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
