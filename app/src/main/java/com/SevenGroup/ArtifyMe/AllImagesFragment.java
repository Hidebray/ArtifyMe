package com.SevenGroup.ArtifyMe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AllImagesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_images, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);

        // Get photo list from repository
        List<Photo> photos = PhotoRepository.getAllPhotos(requireContext());

        PhotoAdapter adapter = new PhotoAdapter(getContext(), photos);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));

        int spacing= 8;
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(spacing));

        recyclerView.setAdapter(adapter);

        return view;
    }
}
