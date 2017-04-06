package com.phaseshiftlab.ocrlib;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.content.Context.MODE_PRIVATE;

public class OcrLanguageSupport {
    private static final String TAG = "Cyrilliscript";
    private static String API_BASE_URL = "https://github.com/";
    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
    private static Retrofit.Builder builder = new Retrofit.Builder().baseUrl(API_BASE_URL);
    private static Retrofit retrofit = builder.client(httpClient.build()).build();
    private static GitHubTessdataInterface client = retrofit.create(GitHubTessdataInterface.class);
    static final String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/TesseractOCR/";
    private static SharedPreferences preferences;

    public static void downloadTesseractData(final String tesseractFile) {
        Log.d(TAG, "attempt to download " + tesseractFile);

        Call<ResponseBody> call = client.tessDataFile(tesseractFile);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "server contacted and has file");

                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            boolean writtenToDisk = writeResponseBodyToDisk(response.body(), tesseractFile);

                            Log.d(TAG, "file download was a success? " + writtenToDisk);
                            if(writtenToDisk) {
                                preferences.edit().putBoolean(tesseractFile, true).apply();
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


    static void prepareTrainedDataFiles(Context context, String langFile, String lang) throws InterruptedException {
        preferences = context.getSharedPreferences(TAG, MODE_PRIVATE);
        String[] paths = new String[]{DATA_PATH, DATA_PATH + "/tessdata"};

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    return;
                } else {
                    Log.v(TAG, "Created directory " + path + " on sdcard");
                }
            }
        }

        if (!(new File(DATA_PATH + "tessdata/" + langFile)).exists()) {
            try {
                Log.v(TAG, "Opening .traineddata asset");
                copyTrainedDataFile(context, langFile);
                preferences.edit().putBoolean(lang, true).apply();
                Log.v(TAG, "Copied " + langFile);
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + langFile + " " + e.toString());
            }
        }
    }

    private static void copyTrainedDataFile(Context context, String langFile) throws IOException {
        InputStream in = context.getAssets().open("tessdata/" + langFile);
        OutputStream out = new FileOutputStream(new File(DATA_PATH + "tessdata/", langFile));

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }



    private static boolean writeResponseBodyToDisk(ResponseBody body, String tesseractFile) {
        try {
            // todo change the file location/name according to your needs
            File futureStudioIconFile = new File(DATA_PATH + "tessdata/" + tesseractFile + ".traineddata");

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(futureStudioIconFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }
}
