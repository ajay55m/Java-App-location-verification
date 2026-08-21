package com.app.fourscontracting.ui.home;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.app.fourscontracting.R;
import com.app.fourscontracting.SessionPrefs;
import com.app.fourscontracting.User;
import com.app.fourscontracting.UserLocalStore;

import java.util.Calendar;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private UserLocalStore userLocalStore;
    private SessionPrefs sessionPrefs;

    private TextView tvUserId;
    private TextView tvSiteName;
    private Spinner spinnerLeaveType;
    private TextView tvStartDate;
    private TextView tvEndDate;
    private EditText etLeaveReason;
    private Button btnSubmitRequest;

    private String selectedStartDate = "";
    private String selectedEndDate = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userLocalStore = new UserLocalStore(requireContext());
        sessionPrefs = new SessionPrefs(requireContext());

        tvUserId = view.findViewById(R.id.tv_home_user_id);
        tvSiteName = view.findViewById(R.id.tv_home_site_name);
        spinnerLeaveType = view.findViewById(R.id.spinner_leave_type);
        tvStartDate = view.findViewById(R.id.tv_start_date);
        tvEndDate = view.findViewById(R.id.tv_end_date);
        etLeaveReason = view.findViewById(R.id.et_leave_reason);
        btnSubmitRequest = view.findViewById(R.id.btn_submit_leave_request);

        bindUserData();
        setupLeaveCategories();
        setupDatePickers(view);

        if (btnSubmitRequest != null) {
            btnSubmitRequest.setOnClickListener(v -> submitLeaveRequest());
        }
    }

    private void bindUserData() {
        User user = userLocalStore.getLoggedInUser();
        String val = user != null ? user.username : "";
        String[] val_list = UserLocalStore.parseUserInfo(val);
        String uid = val_list.length > 0 ? val_list[0] : "--";

        if (tvUserId != null) {
            tvUserId.setText("ID: #" + uid);
        }

        String pName = sessionPrefs.getProjectName();
        if (TextUtils.isEmpty(pName)) {
            pName = userLocalStore.getLoggedInUserProject().projectname;
        }
        if (tvSiteName != null) {
            tvSiteName.setText(TextUtils.isEmpty(pName) ? "Verified Site" : pName);
        }
    }

    private void setupLeaveCategories() {
        if (spinnerLeaveType == null) return;
        String[] categories = {"-- Select Category --", "Casual Leave", "Sick Leave", "Annual Leave", "Emergency Leave"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLeaveType.setAdapter(adapter);
    }

    private void setupDatePickers(View root) {
        View btnStart = root.findViewById(R.id.btn_select_start_date);
        View btnEnd = root.findViewById(R.id.btn_select_end_date);

        if (btnStart != null) {
            btnStart.setOnClickListener(v -> pickDate(true));
        }
        if (btnEnd != null) {
            btnEnd.setOnClickListener(v -> pickDate(false));
        }
    }

    private void pickDate(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String formatted = String.format(Locale.US, "%02d/%02d/%d", dayOfMonth, month + 1, year);
                    if (isStart) {
                        selectedStartDate = formatted;
                        if (tvStartDate != null) tvStartDate.setText(formatted);
                    } else {
                        selectedEndDate = formatted;
                        if (tvEndDate != null) tvEndDate.setText(formatted);
                    }
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void submitLeaveRequest() {
        if (spinnerLeaveType != null && spinnerLeaveType.getSelectedItemPosition() == 0) {
            Toast.makeText(requireContext(), "Please select a leave category", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(selectedStartDate) || TextUtils.isEmpty(selectedEndDate)) {
            Toast.makeText(requireContext(), "Please select start and end dates", Toast.LENGTH_SHORT).show();
            return;
        }

        String reason = etLeaveReason != null ? etLeaveReason.getText().toString().trim() : "";
        if (TextUtils.isEmpty(reason)) {
            Toast.makeText(requireContext(), "Please enter reason for leave request", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(requireContext(), "Leave request submitted successfully", Toast.LENGTH_LONG).show();
        if (etLeaveReason != null) etLeaveReason.setText("");
    }

    public void refreshData() {
        bindUserData();
    }
}