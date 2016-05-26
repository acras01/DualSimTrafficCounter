package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import ua.od.acros.dualsimtrafficcounter.R;
import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;

/**
 * This implementation provides multiple windows. You may extend this class or
 * use it as a reference for a basic foundation for your own windows.
 * 
 * <p>
 * Functionality includes system window decorators, moveable, resizeable,
 * hideable, closeable, and bring-to-frontable.
 * 
 * <p>
 * The persistent notification creates new windows. The hidden notifications
 * restores previously hidden windows.
 * 
 * @author Mark Wei <markwei@gmail.com>
 * 
 */
public class FloatingWindow extends StandOutWindow {
    private Context mContext;
    private SharedPreferences mPrefs;

    @Override
	public String getAppName() {
		return getString(R.string.app_name);
	}

	@Override
	public Bitmap getAppIcon() {
		return BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
	}

    /**
     * Return the icon resource for every window in this implementation. The
     * icon will appear in the default implementations of notifications.
     *
     * @return The icon.
     */
    @Override
    public int getNotificationIcon() {
        return R.drawable.ic_launcher_small;
    }

    @Override
	public String getTitle(int id) {
		return getAppName() + " " + id;
	}

	@Override
	public void createAndAttachView(int id, FrameLayout frame) {
        mContext = CustomApplication.getAppContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		// create a new layout from .xml
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.floating_window, frame, true);
		TextView status = (TextView) view.findViewById(R.id.tv);
        String changedText = DataFormat.formatData(mContext, 0L);
        RelativeLayout back = (RelativeLayout) view.findViewById(R.id.rl);
        status.setTextSize(Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[33], "10")));
        status.setTextColor(mPrefs.getInt(Constants.PREF_OTHER[34], ContextCompat.getColor(mContext, R.color.widget_text)));
        back.setBackgroundColor(mPrefs.getInt(Constants.PREF_OTHER[35], ContextCompat.getColor(mContext, android.R.color.transparent)));
        status.setText(changedText);
	}

	/*@Override
	public String getPersistentNotificationTitle(int id) {
		return getAppName();
	}

	@Override
	public String getPersistentNotificationMessage(int id) {
		return getString(R.string.close_floating_window);
	}

	// return an Intent that shows the FloatingWindow
	@Override
	public Intent getPersistentNotificationIntent(int id) {
		return StandOutWindow.getShowIntent(this, FloatingWindow.class, id);
	}

    @Override
    public int getHiddenIcon() {
        return R.drawable.ic_launcher_small;
    }

    @Override
    public String getHiddenNotificationTitle(int id) {
        return getAppName() + " Hidden";
    }

    @Override
    public String getHiddenNotificationMessage(int id) {
        return getString(R.string.restore_floating_window);
    }

    // return an Intent that restores the MultiWindow
    @Override
    public Intent getHiddenNotificationIntent(int id) {
        return StandOutWindow.getShowIntent(this, getClass(), id);
    }*/

	@Override
	public Animation getShowAnimation(int id) {
		if (isExistingId(id)) {
			// restore
			return AnimationUtils.loadAnimation(this,
					android.R.anim.slide_in_left);
		} else {
			// show
			return super.getShowAnimation(id);
		}
	}

	@Override
	public Animation getHideAnimation(int id) {
		return AnimationUtils.loadAnimation(this,
				android.R.anim.slide_out_right);
	}

	@Override
	public void onReceiveData(int id, int requestCode, Bundle data,
			Class<? extends StandOutWindow> fromCls, int fromId) {
		// receive data from WidgetsWindow's button press
		// to show off the data sending framework
		switch (requestCode) {
			case Constants.FLOATING_WINDOW:
				Window window = getWindow(id);
				if (window == null) {
					String errorText = String.format(Locale.US,
							"%s received data but Window id: %d is not open.",
							getAppName(), id);
					Toast.makeText(mContext, errorText, Toast.LENGTH_SHORT).show();
					return;
				}
                if (mPrefs != null) {
                    int newId = mPrefs.getInt(Constants.PREF_OTHER[38], -1);
                    if (newId == id) {
                        int[] location = new int[2];
                        window.getLocationOnScreen(location);
                        mPrefs.edit()
                                .putInt(Constants.PREF_OTHER[36], location[0])
                                .putInt(Constants.PREF_OTHER[37], location[1])
                                .apply();
                    }
                }
				String changedText = DataFormat.formatData(mContext, data.getLong("total"));
				TextView status = (TextView) window.findViewById(R.id.tv);
                RelativeLayout back = (RelativeLayout) window.findViewById(R.id.rl);
				status.setTextSize(Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[33], "10")));
                status.setTextColor(mPrefs.getInt(Constants.PREF_OTHER[34], ContextCompat.getColor(mContext, R.color.widget_text)));
                back.setBackgroundColor(mPrefs.getInt(Constants.PREF_OTHER[35], ContextCompat.getColor(mContext, android.R.color.transparent)));
				status.setText(changedText);
				break;
			default:
				Log.d("MultiWindow", "Unexpected data received.");
				break;
		}
	}

    @Override
    public StandOutLayoutParams getParams(int id, Window window) {
        mContext = CustomApplication.getAppContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        int x = mPrefs.getInt(Constants.PREF_OTHER[36], -1);
        if (x < 0)
            x = StandOutLayoutParams.CENTER;
        int y = mPrefs.getInt(Constants.PREF_OTHER[37], -1);
        if (y < 0)
            y = StandOutLayoutParams.CENTER;
        return new StandOutLayoutParams(id, StandOutLayoutParams.WRAP_CONTENT, StandOutLayoutParams.WRAP_CONTENT, x, y);
    }

    // move the window by dragging the view
    @Override
    public int getFlags(int id) {
        return super.getFlags(id) | StandOutFlags.FLAG_BODY_MOVE_ENABLE
                | StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE
                | StandOutFlags.FLAG_WINDOW_HIDE_ENABLE
                | StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TAP
                | StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE ;
    }

    @Override
    public boolean onClose(int id, Window window) {
        if (mPrefs != null) {
            int newId = mPrefs.getInt(Constants.PREF_OTHER[38], -1);
            if (newId == id) {
                int[] location = new int[2];
                window.getLocationOnScreen(location);
                mPrefs.edit()
                        .putInt(Constants.PREF_OTHER[36], location[0])
                        .putInt(Constants.PREF_OTHER[37], location[1])
                        .apply();
            }
        }
        return false;
    }
}
