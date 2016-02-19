package ua.od.acros.dualsimtrafficcounter.activities;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.acra.ACRA;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.utils.CheckServiceRunning;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabase;

public class CallsWidgetConfigActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String PREF_PREFIX_KEY = "_calls";
    private int mDim;
    private int mWidgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    private SharedPreferences mPrefs;
    private int mSimQuantity;
    private SharedPreferences.Editor mEdit;
    private Context mContext = this;
    private int mTextColor;
    private int mBackColor;
    private Intent mResultValueIntent;

    public CallsWidgetConfigActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (!CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, mContext))
            mContext.startService(new Intent(mContext, CallLoggerService.class));

        mDim = (int) getResources().getDimension(R.dimen.logo_size);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mWidgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mWidgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        mPrefs = getSharedPreferences(String.valueOf(mWidgetID) + PREF_PREFIX_KEY + Constants.WIDGET_PREFERENCES, Context.MODE_PRIVATE);
        mSimQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));
        mEdit = mPrefs.edit();
        if (mPrefs.getAll().size() == 0) {
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[1], true);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[2], true);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[3], false);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[4], true);
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[5], "none");
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[6], "none");
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[7], "none");
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[8], false);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[9], false);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[10], false);
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[11], Constants.ICON_SIZE);
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[12], Constants.TEXT_SIZE);
            mEdit.putInt(Constants.PREF_WIDGET_TRAFFIC[13], Color.WHITE);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[14], true);
            mEdit.putInt(Constants.PREF_WIDGET_TRAFFIC[15], Color.TRANSPARENT);
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[16], Constants.TEXT_SIZE);
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[17], Constants.ICON_SIZE);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[18], true);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[19], true);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[20], true);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[21], true);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[22], false);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[23], false);
            mEdit.apply();
        }

        mTextColor = mPrefs.getInt(Constants.PREF_WIDGET_TRAFFIC[13], Color.WHITE);
        mBackColor = mPrefs.getInt(Constants.PREF_WIDGET_TRAFFIC[15], Color.TRANSPARENT);

        mResultValueIntent = new Intent();
        mResultValueIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetID);

        setResult(RESULT_CANCELED, mResultValueIntent);

        setContentView(R.layout.calls_info_widget_configure);

        mPrefs.registerOnSharedPreferenceChangeListener(this);

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.widget_config_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            if (item.getItemId() == R.id.save) {
                mEdit.apply();
                Intent intent = new Intent(Constants.CALLS);
                intent.putExtra(Constants.WIDGET_IDS, new int[]{mWidgetID});
                if (!MyDatabase.isEmpty(new MyDatabase(mContext, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION))) {
                    ContentValues dataMap = MyDatabase.readCallsData(MyDatabase.getInstance(mContext));
                    intent.putExtra(Constants.CALLS1, (long) dataMap.get(Constants.CALLS1));
                    intent.putExtra(Constants.CALLS2, (long) dataMap.get(Constants.CALLS2));
                    intent.putExtra(Constants.CALLS3, (long) dataMap.get(Constants.CALLS3));
                    intent.putExtra(Constants.OPERATOR1, MobileUtils.getName(this, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1));
                    if (mSimQuantity >= 2)
                        intent.putExtra(Constants.OPERATOR2, MobileUtils.getName(this, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2));
                    if (mSimQuantity == 3)
                        intent.putExtra(Constants.OPERATOR3, MobileUtils.getName(this, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3));
                } else {
                    intent.putExtra(Constants.CALLS1, 0L);
                    intent.putExtra(Constants.CALLS2, 0L);
                    intent.putExtra(Constants.CALLS3, 0L);
                }
                sendBroadcast(intent);
                setResult(RESULT_OK, mResultValueIntent);
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }
}

