package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.ArrayList;
import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;

public class BlackListAdapter  extends BaseAdapter {


    private static List<String> mList, mChecked;
    private LayoutInflater inflater;

    public BlackListAdapter(Context context, List<String> list) {
        super();
        mChecked = new ArrayList<>();
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (list != null)
            mList = list;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    public static List<String> getCheckedItems(){
        return mChecked;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final CheckBox checkBox;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.black_list_row, null);
            checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
            convertView.setTag(checkBox);
        } else
            checkBox = (CheckBox) convertView.getTag();

        checkBox.setText(mList.get(position));

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String number = (String) buttonView.getText();
                if (isChecked)
                    mChecked.add(number);
                else
                    mChecked.remove(number);
            }
        });

        return convertView;
    }

    // кол-во элементов
    @Override
    public int getCount() {
        return mList.size();
    }

    // элемент по позиции
    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    // id по позиции
    @Override
    public long getItemId(int position) {
        return position;
    }
}