package ua.od.acros.dualsimtrafficcounter.utils;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import ua.od.acros.dualsimtrafficcounter.R;

public class IconsList extends DialogFragment implements AdapterView.OnItemClickListener {

    private String[] listitems;
    private ListView mylist;
    private String logo;
    private static final String ID = "id";

    public static IconsList newInstance(String id) {
        IconsList f = new IconsList();
        Bundle args = new Bundle();
        args.putString(ID, id);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get back arguments
        logo = getArguments().getString(ID, "");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        listitems = getResources().getStringArray(R.array.icons);
        View view = inflater.inflate(R.layout.icons_list_layout, null, false);
        mylist = (ListView) view.findViewById(R.id.list);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listitems);
        mylist.setAdapter(adapter);
        mylist.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        dismiss();
        this.mListener.onComplete(position, logo);
    }

    public interface OnCompleteListener {
        void onComplete(int position, String logo);
    }

    private OnCompleteListener mListener;

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnCompleteListener) activity;
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnCompleteListener");
        }
    }

}