package com.example.saasvilcas.shared.ui;

import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.saasvilcas.R;

/** Common modal shell: brand, rounded surface, dimmed backdrop and close action. */
public abstract class BrandedModalFragment extends DialogFragment {
    private boolean closing;
    private int lastAvailableWidth=-1,lastAvailableHeight=-1;
    private android.view.ViewTreeObserver.OnGlobalLayoutListener resizeListener;
    protected abstract int contentLayout();
    protected boolean fullScreen() { return false; }
    protected int formTitleId() { return 0; }
    protected int[] fixedActionIds() { return new int[0]; }
    @Override public void show(androidx.fragment.app.FragmentManager manager,String tag) {
        if(!fullScreen()) { super.show(manager,tag); return; }
        setShowsDialog(false);
        manager.beginTransaction().setReorderingAllowed(true)
                .add(R.id.fullscreenFormHost,this,tag).commit();
    }
    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if(fullScreen()) setShowsDialog(false);
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state) {
        super.onViewCreated(view,state);
        if(!fullScreen()) return;
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                View drawer=requireActivity().findViewById(R.id.companyDrawerOverlay);
                if(drawer!=null && drawer.getVisibility()==View.VISIBLE) {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                } else onCloseRequested();
            }
        });
    }
    protected boolean showHeaderBrand() { return true; }
    protected boolean showCloseIcon() { return true; }
    protected int surfaceBackground() { return R.drawable.modal_bg_surface; }
    protected void onCloseRequested() { dismiss(); }
    protected float widthFraction() { return .9f; }
    protected float heightFraction() { return .85f; }
    protected int maxWidthDp() { return 420; }
    protected int horizontalInsetDp() { return 0; }
    protected abstract void bindContent(View content);
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,@Nullable Bundle state) {
        View shell=inflater.inflate(fullScreen()?R.layout.modal_fullscreen_shell:R.layout.modal_shell,container,false);
        if(!fullScreen()) {
            shell.setBackgroundResource(surfaceBackground());
            shell.findViewById(R.id.modalBrandHeader).setVisibility(showHeaderBrand()?View.VISIBLE:View.GONE);
        }
        LinearLayout body=shell.findViewById(R.id.modalBody);
        View content=inflater.inflate(contentLayout(),body,false);
        body.addView(content);
        shell.findViewById(R.id.modalCloseIcon).setOnClickListener(v->onCloseRequested());
        shell.findViewById(R.id.modalCloseIcon).setVisibility(showCloseIcon()?View.VISIBLE:View.GONE);
        bindContent(content);
        if(!fullScreen() && fixedActionIds().length>0) {
            LinearLayout actions=shell.findViewById(R.id.modalPopupActions);
            for(int id:fixedActionIds()) {
                View action=content.findViewById(id); if(action==null) continue;
                ViewGroup previous=(ViewGroup)action.getParent(); previous.removeView(action);
                if(previous.getChildCount()==0) previous.setVisibility(View.GONE);
                actions.addView(action);
                if(getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                    LinearLayout.LayoutParams button=new LinearLayout.LayoutParams(0,Math.round(48*getResources().getDisplayMetrics().density),1f);
                    if(actions.getChildCount()>1) button.setMarginStart(Math.round(8*getResources().getDisplayMetrics().density));
                    action.setLayoutParams(button);
                }
            }
            actions.setVisibility(actions.getChildCount()==0?View.GONE:View.VISIBLE);
        }
        if(fullScreen()) {
            TextView title=formTitleId()==0?null:content.findViewById(formTitleId());
            if(title!=null) {
                ((TextView)shell.findViewById(R.id.modalFullscreenTitle)).setText(title.getText());
                title.setVisibility(View.GONE);
            }
            LinearLayout actions=shell.findViewById(R.id.modalFullscreenActions);
            for(int id:fixedActionIds()) {
                View action=content.findViewById(id);
                if(action==null) continue;
                ((ViewGroup)action.getParent()).removeView(action);
                actions.addView(action);
            }
            if(actions.getChildCount()>0) actions.setVisibility(View.VISIBLE);
        }
        applyTypography(shell, ResourcesCompat.getFont(requireContext(), R.font.inter));
        ViewEntranceAnimator.play(fullScreen() ? shell : body);
        return shell;
    }
    private void applyTypography(View view, Typeface font) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            int style = text.getTypeface() == null ? Typeface.NORMAL : text.getTypeface().getStyle();
            text.setTypeface(Typeface.create(font, style));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) applyTypography(group.getChildAt(i), font);
        }
    }
    @Override public void onStart() {
        super.onStart();
        if(!getShowsDialog()) return;
        Window window=requireDialog().getWindow();
        if(window==null) return;
        // Animate the content, not the system surface: a hidden window can still take focus.
        window.setWindowAnimations(0);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params=window.getAttributes();
        params.dimAmount=.55f;
        window.setAttributes(params);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        lastAvailableWidth=-1; lastAvailableHeight=-1;
        resizeModal();
        resizeListener=this::resizeModal;
        window.getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(resizeListener);
        View content=requireView();
        closing=false;
        if(motionEnabled()) {
            content.setAlpha(0f);
            content.setScaleX(.96f); content.setScaleY(.96f);
            content.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(220).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        }
    }
    private void resizeModal() {
        if(getView()==null||getDialog()==null||getDialog().getWindow()==null) return;
        View host=requireActivity().getWindow().getDecorView();
        int availableWidth=host.getWidth(),availableHeight=host.getHeight();
        if(android.os.Build.VERSION.SDK_INT>=30) {
            android.graphics.Rect bounds=requireActivity().getWindowManager().getCurrentWindowMetrics().getBounds();
            availableWidth=bounds.width(); availableHeight=bounds.height();
        }
        androidx.core.view.WindowInsetsCompat insets=androidx.core.view.ViewCompat.getRootWindowInsets(host);
        if(insets!=null) {
            androidx.core.graphics.Insets safe=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()|androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            availableWidth-=safe.left+safe.right;
            availableHeight-=safe.top+Math.max(safe.bottom,insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom);
        }
        android.graphics.Rect visible=new android.graphics.Rect(); host.getWindowVisibleDisplayFrame(visible);
        if(visible.height()>0) availableHeight=Math.min(availableHeight,visible.height());
        if(availableWidth<=0||availableHeight<=0) return;
        if(availableWidth==lastAvailableWidth && availableHeight==lastAvailableHeight) return;
        lastAvailableWidth=availableWidth; lastAvailableHeight=availableHeight;
        float density=getResources().getDisplayMetrics().density;
        boolean landscape=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        int width=Math.min((int)(availableWidth*(landscape?.94f:widthFraction())),(int)((landscape?Math.max(720,maxWidthDp()):maxWidthDp())*density));
        width=Math.max(1,Math.min(width,availableWidth-(int)(2*horizontalInsetDp()*density)));
        int maxHeight=Math.max(1,(int)(availableHeight*(landscape?.94f:heightFraction())));
        View content=requireView();
        content.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(maxHeight,View.MeasureSpec.AT_MOST));
        Window window=getDialog().getWindow(); int height=Math.min(content.getMeasuredHeight(),maxHeight);
        if(window.getAttributes().width!=width||window.getAttributes().height!=height) window.setLayout(width,height);
    }
    protected final void refreshModalSize() {
        lastAvailableWidth=-1; lastAvailableHeight=-1;
        if(getView()!=null && getDialog()!=null) resizeModal();
    }
    private boolean motionEnabled() {
        return android.os.Build.VERSION.SDK_INT<26 || android.animation.ValueAnimator.areAnimatorsEnabled();
    }
    @Override public void dismiss() {
        View content=getView();
        if(!getShowsDialog() || content==null || !content.isAttachedToWindow() || !motionEnabled()) {
            super.dismiss(); return;
        }
        if(closing) return;
        closing=true;
        content.animate().cancel();
        content.animate().alpha(0f).scaleX(.96f).scaleY(.96f).setDuration(150)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(()->BrandedModalFragment.super.dismiss()).start();
    }
    @Override public void onStop() {
        if(resizeListener!=null&&getDialog()!=null&&getDialog().getWindow()!=null) {
            getDialog().getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(resizeListener); resizeListener=null;
        }
        View content=getView();
        if(content!=null) {
            content.animate().cancel(); content.setAlpha(1f);
            content.setScaleX(1f); content.setScaleY(1f);
        }
        super.onStop();
    }
}

