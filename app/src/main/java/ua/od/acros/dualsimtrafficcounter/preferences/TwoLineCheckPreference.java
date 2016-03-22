package ua.od.acros.dualsimtrafficcounter.preferences;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;

public class TwoLineCheckPreference extends android.support.v7.preference.CheckBoxPreference {

    public TwoLineCheckPreference(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
    }

    public TwoLineCheckPreference(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
    }

    public TwoLineCheckPreference(Context ctx) {
        super(ctx);
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
