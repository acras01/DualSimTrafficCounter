package ua.od.acros.dualsimtrafficcounter.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Random;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
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
public class FloatingWindowService extends StandOutWindow {
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
    public void onCreate() {
        super.onCreate();
        if (mContext == null)
            mContext = CustomApplication.getAppContext();
        if (mPrefs == null)
            mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

	@Override
	public void createAndAttachView(int id, FrameLayout frame) {
        if (mContext == null)
            mContext = CustomApplication.getAppContext();
        if (mPrefs == null)
            mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		// create a new layout from .xml
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.floating_window, frame, true);
		TextView status = (TextView) view.findViewById(R.id.tv);
        String changedText = DataFormat.formatData(mContext, 0L);
        status.setTextSize(Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[33], "10")));
        status.setBackgroundColor(mPrefs.getInt(Constants.PREF_OTHER[35], ContextCompat.getColor(mContext, android.R.color.transparent)));
        status.setTextColor(mPrefs.getInt(Constants.PREF_OTHER[34], ContextCompat.getColor(mContext, R.color.widget_text)));
        status.setText(changedText);
	}

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
    public void onMove(int id, Window window, View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
            //case MotionEvent.ACTION_MOVE:
                int newId = mPrefs.getInt(Constants.PREF_OTHER[38], StandOutWindow.DEFAULT_ID);
                if (newId == id) {
                    int[] location = new int[2];
                    window.getLocationOnScreen(location);
                    mPrefs.edit()
                            .putInt(Constants.PREF_OTHER[36], location[0])
                            .putInt(Constants.PREF_OTHER[37], location[1])
                            .apply();
                }
                break;
        }
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

                int newId = mPrefs.getInt(Constants.PREF_OTHER[38], StandOutWindow.DEFAULT_ID);
                if (newId == id) {
                    int[] location = new int[2];
                    window.getLocationOnScreen(location);
                    mPrefs.edit()
                            .putInt(Constants.PREF_OTHER[36], location[0])
                            .putInt(Constants.PREF_OTHER[37], location[1])
                            .apply();
                }
                long seconds = System.currentTimeMillis() / 1000L;
                String changedText;
                int textSize = Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[33], "10"));
                if (mPrefs.getBoolean(Constants.PREF_OTHER[42], false) && seconds % 3 == 0) {
                    changedText = String.format(getResources().getString(R.string.speed),
                            DataFormat.formatData(mContext, data.getLong(Constants.SPEEDRX, 0L))) +
                            "\n" + String.format(getResources().getString(R.string.speed),
                            DataFormat.formatData(mContext, data.getLong(Constants.SPEEDTX, 0L)));
                    textSize = (int) ((double) textSize * 0.5);
                } else
                    changedText = DataFormat.formatData(mContext, data.getLong("total"));
                TextView status = (TextView) window.findViewById(R.id.tv);
                status.setTextSize(textSize);
                int textColor = mPrefs.getInt(Constants.PREF_OTHER[34], ContextCompat.getColor(mContext, R.color.widget_text));
                if (mPrefs.getBoolean(Constants.PREF_OTHER[52], true) && data.getBoolean("flash", false) && seconds % 2 == 0) {
                    String alpha = Integer.toHexString(textColor).substring(0, 2);
                    String color = Integer.toHexString(textColor).substring(2);
                    textColor = 0xFFFFFF - Integer.parseInt(color, 16);
                    color = alpha + Integer.toHexString(textColor);
                    textColor = (int) Long.parseLong(color, 16);
                }
                status.setTextColor(textColor);
                status.setBackgroundColor(mPrefs.getInt(Constants.PREF_OTHER[35], ContextCompat.getColor(mContext, android.R.color.transparent)));
                status.setText(changedText);
                break;
            default:
                Log.d("MultiWindow", "Unexpected data received.");
                break;
        }
	}

    @Override
    public StandOutLayoutParams getParams(int id, Window window) {
        int x = mPrefs.getInt(Constants.PREF_OTHER[36], -1);
        if (x < 0)
            x = StandOutLayoutParams.CENTER;
        int y = mPrefs.getInt(Constants.PREF_OTHER[37], -1);
        if (y < 0)
            y = StandOutLayoutParams.CENTER;
        return new StandOutLayoutParams(id, StandOutLayoutParams.WRAP_CONTENT, StandOutLayoutParams.WRAP_CONTENT, x, y);
    }

    @Override
    public int getFlags(int id) {
        return StandOutFlags.FLAG_BODY_MOVE_ENABLE
                | StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE
                | StandOutFlags.FLAG_WINDOW_HIDE_ENABLE
                | StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TAP
                | StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE ;
    }

    @Override
    public boolean onClose(int id, Window window) {
        int newId = mPrefs.getInt(Constants.PREF_OTHER[38], StandOutWindow.DEFAULT_ID);
        if (newId == id) {
            int[] location = new int[2];
            window.getLocationOnScreen(location);
            mPrefs.edit()
                    .putInt(Constants.PREF_OTHER[36], location[0])
                    .putInt(Constants.PREF_OTHER[37], location[1])
                    .apply();
        }
        return false;
    }

    public static void showFloatingWindow(Context context, SharedPreferences preferences) {
        closeFloatingWindow(context, preferences);
        int id = Math.abs(new Random().nextInt());
        preferences.edit()
                .putInt(Constants.PREF_OTHER[38], id)
                .apply();
        StandOutWindow.show(context, FloatingWindowService.class, id);
    }

    public static void closeFloatingWindow(Context context, SharedPreferences preferences) {
        int id = preferences.getInt(Constants.PREF_OTHER[38], -1);
        if (id >= 0)
            StandOutWindow.close(context, FloatingWindowService.class, id);
        else
            StandOutWindow.closeAll(context, FloatingWindowService.class);
    }
}
