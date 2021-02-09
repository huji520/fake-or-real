package com.fakeorreal.fakeorreal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    static final int CHALLENGE_TO_GAME = 112;

    public static String FIREBASE_COLLECTION_USERS = "users";
    public static String FIREBASE_DOCUMENT_EMOJI = "emoji";
    public static String FIREBASE_DOCUMENT_USERNAME = "username";
    public static String FIREBASE_DOCUMENT_EMAIL = "email";
    public static String FIREBASE_DOCUMENT_PASSWORD = "password";
    public static String FIREBASE_DOCUMENT_CORRECT_GUESSES = "correct_guesses";
    public static String FIREBASE_DOCUMENT_WRONG_GUESSES = "wrong_guesses";
    public static String FIREBASE_DOCUMENT_TOTAL_SCORE = "total_score";
    public static String FIREBASE_DOCUMENT_JOIN_TIME = "join_time";
    public static String FIREBASE_DOCUMENT_ID = "id";
    public static String FIREBASE_DOCUMENT_SOUND = "sound";
    public static String FIREBASE_DOCUMENT_TOKEN = "token";
    public static String ON_SAVE_USER = "user_on_save";
    public static String ON_SAVE_CURRENT_USER = "current_user_on_save";
    public static String INTENT_USER = "user";

    private ProgressBar _progressBar;
    private int _exitCounter = 0;

    TextView _helloUser;
    TextView _userArea;
    Button _records;
    Button _getImage;
    Button soundButton;
    ImageView _personalImage;
    User _user;
    ImageGenerator _imageGenerator;
    private FirebaseFirestore _FireStore;
    int _realIndex;
    int _fakeIndex;


    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FakeOrRealApp app = (FakeOrRealApp) getApplication();
        _FireStore = app.getFireStoreInstance();
        _progressBar = findViewById(R.id.main_progress_bar);
        _imageGenerator = ImageGenerator.getInstance(getReader(true), getReader(false), getApplicationContext());
        if (savedInstanceState != null){
            _user = savedInstanceState.getParcelable(ON_SAVE_USER);
        }
        else {
            _user =  app.getUser();
        }

        soundButton = findViewById(R.id.main_sound_button);
        int soundIcon =  _user.get_sound_choice().equals("true") ? R.drawable.sound_on : R.drawable.sound_off;
        soundButton.setBackgroundResource(soundIcon);
        soundButton.setOnClickListener(soundButtonListener());

        // Load real images to cache
        RequestBuilder<Drawable> _realImageForCache;
        ImageView _dummyRealImageView = findViewById(R.id.dummy_real_image);
        _realIndex = _imageGenerator.getRealRandomIndex();
        _realImageForCache = _imageGenerator.get_realImages().get(_realIndex);
        _realImageForCache.into(_dummyRealImageView);

        // Load fake images to cache
        RequestBuilder<Drawable> _fakeImageForCache;
        ImageView _dummyFakeImageView = findViewById(R.id.dummy_fake_image);
        _fakeIndex = _imageGenerator.getRealRandomIndex();
        _fakeImageForCache = _imageGenerator.get_fakeImages().get(_fakeIndex);
        _fakeImageForCache.into(_dummyFakeImageView);

        displayImage();
        setGoToRecordsButton();
        setGetImageButton();
        setGoToLogin();
        setGoToUserArea();

    }

    View.OnClickListener soundButtonListener()
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sound = _user.get_sound_choice();
                String newSound = sound.equals("true") ? "false" : "true";
                Map<String, Object> map = new HashMap<>();
                map.put(MainActivity.FIREBASE_DOCUMENT_SOUND, newSound);
                _FireStore.collection(MainActivity.FIREBASE_COLLECTION_USERS).document(_user.get_id()).set(map, SetOptions.merge());
                _user.set_sound_choice(newSound);
                int icon = sound.equals("true") ? R.drawable.sound_off : R.drawable.sound_on;
                soundButton.setBackgroundResource(icon);
            }
        };
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(ON_SAVE_USER, _user);
        super.onSaveInstanceState(outState);
    }

    private BufferedReader getReader(boolean real){
        int chosen_batch = ImageGenerator.getRandomIDBatch(real);
        InputStream inputStream = getResources().openRawResource(chosen_batch);
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    private void setGoToUserArea() {
        Button goToUserAreaButton = findViewById(R.id.user_area_button);
        goToUserAreaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, UserAreaActivity.class);
                startActivity(intent);
                finish();
            }
        });
        if (_user.isGuest()){
            Tools.restrictButton(goToUserAreaButton);
        }

    }

    @SuppressLint("SetTextI18n")
    private void displayImage() {
        _helloUser = findViewById(R.id.hello_user);
        _helloUser.setText("Hello " + _user.get_username());
        _userArea = findViewById(R.id.user_area);
        _userArea.setText(_user.get_rendered_records());
        _personalImage = findViewById(R.id.personal_img);
        _personalImage.setImageResource(_user.get_emojiID());
    }

    private void setGoToRecordsButton() {
        _records = findViewById(R.id.records);
        _records.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _progressBar.setVisibility(ProgressBar.VISIBLE);
                _FireStore.collection(MainActivity.FIREBASE_COLLECTION_USERS).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            ArrayList<User> usersList = new ArrayList<>();
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())){
                                Map<String, Object> map = document.getData();
                                usersList.add(new User(map));
                            }
                            Intent intent = new Intent(MainActivity.this, RecordsActivity.class);
                            intent.putParcelableArrayListExtra("users_list", usersList);
                            startActivity(intent);
                            _progressBar.setVisibility(ProgressBar.INVISIBLE);
                            finish();
                        }
                    }
                });
            }
        });
    }

    private void setGetImageButton() {
        _getImage = findViewById(R.id.get_image);
        _getImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                intent.putExtra(GameActivity.REAL_INDEX_KEY, _realIndex);
                intent.putExtra(GameActivity.FAKE_INDEX_KEY, _fakeIndex);
                Random random = new Random();
                intent.putExtra(GameActivity.IS_REAL_KEY, random.nextBoolean());
                startActivity(intent);
                finish();
            }
        });
    }

    private void setGoToLogin() {
        _records = findViewById(R.id.go_to_login);
        _records.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sp = getSharedPreferences(LoginActivity.SP_FILE, MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean(LoginActivity.REMEMBER_ME, false);
                editor.apply();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed(){
        if (_exitCounter > 0) {
            finish();
            return;
        }
        ++_exitCounter;
        Tools.exceptionToast(this, getResources().getString(R.string.exit_toast));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                _exitCounter = 0;
            }
        }, 2000);
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
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(LoginActivity.LOCAL_JSON, new Gson().toJson(_user)).apply();
        }
    }

}
