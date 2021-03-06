package com.phaseshiftlab.cyrilliscript.statswidget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.phaseshiftlab.cyrilliscript.R;
import com.phaseshiftlab.languagelib.StatisticsProvider;
import com.phaseshiftlab.languagelib.UserDictionaryProvider;

import java.util.HashMap;
import java.util.Map;

public class StatsWidgetService extends Service {
    private static final String TAG = "Cyrilliscript";
    private RemoteViews remoteViews;
    private Intent intent;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.intent = intent;

        requestStatistics();

        super.onStartCommand(intent, START_FLAG_REDELIVERY, startId);

        return START_STICKY;
    }

    private void requestStatistics() {
        Log.d(TAG, "executing statistics task...");
        new RequestStatiscticsTask().execute();
    }

    private void updateWidgetViews(Map<String, String> result) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
                .getApplicationContext());

        if(intent != null) {
            int[] allWidgetIds = intent
                    .getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

            for (int widgetId : allWidgetIds) {
                remoteViews = new RemoteViews(this
                        .getApplicationContext().getPackageName(),
                        R.layout.stats_widget_layout);
                // Set the text
                remoteViews.setTextViewText(R.id.m_c_events_totals,
                        result.get("totalEventsCount"));
                remoteViews.setTextViewText(R.id.m_c_events_avg,
                        result.get("totalAverageConfidence"));
                remoteViews.setTextViewText(R.id.user_defined_words,
                        result.get("totalUserDefinedWords"));
                // Register an onClickListener
                Intent clickIntent = new Intent(this.getApplicationContext(),
                        StatsWidgetProvider.class);

                clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
                        allWidgetIds);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        getApplicationContext(), widgetId, clickIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.layout, pendingIntent);
                appWidgetManager.updateAppWidget(widgetId, remoteViews);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class RequestStatiscticsTask extends AsyncTask<String, Integer, Map<String, String>> {

        @Override
        protected Map<String, String> doInBackground(String... params) {
            Map<String, String> resultMap = new HashMap<>();
            Cursor stats = getContentResolver().query(StatisticsProvider.CONTENT_URI, null, null, null, null);
            if(stats != null) {
                while (!stats.isAfterLast()) {
                    Integer totalEventsCount = stats.getInt(0);
                    Integer totalAverageConfidence = stats.getInt(1);
                    resultMap.put("totalEventsCount", "total recognition events: " + totalEventsCount);
                    resultMap.put("totalAverageConfidence", "total average confidence: " + totalAverageConfidence + "%");
                    stats.moveToNext();
                }
                stats.close();
            }

            Uri userDefinedDictWordCountUri = Uri.withAppendedPath(UserDictionaryProvider.CONTENT_URI, "count");
            Cursor userDefinedWordCount = getContentResolver().query(userDefinedDictWordCountUri, null, null, null, null);

            int userDefinedWords = unpackWordCount(userDefinedWordCount);

            resultMap.put("totalUserDefinedWords", "total words saved: " + userDefinedWords);

            return resultMap;
        }

        private int unpackWordCount(Cursor userDefinedWordCount) {
            if(userDefinedWordCount != null && userDefinedWordCount.moveToFirst()) {
                return userDefinedWordCount.getInt(0);
            } else {
                return 0;
            }
        }

        protected void onPostExecute(Map<String, String> result) {
            if (result.size() > 0) {
                updateWidgetViews(result);
            }
        }

    }
}
