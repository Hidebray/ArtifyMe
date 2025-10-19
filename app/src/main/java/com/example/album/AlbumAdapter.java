package com.example.album;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ALBUM = 0;
    private static final int TYPE_ADD = 1;
    private Context context;
    private List<Album> albums;

    public AlbumAdapter(Context context, List<Album> albums) {
        this.context = context;
        this.albums = albums;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == albums.size()) return TYPE_ADD;
        else return TYPE_ALBUM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_add_album, parent, false);
            return new AddViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_album, parent, false);
            return new AlbumViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AlbumViewHolder) {
            Album album = albums.get(position);
            AlbumViewHolder vh = (AlbumViewHolder) holder;
            vh.albumName.setText(album.getName());
            vh.photoCount.setText(context.getString(R.string.photo_count, album.getPhotoCount()));

            vh.albumCover.setImageResource(album.getCoverResId());

            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(context, "Clicked on album: " + album.getName(), Toast.LENGTH_SHORT).show();
            });

        } else if (holder instanceof AddViewHolder) {
            // Handle the click for the "Add new album" button
            holder.itemView.setOnClickListener(v -> {
                // album creation logic here
                Toast.makeText(context, "Create new album", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return albums.size() + 1; // include "add album" at end
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView albumCover;
        TextView albumName, photoCount;

        AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumCover = itemView.findViewById(R.id.albumCover);
            albumName = itemView.findViewById(R.id.albumName);
            photoCount = itemView.findViewById(R.id.photoCount);
        }
    }

    static class AddViewHolder extends RecyclerView.ViewHolder {
        AddViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public List<Album> getAlbums() {
        return albums;
    }

}
