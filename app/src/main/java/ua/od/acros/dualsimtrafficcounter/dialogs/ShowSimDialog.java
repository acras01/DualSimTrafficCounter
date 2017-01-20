package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;

import java.util.ArrayList;
import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class ShowSimDialog extends DialogFragment {

    private AppCompatButton bOK;
    private static boolean[] mSim = new boolean[3];
    private String mActivity;
    private Context mContext;
    private String[] mOperatorNames;
    private int mSimQuantity;

    public static ShowSimDialog newInstance(String activity, boolean[] sim) {
        ShowSimDialog f = new ShowSimDialog();
        Bundle b = new Bundle();
        b.putBooleanArray("sim", sim);
        b.putString("activity", activity);
        f.setArguments(b);
        return f;
    }

    public interface ShowSimDialogClosedListener {
        void OnDialogClosed(String activity, boolean[] sim);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = CustomApplication.getAppContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mActivity = getArguments().getString("activity");
        mSim = getArguments().getBooleanArray("sim");
        mOperatorNames = new String[] {MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
        mSimQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final List<Item> items = new ArrayList<>();
        for (int i = 0; i < mSim.length; i++) {
            items.add(new Item(mOperatorNames[i], mSim[i]));
        }
        final CustomListAdapter adapter = new CustomListAdapter(mContext, R.layout.showsim_list_row, items);
        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.choose_sim)
                .setAdapter(adapter, null)
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
                bOK = (AppCompatButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                bOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ShowSimDialogClosedListener listener = (ShowSimDialogClosedListener) getActivity();
                        for (Item item : adapter.getList()) {
                            mSim[adapter.getList().indexOf(item)] = item.isChecked();
                        }
                        listener.OnDialogClosed(mActivity, mSim);
                        dismiss();
                    }
                });
            }
        });
        return dialog;
    }

    private class Item {
        private String name;
        private boolean checked;

        Item(String name, boolean checked) {
            this.name = name;
            this.checked = checked;
        }

        public String getName() {
            return name;
        }

        public boolean isChecked() {
            return checked;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;
        }
    }

    private class CustomListAdapter extends ArrayAdapter<Item> {

        private ViewHolder holder;
        private List<Item> list;
        private int layout;

        CustomListAdapter(Context context, int layout, List<Item> list) {
            super(context, layout, list);
            this.list = list;
            this.layout = layout;
        }

        private class ViewHolder {
            AppCompatCheckBox item;
        }

        @NonNull
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null ) {
                holder = new ViewHolder();
                convertView = getActivity().getLayoutInflater().inflate(layout, null);
                convertView.setTag(holder);
                holder.item = (AppCompatCheckBox) convertView.findViewById(R.id.checkBox);
            }
            else
                holder = (ViewHolder) convertView.getTag();
            holder.item.setEnabled(true);
            holder.item.setTag(list.get(position));
            holder.item.setText(list.get(position).getName());
            holder.item.setChecked(list.get(position).isChecked());
            holder.item.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    ((Item) buttonView.getTag()).setChecked(isChecked);
                }
            });
            if (position >= mSimQuantity) {
                holder.item.setEnabled(false);
                holder.item.setChecked(false);
            }
            return convertView;
        }

        public List<Item> getList() {
            return list;
        }
    }
}
