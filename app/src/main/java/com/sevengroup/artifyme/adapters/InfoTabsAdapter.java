package com.sevengroup.artifyme.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.sevengroup.artifyme.fragments.detail.InfoDetailsFragment;
import com.sevengroup.artifyme.fragments.detail.InfoHistoryFragment;

public class InfoTabsAdapter extends FragmentStateAdapter {
    private final long projectId;

    public InfoTabsAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, long projectId) {
        super(fragmentManager, lifecycle);
        this.projectId = projectId;
    }

    @NonNull @Override
    public Fragment createFragment(int position) {
        if (position == 0) return InfoHistoryFragment.newInstance(projectId);
        return InfoDetailsFragment.newInstance(projectId);
    }

    @Override public int getItemCount() { return 2; }
}