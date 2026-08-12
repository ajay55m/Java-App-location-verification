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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceFeedAdapter extends RecyclerView.Adapter<AttendanceFeedAdapter.ViewHolder> {

    public interface Listener {
        void onBreakClicked(AttendanceRecordModel record);
        void onPhotoClicked(String photoUrl);
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

        holder.tvEmpName.setText(item.getFirstName());

        // Status Pill
        if (item.isActive()) {
            holder.tvStatusPill.setText("ACTIVE");
            holder.tvStatusPill.setBackgroundResource(R.drawable.bg_status_active);
            holder.tvStatusPill.setTextColor(0xFF10B981);
        } else {
            holder.tvStatusPill.setText("COMPLETED");
            holder.tvStatusPill.setBackgroundResource(R.drawable.bg_status_completed);
            holder.tvStatusPill.setTextColor(0xFFEF4444);
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

        // IN Photo
        if (item.isHasIn() && !item.getInPhotoUrl().isEmpty()) {
            holder.imgInPhoto.setVisibility(View.VISIBLE);
            holder.llInNoPhoto.setVisibility(View.GONE);
            String fullInUrl = sanitizePhotoUrl(item.getInPhotoUrl());
            ImageLoaderHelper.loadImage(context, fullInUrl, holder.imgInPhoto, holder.llInNoPhoto);

            holder.imgInPhoto.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPhotoClicked(fullInUrl);
                }
            });
        } else {
            holder.imgInPhoto.setVisibility(View.GONE);
            holder.llInNoPhoto.setVisibility(View.VISIBLE);
            holder.imgInPhoto.setOnClickListener(null);
        }

        // OUT Photo
        if (item.isHasOut() && !item.getOutPhotoUrl().isEmpty()) {
            holder.imgOutPhoto.setVisibility(View.VISIBLE);
            holder.llOutNoPhoto.setVisibility(View.GONE);
            String fullOutUrl = sanitizePhotoUrl(item.getOutPhotoUrl());
            ImageLoaderHelper.loadImage(context, fullOutUrl, holder.imgOutPhoto, holder.llOutNoPhoto);

            holder.imgOutPhoto.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPhotoClicked(fullOutUrl);
                }
            });
        } else {
            holder.imgOutPhoto.setVisibility(View.GONE);
            holder.llOutNoPhoto.setVisibility(View.VISIBLE);
            holder.imgOutPhoto.setOnClickListener(null);
        }

        // Location Tag
        holder.tvProjName.setText(item.getProjName());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private String sanitizePhotoUrl(String photoUrl) {
        if (photoUrl == null || photoUrl.isEmpty()) return "";
        if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
            return photoUrl;
        }
        return ApiConfig.SUBCONTRACTOR + "/" + photoUrl;
    }

    private String formatTime(String rawTime) {
        if (rawTime == null || rawTime.isEmpty() || "--".equals(rawTime)) {
            return "--:--";
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

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmpName = itemView.findViewById(R.id.tv_emp_name);
            tvStatusPill = itemView.findViewById(R.id.tv_status_pill);
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
