package ua.od.acros.dualsimtrafficcounter.utils;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;

import ua.od.acros.dualsimtrafficcounter.R;

public class WhiteListAdapter extends RecyclerView.Adapter<WhiteListAdapter.ViewHolder> {


    public ArrayList<String> mNames, mNumbers, mList;

    public WhiteListAdapter(ArrayList<String> names, ArrayList<String> numbers, ArrayList<String> list) {
        this.mNames = names;
        this.mNumbers = numbers;
        if (list != null)
            this.mList = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public CheckBox checkBox;
        public TextView txtViewName;
        public TextView txtViewNumber;

        public ViewHolder(View v) {
            super(v);
            checkBox = (CheckBox) v.findViewById(R.id.checkBox);;
            txtViewName = (TextView) v.findViewById(R.id.name);;
            txtViewNumber = (TextView) v.findViewById(R.id.number);;
        }
    }

    @Override
    public WhiteListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.white_list_row, parent, false);

        // тут можно программно менять атрибуты лэйаута (size, margins, paddings и др.)
        ViewHolder viewHolder = new ViewHolder(v);
        final CheckBox checkBox = viewHolder.checkBox;
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String number = (String) buttonView.getTag();
                if (isChecked) {
                    if (!mList.contains(number))
                        mList.add(number);
                } else {
                    if (mList.contains(number))
                        mList.remove(number);
                }
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(WhiteListAdapter.ViewHolder holder, int position) {
        holder.checkBox.setChecked(mList.contains(mNumbers.get(position)));
        //holder.checkBox.setContentDescription(mNumbers.get(position));
        holder.checkBox.setTag(mNumbers.get(position));
        holder.txtViewName.setText(mNames.get(position));
        holder.txtViewNumber.setText(mNumbers.get(position));
    }

    public ArrayList<String> getCheckedItems(){
        return mList;
    }

    // кол-во элементов
    @Override
    public int getItemCount() {
        return mNumbers.size();
    }

    // элемент по позиции
    public Object getItem(int position) {
        return mNumbers.get(position);
    }

    // id по позиции
    @Override
    public long getItemId(int position) {
        return position;
    }
}
