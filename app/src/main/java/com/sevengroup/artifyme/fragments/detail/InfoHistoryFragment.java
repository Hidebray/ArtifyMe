package com.sevengroup.artifyme.fragments.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.adapters.HistoryAdapter;
import com.sevengroup.artifyme.viewmodels.ProjectDetailViewModel;

public class InfoHistoryFragment extends Fragment {
    private HistoryAdapter historyAdapter;
    private long currentProjectId = -1L;

    public static InfoHistoryFragment newInstance(long projectId) {
        InfoHistoryFragment fragment = new InfoHistoryFragment();
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
        return inflater.inflate(R.layout.fragment_info_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView rcvHistory = view.findViewById(R.id.rcvHistory);
        historyAdapter = new HistoryAdapter(getContext());
        rcvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvHistory.setAdapter(historyAdapter);

        if (currentProjectId != -1L) {
            // ViewModel được lấy từ Activity cha để share dữ liệu
            ProjectDetailViewModel viewModel = new ViewModelProvider(requireActivity()).get(ProjectDetailViewModel.class);
            viewModel.getProjectHistory(currentProjectId).observe(getViewLifecycleOwner(), versions -> {
                if (versions != null) historyAdapter.setVersions(versions);
            });
        }
    }
}