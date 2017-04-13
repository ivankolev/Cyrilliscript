package com.phaseshiftlab.ocrlib;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

interface GitHubTessdataInterface {
    @GET("/tesseract-ocr/tessdata/blob/master/{language}.traineddata?raw=true")
    Call<ResponseBody> tessDataFile(@Path("language") String language);
}
