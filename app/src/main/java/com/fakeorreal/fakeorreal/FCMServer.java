package com.fakeorreal.fakeorreal;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

interface FCMServer {
    String AUTH_HEADER = "Authorization: key=AAAAGy6HJFM:APA91bEojC1PwDXRJ9IngoIljVPXGlsodGeIj2P5HSrM82ZVYHHlo1O7X4v8F23zDWxOeZb8HNt_fLtytRI_pFrfuDEbOJ0HashLLlBjWR-M__XuNPmDVpCheY-zqxAogE-tuNccYq1G";
    String CONTENT_HEADER = "Content-Type:application/json";

    class NotificationData {
        String title;
        String msg;
        String imgId;
        boolean isReal;
        String challengerName;
        String challengerToken;
        int challengeScore;
    }


    class PushNotification {
        NotificationData data;
        String to;
    }


    @Headers({AUTH_HEADER, CONTENT_HEADER})
    @POST("fcm/send")
    Call<ResponseBody> PostNotification(@Body PushNotification notification);
}
