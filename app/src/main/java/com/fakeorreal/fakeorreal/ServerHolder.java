package com.fakeorreal.fakeorreal;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

class ServerHolder {
    private static ServerHolder instance = null;
    public static String BASE_URL = "https://fcm.googleapis.com";

    synchronized static ServerHolder getInstance() {
        if (instance == null) {
            instance = new ServerHolder();
        }
        return instance;
    }

    final FCMServer server;

    private ServerHolder() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        server = retrofit.create(FCMServer.class);
    }
}
