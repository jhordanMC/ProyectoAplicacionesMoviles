package com.example.saasvilcas.shared.ui;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import androidx.appcompat.widget.AppCompatEditText;
import com.example.saasvilcas.R;

/** Six numeric cells with automatic advance and support for pasting a full code. */
public class CodeInputView extends LinearLayout {
    public interface Listener { void onCodeChanged(String code); }
    private final AppCompatEditText[] cells = new AppCompatEditText[6];
    private final OtpLiquidDrawable[] liquid=new OtpLiquidDrawable[6];
    private boolean updating;
    private Listener listener;

    public CodeInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setLayoutDirection(LAYOUT_DIRECTION_LTR);
        int gap = Math.round(8 * getResources().getDisplayMetrics().density);
        for (int index = 0; index < cells.length; index++) {
            final int position = index;
            AppCompatEditText cell = new AppCompatEditText(context);
            cells[index] = cell;
            LayoutParams params = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
            if (index > 0) params.leftMargin = gap;
            cell.setLayoutParams(params);
            liquid[index]=new OtpLiquidDrawable(getResources().getDisplayMetrics().density);
            cell.setBackground(liquid[index]);
            cell.setBackgroundTintList(null);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(0, 0, 0, 0);
            cell.setTextSize(22);
            cell.setTextColor(0xFF004D40);
            cell.setSingleLine(true);
            cell.setInputType(InputType.TYPE_CLASS_NUMBER);
            cell.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
            cell.setSaveEnabled(false);
            cell.setContentDescription(context.getString(R.string.code_digit_label, index + 1));
            cell.setOnKeyListener((view, key, event) -> {
                if (key == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN
                        && cell.length() == 0 && position > 0) {
                    cells[position - 1].requestFocus();
                    cells[position - 1].setText("");
                    return true;
                }
                return false;
            });
            cell.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
                @Override public void afterTextChanged(Editable editable) {
                    if (updating) return;
                    String digits = editable.toString().replaceAll("[^0-9]", "");
                    updating = true;
                    int first = digits.length() == 6 ? 0 : position;
                    if (digits.isEmpty()) cell.setText("");
                    for (int offset = 0; offset < digits.length() && first + offset < 6; offset++) {
                        cells[first + offset].setText(digits.substring(offset, offset + 1));
                    }
                    if (!digits.isEmpty()) {
                        int next = Math.min(first + digits.length(), 5);
                        cells[next].requestFocus();
                        cells[next].setSelection(cells[next].length());
                    }
                    updating = false;
                    updateLiquid();
                    if (listener != null) listener.onCodeChanged(getCode());
                }
            });
            addView(cell);
        }
        cells[0].requestFocus();
    }

    public void setListener(Listener listener) { this.listener = listener; }
    public void clear() {
        updating=true;
        for(AppCompatEditText cell:cells) cell.setText("");
        updating=false;
        updateLiquid();
        cells[0].requestFocus();
        if(listener!=null) listener.onCodeChanged("");
    }
    public String getCode() {
        StringBuilder code = new StringBuilder();
        for (AppCompatEditText cell : cells) code.append(cell.getText());
        return code.toString();
    }
    private void updateLiquid() {
        for(int i=0;i<cells.length;i++) {
            boolean filled=cells[i].length()>0;
            liquid[i].setFilled(filled);
            cells[i].setTextColor(filled?0xffFFFFFF:0xff004D40);
        }
    }
    @Override protected void onDetachedFromWindow() {
        for(OtpLiquidDrawable background:liquid) if(background!=null) background.settle();
        super.onDetachedFromWindow();
    }
    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (AppCompatEditText cell : cells) if (cell != null) cell.setEnabled(enabled);
    }
}
