package com.example.sendnotificationtowebhook;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import com.google.android.material.chip.Chip;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<NotificationItem> notifications = new ArrayList<>();
    private RecyclerView recyclerView;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, contentText, timestampText;
        Chip statusChip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.titleText);
            contentText = itemView.findViewById(R.id.contentText);
            timestampText = itemView.findViewById(R.id.timestampText);
            statusChip = itemView.findViewById(R.id.statusChip);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = notifications.get(position);
        holder.titleText.setText(item.getTitle());
        holder.contentText.setText(item.getContent());
        
        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String time = sdf.format(new Date(item.getTimestamp()));
        holder.timestampText.setText(time);

        // Update status chip
        if (item.isSendSuccess()) {
            holder.statusChip.setText("Sent");
            holder.statusChip.setChipBackgroundColorResource(R.color.md_theme_light_success);
        } else {
            holder.statusChip.setText("Failed");
            holder.statusChip.setChipBackgroundColorResource(R.color.md_theme_light_error);
        }
        holder.statusChip.setTextColor(Color.WHITE);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    public void addNotification(NotificationItem item) {
        // Kiểm tra xem item đã tồn tại chưa
        for (NotificationItem existingItem : notifications) {
            if (existingItem.getTimestamp() == item.getTimestamp() &&
                existingItem.getTitle().equals(item.getTitle()) &&
                existingItem.getContent().equals(item.getContent())) {
                // Nếu item đã tồn tại, chỉ cập nhật trạng thái
                existingItem.setSendSuccess(item.isSendSuccess());
                notifyItemChanged(notifications.indexOf(existingItem));
                return;
            }
        }

        // Thêm item mới vào đầu danh sách
        notifications.add(0, item);
        notifyItemInserted(0);
        
        // Cuộn lên đầu danh sách
        if (recyclerView != null) {
            recyclerView.smoothScrollToPosition(0);
        }
    }
} 