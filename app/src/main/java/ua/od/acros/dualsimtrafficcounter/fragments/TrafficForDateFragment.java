package ua.od.acros.dualsimtrafficcounter.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.Format;
import java.text.SimpleDateFormat;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabaseHelper;


public class TrafficForDateFragment extends Fragment implements View.OnClickListener, RadioGroup.OnCheckedChangeListener{

    private int mYear, mMonth, mDay;
    private int mSimChecked = Constants.NULL;
    private int mSimQuantity;
    private Button bSetDate, bOK;
    private ProgressBar pb;
    private RadioGroup radioGroup;

    private OnFragmentInteractionListener mListener;

    private TextView RX, TX, RXN, TXN, TOT, TOTN, day, night;
    private String[] mOperatorNames = new String[3];
    private Context mContext;

    // TODO: Rename and change types and number of parameters
    public static TrafficForDateFragment newInstance(String param1, String param2) {
        return new TrafficForDateFragment();
    }

    public TrafficForDateFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDay = new DateTime().getDayOfMonth();
        mMonth = new DateTime().getMonthOfYear();
        mYear = new DateTime().getYear();
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
        View view = inflater.inflate(R.layout.traffic_for_date_fragment, container, false);
        pb = (ProgressBar) view.findViewById(R.id.progressBar);
        pb.setVisibility(View.GONE);
        radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        bSetDate = (Button) view.findViewById(R.id.setdate);
        bSetDate.setOnClickListener(this);
        SharedPreferences prefs = mContext.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        mSimQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        RadioButton sim1rb = (RadioButton) view.findViewById(R.id.sim1RB);
        sim1rb.setText(mOperatorNames[0]);
        RadioButton sim2rb = (RadioButton) view.findViewById(R.id.sim2RB);
        sim2rb.setText(mOperatorNames[1]);
        RadioButton sim3rb = (RadioButton) view.findViewById(R.id.sim3RB);
        sim3rb.setText(mOperatorNames[2]);
        if (mSimQuantity == 1) {
            sim2rb.setEnabled(false);
            sim3rb.setEnabled(false);
        }
        if (mSimQuantity == 2)
            sim3rb.setEnabled(false);
        radioGroup.setOnCheckedChangeListener(this);
        RX = (TextView) view.findViewById(R.id.rx);
        TX = (TextView) view.findViewById(R.id.tx);
        RXN = (TextView) view.findViewById(R.id.rxnight);
        TXN = (TextView) view.findViewById(R.id.txnight);
        TOT = (TextView) view.findViewById(R.id.total);
        TOTN = (TextView) view.findViewById(R.id.totalnight);
        day = (TextView) view.findViewById(R.id.day);
        night = (TextView) view.findViewById(R.id.night);
        bOK = (Button) view.findViewById(R.id.buttonOK);
        bOK.setOnClickListener(this);
        bSetDate = (Button) view.findViewById(R.id.setdate);
        bSetDate.setOnClickListener(this);
        RXN.setVisibility(View.GONE);
        TXN.setVisibility(View.GONE);
        TOTN.setVisibility(View.GONE);
        night.setVisibility(View.GONE);
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
            day.setText(savedInstanceState.getString("day"));
            night.setText(savedInstanceState.getString("night"));
            RX.setText(savedInstanceState.getString("rx"));
            RXN.setText(savedInstanceState.getString("rxn"));
            TX.setText(savedInstanceState.getString("tx"));
            TXN.setText(savedInstanceState.getString("txn"));
            TOT.setText(savedInstanceState.getString("tot"));
            TOTN.setText(savedInstanceState.getString("totn"));
            bSetDate.setText(savedInstanceState.getString("set"));
        }
        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("sim", mSimChecked);
        outState.putString("day", day.getText().toString());
        outState.putString("night", night.getText().toString());
        outState.putString("rx", RX.getText().toString());
        outState.putString("tx", TX.getText().toString());
        outState.putString("tot", TOT.getText().toString());
        outState.putString("rxn", RXN.getText().toString());
        outState.putString("txn", TXN.getText().toString());
        outState.putString("totn", TOTN.getText().toString());
        outState.putString("set", bSetDate.getText().toString());
    }

    @Override
    public void onResume(){
        super.onResume();
        android.support.v7.widget.Toolbar toolBar = (android.support.v7.widget.Toolbar) getActivity().findViewById(R.id.toolbar);
        toolBar.setSubtitle(R.string.action_show_history);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onTrafficForDateFragmentInteraction(uri);
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

    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.sim1RB:
                mSimChecked = Constants.SIM1;
                break;
            case R.id.sim2RB:
                mSimChecked = Constants.SIM2;
                break;
            case R.id.sim3RB:
                mSimChecked = Constants.SIM3;
                break;
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onTrafficForDateFragmentInteraction(Uri uri);
    }

    class GetTask extends AsyncTask<Integer, Void, Bundle> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bOK.setEnabled(false);
            radioGroup.setEnabled(false);
            for (int i = 0; i < radioGroup.getChildCount(); i++) {
                radioGroup.getChildAt(i).setEnabled(false);
            }
            bSetDate.setEnabled(false);
            pb.setVisibility(View.VISIBLE);
        }

        @Override
        protected Bundle doInBackground(Integer... params) {
            String date = params[0] + "-" + params[1] + "-" + params[2];
            if (isCancelled())
                return null;
            else
                return MyDatabaseHelper.getDataForDate(MyDatabaseHelper.getInstance(mContext),
                        date, params[3], mContext.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE));
        }

        @Override
        protected void onPostExecute(Bundle result) {
            super.onPostExecute(result);
            pb.setVisibility(View.GONE);
            bOK.setEnabled(true);
            radioGroup.setEnabled(true);
            for (int i = 0; i < radioGroup.getChildCount(); i++) {
                if (i < mSimQuantity)
                    radioGroup.getChildAt(i).setEnabled(true);
            }
            bSetDate.setEnabled(true);
            if (result != null) {
                SharedPreferences prefs = mContext.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
                String[] prefsConst = new String[27];
                switch (mSimChecked) {
                    case Constants.SIM1:
                        prefsConst = Constants.PREF_SIM1;
                        break;
                    case Constants.SIM2:
                        prefsConst = Constants.PREF_SIM2;
                        break;
                    case Constants.SIM3:
                        prefsConst = Constants.PREF_SIM3;
                        break;
                }
                String opName = MobileUtils.getName(mContext, prefsConst[5], prefsConst[6], mSimChecked);
                day.setText(opName);
                night.setText(String.format(getResources().getString(R.string.night), opName));

                RX.setText(DataFormat.formatData(mContext, result.getLong("rx")));
                TX.setText(DataFormat.formatData(mContext, result.getLong("tx")));
                TOT.setText(DataFormat.formatData(mContext, result.getLong("tot")));

                if (prefs.getBoolean(prefsConst[17], false)) {
                    RXN.setVisibility(View.VISIBLE);
                    TXN.setVisibility(View.VISIBLE);
                    TOTN.setVisibility(View.VISIBLE);
                    night.setVisibility(View.VISIBLE);
                    RXN.setText(DataFormat.formatData(mContext, result.getLong("rx_n")));
                    TXN.setText(DataFormat.formatData(mContext, result.getLong("tx_n")));
                    TOTN.setText(DataFormat.formatData(mContext, result.getLong("tot_n")));
                }
            } else
                Toast.makeText(mContext, R.string.date_incorrect_or_data_missing, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.setdate:
                DatePickerDialog tpd = new DatePickerDialog(getActivity(), mCallBack, mYear, mMonth - 1, mDay);
                tpd.show();
                break;
            case R.id.buttonOK:
                new GetTask().execute(mYear, mMonth, mDay, mSimChecked);
        }

    }

    DatePickerDialog.OnDateSetListener mCallBack = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            mYear = year;
            mMonth = monthOfYear + 1;
            mDay = dayOfMonth;

            DateTimeFormatter fmt = DateTimeFormat.forPattern(Constants.DATE_FORMAT);
            DateTime date = fmt.parseDateTime(mYear + "-" + mMonth + "-" + mDay);

            Format dateFormat = android.text.format.DateFormat.getDateFormat(mContext);
            String pattern = ((SimpleDateFormat) dateFormat).toLocalizedPattern();

            bSetDate.setText(new SimpleDateFormat(pattern).format(date.toDate()));
        }
    };

}
