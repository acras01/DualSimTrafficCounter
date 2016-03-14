package ua.od.acros.dualsimtrafficcounter.preferences;

import android.content.Context;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.TextView;

public class TwoLineEditTextPreference extends EditTextPreference {
    private EditText editText;

    public TwoLineEditTextPreference(Context context) {
        this(context, null);
    }

    public TwoLineEditTextPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.v7.preference.R.attr.editTextPreferenceStyle);
    }

    public TwoLineEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TwoLineEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        editText = new AppCompatEditText(context, attrs);
        editText.setId(android.R.id.edit);
    }

    public EditText getEditText() {
        return editText;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        TextView textView = (TextView) holder.findViewById(android.R.id.title);
        if (textView != null) {
            textView.setSingleLine(false);
        }
    }
}
