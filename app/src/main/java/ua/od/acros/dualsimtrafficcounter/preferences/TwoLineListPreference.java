package ua.od.acros.dualsimtrafficcounter.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;

public class TwoLineListPreference extends android.support.v7.preference.ListPreference {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TwoLineListPreference(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
    }

    public TwoLineListPreference(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
    }

    public TwoLineListPreference(Context ctx) {
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
