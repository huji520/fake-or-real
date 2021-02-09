package com.fakeorreal.fakeorreal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
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

public class GameActivity extends AppCompatActivity {

    public static String CURRENT_SCORE = "Score for this round: ";
    public static String TOTAL_SCORE = "Total score: ";
    public static String CORRECT_ANSWERS = "Total correct guesses: ";
    public static String WRONG_ANSWERS = "Total wrong guesses: ";
    public static String GOOGLE_API = "https://drive.google.com/thumbnail?id=";
    public static String USERS_KEY = "users_list";
    public static String ID_KEY = "id";
    public static String IS_REAL_KEY = "is_real";
    public static String REAL_INDEX_KEY = "real_index";
    public static String FAKE_INDEX_KEY = "fake_index";
    public static String ON_SAVE_IMAGE = "image_on_save";
    public static String SCORE_KEY = "last_score";
    public static String USER_GUESS_KEY = "user_guess_right";


    private ProgressBar _progressBar;
    private TextView _timerValue;
    private TextView _scoreValue;
    private User _user;
    private long _startTime = 0L;
    private Handler _timerHandler = new Handler();
    long _timeInMilliseconds = 0L;
    long _timeSwapBuff = 0L;
    long _updatedTime = 0L;
    int _score;
    private FirebaseFirestore _FireStore;
    private Button _realButton;
    private Button _fakeButton;
    ImageView _imageView;
    ImageGenerator _imageGenerator;  // singelton for images generation
    RequestBuilder<Drawable> _glideImage;
    String _lastImageId;
    private Button _backButton;
    private Button _soundButton;
    private  MediaPlayer _mp;
    private boolean _fromPopup;
    boolean _isRealImage;
    boolean _prevIsReal;
    int _realIndex;
    private int _prevReal;
    int _fakeIndex;
    private int _prevFake;
    private boolean _challengeMode;  // true iff user in challenge mode
    private boolean _shouldBeResume;
    private boolean _userGuessRight;
    boolean _afterCrash = false;
    FakeOrRealApp _app;


    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        ComponentName caller = getCallingActivity();
        _challengeMode = (caller != null) && caller.getClassName().contains("Challenge");
        _progressBar = findViewById(R.id.game_progress_bar);
        _imageGenerator = ImageGenerator.getInstance(getReader(true), getReader(false), getApplicationContext());
        _app = (FakeOrRealApp) getApplication();
        _FireStore = _app.getFireStoreInstance();

        // buttons
        _timerValue = findViewById(R.id.timer);
        _scoreValue = findViewById(R.id.score);
        _backButton = findViewById(R.id.back_button);
        _backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GameActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        _realButton = findViewById(R.id.real_button);
        _realButton.setVisibility(View.INVISIBLE);
        _fakeButton = findViewById(R.id.fake_button);
        _fakeButton.setVisibility(View.INVISIBLE);
        _soundButton = findViewById(R.id.game_sound_button);

        // get information from last activity, for cache using
        _lastImageId = getIntent().getStringExtra(ID_KEY);
        _realIndex = getIntent().getIntExtra(REAL_INDEX_KEY, -1);
        _fakeIndex = getIntent().getIntExtra(FAKE_INDEX_KEY, -1);
        _isRealImage = getIntent().getBooleanExtra(IS_REAL_KEY, false);
        _prevIsReal = _isRealImage;
        _prevReal = _realIndex;
        _prevFake = _fakeIndex;

