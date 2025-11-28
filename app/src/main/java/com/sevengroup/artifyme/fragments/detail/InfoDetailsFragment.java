package com.sevengroup.artifyme.fragments.detail;

import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.utils.BitmapUtils;
import com.sevengroup.artifyme.utils.DateUtils;
import com.sevengroup.artifyme.viewmodels.ProjectDetailViewModel;

import java.io.File;

public class InfoDetailsFragment extends Fragment {
    private long currentProjectId = -1L;
    TextView txtName, txtTime, txtResolution, txtSize, txtPath;

    public static InfoDetailsFragment newInstance(long projectId) {
        InfoDetailsFragment fragment = new InfoDetailsFragment();
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
        return inflater.inflate(R.layout.fragment_info_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtName = view.findViewById(R.id.imageInfoName);
        txtTime = view.findViewById(R.id.imageInfoTime);
        txtResolution = view.findViewById(R.id.imageInfoResolution);
        txtSize = view.findViewById(R.id.imageInfoSize);
        txtPath = view.findViewById(R.id.imageInfoPath);

        if (currentProjectId != -1L) {
            // ViewModel được lấy từ Activity cha để share dữ liệu
            ProjectDetailViewModel viewModel = new ViewModelProvider(requireActivity()).get(ProjectDetailViewModel.class);
            viewModel.getProjectHistory(currentProjectId).observe(getViewLifecycleOwner(), versions -> {
                if (versions != null) {
                    String imagePath = versions.get(0).imagePath;
                    String imageName = new File(imagePath).getName();
                    String imageTime =  DateUtils.formatDateTime(versions.get(0).createdTime);
                    String imageSize = String.valueOf(BitmapUtils.getImageSizeInByte(versions.get(0).imagePath)/1000) + "KB";

                    Pair resolution = BitmapUtils.getImageResolution(imagePath);
                    String imageResolution = String.valueOf(resolution.first) + "x" + String.valueOf(resolution.second);

                    txtName.setText(imageName);
                    txtTime.setText(imageTime);
                    txtSize.setText(imageSize);
                    txtPath.setText(imagePath);
                    txtResolution.setText(imageResolution);

                }
            });
        }

    }

}