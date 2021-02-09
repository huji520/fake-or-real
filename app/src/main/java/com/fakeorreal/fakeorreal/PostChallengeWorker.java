package com.fakeorreal.fakeorreal;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class PostChallengeWorker extends Worker {
    static final String NOTIFICATION_KEY = "notification";

    public PostChallengeWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Sends a challenge notification to other users
     * @return success or retry depending on task success
     */
    @NonNull
    @Override
    public Result doWork() {
        Gson gson = new Gson();
        String notificationAsJson = getInputData().getString(NOTIFICATION_KEY);
        FCMServer.PushNotification notification = gson.fromJson(notificationAsJson, FCMServer.PushNotification.class);
        Call<ResponseBody> call = ServerHolder.getInstance().server.PostNotification(notification);
        try {
            Response<ResponseBody> response = call.execute();
            Log.d("WORKER", gson.toJson(response));
            if (response.isSuccessful()) {

                return Result.success();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.retry();
    }
}
