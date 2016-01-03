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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import ua.od.acros.dualsimtrafficcounter.MainActivity;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileDataControl;
import ua.od.acros.dualsimtrafficcounter.utils.TrafficDatabase;

public class ShowTrafficForDateDialog extends DialogFragment implements View.OnClickListener{

    private int myYear;
    private int myMonth;
    private int myDay;
    private int chkSIM = Constants.NULL;
    private boolean code = false;
    private Button bOK, bSetDate;
    private Bundle bundle = new Bundle();
    private GetTAsk task = new GetTAsk();

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
        code = getArguments().getBoolean("code");
        myDay = new DateTime().getDayOfMonth();
        myMonth = new DateTime().getMonthOfYear();
        myYear = new DateTime().getYear();
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.show_traffic_dialog, null);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        bSetDate = (Button) view.findViewById(R.id.setdate);
        bSetDate.setOnClickListener(this);
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        int simNumber = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileDataControl.isMultiSim(getActivity())
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        if (simNumber == 1) {
            view.findViewById(R.id.sim2RB).setEnabled(false);
            view.findViewById(R.id.sim3RB).setEnabled(false);
        }
        if (simNumber == 2)
            view.findViewById(R.id.sim3RB).setEnabled(false);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.sim1RB:
                        chkSIM = Constants.SIM1;
                        break;
                    case R.id.sim2RB:
                        chkSIM = Constants.SIM2;
                        break;
                    case R.id.sim3RB:
                        chkSIM = Constants.SIM3;
                        break;
                    case R.id.offRB:
                        chkSIM = Constants.DISABLED;
                        break;
                }
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (task != null)
                            task.cancel(false);
                        dialog.cancel();
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
                        if (chkSIM != Constants.NULL ) {
                            String date = myYear + "-" + myMonth + "-" + myDay;
                            if (bOK.getText().equals(getString(android.R.string.ok))) {
                                task.execute(myYear, myMonth, myDay, chkSIM);
                            } else {
                                if (bundle != null) {
                                    dialog.dismiss();
                                    if (code)
                                        getActivity().finish();
                                    Intent intent = new Intent(MainActivity.getAppContext(), ViewTraffic.class);
                                    intent.putExtra(Constants.SIM_ACTIVE, chkSIM);
                                    intent.putExtra(Constants.SET_USAGE, bundle);
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

    class GetTAsk extends AsyncTask<Integer, Void, Bundle> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bOK.setEnabled(false);
        }

        @Override
        protected Bundle doInBackground(Integer... params) {
            String date = params[0] + "-" + params[1] + "-" + params[2];
            if (isCancelled())
                return null;
            else
                return TrafficDatabase.getDataForDate(new TrafficDatabase(getActivity(), Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION),
                    date, params[3], getActivity().getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE));
        }

        @Override
        protected void onPostExecute(Bundle result) {
            super.onPostExecute(result);
            bOK.setEnabled(true);
            if (result != null) {
                bOK.setText(getString(R.string.view_result));
                bundle = result;
            } else {
                bOK.setText(getString(android.R.string.ok));
                Toast.makeText(getActivity(), R.string.date_incorrect_or_data_missing, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.setdate) {
            DatePickerDialog tpd = new DatePickerDialog(getActivity(), myCallBack, myYear, myMonth - 1, myDay);
            tpd.show();
        }

    }

    DatePickerDialog.OnDateSetListener myCallBack = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            myYear = year;
            myMonth = monthOfYear + 1;
            myDay = dayOfMonth;

            DateTimeFormatter fmt = DateTimeFormat.forPattern(Constants.DATE_FORMAT);
            DateTime date = fmt.parseDateTime(myYear + "-" + myMonth + "-" + myDay);

            Format dateFormat = android.text.format.DateFormat.getDateFormat(getActivity());
            String pattern = ((SimpleDateFormat) dateFormat).toLocalizedPattern();

            bSetDate.setText(new SimpleDateFormat(pattern).format(date.toDate()));
        }
    };
}
