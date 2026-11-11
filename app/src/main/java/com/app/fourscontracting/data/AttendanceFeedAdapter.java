package com.app.fourscontracting.data;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.fourscontracting.R;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceFeedAdapter extends RecyclerView.Adapter<AttendanceFeedAdapter.ViewHolder> {

    public interface Listener {
        void onBreakClicked(AttendanceRecordModel record);
        void onPhotoClicked(String photoUrl);
        void onTimeOutClicked(AttendanceRecordModel record);
    }

    private final List<AttendanceRecordModel> list = new ArrayList<>();
    private final Listener listener;

    public AttendanceFeedAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<AttendanceRecordModel> newItems) {
        list.clear();
        if (newItems != null) {
            list.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_feed, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceRecordModel item = list.get(position);
        Context context = holder.itemView.getContext();

        String empName = item.getFirstName();
        if (empName == null || empName.trim().isEmpty() || "User".equalsIgnoreCase(empName.trim()) || "Worker".equalsIgnoreCase(empName.trim()) || empName.startsWith("Worker #")) {
            if (context != null) {
                com.app.fourscontracting.SessionManager sm = new com.app.fourscontracting.SessionManager(context);
                String myEmpId = sm.getEmpId() > 0 ? String.valueOf(sm.getEmpId()) : "";
                String myUid = sm.getUid() > 0 ? String.valueOf(sm.getUid()) : "";
                String sEmpName = sm.getEmpName();
                String sUser = sm.getUsername();
                String targetName = !android.text.TextUtils.isEmpty(sEmpName) ? sEmpName : sUser;

                if (!android.text.TextUtils.isEmpty(targetName)) {
                    String eId = item.getEmpId() != null ? item.getEmpId().trim() : "";
                    String uId = item.getUserId() != null ? item.getUserId().trim() : "";
                    if ((!myEmpId.isEmpty() && (eId.equalsIgnoreCase(myEmpId) || uId.equalsIgnoreCase(myEmpId)))
                            || (!myUid.isEmpty() && (eId.equalsIgnoreCase(myUid) || uId.equalsIgnoreCase(myUid)))) {
                        empName = targetName;
                    }
                }
            }
        }
        if (empName == null || empName.trim().isEmpty() || "User".equalsIgnoreCase(empName.trim())) {
            empName = (item.getEmpId() != null && !item.getEmpId().trim().isEmpty()) ? "Worker #" + item.getEmpId() : "Worker";
        }
        holder.tvEmpName.setText(empName);

        // Status Pill & Check Out Action Button
        String resolvedTarget = com.app.fourscontracting.ui.attendance.ManageAttendanceFragment.resolveTargetWorkerId(item);
        boolean hasValidWorkerTarget = !resolvedTarget.isEmpty() && !"--".equals(resolvedTarget);

        if (item.isActive() && hasValidWorkerTarget) {
            holder.tvStatusPill.setText("ACTIVE");
            holder.tvStatusPill.setBackgroundResource(R.drawable.bg_luxury_status_active);
            holder.tvStatusPill.setTextColor(0xFFFFFFFF);
            if (holder.btnCheckOut != null) {
                holder.btnCheckOut.setVisibility(View.VISIBLE);
                holder.btnCheckOut.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onTimeOutClicked(item);
                    }
                });
            }
        } else {
            holder.tvStatusPill.setText(item.isActive() ? "ACTIVE" : "COMPLETED");
            holder.tvStatusPill.setBackgroundResource(item.isActive() ? R.drawable.bg_luxury_status_active : R.drawable.bg_luxury_status_completed);
            holder.tvStatusPill.setTextColor(0xFFFFFFFF);
            if (holder.btnCheckOut != null) {
                holder.btnCheckOut.setVisibility(View.GONE);
                holder.btnCheckOut.setOnClickListener(null);
            }
        }

        // Break Button State
        boolean tookBreak = item.getBreakHours() > 0;
        if (tookBreak) {
            holder.btnBreakStatus.setBackgroundResource(R.drawable.bg_break_btn_active);
            holder.tvBreakLabel.setText("Break Taken");
            holder.tvBreakLabel.setTextColor(0xFFFFFFFF);
            holder.imgBreakIcon.setColorFilter(0xFFFFFFFF);
        } else {
            holder.btnBreakStatus.setBackgroundResource(R.drawable.bg_break_btn);
            holder.tvBreakLabel.setText("Break Taken?");
            holder.tvBreakLabel.setTextColor(0xFFEA580C);
            holder.imgBreakIcon.setColorFilter(0xFFEA580C);
        }

        holder.btnBreakStatus.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBreakClicked(item);
            }
        });

        // Time IN / OUT formatting
        holder.tvInTime.setText(formatTime(item.getTimeIn()));
        holder.tvOutTime.setText(formatTime(item.getTimeOut()));

        // Resolve IN & OUT URLs using shift_no to avoid LruCache collisions
        String attendanceId = item.getAttendId();
        int shiftNo = item.getShiftNo();

        String inUrl = ApiConfig.attendancePhotoUrl(item.getInPhotoUrl(), attendanceId, shiftNo, "in");
        String outUrl = ApiConfig.attendancePhotoUrl(item.getOutPhotoUrl(), attendanceId, shiftNo, "out");

        // IN Photo
        if (item.isHasIn() || !inUrl.isEmpty()) {
            if (!inUrl.isEmpty()) {
                holder.imgInPhoto.setVisibility(View.VISIBLE);
                holder.llInNoPhoto.setVisibility(View.GONE);
                ImageLoaderHelper.loadImage(context, inUrl, holder.imgInPhoto, holder.llInNoPhoto);

                View.OnClickListener inClick = v -> {
                    if (listener != null) {
                        listener.onPhotoClicked(inUrl);
                    }
                };
                holder.imgInPhoto.setOnClickListener(inClick);
                holder.llInNoPhoto.setOnClickListener(inClick);
            } else {
                holder.imgInPhoto.setVisibility(View.GONE);
                holder.llInNoPhoto.setVisibility(View.VISIBLE);
                holder.imgInPhoto.setOnClickListener(null);
                holder.llInNoPhoto.setOnClickListener(null);
            }
        } else {
            holder.imgInPhoto.setVisibility(View.GONE);
            holder.llInNoPhoto.setVisibility(View.VISIBLE);
            holder.imgInPhoto.setOnClickListener(null);
            holder.llInNoPhoto.setOnClickListener(null);
        }

        // OUT Photo
        if (item.isHasOut() || !outUrl.isEmpty()) {
            if (!outUrl.isEmpty()) {
                holder.imgOutPhoto.setVisibility(View.VISIBLE);
                holder.llOutNoPhoto.setVisibility(View.GONE);
                ImageLoaderHelper.loadImage(context, outUrl, holder.imgOutPhoto, holder.llOutNoPhoto);

                View.OnClickListener outClick = v -> {
                    if (listener != null) {
                        listener.onPhotoClicked(outUrl);
                    }
                };
                holder.imgOutPhoto.setOnClickListener(outClick);
                holder.llOutNoPhoto.setOnClickListener(outClick);
            } else {
                holder.imgOutPhoto.setVisibility(View.GONE);
                holder.llOutNoPhoto.setVisibility(View.VISIBLE);
                holder.imgOutPhoto.setOnClickListener(null);
                holder.llOutNoPhoto.setOnClickListener(null);
            }
        } else {
            holder.imgOutPhoto.setVisibility(View.GONE);
            holder.llOutNoPhoto.setVisibility(View.VISIBLE);
            holder.imgOutPhoto.setOnClickListener(null);
            holder.llOutNoPhoto.setOnClickListener(null);
        }

        // Location Tag
        String projName = item.getProjName();
        if (projName == null || projName.trim().isEmpty()) {
            projName = "Assigned Site";
        }
        holder.tvProjName.setText(projName);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private String sanitizePhotoUrl(String photoUrl) {
        if (photoUrl == null || photoUrl.trim().isEmpty()) return "";
        String sanitized = ImageLoaderHelper.sanitizeUrl(photoUrl);
        return sanitized != null ? sanitized : "";
    }

    private String formatTime(String rawTime) {
        if (rawTime == null || rawTime.isEmpty() || "--".equals(rawTime) || "--:--".equals(rawTime)) {
            return "--:--";
        }
        if (rawTime.toLowerCase().contains("am") || rawTime.toLowerCase().contains("pm")) {
            return rawTime;
        }
        try {
            SimpleDateFormat inFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
            Date date = inFormat.parse(rawTime);
            if (date != null) {
                SimpleDateFormat outFormat = new SimpleDateFormat("hh:mm a", Locale.US);
                return outFormat.format(date);
            }
        } catch (Exception ignored) {
            try {
                SimpleDateFormat inFormat = new SimpleDateFormat("HH:mm", Locale.US);
                Date date = inFormat.parse(rawTime);
                if (date != null) {
                    SimpleDateFormat outFormat = new SimpleDateFormat("hh:mm a", Locale.US);
                    return outFormat.format(date);
                }
            } catch (Exception ignored2) {}
        }
        return rawTime;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmpName, tvStatusPill, tvBreakLabel, tvInTime, tvOutTime, tvProjName;
        LinearLayout btnBreakStatus, llInNoPhoto, llOutNoPhoto;
        ImageView imgBreakIcon, imgInPhoto, imgOutPhoto;
        MaterialButton btnCheckOut;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmpName = itemView.findViewById(R.id.tv_emp_name);
            tvStatusPill = itemView.findViewById(R.id.tv_status_pill);
            btnCheckOut = itemView.findViewById(R.id.btn_check_out);
            tvBreakLabel = itemView.findViewById(R.id.tv_break_label);
            btnBreakStatus = itemView.findViewById(R.id.btn_break_status);
            imgBreakIcon = itemView.findViewById(R.id.img_break_icon);
            tvInTime = itemView.findViewById(R.id.tv_in_time);
            tvOutTime = itemView.findViewById(R.id.tv_out_time);
            tvProjName = itemView.findViewById(R.id.tv_proj_name);
            imgInPhoto = itemView.findViewById(R.id.img_in_photo);
            imgOutPhoto = itemView.findViewById(R.id.img_out_photo);
            llInNoPhoto = itemView.findViewById(R.id.ll_in_no_photo);
            llOutNoPhoto = itemView.findViewById(R.id.ll_out_no_photo);
        }
    }
}
