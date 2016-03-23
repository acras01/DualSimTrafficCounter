package ua.od.acros.dualsimtrafficcounter.utils;

import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;

public class WhiteListAdapter extends RecyclerView.Adapter<WhiteListAdapter.ViewHolder> {


    public List<ListItem> mList;

    public WhiteListAdapter(List<ListItem> list) {
        if (list != null)
            this.mList = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public AppCompatCheckBox checkBox;
        public TextView txtViewName;
        public TextView txtViewNumber;

        public ViewHolder(View v) {
            super(v);
            checkBox = (AppCompatCheckBox) v.findViewById(R.id.checkBox);
            txtViewName = (TextView) v.findViewById(R.id.name);
            txtViewNumber = (TextView) v.findViewById(R.id.number);
        }
    }

    @Override
    public WhiteListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.white_list_row, parent, false);

        // тут можно программно менять атрибуты лэйаута (size, margins, paddings и др.)
        ViewHolder viewHolder = new ViewHolder(v);
        viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ((ListItem) buttonView.getTag()).setChecked(isChecked);
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(WhiteListAdapter.ViewHolder holder, int position) {
        holder.txtViewName.setText(mList.get(position).getName());
        holder.txtViewNumber.setText(mList.get(position).getNumber());
        holder.checkBox.setTag(mList.get(position));
        holder.checkBox.setChecked(mList.get(position).isChecked());
    }

    public ArrayList<String> getCheckedItems(){
        ArrayList<String> list = new ArrayList<>();
        for (ListItem item : mList)
            if (item.isChecked())
                list.add(item.getNumber());
        return list;
    }

    // кол-во элементов
    @Override
    public int getItemCount() {
        if (mList != null)
            return mList.size();
        else
            return 0;
    }

    // элемент по позиции
    public Object getItem(int position) {
        return mList.get(position);
    }

    // id по позиции
    @Override
    public long getItemId(int position) {
        return position;
    }
}
