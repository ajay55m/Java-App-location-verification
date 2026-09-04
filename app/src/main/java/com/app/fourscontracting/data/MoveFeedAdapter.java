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

public class MoveFeedAdapter extends RecyclerView.Adapter<MoveFeedAdapter.ViewHolder> {

    public interface Listener {
        void onPhotoClicked(String photoUrl);
    }

    private final List<MoveRecordModel> list = new ArrayList<>();
    private final Listener listener;

    public MoveFeedAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<MoveRecordModel> newItems) {
        list.clear();
        if (newItems != null) {
            list.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_move_feed, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MoveRecordModel item = list.get(position);
        Context context = holder.itemView.getContext();

        String empName = item.getFirstName();
        if (empName == null || empName.trim().isEmpty()) {
            empName = "Worker";
        }
        holder.tvEmpName.setText(empName);

        // Status Pill: AT NEXT SITE vs MOVE COMPLETED
        if (item.isActive()) {
            holder.tvStatusPill.setText("AT NEXT SITE");
            holder.tvStatusPill.setBackgroundResource(R.drawable.bg_status_movement);
            holder.tvStatusPill.setTextColor(0xFF6366F1);
        } else {
            holder.tvStatusPill.setText("MOVE COMPLETED");
            holder.tvStatusPill.setBackgroundResource(R.drawable.bg_status_completed);
            holder.tvStatusPill.setTextColor(0xFFEF4444);
        }

        // Time IN / OUT formatting
        holder.tvInTime.setText(formatTime(item.getInTime()));
        holder.tvOutTime.setText(formatTime(item.getOutTime()));

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
        TextView tvEmpName, tvStatusPill, tvInTime, tvOutTime, tvProjName;
        LinearLayout llInNoPhoto, llOutNoPhoto;
        ImageView imgInPhoto, imgOutPhoto;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmpName = itemView.findViewById(R.id.tv_move_emp_name);
            tvStatusPill = itemView.findViewById(R.id.tv_move_status_pill);
            tvInTime = itemView.findViewById(R.id.tv_move_in_time);
            tvOutTime = itemView.findViewById(R.id.tv_move_out_time);
            tvProjName = itemView.findViewById(R.id.tv_move_proj_name);
            imgInPhoto = itemView.findViewById(R.id.img_move_in_photo);
            imgOutPhoto = itemView.findViewById(R.id.img_move_out_photo);
            llInNoPhoto = itemView.findViewById(R.id.ll_move_in_no_photo);
            llOutNoPhoto = itemView.findViewById(R.id.ll_move_out_no_photo);
        }
    }
}
