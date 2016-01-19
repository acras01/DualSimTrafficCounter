package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.Format;
import java.text.SimpleDateFormat;

import ua.od.acros.dualsimtrafficcounter.MainActivity;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.TrafficDatabase;

public class ShowTrafficForDateDialog extends DialogFragment implements View.OnClickListener{

    private int mYear;
    private int mMonth;
    private int mDay;
    private int mSimChecked = Constants.NULL;
    private int mSimQuantity;
    private boolean mCode = false;
    private Button bOK, bSetDate;
    private Bundle mBundle = new Bundle();
    private GetTask mTask;
    private ProgressBar pb;
    private RadioGroup radioGroup;

    public static ShowTrafficForDateDialog newInstance(boolean code) {
        ShowTrafficForDateDialog df = new ShowTrafficForDateDialog();
        Bundle bundle = new Bundle();
        bundle.putBoolean("code", code);
        df.setArguments(bundle);
        return df;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mCode = getArguments().getBoolean("code");
        mDay = new DateTime().getDayOfMonth();
        mMonth = new DateTime().getMonthOfYear();
        mYear = new DateTime().getYear();
        View view = View.inflate(getActivity(), R.layout.show_traffic_dialog, null);
        pb = (ProgressBar) view.findViewById(R.id.progressBar);
        pb.setVisibility(View.GONE);
        radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        bSetDate = (Button) view.findViewById(R.id.setdate);
        bSetDate.setOnClickListener(this);
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        mSimQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(getActivity())
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        if (mSimQuantity == 1) {
            view.findViewById(R.id.sim2RB).setEnabled(false);
            view.findViewById(R.id.sim3RB).setEnabled(false);
        }
        if (mSimQuantity == 2)
            view.findViewById(R.id.sim3RB).setEnabled(false);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
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
                    case R.id.offRB:
                        mSimChecked = Constants.DISABLED;
                        break;
                }
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (mTask != null)
                            mTask.cancel(false);
                        dialog.cancel();
                        if (mCode)
                            getActivity().finish();
                    }
                })
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterface) {
                bOK = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                bOK.setText(getString(android.R.string.ok));
                bOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mSimChecked != Constants.NULL ) {
                            String date = mYear + "-" + mMonth + "-" + mDay;
                            if (bOK.getText().equals(getString(android.R.string.ok))) {
                                mTask = new GetTask();
                                mTask.execute(mYear, mMonth, mDay, mSimChecked);
                            } else {
                                if (mBundle != null) {
                                    dialog.dismiss();
                                    if (mCode)
                                        getActivity().finish();
                                    Intent intent = new Intent(MainActivity.getAppContext(), ViewTraffic.class);
                                    intent.putExtra(Constants.SIM_ACTIVE, mSimChecked);
                                    intent.putExtra(Constants.SET_USAGE, mBundle);
                                    intent.putExtra(Constants.LAST_DATE, date);
                                    getActivity().startActivity(intent);
                                }
                            }
                        } else
                            Toast.makeText(getActivity(), R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        return dialog;
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
                return TrafficDatabase.getDataForDate(TrafficDatabase.getInstance(MainActivity.getAppContext()),
                    date, params[3], getActivity().getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE));
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
                bOK.setText(getString(R.string.view_result));
                mBundle = result;
            } else {
                bOK.setText(getString(android.R.string.ok));
                Toast.makeText(getActivity(), R.string.date_incorrect_or_data_missing, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.setdate) {
            DatePickerDialog tpd = new DatePickerDialog(getActivity(), mCallBack, mYear, mMonth - 1, mDay);
            tpd.show();
        }

    }

    DatePickerDialog.OnDateSetListener mCallBack = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            mYear = year;
            mMonth = monthOfYear + 1;
            mDay = dayOfMonth;

            DateTimeFormatter fmt = DateTimeFormat.forPattern(Constants.DATE_FORMAT);
            DateTime date = fmt.parseDateTime(mYear + "-" + mMonth + "-" + mDay);

            Format dateFormat = android.text.format.DateFormat.getDateFormat(getActivity());
            String pattern = ((SimpleDateFormat) dateFormat).toLocalizedPattern();

            bSetDate.setText(new SimpleDateFormat(pattern).format(date.toDate()));
            bOK.setText(getString(android.R.string.ok));
        }
    };
}
