package ua.od.acros.dualsimtrafficcounter.preferences;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class TwoLineEditTextPreference extends EditTextPreference{

    public TwoLineEditTextPreference(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
    }

    public TwoLineEditTextPreference(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
    }

    public TwoLineEditTextPreference(Context ctx) {
        super(ctx);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        TextView textView = (TextView) view.findViewById(android.R.id.title);
        if (textView != null) {
            textView.setSingleLine(false);
        }
    }
}
