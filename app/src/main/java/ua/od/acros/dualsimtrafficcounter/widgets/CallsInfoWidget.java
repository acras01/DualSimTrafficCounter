package ua.od.acros.dualsimtrafficcounter.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.acra.ACRA;

import java.io.File;

import ua.od.acros.dualsimtrafficcounter.MainActivity;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.CallsWidgetConfigActivity;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabase;

public class CallsInfoWidget extends AppWidgetProvider {

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
        if (action.equals(AppWidgetManager.ACTION_APPWIDGET_DELETED)) {
            final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                deleteWidgetPreferences(context, new int[]{appWidgetId});
            }
        } else if (action.equals(Constants.CALLS) && widgetIds != null)
            updateWidget(context, AppWidgetManager.getInstance(context), widgetIds, intent.getExtras());
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int[] ids, Bundle bundle) {
        if (bundle.size() == 0) {
            ContentValues dataMap = MyDatabase.readCallsData(MyDatabase.getInstance(context));
            bundle.putLong(Constants.CALLS1, (long) dataMap.get(Constants.CALLS1));
            bundle.putLong(Constants.CALLS2, (long) dataMap.get(Constants.CALLS2));
            bundle.putLong(Constants.CALLS3, (long) dataMap.get(Constants.CALLS3));
        }
        for (int i : ids) {
            SharedPreferences prefs = context.getSharedPreferences(i + Constants.CALLS_TAG + Constants.WIDGET_PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences prefsSIM = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
            if (prefs.getAll().size() == 0) {

            }
            Intent settIntent = new Intent(context, CallsWidgetConfigActivity.class);
            Bundle extras = new Bundle();
            extras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, i);
            settIntent.putExtras(extras);
            settIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent settPIntent = PendingIntent.getActivity(context, i, settIntent, 0);

            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, i, intent, 0);

            int dim = Integer.parseInt(prefs.getString(Constants.PREF_WIDGET_CALLS[11], Constants.ICON_SIZE));
            int dims = Integer.parseInt(prefs.getString(Constants.PREF_WIDGET_CALLS[17], Constants.ICON_SIZE));

            String sizestr = prefs.getString(Constants.PREF_WIDGET_CALLS[12], Constants.TEXT_SIZE);
            String sizestrs = prefs.getString(Constants.PREF_WIDGET_CALLS[16], Constants.TEXT_SIZE);

            RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.calls_info_widget);
        }
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
                if (aChildren.replace(".xml", "").equalsIgnoreCase(String.valueOf(j) + Constants.CALLS_TAG + Constants.WIDGET_PREFERENCES))
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
                if (aChildren.replace(".xml", "").equalsIgnoreCase(String.valueOf(j) + Constants.CALLS_TAG + Constants.WIDGET_PREFERENCES))
                    if (new File(dir, aChildren).delete())
                        Toast.makeText(context, R.string.deleted, Toast.LENGTH_LONG).show();
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

