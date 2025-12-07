package com.sevengroup.artifyme.fragments.share;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.adapters.ShareAppAdapter;
import com.sevengroup.artifyme.models.ShareApp;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ShareBottomSheetFragment extends BottomSheetDialogFragment {
    private String imagePath;
    private OnExportClickListener exportListener;

    public interface OnExportClickListener {
        void onExportClick();
    }

    public static ShareBottomSheetFragment newInstance(String imagePath) {
        ShareBottomSheetFragment fragment = new ShareBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("IMAGE_PATH", imagePath);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnExportClickListener(OnExportClickListener listener) {
        this.exportListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imagePath = getArguments().getString("IMAGE_PATH");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_share_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rcvShareApps = view.findViewById(R.id.rcvShareApps);
        Button btnExport = view.findViewById(R.id.btnExport);

        // Get list of apps that can share images
        List<ShareApp> shareApps = getShareableApps();

        android.util.Log.d("ShareBottomSheet", "Total apps to show: " + shareApps.size());

        // Setup RecyclerView
        if (!shareApps.isEmpty()) {
            ShareAppAdapter adapter = new ShareAppAdapter(shareApps, app -> shareToApp(app));
            rcvShareApps.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            rcvShareApps.setAdapter(adapter);
        } else {
            // Show message if no apps available
            Toast.makeText(getContext(), "Không tìm thấy ứng dụng chia sẻ", Toast.LENGTH_SHORT).show();
        }

        // Export button
        btnExport.setOnClickListener(v -> {
            if (exportListener != null) {
                exportListener.onExportClick();
            }
            dismiss();
        });
    }

    private List<ShareApp> getShareableApps() {
        List<ShareApp> apps = new ArrayList<>();

        // Create a generic share intent to find compatible apps
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");

        PackageManager pm = requireContext().getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(shareIntent, PackageManager.MATCH_ALL);

        android.util.Log.d("ShareBottomSheet", "Found " + resolveInfos.size() + " apps");

        // Popular apps to prioritize (package names)
        String[] popularApps = {
                "com.facebook.orca",           // Messenger
                "com.instagram.android",       // Instagram
                "com.twitter.android",         // Twitter/X
                "com.google.android.gm",       // Gmail
                "com.whatsapp",                // WhatsApp
                "com.facebook.katana",         // Facebook
                "com.snapchat.android",        // Snapchat
                "com.telegram.messenger",      // Telegram
                "org.telegram.messenger",      // Telegram (alt)
                "com.discord",                 // Discord
                "com.linkedin.android",        // LinkedIn
                "com.viber.voip",              // Viber
                "jp.naver.line.android",       // LINE
                "com.skype.raider"             // Skype
        };

        // Add popular apps first if they exist
        for (String packageName : popularApps) {
            for (ResolveInfo info : resolveInfos) {
                if (info.activityInfo.packageName.equals(packageName)) {
                    ShareApp app = new ShareApp(
                            info.loadLabel(pm).toString(),
                            info.loadIcon(pm),
                            info.activityInfo.packageName,
                            info.activityInfo.name
                    );
                    apps.add(app);
                    android.util.Log.d("ShareBottomSheet", "Added app: " + app.name);
                    break;
                }
            }
        }

        // If no popular apps found, add any available apps (up to 8)
        if (apps.isEmpty()) {
            android.util.Log.d("ShareBottomSheet", "No popular apps found, adding all available apps");
            for (int i = 0; i < Math.min(8, resolveInfos.size()); i++) {
                ResolveInfo info = resolveInfos.get(i);
                apps.add(new ShareApp(
                        info.loadLabel(pm).toString(),
                        info.loadIcon(pm),
                        info.activityInfo.packageName,
                        info.activityInfo.name
                ));
            }
        }

        // Limit to 10 apps
        return apps.size() > 10 ? apps.subList(0, 10) : apps;
    }

    private void shareToApp(ShareApp app) {
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                Toast.makeText(getContext(), "Không tìm thấy ảnh", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use FileProvider to share the file
            Uri imageUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    imageFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Target specific app
            shareIntent.setComponent(new ComponentName(app.packageName, app.activityName));

            startActivity(shareIntent);
            dismiss();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Không thể chia sẻ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}