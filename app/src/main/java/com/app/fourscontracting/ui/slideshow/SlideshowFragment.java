package com.app.fourscontracting.ui.slideshow;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.app.fourscontracting.R;
import com.app.fourscontracting.SessionPrefs;
import com.app.fourscontracting.UserLocalStore;

public class SlideshowFragment extends Fragment {

    private UserLocalStore userLocalStore;
    private SessionPrefs sessionPrefs;

    private TextView tvApprovalSiteName;
    private TextView tvPendingCount;
    private TextView tvApprovedCount;
    private TextView tvRejectedCount;
    private Button btnRefreshList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_slideshow, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userLocalStore = new UserLocalStore(requireContext());
        sessionPrefs = new SessionPrefs(requireContext());

        tvApprovalSiteName = view.findViewById(R.id.tv_approval_site_name);
        tvPendingCount = view.findViewById(R.id.tv_pending_approval_count);
        tvApprovedCount = view.findViewById(R.id.tv_approved_count);
        tvRejectedCount = view.findViewById(R.id.tv_rejected_count);
        btnRefreshList = view.findViewById(R.id.btn_refresh_approval_list);

        bindData();

        if (btnRefreshList != null) {
            btnRefreshList.setOnClickListener(v -> {
                bindData();
                Toast.makeText(requireContext(), "Approval list refreshed", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void bindData() {
        String pName = sessionPrefs.getProjectName();
        if (TextUtils.isEmpty(pName)) {
            pName = userLocalStore.getLoggedInUserProject().projectname;
        }
        if (tvApprovalSiteName != null) {
            tvApprovalSiteName.setText(TextUtils.isEmpty(pName) ? "Verified Site" : pName);
        }

        if (tvPendingCount != null) tvPendingCount.setText("0");
        if (tvApprovedCount != null) tvApprovedCount.setText("0");
        if (tvRejectedCount != null) tvRejectedCount.setText("0");
    }

    public void refreshData() {
        bindData();
    }
}