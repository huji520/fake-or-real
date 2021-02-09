package com.fakeorreal.fakeorreal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.Timestamp;

import java.util.Map;
import java.util.Objects;

public class User implements Parcelable {
    private String _username;
    private String _email;
    private String _password;
    private String _emoji;
    private String _id;
    private Records _records;
    private String _firebaseToken;
    private String _soundOn;

    /**
     * Constructor for new users
     * @param username String
     * @param email String
     * @param password String
     */
    public User(String username, String email, String password) {
        _username = username;
        _email = email;
        _password = password;
        _emoji = "emoji0";
        _id = Timestamp.now().toString();
        _records = new Records();
        _firebaseToken = "";
        _soundOn = "true";
    }

    /**
     * Constructor for reading users from firebase
     * @param map contains String as key and Object as value
     */
    public User(Map<String, Object> map){
        _username = (String)map.get(MainActivity.FIREBASE_DOCUMENT_USERNAME);
        _email = (String)map.get(MainActivity.FIREBASE_DOCUMENT_EMAIL);
        _password = (String)map.get(MainActivity.FIREBASE_DOCUMENT_PASSWORD);
        _emoji = (String)map.get(MainActivity.FIREBASE_DOCUMENT_EMOJI);
        _id = (String)map.get(MainActivity.FIREBASE_DOCUMENT_ID);
        _records = new Records(
                Integer.parseInt((String) Objects.requireNonNull(map.get(MainActivity.FIREBASE_DOCUMENT_CORRECT_GUESSES))),
                Integer.parseInt((String) Objects.requireNonNull(map.get(MainActivity.FIREBASE_DOCUMENT_WRONG_GUESSES))),
                Integer.parseInt((String) Objects.requireNonNull(map.get(MainActivity.FIREBASE_DOCUMENT_TOTAL_SCORE))));
        _firebaseToken = (String)map.get(MainActivity.FIREBASE_DOCUMENT_TOKEN);
        _soundOn = (String)map.get(MainActivity.FIREBASE_DOCUMENT_SOUND);
    }

    /**
     * Constructor for Parcelable interface
     * @param in Parcel
     */
    private User(Parcel in) {
        _username = in.readString();
        _email = in.readString();
        _password = in.readString();
        _emoji = in.readString();
        _id = in.readString();
        _records = in.readParcelable(Records.class.getClassLoader());
        _firebaseToken = in.readString();
        _soundOn = in.readString();
    }

    public boolean isGuest() {
        return _username.equals(LoginActivity.GUEST);
    }

    public String get_id() {
        return _id;
    }

    public String get_username() {
        return _username;
    }

    public Records get_records() {
        return _records;
    }

    public String get_email() {
        return _email;
    }

    public String get_password() {
        return _password;
    }

    public String get_emoji(){
        return _emoji;  // String represent the emoji
    }

    public int get_emojiID() {
        return Tools.get_imojiID(_emoji);  // the id (integer) represent the drawable
    }

    public String get_sound_choice() {
        return _soundOn;  // "true" is soundOn, "false" otherwise
    }

    public void set_sound_choice(String soundOn) { this._soundOn = soundOn; }

    public void set_emoji(String emoji) {
        // this should be the string represent the emoji (emojiX) where X is index in range [0,32]
        this._emoji = emoji;
    }

    public void set_email(String email) {
        this._email = email;
    }

    public void set_password(String password) { this._password = password; }

    public void set_username(String username) {
        this._username = username;
    }

    public void increase_correctGuesses(){
        _records.increase_correctGuesses();
    }

    public void increase_wrongGuesses(){
        _records.increase_wrongGuesses();
    }

    public void update_totalScore(int delta){
        _records.update_totalScore(delta);
    }

    public String get_rendered_records(){
        return _records.render_records();
    }

    // using for Parcelable interface
    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    // using for Parcelable interface
    @Override
    public int describeContents() {
        return 0;
    }

    // using for Parcelable interface
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_username);
        dest.writeString(_email);
        dest.writeString(_password);
        dest.writeString(_emoji);
        dest.writeString(_id);
        dest.writeParcelable(_records, flags);
        dest.writeString(_firebaseToken);
        dest.writeString(_soundOn);
    }

    public String getFirebaseToken() {
        return _firebaseToken;
    }

    public void setFirebaseToken(String firebaseToken) {
        _firebaseToken = firebaseToken;
    }
}
