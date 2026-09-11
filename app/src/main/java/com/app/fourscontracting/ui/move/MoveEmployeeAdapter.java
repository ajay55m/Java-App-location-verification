package com.app.fourscontracting.ui.move;

import android.graphics.Bitmap;
import android.graphics.Color;
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
import com.app.fourscontracting.data.MoveEmployeeModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class MoveEmployeeAdapter extends RecyclerView.Adapter<MoveEmployeeAdapter.Holder> {

    public interface Listener {
        void onAction(MoveEmployeeModel employee);
    }

    private final List<MoveEmployeeModel> items = new ArrayList<>();
    private final Listener listener;

    public MoveEmployeeAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<MoveEmployeeModel> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_move_employee, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        MoveEmployeeModel emp = items.get(position);
        holder.name.setText(emp.firstName);
        holder.meta.setText("Primary " + emp.timeInDisplay + " → " + emp.timeOutDisplay);

        String photoUrl = emp.photoUrl;
        com.app.fourscontracting.data.ImageLoaderHelper.loadImage(holder.itemView.getContext(), photoUrl, holder.photo, holder.avatar);

        if (emp.onMove) {
            holder.badge.setText("ON MOVE");
            holder.badge.setTextColor(Color.parseColor("#0077B6"));
            holder.action.setVisibility(View.GONE);
            holder.action.setOnClickListener(null);
        } else {
            holder.badge.setText("READY FOR NEXT SITE");
            holder.badge.setTextColor(Color.parseColor("#0077B6"));
            holder.action.setText("MOVE");
            holder.action.setVisibility(View.VISIBLE);
            holder.action.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#03045E")));
            holder.action.setOnClickListener(v -> {
                if (listener != null) listener.onAction(emp);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final View avatar;
        final ShapeableImageView photo;
        final TextView name;
        final TextView badge;
        final TextView meta;
        final MaterialButton action;

        Holder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.tv_emp_avatar);
            photo = itemView.findViewById(R.id.img_emp_photo);
            name = itemView.findViewById(R.id.tv_emp_name);
            badge = itemView.findViewById(R.id.tv_emp_badge);
            meta = itemView.findViewById(R.id.tv_emp_meta);
            action = itemView.findViewById(R.id.btn_emp_action);
        }
    }
}
