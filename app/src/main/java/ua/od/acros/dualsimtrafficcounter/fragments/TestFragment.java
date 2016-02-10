package ua.od.acros.dualsimtrafficcounter.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class TestFragment extends Fragment implements View.OnClickListener, RadioGroup.OnCheckedChangeListener{

    private String mSimChecked = "";
    private Button bOK;
    private boolean mAlternative = false;
    private SharedPreferences.Editor edit;
    private String[] mOperatorNames = new String[3];
    private OnFragmentInteractionListener mListener;


    public static TestFragment newInstance() {
        return new TestFragment();
    }

    public TestFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mOperatorNames[0] = MobileUtils.getName(getActivity(), Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1);
        mOperatorNames[1] = MobileUtils.getName(getActivity(), Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2);
        mOperatorNames[2] = MobileUtils.getName(getActivity(), Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.test_fragment, container, false);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        RadioButton sim1rb = (RadioButton) view.findViewById(R.id.sim1RB);
        sim1rb.setText(mOperatorNames[0]);
        RadioButton sim2rb = (RadioButton) view.findViewById(R.id.sim2RB);
        sim2rb.setText(mOperatorNames[1]);
        RadioButton sim3rb = (RadioButton) view.findViewById(R.id.sim3RB);
        sim3rb.setText(mOperatorNames[2]);
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        edit = prefs.edit();
        int simQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(getActivity())
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        if (simQuantity == 1) {
            sim2rb.setEnabled(false);
            sim3rb.setEnabled(false);
        }
        if (simQuantity == 2)
            sim3rb.setEnabled(false);
        radioGroup.setOnCheckedChangeListener(this);
        view.findViewById(R.id.buttonOK).setOnClickListener(this);
        view.findViewById(R.id.test).setOnClickListener(this);
        return view;
    }

    @Override
    public void onResume(){
        super.onResume();
        android.support.v7.widget.Toolbar toolBar = (android.support.v7.widget.Toolbar) getActivity().findViewById(R.id.toolbar);
        toolBar.setSubtitle(R.string.action_show_test);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonOK:
                edit.putBoolean(Constants.PREF_OTHER[20], mAlternative);
                edit.apply();
                getActivity().onBackPressed();
                break;
            case R.id.test:
                if (!mSimChecked.equals("")) {
                    int sim = Constants.DISABLED;
                    try {
                        sim = (int) Settings.System.getLong(getActivity().getContentResolver(), "gprs_connection_sim_setting");
                        edit.putInt(mSimChecked, sim);
                        mAlternative = true;
                    } catch (Settings.SettingNotFoundException e0) {
                        e0.printStackTrace();
                        try {
                            sim = (int) Settings.System.getLong(getActivity().getContentResolver(), "gprs_connection_setting");
                            edit.putInt(mSimChecked, sim);
                            mAlternative = true;
                        } catch (Settings.SettingNotFoundException e1) {
                            e1.printStackTrace();
                        }
                    }
                    if (mAlternative)
                        Toast.makeText(getActivity(), mSimChecked + ": " + sim, Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(getActivity(), R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.sim1RB:
                mSimChecked = "sim1";
                break;
            case R.id.sim2RB:
                mSimChecked = "sim2";
                break;
            case R.id.sim3RB:
                mSimChecked = "sim3";
                break;
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onTestFragmentInteraction(Uri uri);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onTestFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
