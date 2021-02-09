package com.fakeorreal.fakeorreal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class UserAreaActivity extends AppCompatActivity {

    public static String USERNAME_EMPTY = "Username cannot be empty!";
    public static String USERNAME_EXIST = "Username already exist!";
    public static String EMAIL_EMPTY = "Email cannot be empty!";
    public static String EMAIL_EXIST = "Email already exist!";
    public static String PASSWORDS_EMPTY = "Fields cannot be empty!";
    public static String PASSWORDS_NOT_MATCH = "Passwords not identical!";
    public static String USERNAME_CHANGE_TO = "Username change to ";
    public static String PASSWORD_CHANGE_TO = "Password change to ";
    public static String EMAIL_CHANGE_TO = "Email change to ";
    public static String HIDDEN_PASSWORD = "**********";

    User _user;
    TextView _usernameContent;
    TextView _passwordContent;
    TextView _emailContent;
    ImageButton _emojiButton;
    private FirebaseFirestore _FireStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_area);
        FakeOrRealApp app = (FakeOrRealApp) getApplication();
        if (savedInstanceState != null){
            _user = savedInstanceState.getParcelable(MainActivity.ON_SAVE_USER);
        }
        else {
            _user = app.getUser();
        }
        _FireStore = app.getFireStoreInstance();
        // set emoji
        _emojiButton = findViewById(R.id.avatar_image);
        _emojiButton.setImageResource(_user.get_emojiID());
        // set username
        _usernameContent = findViewById(R.id.username_content);
        _usernameContent.setText(_user.get_username());
        // set password
        _passwordContent = findViewById(R.id.password_user_area_content); // default is *********
        // set email
        _emailContent = findViewById(R.id.email_user_area_content);
        _emailContent.setText(_user.get_email());

        setEmojiButton();
        setUsernameButton();
        setEmailButton();
        setPasswordButton();
        setShowPassword();
        setBackToMain();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(MainActivity.ON_SAVE_USER, _user);
        super.onSaveInstanceState(outState);
    }

    private View.OnClickListener emojiClickListener()
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // inflate the layout of the game_popup_bg window
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.popup_emoji_grid, null);

                // create the game_popup_bg window
                int width = LinearLayout.LayoutParams.MATCH_PARENT;
                int height = LinearLayout.LayoutParams.MATCH_PARENT;
                final PopupWindow popupWindow = new PopupWindow(popupView, width, height);
                popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);

                hideKeyboard(popupView);
                RecyclerView.LayoutManager layoutManager = new GridLayoutManager(UserAreaActivity.this, 4);
                RecyclerView recyclerView = popupView.findViewById(R.id.recycler_view);
                recyclerView.setLayoutManager(layoutManager);
                recyclerView.setHasFixedSize(true);
                RecyclerGridAdapter.OnItemClickListener onClick = new RecyclerGridAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(int position) {
                        // when user click on emoji from the grid
                        Map<String, Object> map = new HashMap<>();
                        map.put(MainActivity.FIREBASE_DOCUMENT_EMOJI, String.valueOf(RecyclerGridAdapter.getID(position)));
                        _FireStore.collection(MainActivity.FIREBASE_COLLECTION_USERS).document(_user.get_id()).set(map, SetOptions.merge());
                        _user.set_emoji(RecyclerGridAdapter.getID(position));
                        _emojiButton.setImageResource(_user.get_emojiID());
                        popupWindow.dismiss();
                    }};

                RecyclerGridAdapter recyclerViewAdapter = new RecyclerGridAdapter(onClick);
                recyclerView.setAdapter(recyclerViewAdapter);
            }
        };
    }

    private void setEmojiButton() {
        // opening the grid
        Button setEmojiButton = findViewById(R.id.set_emoji);
        setEmojiButton.setOnClickListener(emojiClickListener());
        _emojiButton.setOnClickListener(emojiClickListener());
    }

    private void setUsernameButton() {
        Button setUsernameButton;
        setUsernameButton = findViewById(R.id.set_userame);
        setUsernameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // inflate the layout of the game_popup_bg window
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.popup_set_username_and_email, null);
                final TextView username = popupView.findViewById(R.id.username_in_set_username_and_mail);
                username.setText(_user.get_username());

                hideKeyboard(popupView);

                // create the game_popup_bg window
                int width = LinearLayout.LayoutParams.WRAP_CONTENT;
                int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                final PopupWindow popupWindow = new PopupWindow(popupView, width, height);
                popupWindow.setFocusable(true);
                popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);

                Button back_button = popupView.findViewById(R.id.back_set_username_and_mail);
                back_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });

                Button save_button = popupView.findViewById(R.id.save_username_and_mail);
                save_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String _newUsername = username.getText().toString().trim();
                        if (_newUsername.equals("")) {
                            // don't allow an empty username
                            Tools.exceptionToast(getApplicationContext(), USERNAME_EMPTY);
                            return;
                        }
                        _FireStore.collection(MainActivity.FIREBASE_COLLECTION_USERS).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()){
                                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())){
                                        Map<String, Object> map = document.getData();
                                        String username = (String)map.get(MainActivity.FIREBASE_DOCUMENT_USERNAME);
                                        if (_newUsername.equals(username)) {
                                            // checking if username already exist in firebase
                                            Tools.exceptionToast(getApplicationContext(), USERNAME_EXIST);
                                            return;
                                        }
                                    }
                                    // user change his name successfully
                                    Map<String, Object> map = new HashMap<>();
                                    map.put(MainActivity.FIREBASE_DOCUMENT_USERNAME, _newUsername);
                                    _FireStore.collection(MainActivity.FIREBASE_COLLECTION_USERS).document(_user.get_id()).set(map, SetOptions.merge());
                                    _user.set_username(_newUsername);
                                    _usernameContent.setText(_newUsername);
                                    popupWindow.dismiss();
                                    Tools.exceptionToast(getApplicationContext(), USERNAME_CHANGE_TO + _newUsername);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void setEmailButton() {
        Button setEmailButton;
        setEmailButton = findViewById(R.id.set_email);
        setEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // inflate the layout of the game_popup_bg window
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.popup_set_username_and_email, null);
                final TextView email = popupView.findViewById(R.id.username_in_set_username_and_mail);
                email.setText(_user.get_email());
                hideKeyboard(popupView);

                // create the game_popup_bg window
                int width = LinearLayout.LayoutParams.WRAP_CONTENT;
                int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                final PopupWindow popupWindow = new PopupWindow(popupView, width, height);
                popupWindow.setFocusable(true);
                popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);

                Button back_button = popupView.findViewById(R.id.back_set_username_and_mail);
                back_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });

                Button save_button = popupView.findViewById(R.id.save_username_and_mail);
                save_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String _newEmail = email.getText().toString().trim();
                        if (_newEmail.equals("")) {
                            // don't allow an empty email
                            Tools.exceptionToast(getApplicationContext(), EMAIL_EMPTY);
                            return;
                        }
                        if (!Tools.isValidEmail(_newEmail)) {
                            // checking if the email is valid
                            Tools.exceptionToast(getApplicationContext(), RegisterActivity.UNVALID_EMAIL);
                            return;
                        }
                        _FireStore.collection(MainActivity.FIREBASE_COLLECTION_USERS).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()){
                                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())){
                                        Map<String, Object> map = document.getData();
                                        String email = (String)map.get(MainActivity.FIREBASE_DOCUMENT_EMAIL);
                                        if (_newEmail.equals(email)) {
                                            // checking if email already exist in firebase
                                            Tools.exceptionToast(getApplicationContext(), EMAIL_EXIST);
                                            return;
                                        }
                                    }
                                    // user change his email successfully
                                    Map<String, Object> map = new HashMap<>();
                                    map.put(MainActivity.FIREBASE_DOCUMENT_EMAIL, _newEmail);
                                    _FireStore.collection(MainActivity.FIREBASE_COLLECTION_USERS).document(_user.get_id()).set(map, SetOptions.merge());
                                    _user.set_email(_newEmail);
                                    _emailContent.setText(_newEmail);
                                    popupWindow.dismiss();
                                    Tools.exceptionToast(getApplicationContext(), EMAIL_CHANGE_TO + _newEmail);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * hides the keyboard when clicking on the screen (outside of an editText field)
     */
    private void hideKeyboard(final View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) UserAreaActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                return true;
            }
        });
    }

    private void setPasswordButton() {
        Button setPasswordButton = findViewById(R.id.set_password);
        setPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // inflate the layout of the game_popup_bg window
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.popup_set_password, null);
                hideKeyboard(popupView);

                // create the game_popup_bg window
                int width = LinearLayout.LayoutParams.WRAP_CONTENT;
                int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                final PopupWindow popupWindow = new PopupWindow(popupView, width, height);
                popupWindow.setFocusable(true);
                popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
                final TextView password = popupView.findViewById(R.id.set_password_content);
                final TextView re_password = popupView.findViewById(R.id.set_re_password_content);

                Button back_button = popupView.findViewById(R.id.back_set_password);
                back_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });

                Button save_button = popupView.findViewById(R.id.save_password);
                save_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String _newPassword = password.getText().toString().trim();
                        String _rePassword = re_password.getText().toString().trim();
                        if (_newPassword.equals("") || _rePassword.equals("")) {
                            // password can't be empty
                            Tools.exceptionToast(getApplicationContext(), PASSWORDS_EMPTY);
                            return;
                        }

                        if (!_newPassword.equals(_rePassword)) {
                            // passwords has to be equals
                            Tools.exceptionToast(getApplicationContext(), PASSWORDS_NOT_MATCH);
                            return;
                        }

                        // user change his password successfully
                        Map<String, Object> map = new HashMap<>();
                        map.put(MainActivity.FIREBASE_DOCUMENT_PASSWORD, _newPassword);
                        _FireStore.collection(MainActivity.FIREBASE_COLLECTION_USERS).document(_user.get_id()).set(map, SetOptions.merge());
                        _user.set_password(_newPassword);
                        String password = _passwordContent.getText().toString().trim();
                        if (!password.equals(HIDDEN_PASSWORD)){
                            // if password not on hidden mode (in user area) - change it to the new one
                            _passwordContent.setText(_newPassword);
                        }
                        popupWindow.dismiss();
                        Tools.exceptionToast(getApplicationContext(), PASSWORD_CHANGE_TO + _newPassword);
                    }
                });
            }
        });
    }

    private void setShowPassword() {
        final Button showPasswordButton;
        showPasswordButton = findViewById(R.id.show_password);
        showPasswordButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                String password = _passwordContent.getText().toString().trim();
                if (password.equals(HIDDEN_PASSWORD)){
                    _passwordContent.setText(_user.get_password());
                    showPasswordButton.setBackgroundResource(R.drawable.hide_password);
                }
                else {
                    _passwordContent.setText(HIDDEN_PASSWORD);
                    showPasswordButton.setBackgroundResource(R.drawable.show_password);
                }
            }
        });
    }

    private void setBackToMain() {
        Button backToMainButton;
        backToMainButton = findViewById(R.id.user_area_back_to_main);
        backToMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserAreaActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveOnSharedPreferences();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveOnSharedPreferences();
    }

    private void saveOnSharedPreferences(){
        SharedPreferences sp = getSharedPreferences(LoginActivity.SP_FILE, MODE_PRIVATE);
        boolean remember_me = sp.getBoolean(LoginActivity.REMEMBER_ME, false);
        if (remember_me){
            // we saving the user in the sp file only in remember_me mode. otherwise no need.
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(LoginActivity.LOCAL_JSON, new Gson().toJson(_user)).apply();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Button backToMainButton = findViewById(R.id.user_area_back_to_main);
        backToMainButton.callOnClick();
    }
}
