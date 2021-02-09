package com.fakeorreal.fakeorreal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout _userNameInput;
    private TextInputLayout _emailInput;
    private TextInputLayout _passwordInput;
    private TextInputLayout _rePasswordInput;
    private FirebaseFirestore _FireStore;
    private ProgressBar _progressBar;
    private FakeOrRealApp _app;

    public static String EMPTY_FIELD = "Field can't be empty!";
    public static String UNVALID_EMAIL = "Please enter a valid email!";
    public static String PASSWORD_NOT_MATCH = "Passwords not identical!";
    public static String USERNAME_OR_MAIL_EXIST = "Username or Email already exists!";
    public static String USERNAME_DEFAULT_HINT = "Username";
    public static String EMAIL_DEFAULT_HINT = "exmaple@gmail.com";
    public static String PASSWORD_DEFAULT_HINT = "Password";
    public static String RE_PASSWORD_DEFAULT_HINT = "re-Password";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        _progressBar = findViewById(R.id.register_progress_bar);

        hideKeyboard();
        _userNameInput = findViewById(R.id.username);
        _emailInput = findViewById(R.id.email);
        _passwordInput = findViewById(R.id.password);
        _rePasswordInput = findViewById(R.id.re_password);

        if (savedInstanceState != null){
            Objects.requireNonNull(_userNameInput.getEditText()).setText(savedInstanceState.getString("username_input"));
            Objects.requireNonNull(_emailInput.getEditText()).setText(savedInstanceState.getString("email_input"));
            Objects.requireNonNull(_passwordInput.getEditText()).setText(savedInstanceState.getString("password_input"));
            Objects.requireNonNull(_rePasswordInput.getEditText()).setText(savedInstanceState.getString("re_password_input"));
        }
        _app = (FakeOrRealApp) getApplication();
        _FireStore = _app.getFireStoreInstance();
        submitRegister();
        backToLogin();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("username_input", Objects.requireNonNull(_userNameInput.getEditText()).getText().toString().trim());
        outState.putString("email_input", Objects.requireNonNull(_emailInput.getEditText()).getText().toString().trim());
        outState.putString("password_input", Objects.requireNonNull(_passwordInput.getEditText()).getText().toString().trim());
        outState.putString("re_password_input", Objects.requireNonNull(_rePasswordInput.getEditText()).getText().toString().trim());
        super.onSaveInstanceState(outState);
    }

    /**
     * hides the keyboard when clicking on the screen (outside of an editText field)
     */
    private void hideKeyboard()
    {
        final View mainScreen = findViewById(R.id.register);
        mainScreen.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) RegisterActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mainScreen.getWindowToken(), 0);
                return true;
            }
        });
    }

    public void backToLogin(){
        Button back = findViewById(R.id.back_register);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // in case we back to login from the main, we give-up on the remember mode
                SharedPreferences sp = getSharedPreferences("sp", MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean("remember_me", false);
                editor.apply();
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    public void submitRegister(){
        Button button = findViewById(R.id.submit_register);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = Objects.requireNonNull(_userNameInput.getEditText()).getText().toString().trim();
                String email = Objects.requireNonNull(_emailInput.getEditText()).getText().toString().trim();
                String password = Objects.requireNonNull(_passwordInput.getEditText()).getText().toString().trim();
                String re_password = Objects.requireNonNull(_rePasswordInput.getEditText()).getText().toString().trim();
                _userNameInput.setHint(USERNAME_DEFAULT_HINT);
                _emailInput.setHint(EMAIL_DEFAULT_HINT);
                _passwordInput.setHint(PASSWORD_DEFAULT_HINT);
                _rePasswordInput.setHint(RE_PASSWORD_DEFAULT_HINT);

                // username
                if (username.isEmpty()){
                    _userNameInput.setError(EMPTY_FIELD);
                    _userNameInput.setHint(EMPTY_FIELD);
                    return;
                }
                _userNameInput.setError(null);

                // email
                if (email.isEmpty()){
                    _emailInput.setError(EMPTY_FIELD);
                    _emailInput.setHint(EMPTY_FIELD);
                    return;
                }
                _emailInput.setError(null);
                if (!Tools.isValidEmail(email)){
                    _emailInput.setError(UNVALID_EMAIL);
                    _emailInput.setHint(UNVALID_EMAIL);
                    return;
                }
                _emailInput.setError(null);

                // password
                if (password.isEmpty()){
                    _passwordInput.setError(EMPTY_FIELD);
                    _passwordInput.setHint(EMPTY_FIELD);
                    return;
                }
                _passwordInput.setError(null);

                // re-password
                if (re_password.isEmpty()){
                    _rePasswordInput.setError(EMPTY_FIELD);
                    _rePasswordInput.setHint(EMPTY_FIELD);
                    return;
                }
                _rePasswordInput.setError(null);

                if (!re_password.equals(password)){
                    _rePasswordInput.setError(PASSWORD_NOT_MATCH);
                    _rePasswordInput.setHint(PASSWORD_NOT_MATCH);
                    return;
                }
                _rePasswordInput.setError(null);

                // Disappear the keyboard
                InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

                // We get here only if the inputs are valid (regardless the firebase checking)
                register(username, email, password);
            }
        });
    }

    void addUserToFirebase(User user){
        Map<String, Object> map = new HashMap<>();
        map.put(MainActivity.FIREBASE_DOCUMENT_USERNAME, user.get_username());
        map.put(MainActivity.FIREBASE_DOCUMENT_EMAIL, user.get_email());
        map.put(MainActivity.FIREBASE_DOCUMENT_PASSWORD, user.get_password());
        map.put(MainActivity.FIREBASE_DOCUMENT_EMOJI, String.valueOf(user.get_emoji()));
        map.put(MainActivity.FIREBASE_DOCUMENT_CORRECT_GUESSES, "0");
        map.put(MainActivity.FIREBASE_DOCUMENT_WRONG_GUESSES, "0");
        map.put(MainActivity.FIREBASE_DOCUMENT_TOTAL_SCORE, "0");
        map.put(MainActivity.FIREBASE_DOCUMENT_JOIN_TIME, Timestamp.now());
        map.put(MainActivity.FIREBASE_DOCUMENT_ID, user.get_id());
        map.put(MainActivity.FIREBASE_DOCUMENT_SOUND, user.get_sound_choice());
        _FireStore.collection(MainActivity.FIREBASE_COLLECTION_USERS).document(user.get_id()).set(map);
    }

    void register(final String inputUsername, final String inputEmail, final String inputPassword){
        // if we here - inputs are valid. But still need to check validation against the firebase
        _progressBar.setVisibility(View.VISIBLE);
        _FireStore.collection(MainActivity.FIREBASE_COLLECTION_USERS).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()){
                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())){
                        Map<String, Object> map = document.getData();
                        String username = (String)map.get(MainActivity.FIREBASE_DOCUMENT_USERNAME);
                        String email = (String)map.get(MainActivity.FIREBASE_DOCUMENT_EMAIL);
                        if (inputUsername.equals(username) || inputEmail.equals(email)) {
                            // check if email or username are already in firebase
                            _progressBar.setVisibility(View.INVISIBLE);
                            Tools.exceptionToast(getApplicationContext(), USERNAME_OR_MAIL_EXIST);
                            return;
                        }
                    }
                    // create a new user succeeded
                    User user = new User(inputUsername, inputEmail, inputPassword);
                    addUserToFirebase(user);
                    // moving to the main activity with the new user
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    _app.setUser(user);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Button back = findViewById(R.id.back_register);
        back.callOnClick();
    }
}
