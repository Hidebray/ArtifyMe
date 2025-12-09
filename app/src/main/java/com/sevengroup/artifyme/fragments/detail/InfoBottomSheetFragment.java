package com.sevengroup.artifyme.fragments.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.adapters.InfoTabsAdapter;
import com.sevengroup.artifyme.viewmodels.ProjectDetailViewModel;

public class InfoBottomSheetFragment extends BottomSheetDialogFragment {
    private long currentProjectId = -1L;
    private final String[] tabTitles = new String[]{"Lịch sử", "Chi tiết"};

    public static InfoBottomSheetFragment newInstance(long projectId) {
        InfoBottomSheetFragment fragment = new InfoBottomSheetFragment();
        Bundle args = new Bundle();
        args.putLong("PROJECT_ID", projectId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) currentProjectId = getArguments().getLong("PROJECT_ID", -1L);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout_info);
        ViewPager2 viewPager = view.findViewById(R.id.view_pager_info);
        InfoTabsAdapter tabsAdapter = new InfoTabsAdapter(getChildFragmentManager(), getLifecycle(), currentProjectId);
        viewPager.setAdapter(tabsAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText(R.string.tab_history);
            else tab.setText(R.string.tab_details);
        }).attach();

        ProjectDetailViewModel viewModel = new ViewModelProvider(requireActivity()).get(ProjectDetailViewModel.class);
        viewModel.getNavigateToEditor().observe(getViewLifecycleOwner(), path -> {
            if (path != null) {
                dismiss();
            }
        });
    }
}