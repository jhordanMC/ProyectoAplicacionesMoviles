package com.example.saasvilcas.shared.ui;

import android.view.View;
import android.view.animation.PathInterpolator;

/** Coordinates the drawer and scrim without putting animation logic in navigation. */
public final class SidebarAnimator {
    // Matches shared/sidebar/sidebar.css and styles/tokens.css in the web frontend.
    private static final long DRAWER_DURATION_MS = 280;
    private static final long BACKDROP_DURATION_MS = 200;
    private static final PathInterpolator DRAWER_EASING = new PathInterpolator(.22f, .8f, .35f, 1f);
    private static final PathInterpolator BACKDROP_EASING = new PathInterpolator(.25f, .1f, .25f, 1f);
    private final View overlay, drawer, scrim, openButton, closeButton;
    private boolean opening;
    private boolean disposed;
    private int transition;
    private java.util.function.Consumer<Boolean> visibilityChanged = visible -> { };
    public void setVisibilityListener(java.util.function.Consumer<Boolean> listener) { visibilityChanged=listener; }

    public SidebarAnimator(View overlay, View drawer, View scrim, View openButton, View closeButton) {
        this.overlay = overlay;
        this.drawer = drawer;
        this.scrim = scrim;
        this.openButton = openButton;
        this.closeButton = closeButton;
    }

    public void open() {
        if (disposed || opening) return;
        opening = true;
        visibilityChanged.accept(true);
        int current = ++transition;
        cancelAnimations();
        boolean wasHidden = overlay.getVisibility() != View.VISIBLE;
        if (wasHidden) {
            drawer.setTranslationX(hiddenOffset());
            scrim.setAlpha(0f);
            overlay.setVisibility(View.VISIBLE);
        }
        // Wait for measurement when opening the initially hidden drawer.
        overlay.post(() -> {
            if (disposed || current != transition) return;
            if (wasHidden) drawer.setTranslationX(hiddenOffset());
            drawer.animate().translationX(0f).setDuration(DRAWER_DURATION_MS)
                    .setInterpolator(DRAWER_EASING)
                    .withEndAction(() -> {
                        if (!disposed && current == transition) closeButton.requestFocus();
                    }).start();
            scrim.animate().alpha(1f).setDuration(BACKDROP_DURATION_MS)
                    .setInterpolator(BACKDROP_EASING).start();
        });
    }

    public void close() {
        if (disposed || !opening) return;
        opening = false;
        int current = ++transition;
        cancelAnimations();
        drawer.animate().translationX(hiddenOffset()).setDuration(DRAWER_DURATION_MS)
                .setInterpolator(DRAWER_EASING)
                .withEndAction(() -> {
                    if (disposed || current != transition) return;
                    overlay.setVisibility(View.GONE);
                    visibilityChanged.accept(false);
                    openButton.requestFocus();
                }).start();
        // The web backdrop is removed immediately by *ngIf when mobileOpen becomes false.
        scrim.setAlpha(0f);
    }

    private float hiddenOffset() {
        int width = drawer.getWidth() > 0 ? drawer.getWidth() : drawer.getLayoutParams().width;
        return drawer.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? width : -width;
    }

    private void cancelAnimations() {
        drawer.animate().withEndAction(null);
        drawer.animate().cancel();
        scrim.animate().cancel();
    }

    public void dispose() {
        disposed = true;
        transition++;
        cancelAnimations();
    }
}
