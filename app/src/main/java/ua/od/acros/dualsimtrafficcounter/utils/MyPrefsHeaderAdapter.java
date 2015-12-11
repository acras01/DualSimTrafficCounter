package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.Context;
import android.preference.PreferenceActivity.Header;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;

public class MyPrefsHeaderAdapter extends ArrayAdapter<Header> {

	static final int HEADER_TYPE_CATEGORY = 0;
	static final int HEADER_TYPE_NORMAL = 1;
	static final int HEADER_TYPE_SWITCH = 2;

	private LayoutInflater mInflater;
    private SoundEnabler mSoundEnabler;

	public MyPrefsHeaderAdapter(Context context, List<Header> objects) {
		super(context, 0, objects);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mSoundEnabler = new SoundEnabler(context, new Switch(context));
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		Header header = getItem(position);
		int headerType = getHeaderType(header);
        View view = null;
		switch (headerType) {
			case HEADER_TYPE_CATEGORY:
				view = mInflater.inflate(android.R.layout.preference_category, parent, false);
				((TextView) view.findViewById(android.R.id.title)).setText(header.getTitle(getContext()
						.getResources()));
				break;
			case HEADER_TYPE_SWITCH:
				view = mInflater.inflate(R.layout.preference_header_switch_item, parent, false);
				((TextView) view.findViewById(android.R.id.title)).setText(header.getTitle(getContext()
						.getResources()));
				((TextView) view.findViewById(android.R.id.summary)).setText(header
						.getSummary(getContext().getResources()));
				if (header.id == R.id.notif_settings)
					mSoundEnabler.setSwitch((Switch) view.findViewById(R.id.switchWidget));
				break;
			case HEADER_TYPE_NORMAL:
				view = mInflater.inflate(R.layout.preference_header_item, parent, false);
				((TextView) view.findViewById(android.R.id.title)).setText(header.getTitle(getContext()
						.getResources()));
				((TextView) view.findViewById(android.R.id.summary)).setText(header
						.getSummary(getContext().getResources()));
				break;
		}
		return view;
	}

	public static int getHeaderType(Header header) {
		if ((header.fragment == null) && (header.intent == null))
			return HEADER_TYPE_CATEGORY;
		else if (header.id == R.id.notif_settings)
			return HEADER_TYPE_SWITCH;
		else
			return HEADER_TYPE_NORMAL;
	}

	public void resume() {
        mSoundEnabler.resume();
	}

	public void pause() {
		mSoundEnabler.pause();
	}
}
