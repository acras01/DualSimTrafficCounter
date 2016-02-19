package ua.od.acros.dualsimtrafficcounter.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.acra.ACRA;

import java.io.File;

import ua.od.acros.dualsimtrafficcounter.MainActivity;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.CallsWidgetConfigActivity;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
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
        } else if (action.equals(Constants.CALLS_BROADCAST_ACTION) && widgetIds != null)
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
            if (prefs.getAll().size() == 0) {
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[1], true); //Show names
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[2], true); //Show icons
                edit.putString(Constants.PREF_WIDGET_CALLS[3], "none"); //SIM1 icon
                edit.putString(Constants.PREF_WIDGET_CALLS[4], "none"); //SIM2 icon
                edit.putString(Constants.PREF_WIDGET_CALLS[5], "none"); //SIM3 icon
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[6], false); //SIM1 user icon
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[7], false); //SIM2 user icon
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[8], false); //SIM3 user icon
                edit.putString(Constants.PREF_WIDGET_CALLS[9], Constants.ICON_SIZE); //Icon size
                edit.putString(Constants.PREF_WIDGET_CALLS[10], Constants.TEXT_SIZE); //Font size
                edit.putInt(Constants.PREF_WIDGET_CALLS[11], Color.WHITE); //Text color
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[12], true); //Use background
                edit.putInt(Constants.PREF_WIDGET_CALLS[13], Color.TRANSPARENT); //Background color
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[14], true); //Show divider
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[15], true); //Show SIM1
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[16], true); //Show SIM2
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[17], true); //Show SIM3
                edit.apply();
            }
            Intent settIntent = new Intent(context, CallsWidgetConfigActivity.class);
            Bundle extras = new Bundle();
            extras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, i);
            settIntent.putExtras(extras);
            settIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent settPIntent = PendingIntent.getActivity(context, i, settIntent, 0);

            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, i, intent, 0);

            int dim = Integer.parseInt(prefs.getString(Constants.PREF_WIDGET_CALLS[9], Constants.ICON_SIZE));

            String sizestr = prefs.getString(Constants.PREF_WIDGET_CALLS[10], Constants.TEXT_SIZE);

            RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.calls_info_widget);

            //SIM1
            if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[15], true)) {
                updateViews.setTextViewText(R.id.totSIM1, DataFormat.formatCallDuration(context, bundle.getLong(Constants.CALLS1, 0)));
                String title1 = "";
                updateViews.setViewVisibility(R.id.operSIM1, View.GONE);
                if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[1], true)) {
                    if (bundle.getString(Constants.OPERATOR1, "").equals(""))
                        title1 = "SIM1";
                    else
                        title1 = bundle.getString(Constants.OPERATOR1, "");
                    updateViews.setViewVisibility(R.id.operSIM1, View.VISIBLE);
                    updateViews.setTextViewText(R.id.operSIM1, title1);
                }

                if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[2], true)) {
                    if (!prefs.getBoolean(Constants.PREF_WIDGET_CALLS[6], false))
                        Picasso.with(context)
                                .load(context.getResources().getIdentifier(prefs.getString(Constants.PREF_WIDGET_CALLS[3], "none"), "drawable", context.getPackageName()))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo1, new int[]{i});
                    else
                        Picasso.with(context)
                                .load(new File(prefs.getString(Constants.PREF_WIDGET_CALLS[3], "")))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo1, new int[]{i});

                    updateViews.setViewVisibility(R.id.logo1, View.VISIBLE);
                    updateViews.setOnClickPendingIntent(R.id.logo1, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.operSIM1, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM1, pendingIntent);
                } else {
                    updateViews.setViewVisibility(R.id.logo1, View.GONE);
                    updateViews.setOnClickPendingIntent(R.id.operSIM1, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM1, settPIntent);
                }
                if (!sizestr.equals("")) {
                    updateViews.setFloat(R.id.totSIM1, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.operSIM1, "setTextSize", Float.parseFloat(sizestr));
                } else {
                    updateViews.setFloat(R.id.totSIM1, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.operSIM1, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                }
                updateViews.setInt(R.id.totSIM1, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_CALLS[11], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setInt(R.id.operSIM1, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_CALLS[11], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setViewVisibility(R.id.simLayout1, View.VISIBLE);
            } else
                updateViews.setViewVisibility(R.id.simLayout1, View.GONE);

            //SIM2
            if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[16], true)) {
                updateViews.setTextViewText(R.id.totSIM2, DataFormat.formatCallDuration(context, bundle.getLong(Constants.CALLS2, 0)));
                String title2 = "";
                updateViews.setViewVisibility(R.id.operSIM2, View.GONE);
                if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[1], true)) {
                    if (bundle.getString(Constants.OPERATOR2, "").equals(""))
                        title2 = "SIM2";
                    else
                        title2 = bundle.getString(Constants.OPERATOR2);
                    updateViews.setViewVisibility(R.id.operSIM2, View.VISIBLE);
                    updateViews.setTextViewText(R.id.operSIM2, title2);
                }
                if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[2], true)) {
                    if (!prefs.getBoolean(Constants.PREF_WIDGET_CALLS[7], false))
                        Picasso.with(context)
                                .load(context.getResources().getIdentifier(prefs.getString(Constants.PREF_WIDGET_CALLS[4], "none"), "drawable", context.getPackageName()))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo2, new int[]{i});
                    else
                        Picasso.with(context)
                                .load(new File(prefs.getString(Constants.PREF_WIDGET_CALLS[4], "")))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo2, new int[]{i});

                    updateViews.setViewVisibility(R.id.logo2, View.VISIBLE);
                    updateViews.setOnClickPendingIntent(R.id.logo2, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.operSIM2, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM2, pendingIntent);
                } else {
                    updateViews.setViewVisibility(R.id.logo2, View.GONE);
                    updateViews.setOnClickPendingIntent(R.id.operSIM2, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM2, settPIntent);
                }
                if (!sizestr.equals("")) {
                    updateViews.setFloat(R.id.totSIM2, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.operSIM2, "setTextSize", Float.parseFloat(sizestr));
                } else {
                    updateViews.setFloat(R.id.totSIM2, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.operSIM2, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                }
                updateViews.setInt(R.id.totSIM2, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_CALLS[11], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setInt(R.id.operSIM2, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_CALLS[11], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setViewVisibility(R.id.simLayout2, View.VISIBLE);
                if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[15], true)) {
                    if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[14], true))
                        updateViews.setViewVisibility(R.id.stub1, View.VISIBLE);
                    else
                        updateViews.setViewVisibility(R.id.stub1, View.GONE);
                } else
                    updateViews.setViewVisibility(R.id.stub1, View.GONE);
            } else {
                updateViews.setViewVisibility(R.id.simLayout2, View.GONE);
                updateViews.setViewVisibility(R.id.stub1, View.GONE);
            }

            //SIM3
            if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[17], true)) {
                updateViews.setTextViewText(R.id.totSIM3, DataFormat.formatCallDuration(context, bundle.getLong(Constants.CALLS3, 0)));
                String title3 = "";
                if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[1], true)) {
                    updateViews.setViewVisibility(R.id.operSIM3, View.GONE);
                    if (bundle.getString(Constants.OPERATOR3, "").equals(""))
                        title3 = "SIM3";
                    else
                        title3 = bundle.getString(Constants.OPERATOR3);
                    updateViews.setViewVisibility(R.id.operSIM3, View.VISIBLE);
                    updateViews.setTextViewText(R.id.operSIM3, title3);
                }

                if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[2], true)) {

                    if (!prefs.getBoolean(Constants.PREF_WIDGET_CALLS[8], false))
                        Picasso.with(context)
                                .load(context.getResources().getIdentifier(prefs.getString(Constants.PREF_WIDGET_CALLS[5], "none"), "drawable", context.getPackageName()))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo3, new int[]{i});
                    else
                        Picasso.with(context)
                                .load(new File(prefs.getString(Constants.PREF_WIDGET_CALLS[5], "")))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo3, new int[]{i});

                    updateViews.setViewVisibility(R.id.logo3, View.VISIBLE);
                    updateViews.setOnClickPendingIntent(R.id.logo3, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.operSIM3, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM3, pendingIntent);
                } else {
                    updateViews.setViewVisibility(R.id.logo3, View.GONE);
                    updateViews.setOnClickPendingIntent(R.id.operSIM3, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM3, settPIntent);
                }
                if (!sizestr.equals("")) {
                    updateViews.setFloat(R.id.totSIM3, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.operSIM3, "setTextSize", Float.parseFloat(sizestr));
                } else {
                    updateViews.setFloat(R.id.totSIM3, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.operSIM3, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                }
                updateViews.setInt(R.id.totSIM3, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_CALLS[11], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setInt(R.id.operSIM3, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_CALLS[11], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setViewVisibility(R.id.simLayout3, View.VISIBLE);
                if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[15], true) ||
                        prefs.getBoolean(Constants.PREF_WIDGET_CALLS[16], true)) {
                    if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[14], true))
                        updateViews.setViewVisibility(R.id.stub3, View.VISIBLE);
                    else
                        updateViews.setViewVisibility(R.id.stub3, View.GONE);
                } else
                    updateViews.setViewVisibility(R.id.stub3, View.GONE);
            } else {
                updateViews.setViewVisibility(R.id.simLayout3, View.GONE);
                updateViews.setViewVisibility(R.id.stub3, View.GONE);
            }

            //BACKGROUND
            if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[12], true)) {
                updateViews.setInt(R.id.background, "setColorFilter", prefs.getInt(Constants.PREF_WIDGET_CALLS[13], ContextCompat.getColor(context, R.color.background)));
                updateViews.setViewVisibility(R.id.background, View.VISIBLE);
            } else
                updateViews.setViewVisibility(R.id.background, View.GONE);

            //UPDATE
            appWidgetManager.updateAppWidget(i, updateViews);
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

