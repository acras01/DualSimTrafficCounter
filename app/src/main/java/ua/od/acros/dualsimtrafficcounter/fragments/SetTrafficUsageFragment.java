package ua.od.acros.dualsimtrafficcounter.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.events.SetTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class SetTrafficUsageFragment extends Fragment implements CompoundButton.OnCheckedChangeListener,
        RadioGroup.OnCheckedChangeListener, AdapterView.OnItemSelectedListener, View.OnClickListener {

    private EditText txInput, rxInput;
    private int mTXSpinnerSel, mRXSpinnerSel;
    private int mSimChecked = Constants.DISABLED;
    private AppCompatSpinner rxSpinner, txSpinner;
    private String[] mOperatorNames;
    private AppCompatCheckBox total;
    private OnFragmentInteractionListener mListener;
    private Context mContext;
    private Boolean mOnlyReceived;
    private SharedPreferences mPrefs;
    private AppCompatButton buttonOk;


    public static SetTrafficUsageFragment newInstance() {
        return new SetTrafficUsageFragment();
    }

    public SetTrafficUsageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mContext == null)
            mContext = CustomApplication.getAppContext();
        mOperatorNames = new String[]{MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mContext == null)
            mContext = CustomApplication.getAppContext();
        View view = inflater.inflate(R.layout.usage_fragment, container, false);
        txInput = (EditText) view.findViewById(R.id.txamount);
        txInput.setEnabled(false);
        rxInput = (EditText) view.findViewById(R.id.rxamount);
        rxInput.setEnabled(false);
        txSpinner = (AppCompatSpinner) view.findViewById(R.id.spinnertx);
        rxSpinner = (AppCompatSpinner) view.findViewById(R.id.spinnerrx);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.sim_group);
        AppCompatRadioButton sim1rb = (AppCompatRadioButton) view.findViewById(R.id.sim1RB);
        sim1rb.setText(mOperatorNames[0]);
        AppCompatRadioButton sim2rb = (AppCompatRadioButton) view.findViewById(R.id.sim2RB);
        sim2rb.setText(mOperatorNames[1]);
        AppCompatRadioButton sim3rb = (AppCompatRadioButton) view.findViewById(R.id.sim3RB);
        sim3rb.setText(mOperatorNames[2]);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        int simQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));
        if (simQuantity == 1) {
            sim2rb.setEnabled(false);
            sim3rb.setEnabled(false);
        }
        if (simQuantity == 2)
            sim3rb.setEnabled(false);
        total = (AppCompatCheckBox) view.findViewById(R.id.checktotal);
        total.setChecked(false);
        total.setOnCheckedChangeListener(this);
        total.setEnabled(false);
        radioGroup.setOnCheckedChangeListener(this);
        txSpinner.setOnItemSelectedListener(this);
        txSpinner.setEnabled(false);
        rxSpinner.setOnItemSelectedListener(this);
        rxSpinner.setEnabled(false);
        buttonOk = (AppCompatButton) view.findViewById(R.id.buttonOK);
        buttonOk.setOnClickListener(this);
        buttonOk.setEnabled(false);
        if (savedInstanceState != null) {
            mOnlyReceived = savedInstanceState.getBoolean("received");
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
            txInput.setText(savedInstanceState.getString("tx"));
            rxInput.setText(savedInstanceState.getString("rx"));
            total.setChecked(savedInstanceState.getBoolean("tot"));
            rxSpinner.setSelection(savedInstanceState.getInt("rxs"));
            txSpinner.setSelection(savedInstanceState.getInt("txs"));
            if (mOnlyReceived != null) {
                txInput.setEnabled(!mOnlyReceived);
                txSpinner.setEnabled(!mOnlyReceived);
                total.setEnabled(!mOnlyReceived);
                if (total.isEnabled()) {
                    rxInput.setEnabled(!total.isChecked());
                    rxSpinner.setEnabled(!total.isChecked());
                } else {
                    rxInput.setEnabled(mOnlyReceived);
                    rxSpinner.setEnabled(mOnlyReceived);
                }
            }
            buttonOk.setEnabled(true);
        }
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("sim", mSimChecked);
        outState.putBoolean("received", mOnlyReceived);
        outState.putInt("rxs", mTXSpinnerSel);
        outState.putString("rx", rxInput.getText().toString());
        outState.putInt("txs", mRXSpinnerSel);
        outState.putString("tx", txInput.getText().toString());
        outState.putBoolean("tot", total.isChecked());
    }

    @Override
    public void onResume(){
        super.onResume();
        ((Toolbar) getActivity().findViewById(R.id.toolbar)).setSubtitle(R.string.action_set_usage);
    }

    @Override
    public void onClick(View view) {
        if ((mSimChecked != Constants.DISABLED && !rxInput.getText().toString().equals("") &&
                !txInput.getText().toString().equals("")) ||
                (mSimChecked != Constants.DISABLED && total.isChecked() && !txInput.getText().toString().equals(""))) {
            if (CustomApplication.isMyServiceRunning(TrafficCountService.class)) {
                String rx = "0";
                if (!total.isChecked())
                    rx = rxInput.getText().toString();
                String tx = "0";
                if (mOnlyReceived != null && !mOnlyReceived)
                    tx = txInput.getText().toString();
                SetTrafficEvent event = new SetTrafficEvent(tx, rx, mSimChecked, mTXSpinnerSel, mRXSpinnerSel);
                EventBus.getDefault().post(event);
                getActivity().onBackPressed();
            } else
                Toast.makeText(mContext, R.string.service_stop, Toast.LENGTH_LONG).show();
        } else
            Toast.makeText(mContext, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        mOnlyReceived = null;
        switch (checkedId) {
            case R.id.sim1RB:
                mSimChecked =  Constants.SIM1;
                mOnlyReceived = mPrefs.getBoolean(Constants.PREF_SIM1[32], false);
                break;
            case R.id.sim2RB:
                mSimChecked =  Constants.SIM2;
                mOnlyReceived = mPrefs.getBoolean(Constants.PREF_SIM2[32], false);
                break;
            case R.id.sim3RB:
                mSimChecked =  Constants.SIM3;
                mOnlyReceived = mPrefs.getBoolean(Constants.PREF_SIM3[32], false);
                break;
        }
        if (mOnlyReceived != null) {
            txInput.setEnabled(!mOnlyReceived);
            txSpinner.setEnabled(!mOnlyReceived);
            total.setEnabled(!mOnlyReceived);
            if (total.isEnabled()) {
                rxInput.setEnabled(!total.isChecked());
                rxSpinner.setEnabled(!total.isChecked());
            } else {
                rxInput.setEnabled(mOnlyReceived);
                rxSpinner.setEnabled(mOnlyReceived);
            }
        }
        buttonOk.setEnabled(true);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case (R.id.spinnertx):
                mTXSpinnerSel = parent.getSelectedItemPosition();
                break;
            case (R.id.spinnerrx):
                mRXSpinnerSel = parent.getSelectedItemPosition();
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        rxSpinner.setEnabled(!isChecked);
        rxInput.setEnabled(!isChecked);
        if (isChecked)
            txInput.setHint(R.string.total);
        else
            txInput.setHint(R.string.transmitted);
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onSetUsageFragmentInteraction(Uri uri);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onSetUsageFragmentInteraction(uri);
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