        boolean fromCrash = getIntent().getBooleanExtra("from_resume", false);
        if (fromCrash){
            // coming back from challenge activity or from share-with. Restore the last screen
            // without starting a new image.
            _challengeMode = false;
            _afterCrash = true;
            onReturnToActivity();
        }
        else {
            // next image operation
            onCreateHelper(savedInstanceState);
        }
    }

    private void onCreateHelper(Bundle savedInstanceState){
        if (savedInstanceState != null){
            _user = savedInstanceState.getParcelable(MainActivity.ON_SAVE_USER);
        }
        else {
            _user = _app.getUser();
            if (_user == null){
                SharedPreferences sp = getSharedPreferences(LoginActivity.SP_FILE, MODE_PRIVATE);
                _user = new Gson().fromJson(sp.getString(LoginActivity.LOCAL_JSON, null), User.class);
            }
        }

        assert _user != null;
        // sound on/off when user click on real/fake image
        int soundIcon =  _user.get_sound_choice().equals("true") ? R.drawable.sound_on : R.drawable.sound_off;
        _soundButton.setBackgroundResource(soundIcon);
        _soundButton.setOnClickListener(soundButtonListener());
        NextImage(); // showing the next image
    }

    private void onReturnToActivity(){
        // collecting all the data needed for restore the screen
        SharedPreferences sp = getSharedPreferences(LoginActivity.SP_FILE, MODE_PRIVATE);
        _userGuessRight = sp.getBoolean(USER_GUESS_KEY, false);  // is the user guess right before leaving the popup?
        _lastImageId = sp.getString(ID_KEY, null); // last image id that shown
        _glideImage = _imageGenerator.getImageById(_lastImageId);  // load the image into Glide
        _prevIsReal = sp.getBoolean(IS_REAL_KEY, false);  // is the image was real or fake?
        _user = new Gson().fromJson(sp.getString(LoginActivity.LOCAL_JSON, null), User.class);
        _score = sp.getInt(SCORE_KEY, -1);  // what score that the user earn?

        // buttons
        _backButton.setVisibility(View.INVISIBLE);
        int soundIcon =  _user.get_sound_choice().equals("true") ? R.drawable.sound_on : R.drawable.sound_off;
        _soundButton.setBackgroundResource(soundIcon);
        _soundButton.setOnClickListener(soundButtonListener());

        int userAnswerText, userAnswerBorder;
        if (_userGuessRight){
            // display as a right guess (green border and 'Correct' text)
            userAnswerText = R.drawable.correct_answer;
            userAnswerBorder = R.drawable.image_border_correct;
        }
        else {
            // display as a wrong guess (red border and 'Wrong' text)
            userAnswerText = R.drawable.wrong_answer;
            userAnswerBorder = R.drawable.image_border_wrong;
        }
        dummyLoading(); // dummy loading for cache

        // start the popup activity for correct/wrong answer
        setResultPopup(userAnswerText, userAnswerBorder, this.findViewById(android.R.id.content), 0);
    }


    @SuppressLint("CheckResult")
    private void NextImage() {
        _afterCrash = false;
        _score = 0;
        _realButton.setOnClickListener(createButtonListener(true, _isRealImage));
        _fakeButton.setOnClickListener(createButtonListener(false, _isRealImage));
        // We need the index because we need the corresponding between the image and the id
        if (_isRealImage && !_challengeMode) {
            _glideImage = _imageGenerator.get_realImages().get(_realIndex);
            _lastImageId = _imageGenerator.get_realIDs().get(_realIndex);
        }
        else if (!_isRealImage && !_challengeMode){
            _glideImage = _imageGenerator.get_fakeImages().get(_fakeIndex);
            _lastImageId = _imageGenerator.get_fakeIDs().get(_fakeIndex);
        }
        else {
            // in challenge mode we want to keep the id
            _glideImage = _imageGenerator.getImageById(_lastImageId);
        }
        _fromPopup = false;
        _progressBar.setVisibility(ProgressBar.VISIBLE); // progress bar for loading the image

        // we add listener because we want to know when the image is ready, the just then to show
        // the buttons and start the timer.
        _glideImage.addListener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                _progressBar.setVisibility(View.INVISIBLE); // stopping the progress bar

                // make everything visible - together.
                _imageView.setVisibility(View.VISIBLE);
                _realButton.setVisibility(View.VISIBLE);
                _fakeButton.setVisibility(View.VISIBLE);
                _timerValue.setVisibility(TextView.VISIBLE);
                _scoreValue.setVisibility(TextView.VISIBLE);

                if (!_fromPopup) {
                    //set timer
                    _startTime = SystemClock.uptimeMillis();
                    _timerHandler.postDelayed(updateTimerThread, 0);
                    _fromPopup = true;
                }
                return false;
            }
        });

        //set image
        _imageView = findViewById(R.id.image);
        _glideImage.into(_imageView);

        if (!_challengeMode) {
            dummyLoading(); // dummy loading for cache
        }
    }

    private void dummyLoading(){
        // dummy loading for cache
        Random random = new Random();
        _isRealImage = random.nextBoolean();  // choose randomly fake/real image
        RequestBuilder<Drawable> _imageForCache;
        ImageView _dummyImageView = findViewById(R.id.image2);
        if (_isRealImage) {
            _realIndex = _imageGenerator.getRealRandomIndex();
            _imageForCache = _imageGenerator.get_realImages().get(_realIndex);
        } else {
            _fakeIndex = _imageGenerator.getFakeRandomIndex();
            _imageForCache = _imageGenerator.get_fakeImages().get(_fakeIndex);
        }
        _dummyImageView.setImageBitmap(null);
        _imageForCache.into(_dummyImageView);
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
                _soundButton.setBackgroundResource(icon);
            }
        };
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(MainActivity.ON_SAVE_USER, _user);
        outState.putString(ON_SAVE_IMAGE, _lastImageId);
        super.onSaveInstanceState(outState);
    }

    private BufferedReader getReader(boolean real){
        // get BufferedReader for imageGenerator constructor
        int chosen_batch = ImageGenerator.getRandomIDBatch(real);
        InputStream inputStream = getResources().openRawResource(chosen_batch);
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    private View.OnClickListener createButtonListener(final boolean userClickedReal, final boolean isRealImage)
    {
        return new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {

                // disable real\fake buttons and back button while the popup is up
                _realButton.setEnabled(false);
                _fakeButton.setEnabled(false);
                _backButton.setEnabled(false);
                _backButton.setVisibility(Button.INVISIBLE);

                _timerHandler.removeCallbacks(updateTimerThread); // stop timer handler
                _timerValue.setText("");  // delete timer from screen
                _scoreValue.setText("");

                int userAnswerText, userAnswerBorder;
                if (userClickedReal == isRealImage){
                    _userGuessRight = true;  // using for coming back from challenge mode or share-with
                    if (!isRealImage && !_challengeMode){
                        // save information only on fake images
                        _imageGenerator.increaseCorrectAnswer(_lastImageId);
                    }
                    _user.increase_correctGuesses();
                    _score = Math.max(7 - (int)(_updatedTime/1000), 1);
                    userAnswerText = R.drawable.correct_answer;
                    userAnswerBorder = R.drawable.image_border_correct;
                }
                else {
                    _userGuessRight = false;  // using for coming back from challenge mode or share-with
                    if (!isRealImage && !_challengeMode) {
                        // save information only on fake images
                        _imageGenerator.increaseWrongAnswer(_lastImageId);
                    }
                    _user.increase_wrongGuesses();
                    _score = -10;
                    userAnswerText = R.drawable.wrong_answer;
                    userAnswerBorder = R.drawable.image_border_wrong;
                }
                if (_challengeMode){
                    _score = _score * 2;
                }
                _user.update_totalScore(_score);

                if (!_user.isGuest()){
                    // save information to FireStore only if user is not a guest
                    Map<String, Object> map = new HashMap<>();
                    map.put(MainActivity.FIREBASE_DOCUMENT_TOTAL_SCORE, String.valueOf(_user.get_records().get_totalScore()));
                    map.put(MainActivity.FIREBASE_DOCUMENT_CORRECT_GUESSES, String.valueOf(_user.get_records().get_correctGuesses()));
                    map.put(MainActivity.FIREBASE_DOCUMENT_WRONG_GUESSES, String.valueOf(_user.get_records().get_wrongGuesses()));
                    _FireStore.collection(MainActivity.FIREBASE_COLLECTION_USERS).document(_user.get_id()).set(map, SetOptions.merge());
                }

                if (!isRealImage && !_challengeMode){
                    // save information to FireStore only if the image is fake and it not challenge mode
                    // challenge mode is problematic since the user maybe not load the batch that have
                    // this image.
                    Map<String, Object> map = new HashMap<>();
                    map.put(ImageGenerator.FIREBASE_DOCUMENT_IMAGES_CORRECT_GUESSES, String.valueOf(_imageGenerator.getNumberOfCorrectAnswers(_lastImageId)));
                    map.put(ImageGenerator.FIREBASE_DOCUMENT_IMAGES_WRONG_GUESSES, String.valueOf(_imageGenerator.getNumberOfWrongAnswers(_lastImageId)));
                    _FireStore.collection(ImageGenerator.FIREBASE_COLLECTION_IMAGES).document(_lastImageId).set(map, SetOptions.merge());
                }

                int sound = (userClickedReal == isRealImage) ? R.raw.correct_2 : R.raw.wrong_2;
                // show popup activity
                setResultPopup(userAnswerText, userAnswerBorder, v, sound);
            }
        };

    }

    @SuppressLint("SetTextI18n")
    void setResultPopup(int userAnswerText, int userAnswerBorder, final View v, int sound)
    {
        // inflate the layout of the game_popup_bg window
        final LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") final View popupView = inflater.inflate(R.layout.popup_game_results, null);

        // set text for each line in the popup
        ImageView answerText = popupView.findViewById(R.id.answer_text);
        ImageView answerImageBorder = popupView.findViewById(R.id.answer_border);
        answerText.setImageResource(userAnswerText);
        _glideImage.into(answerImageBorder);
        answerImageBorder.setBackground(getDrawable(userAnswerBorder));

        TextView currentScore = popupView.findViewById(R.id.current_score);
        currentScore.setText(CURRENT_SCORE + _score);
        TextView totalScore = popupView.findViewById(R.id.total_score);
        totalScore.setText(TOTAL_SCORE + _user.get_records().get_totalScore());
        TextView correctAnswers = popupView.findViewById(R.id.right_answers);
        correctAnswers.setText(CORRECT_ANSWERS + _user.get_records().get_correctGuesses() );
        TextView wrongAnswers =  popupView.findViewById(R.id.wrong_answers);
        wrongAnswers.setText(WRONG_ANSWERS + _user.get_records().get_wrongGuesses());

        // create the game_popup_bg window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height);

        if (_afterCrash){
            // It's a known problem, can't find the references to StackOverFlow, but the solution
            // is a short delay
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
                }
            }, 100);
        }
        else {
            popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
        }

        if (!_afterCrash){
            // if we just restore the screen, we don't want the sound
            _mp = MediaPlayer.create(getApplicationContext(), sound);
            if (_user.get_sound_choice().equals("true")) _mp.start();
        }

        Button back_button = popupView.findViewById(R.id.back);
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // stop the sound in case we click the button and the sound didn't finished yet
                if (!_afterCrash) { _mp.stop(); }
                popupWindow.dismiss();
                Intent intent = new Intent(GameActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        Button next_img_button = popupView.findViewById(R.id.next_image);
        next_img_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // stop the sound in case we click the button and the sound didn't finished yet
                if (!_afterCrash) { _mp.stop(); }
                popupWindow.dismiss();
                NextImage(); // show the next image
                // enable all buttons again
                _realButton.setEnabled(true);
                _fakeButton.setEnabled(true);
                _backButton.setEnabled(true);
                _backButton.setVisibility(Button.VISIBLE);
            }
        });

        Button challenge_friend_button = popupView.findViewById(R.id.challenge);
        challenge_friend_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _FireStore.collection(MainActivity.FIREBASE_COLLECTION_USERS).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            ArrayList<User> usersList = new ArrayList<>();
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())){
                                Map<String, Object> map = document.getData();
                                if (_user.get_username().equals(map.get(MainActivity.FIREBASE_DOCUMENT_USERNAME))) {
                                    continue;  // don't pass the current username
                                }
                                usersList.add(new User(map)); // adding all the users in the firebase
                            }
                            saveInfoForResume();
                            Intent intent = new Intent(GameActivity.this, ChallengeActivity.class);
                            // we send the users as a list of User instead of reading the users from
                            // firebase in the challengeActivity, for better performnce.
                            intent.putParcelableArrayListExtra(USERS_KEY, usersList);
                            intent.putExtra(ID_KEY, _lastImageId);
                            intent.putExtra(IS_REAL_KEY, _prevIsReal);
                            intent.putExtra(REAL_INDEX_KEY, _prevReal);
                            intent.putExtra(FAKE_INDEX_KEY, _prevFake);
                            startActivity(intent);
                            finish();
                        }
                    }
                });

            }
        });

        // in case that the user is a guest, don't allow him to challenge other users, just raise a toast.
        if (_user.isGuest()) Tools.restrictButton(challenge_friend_button);

        Button share_with_button = popupView.findViewById(R.id.share_with);
        share_with_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!_afterCrash) {
                    _mp.stop(); // stop the sound in case we click the button and the sound didn't finished yet
                    Bitmap img = getImageWithLogo();  // image to share with
                    // we store the image on this path (on the phone)
                    String bitmapPath = MediaStore.Images.Media.insertImage(getContentResolver(), img, Timestamp.now().toString(), null);
                    Uri bitmapUri = Uri.parse(bitmapPath);
                    Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_STREAM, bitmapUri); // send the image that stored
                    String link = "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID;
                    String message = "#Can YOU guess?\n" +
                            "Try it yourself now!\n" +
                            "Link to app: ";
                    message += link;

                    intent.putExtra(Intent.EXTRA_TEXT, message);  // show this message under the image
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setType("*/*");  // supporting text and images
                    popupWindow.dismiss();
                    startActivity(intent);
                }
            }
        });

        Button back_button2 = popupView.findViewById(R.id.back2);
        if (_challengeMode) {
            // in case we are in challenge mode, popup is appear, but not all the buttons shown
            // (such as challenge, or next image) so the only button we need is back button, but we
            // want it on the center. So back_button2 is another back button the locate on the center
            // and shown only in challenge mode (and the original back_button not shown).
            next_img_button.setVisibility(View.INVISIBLE);
            share_with_button.setVisibility(View.INVISIBLE);
            back_button.setVisibility(View.INVISIBLE);
            challenge_friend_button.setVisibility(View.INVISIBLE);
            back_button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!_afterCrash) { _mp.stop(); }
                    popupWindow.dismiss();
                    Intent intent = new Intent();
                    intent.putExtra(PendingChallengeActivity.CHALLENGE_SCORE_KEY, _score);
                    intent.putExtra(PendingChallengeActivity.CHALLENGED_NAME, _user.get_username());
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
            back_button2.setVisibility(View.VISIBLE);
        }
        else {
            back_button2.setVisibility(View.INVISIBLE);
        }
    }

    private void saveInfoForResume(){
        // saving information that will be needed to restore the screen (after challenging or share-with)
        SharedPreferences sp = getSharedPreferences(LoginActivity.SP_FILE, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(USER_GUESS_KEY, _userGuessRight);
        editor.putString(ID_KEY, _lastImageId);
        editor.putBoolean(IS_REAL_KEY, _prevIsReal);
        editor.putString(LoginActivity.LOCAL_JSON, new Gson().toJson(_user));
        editor.putInt(SCORE_KEY, _score);
        editor.apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        _shouldBeResume = true;
        saveInfoForResume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (_shouldBeResume) {
            _shouldBeResume = false;
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("from_resume", true);
            startActivity(intent);
            finish();
        }
    }

    /**
     * creates & displays the timer
     */
    private Runnable updateTimerThread = new Runnable() {

        public void run() {
            _timeInMilliseconds = SystemClock.uptimeMillis() - _startTime;
            _updatedTime = _timeSwapBuff + _timeInMilliseconds;
            String time = getResources().getString(R.string.timer_text, calculateTime());
            String score_value = String.valueOf(Math.max(7 - (int)(_updatedTime/1000), 1));
            String score = getResources().getString(R.string.score_text, score_value);
            _scoreValue.setText(score);
            _timerValue.setText(time);
            _timerHandler.postDelayed(this, 0);
        }

    };

    /**
     * calculates the time passed since this activity started to run, by converting the number of
     * milliseconds that passed into a string in the format of <minutes>:<seconds>:<milliseconds>
     * @return the formatted string
     */
    private String calculateTime()
    {
        int secs = (int) (_updatedTime / 1000);
        int mins = secs / 60;
        int milliseconds = (int) (_updatedTime % 1000);
        secs = secs % 60;
        String minText = String.valueOf(mins);
        @SuppressLint("DefaultLocale") String secsText =  String.format("%02d", secs);
        @SuppressLint("DefaultLocale") String millisecondsText = String.format("%03d", milliseconds);

        return String.format("%s:%s:%s", minText, secsText, millisecondsText);
    }

    /**
     * Creating a stylish image for share-with
     * @return the stylish image. with our logo, and invitation to google play
     */
    private Bitmap getImageWithLogo() {
        Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.fake_or_real);
        Bitmap img = ((BitmapDrawable)_imageView.getDrawable()).getBitmap();
        Bitmap googlePlayInvitation = BitmapFactory.decodeResource(getResources(), R.drawable.google_play_invite);
        googlePlayInvitation = getResizedBitmap(googlePlayInvitation, googlePlayInvitation.getWidth() * 2, googlePlayInvitation.getHeight() * 2);
        int width = logo.getWidth();
        float ratio = (float)logo.getWidth() / (float)img.getWidth();
        img = getResizedBitmap(img, width, (int)(img.getHeight() * ratio));
        int height = logo.getHeight() + img.getHeight();

        Bitmap cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas comboImage = new Canvas(cs);
        comboImage.drawBitmap(logo, 0f, 0f, null);
        comboImage.drawBitmap(img, 0, logo.getHeight(), null);
        int iconHeight = logo.getHeight() + img.getHeight() - googlePlayInvitation.getHeight();
        comboImage.drawBitmap(googlePlayInvitation, 10, iconHeight, null);
        return cs;
    }

    /**
     * Resizing bitmap image with given height and width
     * @param bm - bitmap image to resize
     * @param newWidth - new width
     * @param newHeight - new height
     * @return - the resized image (as Bitmap)
     */
    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        if (!bm.isRecycled()) {
            bm.recycle();
        }
        return resizedBitmap;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        _backButton.callOnClick();
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

}
