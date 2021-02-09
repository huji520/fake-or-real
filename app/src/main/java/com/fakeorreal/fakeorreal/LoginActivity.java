package com.fakeorreal.fakeorreal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.util.Map;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    public static String GUEST = "Guest";
    public static String WRONG_PASSWORD = "Wrong password!";
    public static String USERNAME_NOT_EXIST = "Username does not exist!";
    public static String PLEASE_ENTER_USERNAME = "Please enter username!";
    public static String REMEMBER_ME = "remember_me";
    public static String LOCAL_JSON = "user_json";
    public static String SP_FILE = "sp";


    private FirebaseFirestore _FireStore;
    private TextInputLayout _userNameInput;
    private TextInputLayout _passwordInput;
    private CheckBox _rememberMe;
    private ProgressBar _progressBar;
    private FakeOrRealApp _app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        _app = (FakeOrRealApp) getApplication();
        _FireStore = _app.getFireStoreInstance();

        // first, we check if the phone on remember me mode
        SharedPreferences sp = getSharedPreferences(SP_FILE, MODE_PRIVATE);
        boolean remember_me = sp.getBoolean(REMEMBER_ME, false);
        if (remember_me){
            String json = sp.getString(LOCAL_JSON, null);
            User user = new Gson().fromJson(json, User.class);
            _app.setUser(user);
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra(MainActivity.INTENT_USER, user);
            startActivity(intent);
            finish();
        }
        setSkipLoginButton();
        setLoginButton();
        setRegisterButton();
        setForgotPasswordButton();


    }

    private void setLoginButton()
    {
        Button login = findViewById(R.id.sign_in);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // inflate the layout of the game_popup_bg window
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                final View popupView = inflater.inflate(R.layout.popup_sign_in, null);

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
                        InputMethodManager imm = (InputMethodManager) LoginActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(popupView.getWindowToken(), 0);
                        return true;
                    }
                });
                setPopupLoginButton(popupView);
            }
        });
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

                // in case of username and password are not empty, continue to find a match
                findMatch(username, password, _rememberMe.isChecked());

            }
        });
    }

    private void setRegisterButton() {
        Button sign_up = findViewById(R.id.sign_up);
        sign_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setSkipLoginButton() {
        Button skip_login = findViewById(R.id.skip_login);
        skip_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // login as a guest
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                _app.setUser(new User(GUEST, "", ""));
                startActivity(intent);
                finish();
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
                                // username exist and password is match! bingo!
                                loginSuccess(map, rememberMe);
                            }
                            else {
                                // password is not match to that username
                                Tools.exceptionToast(getApplicationContext(), WRONG_PASSWORD);
                                _progressBar.setVisibility(ProgressBar.INVISIBLE);
                            }
                            return;
                        }
                    }
                    // this username not exist in fireBase
                    Tools.exceptionToast(getApplicationContext(), USERNAME_NOT_EXIST);
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
            // if user checked the remember me checkbox, then save his username on the phone.
            editor.putBoolean(REMEMBER_ME, true);
            editor.putString(LOCAL_JSON, new Gson().toJson(user)).apply();
        }
        else {
            // username didn't check, remove the user from the phone, if exist.
            editor.putBoolean(REMEMBER_ME, false);
            editor.remove(LOCAL_JSON);
        }
        editor.apply();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void setForgotPasswordButton() {
        Button forgot_password = findViewById(R.id.forgot_password);
        forgot_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // inflate the layout of the game_popup_bg window
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                final View popupView = inflater.inflate(R.layout.popup_forgot_password, null);
                popupView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        InputMethodManager imm = (InputMethodManager) LoginActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(popupView.getWindowToken(), 0);
                        return true;
                    }
                });
                final EditText forgot_password_username = popupView.findViewById(R.id.forgot_password_username);

                // create the game_popup_bg window
                int width = 1000;
                int height = 500;
                final PopupWindow popupWindow = new PopupWindow(popupView, width, height);
                popupWindow.setFocusable(true);
                popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);

                Button exit_popup = popupView.findViewById(R.id.exit_popup);
                exit_popup.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });

                Button _send_password_button = popupView.findViewById(R.id.send_password);
                _send_password_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String input_username = forgot_password_username.getText().toString().trim();

                        // Disappear the keyboard
                        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        mgr.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

                        if (input_username.equals("")) {
                            // don't allow empty username
                            Tools.exceptionToast(getApplicationContext(), PLEASE_ENTER_USERNAME);
                            return;
                        }

                        _FireStore.collection("users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @SuppressLint("IntentReset")
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()){
                                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())){
                                        Map<String, Object> map = document.getData();
                                        String username = (String)map.get(MainActivity.FIREBASE_DOCUMENT_USERNAME);
                                        if (input_username.equals(username)) {
                                            popupWindow.dismiss();
                                            // send mail to that username
                                            sendEmail(username, (String)map.get(MainActivity.FIREBASE_DOCUMENT_EMAIL), (String)map.get(MainActivity.FIREBASE_DOCUMENT_PASSWORD));
                                            return;
                                        }
                                    }
                                    // username not exist, raise a toast
                                    Tools.exceptionToast(getApplicationContext(), USERNAME_NOT_EXIST);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    @SuppressLint("IntentReset")
    private void sendEmail(final String username, final String email, final String password) {
        String subject = "Forgot Password";
        String message = "Hello " + username + ",\nYour password is: " + password;
        JavaMailAPI javaMailAPI = new JavaMailAPI(email, subject, message);
        javaMailAPI.execute();
        Tools.exceptionToast(getApplicationContext(), "Password was sent to " + email);
    }

}