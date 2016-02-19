package ua.od.acros.dualsimtrafficcounter.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.acra.ACRA;

import java.io.File;

import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabase;

public class CallsInfoWidget extends AppWidgetProvider {

    private static final String PREF_PREFIX_KEY = "_calls";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Bundle bundle = new Bundle();
        if (!MyDatabase.isEmpty(new MyDatabase(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION))) {
            ContentValues dataMap = MyDatabase.readCallsData(MyDatabase.getInstance(context));
            bundle.putLong(Constants.CALLS1, (long) dataMap.get(Constants.CALLS1));
            bundle.putLong(Constants.CALLS2, (long) dataMap.get(Constants.CALLS2));
            bundle.putLong(Constants.CALLS3, (long) dataMap.get(Constants.CALLS3));
        } else {
            bundle.putLong(Constants.CALLS1, 0L);
            bundle.putLong(Constants.CALLS2, 0L);
            bundle.putLong(Constants.CALLS3, 0L);
        }
        updateWidget(context, appWidgetManager, appWidgetIds, bundle);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int[] widgetIds = intent.getIntArrayExtra(Constants.WIDGET_IDS);
        if (action.equals(Constants.BROADCAST_ACTION) && widgetIds != null)
            updateWidget(context, AppWidgetManager.getInstance(context), widgetIds, intent.getExtras());
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int[] ids, Bundle bundle) {

    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        deleteWidgetPreferences(context, appWidgetIds);
    }

    private void deleteWidgetPreferences(Context context, int[] appWidgetIds) {
        File dir = new File(context.getFilesDir().getParent() + "/shared_prefs/");
        String[] children = dir.list();
        for (String aChildren : children) {
            for (int j : appWidgetIds)
                if (aChildren.replace(".xml", "").equalsIgnoreCase(String.valueOf(j) + PREF_PREFIX_KEY + Constants.WIDGET_PREFERENCES))
                    context.getSharedPreferences(aChildren.replace(".xml", ""), Context.MODE_PRIVATE).edit().clear().commit();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        for (String aChildren : children) {
            for (int j : appWidgetIds)
                if (aChildren.replace(".xml", "").equalsIgnoreCase(String.valueOf(j) + PREF_PREFIX_KEY + Constants.WIDGET_PREFERENCES))
                    new File(dir, aChildren).delete();
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

