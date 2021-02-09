package com.fakeorreal.fakeorreal;

import android.app.Application;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.HashMap;
import java.util.Map;

public class FakeOrRealApp extends Application {
    private User _user;
    private String _firebaseToken;
    private FirebaseFirestore _FireStore;
    private int _scoreToUpdate;

    @Override
    public void onCreate() {
        super.onCreate();
        _scoreToUpdate = -1;
        _FireStore = FirebaseFirestore.getInstance();
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                setFirebaseToken(instanceIdResult.getToken());
            }
        });
    }

    public User getUser() {
        return _user;
    }

    private void postToken() {
        if (_firebaseToken != null && !_user.isGuest() && !_firebaseToken.equals(_user.getFirebaseToken())) {
            _user.setFirebaseToken(_firebaseToken);
            Map<String, Object> map = new HashMap<>();
            map.put(MainActivity.FIREBASE_DOCUMENT_TOKEN, _firebaseToken);
            _FireStore.collection(MainActivity.FIREBASE_COLLECTION_USERS).document(_user.get_id()).set(map, SetOptions.merge());
        }
    }

    public void setUser(User user) {
        _user = user;
        if (_scoreToUpdate > 0) {
            _user.update_totalScore(_scoreToUpdate);
            _scoreToUpdate = -1;
            Map<String, Object> map = new HashMap<>();
            map.put(MainActivity.FIREBASE_DOCUMENT_TOTAL_SCORE, String.valueOf(user.get_records().get_totalScore()));
            _FireStore.collection(MainActivity.FIREBASE_COLLECTION_USERS).document(_user.get_id()).set(map, SetOptions.merge());
        }
        postToken();
    }

    public void setFirebaseToken(String firebaseToken) {
        _firebaseToken = firebaseToken;
        if (_user != null) {
            postToken();
        }
    }

    public FirebaseFirestore getFireStoreInstance() {
        return _FireStore;
    }

    public void setScoreToUpdate(int scoreToUpdate) {
        _scoreToUpdate = scoreToUpdate;
    }
}
