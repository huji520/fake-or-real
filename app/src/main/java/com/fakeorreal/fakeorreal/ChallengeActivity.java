package com.fakeorreal.fakeorreal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChallengeActivity extends AppCompatActivity {

    public static String MINIMUM_RECIPIENTS = "You must choose at least one user to challenge";

    private User _currentUser;
    private ChallengeAdapter _adapter;
    private List<User> _usersList;  // list of all application users
    private List<User> _challengeRecipients;  // list of all the chosen recipients users
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);
        _challengeRecipients = new ArrayList<>();
        _usersList = getIntent().getParcelableArrayListExtra("users_list");
        recyclerView = findViewById(R.id.challenge_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        _adapter = new ChallengeAdapter(_usersList, _challengeRecipients, this);
        recyclerView.setAdapter(_adapter);

        if (savedInstanceState != null){
            _currentUser = savedInstanceState.getParcelable(MainActivity.ON_SAVE_CURRENT_USER);
        }
        else {
            FakeOrRealApp app = (FakeOrRealApp) getApplication();
            _currentUser = app.getUser();
        }

        setCancelButton();
        setChallengeButton();
        renderUsersList();
        setSearchUserBar();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(MainActivity.ON_SAVE_CURRENT_USER, _currentUser);
        super.onSaveInstanceState(outState);
    }

    private void setCancelButton(){
        Button cancel = findViewById(R.id.cancel_button);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Since we want to go back to the popup window with the results (and the image marked
                // as green or red), we treat it like it recovery from resume (like in the share-with)
                Intent intent = new Intent(ChallengeActivity.this, GameActivity.class);
                intent.putExtra("from_resume", true);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setChallengeButton(){
        Button challengeButton = findViewById(R.id.accept_challenge);
        challengeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_challengeRecipients.isEmpty()) {
                    // in case of clicking on challenge button and no there are no users in the
                    // recipients list - raise a toast.
                    Toast toast = Toast.makeText(ChallengeActivity.this, MINIMUM_RECIPIENTS ,Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return;
                }
                // preparing for sending challenge notification to the recipients
                Gson gson = new Gson();
                String to;
                FCMServer.NotificationData data = new FCMServer.NotificationData();
                Intent fromIntent = getIntent();
                data.title = getResources().getString(R.string.challenge_notification_title);
                data.msg = getResources().getString(R.string.challenge_notification_msg);
                data.imgId = fromIntent.getStringExtra(GameActivity.ID_KEY);
                data.isReal = fromIntent.getBooleanExtra(GameActivity.IS_REAL_KEY, false);
                data.challengerName = _currentUser.get_username();
                data.challengerToken = _currentUser.getFirebaseToken();
                FCMServer.PushNotification notification = new FCMServer.PushNotification();
                notification.data = data;
                StringBuilder toShow = new StringBuilder();
                for (User recipient: _challengeRecipients) {
                    // send the challenge to every recipient in the list
                    toShow.append(recipient.get_username()).append("\n");
                    to = recipient.getFirebaseToken();
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
                    WorkManager manager = WorkManager.getInstance(ChallengeActivity.this);
                    manager.enqueue(request);
                }

                // inform user with challenge recipients
                toShow = new StringBuilder(toShow.substring(0, toShow.length() - 1)); // remove last '\n'
                toShow.insert(0, getResources().getString(R.string.challenge_sent_to));
                Toast toast = Toast.makeText(ChallengeActivity.this, toShow.toString(), Toast.LENGTH_SHORT);
                LinearLayout layout = (LinearLayout) toast.getView();
                if (layout.getChildCount() > 0) {
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                }
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                // go back to popup results screen (like the cancel button)
                Intent intent = new Intent(ChallengeActivity.this, GameActivity.class);
                intent.putExtra("from_resume", true);
                startActivity(intent);
                finish();
            }
        });
    }


    private void setSearchUserBar()
    {
        SearchView searchView = findViewById(R.id.search_user);
        searchView.setQueryHint("Search for a user name");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {
                // this method handle with clicking on the search icon. No much meaning for that,
                // just remember if found any match, ad inform the user with toast if no one choose.
                boolean foundMatch = false;
                String searchString = query.toLowerCase();
                for (User user : _usersList){
                    if (user.get_username().toLowerCase().startsWith(searchString))
                    {
                        foundMatch = true;
                        break;
                    }
                }
                if (!foundMatch)
                {
                    Toast toast = Toast.makeText(ChallengeActivity.this, "No Match found",Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                newText = newText.toLowerCase();
                if (newText.equals(""))
                {
                    // in case of no input - show all the users
                    renderUsersList();
                }
                else {
                    List<User> matchingResults = new ArrayList<>();
                    for (User user : _usersList){
                        if (user.get_username().toLowerCase().startsWith(newText))
                        {
                            // show all users that the text in the search bar is in their username
                            matchingResults.add(user);
                        }
                    }
                    _adapter = new ChallengeAdapter(matchingResults, _challengeRecipients, ChallengeActivity.this);
                    recyclerView.setAdapter(_adapter);
                    _adapter.notifyDataSetChanged();
                }
                return false;
            }
        });
    }

    private void renderUsersList() {
        int i = 0;
        List<User> listToShow = new ArrayList<>();
        _adapter = new ChallengeAdapter(listToShow, _challengeRecipients, ChallengeActivity.this);
        recyclerView.setAdapter(_adapter);
        for (User user : _usersList){
            if (i == 50) break;  // number of players do display
            if (user.get_username().equals(_currentUser.get_username())) continue;
            listToShow.add(user);
            _adapter.notifyDataSetChanged();
            i++;
        }
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
            editor.putString(LoginActivity.LOCAL_JSON, new Gson().toJson(_currentUser)).apply();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Button cancel = findViewById(R.id.cancel_button);
        cancel.callOnClick();
    }
}
