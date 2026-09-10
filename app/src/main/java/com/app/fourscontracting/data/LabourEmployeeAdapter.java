package com.app.fourscontracting.data;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageRequest;
import com.app.fourscontracting.MySingleton;
import com.app.fourscontracting.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for rendering worker cards in native Labour Management screen.
 * Driven by api_labour_manager.php JSON response.
 */
public class LabourEmployeeAdapter extends RecyclerView.Adapter<LabourEmployeeAdapter.ViewHolder> {

    public interface Listener {
        void onTimeInClicked(LabourEmployeeModel item);
        void onTimeOutClicked(LabourEmployeeModel item);
    }

    private final List<LabourEmployeeModel> list = new ArrayList<>();
    private final Listener listener;

    public LabourEmployeeAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<LabourEmployeeModel> items) {
        list.clear();
        if (items != null) list.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_labour_employee, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        LabourEmployeeModel worker = list.get(position);

        h.tvName.setText(worker.getName() + (!worker.getId().isEmpty() ? " (" + worker.getId() + ")" : ""));

        // Employee Photo Loading with ImageLoaderHelper (handles get_photo.php PHP stream decoding)
        String photoUrl = worker.getPhotoUrl();
        if (h.tvAvatar != null) {
            h.tvAvatar.setImageResource(R.drawable.ic_court_suit_avatar);
        }
        ImageLoaderHelper.loadImage(h.itemView.getContext(), photoUrl, h.imgPhoto, h.tvAvatar);

        // Status Text & Badge
        String displayStatus = !TextUtils.isEmpty(worker.getDisplayText())
                ? worker.getDisplayText()
                : (!TextUtils.isEmpty(worker.getSiteName()) ? worker.getSiteName() : "NOT MARKED");

        h.tvBadge.setText(displayStatus);

        // 1. IN Button Logic (Labour screen is strictly for Check-In)
        h.btnTimeIn.setVisibility(worker.isClockedIn() ? View.GONE : View.VISIBLE);
        h.btnTimeIn.setOnClickListener(v -> {
            if (listener != null) listener.onTimeInClicked(worker);
        });

        // 2. OUT Button Logic (Moved to Manage Attendance screen)
        if (h.btnTimeOut != null) {
            h.btnTimeOut.setVisibility(View.GONE);
            h.btnTimeOut.setOnClickListener(null);
        }

        // Meta Text (e.g. Status: Not Clocked In / Site Name)
        String metaText = !TextUtils.isEmpty(worker.getSiteName())
                ? "Status: " + worker.getSiteName()
                : "Status: " + displayStatus;
        h.tvMeta.setText(metaText);
        h.tvMeta.setVisibility(View.VISIBLE);

        // 3. UI Feedback for "Locked" and "Status"
        String statusCode = worker.getStatusCode();
        if ("LOCKED_OTHER".equalsIgnoreCase(statusCode)) {
            h.tvBadge.setTextColor(Color.parseColor("#03045E"));
            setBadgeBackground(h.tvBadge, "#CAF0F8", "#90E0EF");
            h.itemView.setBackgroundResource(R.drawable.bg_move_emp_card);
        } else if ("IN_HERE".equalsIgnoreCase(statusCode) || "PRESENT".equalsIgnoreCase(statusCode)) {
            h.tvBadge.setTextColor(Color.parseColor("#0077B6"));
            setBadgeBackground(h.tvBadge, "#CAF0F8", "#00B4D8");
            h.itemView.setBackgroundResource(R.drawable.bg_move_emp_card);
        } else {
            h.tvBadge.setTextColor(Color.parseColor("#0077B6"));
            setBadgeBackground(h.tvBadge, "#CAF0F8", "#90E0EF");
            h.itemView.setBackgroundResource(R.drawable.bg_move_emp_card);
        }
    }

    private void setBadgeBackground(TextView tv, String bgColor, String borderColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(16f);
        shape.setColor(Color.parseColor(bgColor));
        shape.setStroke(2, Color.parseColor(borderColor));
        tv.setBackground(shape);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView tvAvatar;
        TextView tvName, tvBadge, tvMeta;
        ShapeableImageView imgPhoto;
        MaterialButton btnTimeIn, btnTimeOut;

        ViewHolder(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tv_emp_avatar);
            imgPhoto = v.findViewById(R.id.img_emp_photo);
            tvName = v.findViewById(R.id.tv_emp_name);
            tvBadge = v.findViewById(R.id.tv_emp_badge);
            tvMeta = v.findViewById(R.id.tv_emp_meta);
            btnTimeIn = v.findViewById(R.id.btn_time_in);
            btnTimeOut = v.findViewById(R.id.btn_time_out);
        }
    }
}

