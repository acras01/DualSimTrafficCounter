package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.joda.time.DateTime;

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
    private Button bOK, bSetDate;


    public static ShowTrafficForDateDialog newInstance() {
        return new ShowTrafficForDateDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
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
                        dialog.cancel();
                    }
                })
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterface) {
                bOK = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                bOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (chkSIM != Constants.NULL ) {
                            String date = myYear + "-" + myMonth + "-" + myDay;
                            Bundle bundle = TrafficDatabase.getDataForDate(new TrafficDatabase(getActivity(), Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION),
                                    date, chkSIM, MainActivity.getAppContext().getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE));
                            if (bundle != null) {
                                dialog.dismiss();
                                Intent intent = new Intent(MainActivity.getAppContext(), ViewTraffic.class);
                                intent.putExtra("sim", chkSIM);
                                intent.putExtra("data", bundle);
                                getActivity().startActivity(intent);
                            } else
                                Toast.makeText(getActivity(), R.string.date_incorrect_or_data_missing, Toast.LENGTH_LONG).show();
                        } else
                            Toast.makeText(getActivity(), R.string.fill_all_fields, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        return dialog;
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
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
            bSetDate.setText(String.format(getActivity().getResources().getString(R.string.time), myDay, myMonth, myYear));
        }
    };
}
