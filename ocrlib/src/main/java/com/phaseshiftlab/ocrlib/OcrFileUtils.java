package com.phaseshiftlab.ocrlib;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import com.phaseshiftlab.cyrilliscript.eventslib.DownloadSuccessEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;

import static android.content.Context.MODE_PRIVATE;

class OcrFileUtils {
    private static final String TAG = "Cyrilliscript";
    private static final String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/TesseractOCR/";
    private static final String TESSDATA_PATH = DATA_PATH + "tessdata/";


    public static String getDataPath() {
        return DATA_PATH;
    }

    private static String getTessdataPath() {
        return TESSDATA_PATH;
    }

    static void prepareTrainedDataFiles(Context context, String langFile, String lang) {
        SharedPreferences preferences = context.getSharedPreferences(TAG, MODE_PRIVATE);
        String[] paths = new String[]{getDataPath(), getTessdataPath()};

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

        if (!tesseractTraineddataFileExist(langFile)) {
            try {
                Log.v(TAG, "Opening .traineddata asset");
                copyTrainedDataFile(context, langFile);
                preferences.edit().putBoolean(lang, true).apply();
                EventBus.getDefault().post(new DownloadSuccessEvent("SUCCESS"));
                Log.v(TAG, "Copied " + langFile);
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + langFile + " " + e.toString());
            }
        }
    }

    public static boolean tesseractTraineddataFileExist(String langFile) {
        return (new File(getTessdataPath() + langFile)).exists();
    }

    public static File[] listLanguageFiles() {
        File directory = new File(getTessdataPath());
        File[] files = directory.listFiles();
        Log.d("Files", "Size: " + files.length);
        for (File file : files) {
            Log.d("Files", "FileName:" + file.getName());
        }
        return files;
    }

    public static boolean deleteLanguageFile(String language) {
        File toDelete = new File(getTessdataPath() + "/" + language + ".traineddata");
        return toDelete.exists() && toDelete.delete();
    }

    private static void copyTrainedDataFile(Context context, String langFile) throws IOException {
        InputStream in = context.getAssets().open("tessdata/" + langFile);
        OutputStream out = new FileOutputStream(new File(getTessdataPath(), langFile));

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }


    public static boolean writeResponseBodyToDisk(ResponseBody body, String tesseractFile) {
        try {
            File languageTrainedDataFile = new File(getTessdataPath() + tesseractFile + ".traineddata");

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(languageTrainedDataFile);

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
