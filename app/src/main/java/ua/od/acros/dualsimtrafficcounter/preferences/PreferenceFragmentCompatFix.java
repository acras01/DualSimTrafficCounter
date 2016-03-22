package ua.od.acros.dualsimtrafficcounter.preferences;

import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

public abstract class PreferenceFragmentCompatFix extends PreferenceFragmentCompat {

    private static final String FRAGMENT_DIALOG_TAG = "android.support.v7.preference.PreferenceFragment.DIALOG";

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (this.getFragmentManager().findFragmentByTag(FRAGMENT_DIALOG_TAG) == null) {
            Object f = null;
            if (preference instanceof TwoLineEditTextPreference) {
                f = EditTextPreferenceDialogFragmentCompatFix.newInstance(preference.getKey());
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
            if (f != null) {
                ((DialogFragment) f).setTargetFragment(this, 0);
                ((DialogFragment) f).show(this.getFragmentManager(), FRAGMENT_DIALOG_TAG);
            }
        }
    }
}
