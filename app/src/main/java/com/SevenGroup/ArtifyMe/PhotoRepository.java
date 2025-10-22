package com.SevenGroup.ArtifyMe;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class PhotoRepository {

    public static List<Photo> getAllPhotos(Context context) {
        List<Photo> list = new ArrayList<>();
        // Dummy
        list.add(new Photo(R.drawable.a));
        list.add(new Photo(R.drawable.b));
        list.add(new Photo(R.drawable.c));
        list.add(new Photo(R.drawable.d));
        list.add(new Photo(R.drawable.e));
        list.add(new Photo(R.drawable.f));
        list.add(new Photo(R.drawable.g));
        list.add(new Photo(R.drawable.i));
        list.add(new Photo(R.drawable.j));
        list.add(new Photo(R.drawable.k));
        list.add(new Photo(R.drawable.l));
        list.add(new Photo(R.drawable.m));
        list.add(new Photo(R.drawable.n));

        return list;
    }
}

