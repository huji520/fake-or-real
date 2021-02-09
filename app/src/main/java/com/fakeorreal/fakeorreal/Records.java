package com.fakeorreal.fakeorreal;

import android.os.Parcel;
import android.os.Parcelable;

public class Records implements Parcelable {
    private int _correctGuesses;
    private int _wrongGuesses;
    private int _totalScore;

    Records() {
        _correctGuesses = 0;
        _wrongGuesses = 0;
        _totalScore = 0;
    }

    Records(int correctGuesses, int wrongGuesses, int totalScore){
        _correctGuesses= correctGuesses;
        _wrongGuesses = wrongGuesses;
        _totalScore = totalScore;
    }

    private Records(Parcel in) {
        _correctGuesses = in.readInt();
        _wrongGuesses = in.readInt();
        _totalScore = in.readInt();
    }

    public int get_correctGuesses() {
        return _correctGuesses;
    }

    public int get_totalScore() {
        return _totalScore;
    }

    public int get_wrongGuesses() {
        return _wrongGuesses;
    }

    public void set_correctGuesses(int correctGuesses) {
        this._correctGuesses = correctGuesses;
    }

    public void set_wrongGuesses(int wrongGuesses) {
        this._wrongGuesses = wrongGuesses;
    }

    public void set_totalScore(int totalScore) {
        this._totalScore = totalScore;
    }

    public void increase_correctGuesses(){
        _correctGuesses++;
    }

    public void increase_wrongGuesses(){
        _wrongGuesses++;
    }

    public void update_totalScore(int delta){
        _totalScore += delta;
    }

    public String render_records(){
        String s = "Total Score: " + _totalScore + "\n";
        s += "Correct Guesses: " + _correctGuesses + "\n";
        s += "Wrong Guesses: " + _wrongGuesses;
        return s;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_correctGuesses);
        dest.writeInt(_wrongGuesses);
        dest.writeInt(_totalScore);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Records> CREATOR = new Creator<Records>() {
        @Override
        public Records createFromParcel(Parcel in) {
            return new Records(in);
        }

        @Override
        public Records[] newArray(int size) {
            return new Records[size];
        }
    };
}
