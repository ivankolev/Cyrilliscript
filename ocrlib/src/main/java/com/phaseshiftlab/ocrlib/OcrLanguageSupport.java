package com.phaseshiftlab.ocrlib;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.phaseshiftlab.cyrilliscript.eventslib.DownloadSuccessEvent;

import org.greenrobot.eventbus.EventBus;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.content.Context.MODE_PRIVATE;

public class OcrLanguageSupport {
    private static final String TAG = "Cyrilliscript";
    private static final String API_BASE_URL = "https://github.com/";
    private static final OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
    private static final Retrofit.Builder builder = new Retrofit.Builder().baseUrl(API_BASE_URL);
    private static final Retrofit retrofit = builder.client(httpClient.build()).build();
    private static final GitHubTessdataInterface client = retrofit.create(GitHubTessdataInterface.class);

    public static void downloadTesseractData(final Context context, final String tesseractFile) {
        Log.d(TAG, "attempt to download " + tesseractFile);
        Call<ResponseBody> call = client.tessDataFile(tesseractFile);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "server contacted and has file");
                    final SharedPreferences preferences = context.getSharedPreferences(TAG, MODE_PRIVATE);
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            boolean writtenToDisk = OcrFileUtils.writeResponseBodyToDisk(response.body(), tesseractFile);

                            Log.d(TAG, "file download was a success? " + writtenToDisk);
                            if(writtenToDisk) {
                                NotificationCompat.Builder mBuilder =
                                        new NotificationCompat.Builder(context)
                                                .setSmallIcon(R.drawable.ic_check_box_black_24dp)
                                                .setContentTitle("Download success")
                                                .setContentText("You can now switch between languages in Cyrilliscript");
                                NotificationManager mNotificationManager =
                                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                mNotificationManager.notify(0, mBuilder.build());
                                preferences.edit().putBoolean(tesseractFile, true).apply();
                                EventBus.getDefault().post(new DownloadSuccessEvent("SUCCESS"));
                            }
                            return null;
                        }
                    }.execute();
                }
                else {
                    Log.d(TAG, "server contact failed");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "error");
            }
        });
    }
}
