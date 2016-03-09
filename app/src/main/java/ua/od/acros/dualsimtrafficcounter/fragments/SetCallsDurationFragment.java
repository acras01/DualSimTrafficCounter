package ua.od.acros.dualsimtrafficcounter.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.events.SetCallsEvent;
import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyApplication;

public class SetCallsDurationFragment extends Fragment implements RadioGroup.OnCheckedChangeListener,
        AdapterView.OnItemSelectedListener, View.OnClickListener {

    private EditText duration;
    private int mSimChecked = Constants.DISABLED;
    private String[] mOperatorNames;
    private OnFragmentInteractionListener mListener;
    private int mSpinnerSel;
    private Context mContext;


    public static SetCallsDurationFragment newInstance() {
        return new SetCallsDurationFragment();
    }

    public SetCallsDurationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mContext == null)
            mContext = getActivity().getApplicationContext();
        mOperatorNames = new String[]{MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mContext == null)
            mContext = getActivity().getApplicationContext();
        View view = inflater.inflate(R.layout.duration_fragment, container, false);
        duration = (EditText) view.findViewById(R.id.duration);
        Spinner spinner = (Spinner) view.findViewById(R.id.spinner);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        RadioButton sim1rb = (RadioButton) view.findViewById(R.id.sim1RB);
        sim1rb.setText(mOperatorNames[0]);
        RadioButton sim2rb = (RadioButton) view.findViewById(R.id.sim2RB);
        sim2rb.setText(mOperatorNames[1]);
        RadioButton sim3rb = (RadioButton) view.findViewById(R.id.sim3RB);
        sim3rb.setText(mOperatorNames[2]);
        SharedPreferences prefs = mContext.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        int simQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        if (simQuantity == 1) {
            sim2rb.setEnabled(false);
            sim3rb.setEnabled(false);
        }
        if (simQuantity == 2)
            sim3rb.setEnabled(false);
        radioGroup.setOnCheckedChangeListener(this);
        spinner.setOnItemSelectedListener(this);
        view.findViewById(R.id.buttonOK).setOnClickListener(this);
        if (savedInstanceState != null) {
            switch (savedInstanceState.getInt("sim")) {
                case Constants.SIM1:
                    sim1rb.setChecked(true);
                    break;
                case Constants.SIM2:
                    sim2rb.setChecked(true);
                    break;
                case Constants.SIM3:
                    sim3rb.setChecked(true);
                    break;
            }
            duration.setText(savedInstanceState.getString("duration"));
            spinner.setSelection(savedInstanceState.getInt("spinner"));
        }
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("sim", mSimChecked);
        outState.putInt("spinner", mSpinnerSel);
        outState.putString("duration", duration.getText().toString());
    }

    @Override
    public void onResume(){
        super.onResume();
        android.support.v7.widget.Toolbar toolBar = (android.support.v7.widget.Toolbar) getActivity().findViewById(R.id.toolbar);
        toolBar.setSubtitle(R.string.action_set_duration);
    }

    @Override
    public void onClick(View view) {
        if (mSimChecked != Constants.DISABLED && !duration.getText().toString().equals("")) {
            if (MyApplication.isMyServiceRunning(CallLoggerService.class, mContext)) {
                SetCallsEvent event = new SetCallsEvent(mSimChecked, duration.getText().toString(), mSpinnerSel);
                EventBus.getDefault().post(event);
                getActivity().onBackPressed();
            } else
                Toast.makeText(mContext, R.string.service_stop, Toast.LENGTH_LONG).show();
        } else
            Toast.makeText(mContext, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.sim1RB:
                mSimChecked =  Constants.SIM1;
                break;
            case R.id.sim2RB:
                mSimChecked =  Constants.SIM2;
                break;
            case R.id.sim3RB:
                mSimChecked =  Constants.SIM3;
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case (R.id.spinner):
                mSpinnerSel = parent.getSelectedItemPosition();
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onSetDurationFragmentInteraction(Uri uri);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onSetDurationFragmentInteraction(uri);
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
