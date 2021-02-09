package com.fakeorreal.fakeorreal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

public class RecordsActivity extends AppCompatActivity {

    TabLayout tabLayout;
    View indicator;
    ViewPager viewPager;
    User _user;
    private TreeMap<Integer, List<String>> _scoresMap;
    private TreeMap<Integer, List<String>> _guessesMap;
    private RecordsAdapter _scoresAdapter;
    private RecordsAdapter _guessesAdapter;

    private List<User> _usersList;

    private int indicatorWidth;
    private List<String> _scoreLeadersNames;
    private List<String> _guessLeadersNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);
        _usersList = getIntent().getParcelableArrayListExtra("users_list");
        FakeOrRealApp app = (FakeOrRealApp) getApplication();
        if (savedInstanceState != null){
            _user = savedInstanceState.getParcelable(com.fakeorreal.fakeorreal.MainActivity.ON_SAVE_USER);
        }
        else {
            _user = app.getUser();
        }

        _scoresMap = new TreeMap<>();
        _guessesMap = new TreeMap<>();
        _scoreLeadersNames = new ArrayList<>();
        _guessLeadersNames = new ArrayList<>();
        _scoresAdapter = new RecordsAdapter(_scoreLeadersNames, this);
        _guessesAdapter = new RecordsAdapter(_guessLeadersNames, this);
        setTabs();
        renderTopScore(true, _scoresMap, _scoreLeadersNames);
        renderTopScore(false, _guessesMap, _guessLeadersNames);

    }

    private void renderTopScore(boolean score, TreeMap<Integer, List<String>> map, List<String> leadersNames) {
        for (User user : _usersList){
            List<String> userNameList = new ArrayList<>();
            int userRate;
            if (score) userRate = user.get_records().get_totalScore();
            else userRate = user.get_records().get_correctGuesses();
            if (map.containsKey(userRate)){
                userNameList = new ArrayList<>(Objects.requireNonNull(map.get(userRate)));
            }
            userNameList.add(user.get_username());
            map.put(userRate, userNameList);
        }

        RecordsAdapter adapter = score? _scoresAdapter : _guessesAdapter;
        renderTopTen(map, adapter, leadersNames);
        setBackToMain();
    }

    private void renderTopTen(TreeMap<Integer, List<String>> map, RecordsAdapter adapter, List<String> leadersNames) {
//        leadersNames = new ArrayList<>();
        int i = 1;
        for (Integer key : map.descendingKeySet()){
            for (String user : Objects.requireNonNull(map.get(key))){
                String s = user + " - " + key;
                leadersNames.add(s);
                i++;
                if (i == 11) break;
            }
            if (i == 11) break;
        }
        adapter.notifyDataSetChanged();
    }

    private void setBackToMain() {
        Button backToMain = findViewById(R.id.records_to_main);
        backToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecordsActivity.this, MainActivity.class);
                intent.putExtra(MainActivity.INTENT_USER, _user);
                startActivity(intent);
                finish();
            }
        });
    }

    void setTabs()
    {
        //Assign view reference
        tabLayout = findViewById(R.id.tab);
        indicator = findViewById(R.id.indicator);
        viewPager = findViewById(R.id.viewPager);

        //Set up the view pager and fragments
        TabFragmentAdapter tabFragmentAdapter = new TabFragmentAdapter(getSupportFragmentManager());
        ScoresTab scoresTab = new ScoresTab(_scoresAdapter);
        _scoresAdapter = scoresTab.get_adapter();
        tabFragmentAdapter.addFragment(scoresTab, "Top score");


        GuessesTab guessesTab = new GuessesTab(_guessesAdapter);
        _guessesAdapter = guessesTab.get_adapter();
        tabFragmentAdapter.addFragment(guessesTab, "Top guesses");

        viewPager.setAdapter(tabFragmentAdapter);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.score_icon);
        tabLayout.getTabAt(1).setIcon(R.drawable.guess_icon);

        //Determine indicator width at runtime
        tabLayout.post(new Runnable() {
            @Override
            public void run() {
                indicatorWidth = tabLayout.getWidth() / tabLayout.getTabCount();

                //Assign new width
                FrameLayout.LayoutParams indicatorParams = (FrameLayout.LayoutParams) indicator.getLayoutParams();
                indicatorParams.width = indicatorWidth;
                indicator.setLayoutParams(indicatorParams);
            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            //To move the indicator as the user scroll, we will need the scroll offset values
            //positionOffset is a value from [0..1] which represents how far the page has been scrolled
            //see https://developer.android.com/reference/android/support/v4/view/ViewPager.OnPageChangeListener
            @Override
            public void onPageScrolled(int i, float positionOffset, int positionOffsetPx) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) indicator.getLayoutParams();

                //Multiply positionOffset with indicatorWidth to get translation
                float translationOffset = (positionOffset + i) * indicatorWidth;
                params.leftMargin = (int) translationOffset;
                indicator.setLayoutParams(params);
            }

            @Override
            public void onPageSelected(int i) {

            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(MainActivity.ON_SAVE_USER, _user);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Button backToMain = findViewById(R.id.records_to_main);
        backToMain.callOnClick();
    }
}
