package com.fakeorreal.fakeorreal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecordsAdapter extends RecyclerView.Adapter<RecordsAdapter.RecordsViewHolder> {

    private Context _context;
    private List<String> _leadersNames;
    private static int[] _imageArray = {
            R.drawable._1st, R.drawable._2nd, R.drawable._3rd, R.drawable._4th,
            R.drawable._5th, R.drawable._6th, R.drawable._7th, R.drawable._8th,
            R.drawable._9th, R.drawable._10th
    };
    private View _recyclerView;

    RecordsAdapter(List<String> leadersNames, Context context) {
        _leadersNames = leadersNames;
        _context = context;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public RecordsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycle_view_records, parent, false);
        _recyclerView = view;
        return new RecordsViewHolder(view, _leadersNames);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordsViewHolder holder, int position) {
        holder._imageView.setImageResource(_imageArray[position]);
        if (_leadersNames.size() != 0)
        {
            holder._textView.setText(_leadersNames.get(position));
        }
        int color = (position % 2 == 0) ? R.color.background : R.color.records;
        _recyclerView.setBackgroundColor(_context.getResources().getColor(color));
    }

    @Override
    public int getItemCount() {
        return _imageArray.length;
    }


    public static class RecordsViewHolder extends RecyclerView.ViewHolder {

        ImageView _imageView;
        TextView _textView;

        public RecordsViewHolder(@NonNull View itemView, final List<String> leadresNames) {
            super(itemView);
            _imageView = itemView.findViewById(R.id.place);
            _textView = itemView.findViewById(R.id.top_ten_content);
        }
    }
}