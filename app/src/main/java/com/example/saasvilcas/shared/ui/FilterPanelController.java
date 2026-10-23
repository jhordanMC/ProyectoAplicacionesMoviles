package com.example.saasvilcas.shared.ui;

import android.animation.*;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.*;
import com.example.saasvilcas.R;
import java.util.*;

/** Collapsing changes presentation only: selected filter values and endpoint requests stay intact. */
public final class FilterPanelController {
    public static final class State extends ViewModel {
        private final Map<String,Boolean> expanded=new HashMap<>();
    }
    private final ViewGroup body;
    private final View toggle,chevron;
    private final TextView label;
    private final State state;
    private final String key;
    private boolean expanded;
    private ValueAnimator height;
    private int generation;
    private List<View> steps=Collections.emptyList();
    public static void bind(Fragment fragment,View root,String key) {
        State state=new ViewModelProvider(fragment.requireParentFragment()).get(State.class);
        FilterPanelController controller=new FilterPanelController(root,state,key);
        controller.toggle.setOnClickListener(v->{
            if(controller.expanded && controller.body.findFocus()!=null) {
                controller.body.findFocus().clearFocus();
                new androidx.core.view.WindowInsetsControllerCompat(fragment.requireActivity().getWindow(),root)
                    .hide(androidx.core.view.WindowInsetsCompat.Type.ime());
            }
            controller.setExpanded(!controller.expanded,true);
        });
    }
    private FilterPanelController(View root,State state,String key) {
        this.state=state; this.key=key;
        body=root.findViewById(R.id.filterPanelBody); toggle=root.findViewById(R.id.filterPanelToggle);
        chevron=root.findViewById(R.id.filterPanelChevron);label=root.findViewById(R.id.filterPanelLabel);
        expanded=state.expanded.getOrDefault(key,true); setExpanded(expanded,false);
        body.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View view) { }
            public void onViewDetachedFromWindow(View view) { cancel(); resetSteps(); body.getLayoutParams().height=ViewGroup.LayoutParams.WRAP_CONTENT; }
        });
    }
    private void collect(View view,List<View> result) {
        if(view.getVisibility()!=View.VISIBLE) return;
        if(view instanceof Spinner || view instanceof EditText || view instanceof TextView || view instanceof com.google.android.material.chip.ChipGroup) {result.add(view);return;}
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) collect(((ViewGroup)view).getChildAt(i),result);
    }
    private void cancel() {
        generation++; if(height!=null) {height.cancel();height=null;}
        chevron.animate().cancel(); for(View step:steps) step.animate().cancel();
    }
    private void resetSteps() { for(View step:steps) {step.animate().setStartDelay(0);step.setAlpha(1);step.setTranslationY(0);} }
    private void setExpanded(boolean value,boolean animate) {
        cancel(); expanded=value;state.expanded.put(key,value);
        label.setText(value?R.string.filter_panel_hide:R.string.filter_panel_show);
        toggle.setContentDescription(label.getText());
        androidx.core.view.ViewCompat.setStateDescription(toggle,toggle.getResources().getString(value?R.string.filter_panel_expanded:R.string.filter_panel_collapsed));
        int availableWidth=body.getWidth();
        if(availableWidth==0 && body.getParent() instanceof ViewGroup) {
            ViewGroup parent=(ViewGroup)body.getParent();
            availableWidth=parent.getWidth()-parent.getPaddingLeft()-parent.getPaddingRight();
        }
        if(!animate || availableWidth<=0 || (android.os.Build.VERSION.SDK_INT>=26 && !ValueAnimator.areAnimatorsEnabled())) {
            resetSteps(); body.getLayoutParams().height=ViewGroup.LayoutParams.WRAP_CONTENT;
            body.setVisibility(value?View.VISIBLE:View.GONE);body.requestLayout();chevron.setRotation(value?180:0);return;
        }
        boolean wasGone=body.getVisibility()==View.GONE;
        int current=wasGone?0:body.getHeight();
        body.setVisibility(View.VISIBLE);
        body.measure(View.MeasureSpec.makeMeasureSpec(availableWidth,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        int target=value?body.getMeasuredHeight():0;
        List<View> controls=new ArrayList<>();collect(body,controls);steps=controls;
        float distance=8*body.getResources().getDisplayMetrics().density;
        for(int i=0;i<steps.size();i++) {
            View step=steps.get(i);
            if(value && wasGone) {step.setAlpha(0);step.setTranslationY(-distance);}
            int position=value?i:steps.size()-1-i;
            step.animate().alpha(value?1:0).translationY(value?0:-distance)
                .setStartDelay(Math.min(position*28,value?196:112)).setDuration(value?190:160)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        }
        chevron.animate().rotation(value?180:0).setDuration(250).start();
        int run=generation;
        height=ValueAnimator.ofInt(current,target);height.setDuration(value?420:300);
        height.setInterpolator(new android.view.animation.DecelerateInterpolator());
        height.addUpdateListener(animation->{body.getLayoutParams().height=(int)animation.getAnimatedValue();body.requestLayout();});
        height.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator animation){
            if(run!=generation) return;
            body.getLayoutParams().height=ViewGroup.LayoutParams.WRAP_CONTENT;body.setVisibility(expanded?View.VISIBLE:View.GONE);resetSteps();body.requestLayout();height=null;
        }});height.start();
    }
}
