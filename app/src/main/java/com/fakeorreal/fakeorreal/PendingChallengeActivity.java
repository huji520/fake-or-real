package com.fakeorreal.fakeorreal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.fakeorreal.fakeorreal.LoginActivity.LOCAL_JSON;
import static com.fakeorreal.fakeorreal.LoginActivity.REMEMBER_ME;
import static com.fakeorreal.fakeorreal.LoginActivity.SP_FILE;

public class PendingChallengeActivity extends AppCompatActivity {
    public static String CHALLENGER_NAME_KEY = "challenger_name";
    public static String CHALLENGER_TOKEN_KEY = "challenger_token";
    public static String CHALLENGE_SCORE_KEY = "challenge_score";
    public static String CHALLENGED_NAME = "challenged_name";

    private User _user;
    private String _challengerName;
    private String _challengerToken;
    private LayoutInflater _inflater;
    private FirebaseFirestore _FireStore;
    private TextInputLayout _userNameInput;
    private TextInputLayout _passwordInput;
    private CheckBox _rememberMe;
    private ProgressBar _progressBar;
    private FakeOrRealApp _app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_challenge);

        _app = (FakeOrRealApp) getApplication();
        _FireStore = _app.getFireStoreInstance();
        // create the game_popup_bg window
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        String challengeInstructions = getResources().getString(R.string.got_challenge_instructions);
        _challengerName = getIntent().getStringExtra(CHALLENGER_NAME_KEY);
        _challengerToken = getIntent().getStringExtra(CHALLENGER_TOKEN_KEY);
        challengeInstructions = String.format(challengeInstructions, _challengerName);
        TextView challengeText = findViewById(R.id.got_challenge_instructions);
        challengeText.setText(challengeInstructions);
        Button cancel = findViewById(R.id.not_accepting_challenge);
        cancel.setOnClickListener(CancelChallenge());
        FakeOrRealApp app = (FakeOrRealApp) getApplication();
        SharedPreferences sp = getSharedPreferences(SP_FILE, MODE_PRIVATE);
        boolean remember_me = sp.getBoolean(REMEMBER_ME, false);
        if (remember_me){
            String json = sp.getString(LOCAL_JSON, null);
            _user = new Gson().fromJson(json, User.class);
            app.setUser(_user);
        }
        else {
            _user = app.getUser();
        }
        Button accept = findViewById(R.id.accept_challenge);
        accept.setOnClickListener(AcceptChallenge(inflater));
    }

    private View.OnClickListener CancelChallenge() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        };
    }

    private View.OnClickListener AcceptChallenge(final LayoutInflater inflater) {
        _inflater = inflater;
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_user != null) {
                    StartChallenge();
                }
                else {
                    loginPopup(v);
                }
            }
        };
    }

    private void StartChallenge() {
        Intent fromIntent = getIntent();
        Intent intent = new Intent(PendingChallengeActivity.this, GameActivity.class);
        intent.putExtras(fromIntent);
        startActivityForResult(intent, MainActivity.CHALLENGE_TO_GAME);
    }

    private void loginPopup(View v)
    {
        final View popupView = _inflater.inflate(R.layout.popup_sign_in, null);
        // create the game_popup_bg window
        int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        int width = ViewGroup.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height);
        popupWindow.setFocusable(true);
        popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);

        Button exit_popup = popupView.findViewById(R.id.exit_sign_in);
        exit_popup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) PendingChallengeActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(popupView.getWindowToken(), 0);
                return true;
            }
        });
        setPopupLoginButton(popupView);
    }

    void setPopupLoginButton(View popup) {
        Button popupLogin = popup.findViewById(R.id.popup_sign_in);
        _userNameInput = popup.findViewById(R.id.username_login);
        _passwordInput = popup.findViewById(R.id.password_login);
        _rememberMe = popup.findViewById(R.id.remember_me_checkbox);
        _progressBar = popup.findViewById(R.id.sign_in_progress_bar);

        popupLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _progressBar.setVisibility(ProgressBar.VISIBLE);
                String username = Objects.requireNonNull(_userNameInput.getEditText()).getText().toString().trim();
                String password = Objects.requireNonNull(_passwordInput.getEditText()).getText().toString().trim();
                _userNameInput.setHint(RegisterActivity.USERNAME_DEFAULT_HINT);
                _passwordInput.setHint(RegisterActivity.PASSWORD_DEFAULT_HINT);

                // username
                if (username.isEmpty()) {
                    _progressBar.setVisibility(ProgressBar.INVISIBLE);
                    _userNameInput.setError(RegisterActivity.EMPTY_FIELD);
                    _userNameInput.setHint(RegisterActivity.EMPTY_FIELD);
                    return;
                }
                _userNameInput.setError(null);

                // password
                if (password.isEmpty()) {
                    _progressBar.setVisibility(ProgressBar.INVISIBLE);
                    _passwordInput.setError(RegisterActivity.EMPTY_FIELD);
                    _passwordInput.setHint(RegisterActivity.EMPTY_FIELD);
                    return;
                }
                _passwordInput.setError(null);
                findMatch(username, password, _rememberMe.isChecked());

            }
        });
    }

    void findMatch(final String inputUsername, final String inputPassword, final boolean rememberMe){
        _FireStore.collection(MainActivity.FIREBASE_COLLECTION_USERS).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()){
                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())){
                        Map<String, Object> map = document.getData();
                        String username = (String)map.get(MainActivity.FIREBASE_DOCUMENT_USERNAME);
                        String password = (String)map.get(MainActivity.FIREBASE_DOCUMENT_PASSWORD);
                        if (inputUsername.equals(username)) {
                            if (inputPassword.equals(password)) {
                                loginSuccess(map, rememberMe);
                            }
                            else {
                                Tools.exceptionToast(getApplicationContext(), LoginActivity.WRONG_PASSWORD);
                                _progressBar.setVisibility(ProgressBar.INVISIBLE);
                            }
                            return;
                        }
                    }
                    Tools.exceptionToast(getApplicationContext(), LoginActivity.USERNAME_NOT_EXIST);
                    _progressBar.setVisibility(ProgressBar.INVISIBLE);
                }
            }
        });
    }

    private void loginSuccess(Map<String, Object> map, boolean rememberMe) {
        User user = new User(map);
        _app.setUser(user);

        SharedPreferences sp = getSharedPreferences(SP_FILE, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        if (rememberMe){
            editor.putBoolean(REMEMBER_ME, true);
            editor.putString(LOCAL_JSON, new Gson().toJson(user)).apply();
        }
        else {
            editor.putBoolean(REMEMBER_ME, false);
            editor.remove(LOCAL_JSON);
        }
        editor.apply();
        StartChallenge();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final FakeOrRealApp app = (FakeOrRealApp) getApplication();
        User challenged = app.getUser();
        // challenger name and id is under private members
        if (data != null){
            AnswerChallenger(data);
        }
        Intent intent = new Intent(PendingChallengeActivity.this, MainActivity.class);
        startActivity(intent);
        finish();

    }

    private void AnswerChallenger(Intent challengeData) {
        Gson gson = new Gson();
        String to = _challengerToken;
        FCMServer.NotificationData data = new FCMServer.NotificationData();
        String challengedName = challengeData.getStringExtra(CHALLENGED_NAME);
        int score = challengeData.getIntExtra(CHALLENGE_SCORE_KEY, -1);
        String posTitle = String.format(getResources().getString(R.string.challenge_success_title), challengedName);
        String negTitle = String.format(getResources().getString(R.string.challenge_fail_title), challengedName);
        data.title = score > 0 ? posTitle : negTitle;
        String posMsg = String.format(getResources().getString(R.string.challenge_success_msg), score);
        String negMsg = getResources().getString(R.string.challenge_fail_msg);
        data.msg = score > 0 ? posMsg : negMsg;
        data.challengeScore = score;
        FCMServer.PushNotification notification = new FCMServer.PushNotification();
        notification.data = data;
        notification.to = to;
        Data inputData = new Data.Builder().putString(PostChallengeWorker.NOTIFICATION_KEY, gson.toJson(notification)).build();
        UUID workTag = UUID.randomUUID();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest.Builder builder = new OneTimeWorkRequest.Builder(PostChallengeWorker.class);
        builder.setConstraints(constraints);
        builder.setInputData(inputData);
        builder.addTag(workTag.toString());
        WorkRequest request = builder.build();
        WorkManager manager = WorkManager.getInstance(this);
        manager.enqueue(request);
    }
}
