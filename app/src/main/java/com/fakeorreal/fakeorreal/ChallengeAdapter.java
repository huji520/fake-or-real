package com.fakeorreal.fakeorreal;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChallengeAdapter extends RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder> {

    private Context _context;
    private List<User> _userslist;
    private View _recyclerView;
    private List<User> _challengeRecipients;
    private List<Boolean> _checkBoxes;


    ChallengeAdapter(List<User> usersList, List<User> challengeRecipients, Context context) {
        _userslist = usersList;
        _context = context;
        _challengeRecipients = challengeRecipients;
        _checkBoxes = new ArrayList<>();
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ChallengeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_challenge, parent, false);
        _recyclerView = view;
        ChallengeViewHolder holder = new ChallengeViewHolder(view);

        return holder;
    }
    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(@NonNull final ChallengeViewHolder holder, int position) {
        User currentUser = _userslist.get(position);
        holder._imageView.setImageResource(currentUser.get_emojiID());
        holder._textView.setText(currentUser.get_username());
        holder._user = currentUser;
        int color = (position % 2 == 0) ? R.color.background : R.color.records;
        _recyclerView.setBackgroundColor(_context.getResources().getColor(color));
        _checkBoxes.add(false);

        holder._checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if (isChecked) _challengeRecipients.add(holder._user);
                else _challengeRecipients.remove(holder._user);
            }
        }
        );
    }

    @Override
    public int getItemCount() {
        return _userslist.size();
    }


    public static class ChallengeViewHolder extends RecyclerView.ViewHolder {

        ImageView _imageView;
        TextView _textView;
        CheckBox _checkBox;
        User _user;

        public ChallengeViewHolder(@NonNull View itemView) {
            super(itemView);
            _imageView = itemView.findViewById(R.id.user_emoji);
            _textView = itemView.findViewById(R.id.user_name);
            _checkBox = itemView.findViewById(R.id.challengeCheckBox);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    _checkBox.setChecked(!_checkBox.isChecked());
                }
            });

        }
    }
}