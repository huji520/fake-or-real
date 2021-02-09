package com.fakeorreal.fakeorreal;

        import android.os.Bundle;

        import androidx.annotation.NonNull;
        import androidx.annotation.Nullable;
        import androidx.fragment.app.Fragment;
        import androidx.recyclerview.widget.LinearLayoutManager;
        import androidx.recyclerview.widget.RecyclerView;

        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;

        import java.util.ArrayList;
        import java.util.List;


public class ScoresTab extends Fragment {

    private View _view;
    private RecyclerView _recyclerView;
    private RecordsAdapter _adapter;

    public  ScoresTab (RecordsAdapter adapter){
        _adapter = adapter;

    }

    public RecordsAdapter get_adapter()
    {
        return _adapter;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _view = inflater.inflate(R.layout.score_tab, container, false);
        _recyclerView = _view.findViewById(R.id.scores_table);
        _recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        _recyclerView.setAdapter(_adapter);
        return _view;
    }
}