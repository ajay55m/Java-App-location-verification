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

public class MoveFeedAdapter extends RecyclerView.Adapter<MoveFeedAdapter.ViewHolder> {

    public interface Listener {
        void onPhotoClicked(String photoUrl);
        void onMoveOutClicked(MoveRecordModel record);
    }

    private final List<MoveRecordModel> list = new ArrayList<>();
    private final Listener listener;
    private boolean moveOutActionEnabled = true;

    public MoveFeedAdapter(Listener listener) {
        this(listener, true);
    }

    public MoveFeedAdapter(Listener listener, boolean moveOutActionEnabled) {
        this.listener = listener;
        this.moveOutActionEnabled = moveOutActionEnabled;
    }

    public void setMoveOutActionEnabled(boolean moveOutActionEnabled) {
        this.moveOutActionEnabled = moveOutActionEnabled;
        notifyDataSetChanged();
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

        // Status Pill: AT NEXT SITE vs MISSED OUT vs MOVE COMPLETED
        if (item.isActive()) {
            if (item.isPastDate()) {
                // Moved on a previous date, but forgot to check out - checkout restricted on subsequent dates!
                holder.tvStatusPill.setText("MISSED OUT");
                holder.tvStatusPill.setBackgroundResource(R.drawable.bg_status_missed_out);
                holder.tvStatusPill.setTextColor(0xFF64748B);
                if (holder.btnMoveOut != null) {
                    holder.btnMoveOut.setVisibility(View.GONE);
                    holder.btnMoveOut.setOnClickListener(null);
                }
            } else if (moveOutActionEnabled && item.canCheckOut()) {
                holder.tvStatusPill.setText("AT NEXT SITE");
                holder.tvStatusPill.setBackgroundResource(R.drawable.bg_status_movement);
                holder.tvStatusPill.setTextColor(0xFF6366F1);
                if (holder.btnMoveOut != null) {
                    holder.btnMoveOut.setVisibility(View.VISIBLE);
                    holder.btnMoveOut.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onMoveOutClicked(item);
                        }
                    });
                }
            } else {
                holder.tvStatusPill.setText("AT NEXT SITE");
                holder.tvStatusPill.setBackgroundResource(R.drawable.bg_status_movement);
                holder.tvStatusPill.setTextColor(0xFF6366F1);
                if (holder.btnMoveOut != null) {
                    holder.btnMoveOut.setVisibility(View.GONE);
                    holder.btnMoveOut.setOnClickListener(null);
                }
            }
        } else {
            holder.tvStatusPill.setText("MOVE COMPLETED");
            holder.tvStatusPill.setBackgroundResource(R.drawable.bg_status_completed);
            holder.tvStatusPill.setTextColor(0xFFEF4444);
            if (holder.btnMoveOut != null) {
                holder.btnMoveOut.setVisibility(View.GONE);
                holder.btnMoveOut.setOnClickListener(null);
            }
        }

        // Time IN / OUT formatting
        holder.tvInTime.setText(formatTime(item.getInTime()));
        holder.tvOutTime.setText(formatTime(item.getOutTime()));

        // Resolve IN & OUT URLs using shift_no to avoid LruCache collisions
        String attendanceId = item.getAttendanceId();
        int shiftNo = item.getShiftNo();

        final String inUrl;
        final String outUrl;
        if (!android.text.TextUtils.isEmpty(attendanceId) && !"0".equals(attendanceId)) {
            inUrl = ApiConfig.attendancePhotoUrl(item.getInPhotoUrl(), attendanceId, shiftNo, "in");
            outUrl = ApiConfig.attendancePhotoUrl(item.getOutPhotoUrl(), attendanceId, shiftNo, "out");
        } else {
            inUrl = ApiConfig.movePhotoUrl(item.getInPhotoUrl(), item.getMoveId(), "in");
            outUrl = ApiConfig.movePhotoUrl(item.getOutPhotoUrl(), item.getMoveId(), "out");
        }

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
        MaterialButton btnMoveOut;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmpName = itemView.findViewById(R.id.tv_move_emp_name);
            tvStatusPill = itemView.findViewById(R.id.tv_move_status_pill);
            btnMoveOut = itemView.findViewById(R.id.btn_move_out);
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
