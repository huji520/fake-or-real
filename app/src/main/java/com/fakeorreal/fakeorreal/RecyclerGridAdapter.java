package com.fakeorreal.fakeorreal;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerGridAdapter extends RecyclerView.Adapter<RecyclerGridAdapter.MyViewHolder> {

    private OnItemClickListener _listenerClick;

    private static String[] _imageArray = {
            "emoji0", "emoji1", "emoji2", "emoji3", "emoji4", "emoji5", "emoji6", "emoji7",
            "emoji8", "emoji9", "emoji10", "emoji11", "emoji12", "emoji13", "emoji14", "emoji15",
            "emoji16", "emoji17", "emoji18", "emoji19", "emoji20", "emoji21", "emoji22", "emoji23",
            "emoji24", "emoji25", "emoji26", "emoji27", "emoji28", "emoji29", "emoji30", "emoji31",
            "emoji32"
    };

    RecyclerGridAdapter(OnItemClickListener listenerClick) {
        _listenerClick = listenerClick;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_emoji, parent, false);
        return new MyViewHolder(view, _listenerClick);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder._imageView.setImageResource(Tools.get_imojiID(_imageArray[position]));
    }

    @Override
    public int getItemCount() {
        return _imageArray.length;
    }

    public static String getID(int position) {
        return _imageArray[position];
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        ImageView _imageView;

        public MyViewHolder(@NonNull View itemView, final RecyclerGridAdapter.OnItemClickListener listenerClick) {
            super(itemView);
            _imageView = itemView.findViewById(R.id.emoji);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listenerClick != null){
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION){
                            listenerClick.onItemClick(position);
                        }
                    }
                }
            });
        }
    }
}
