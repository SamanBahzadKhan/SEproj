package com.fridge.caps.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.TimeSlot;
import com.fridge.caps.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * TimeSlotsAdapter.java
 * RecyclerView adapter for displaying available time slots in a grid.
 * View in the MVC pattern.
 */
public class TimeSlotsAdapter extends RecyclerView.Adapter<TimeSlotsAdapter.SlotViewHolder> {

    public interface OnSlotClickListener {
        void onSlotClick(TimeSlot slot);
    }

    private final List<TimeSlot>      slots;
    private final OnSlotClickListener listener;
    private final SimpleDateFormat    timeFormat;
    private final SimpleDateFormat    dateFormat;

    public TimeSlotsAdapter(List<TimeSlot> slots, OnSlotClickListener listener) {
        this.slots      = slots;
        this.listener   = listener;
        this.timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time_slot, parent, false);
        return new SlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        TimeSlot slot = slots.get(position);
        if (slot.getDate() != null && slot.getStartTime() != null) {
            holder.tvDate.setText(DateUtils.toDisplayDate(slot.getDate()));
            holder.tvTime.setText(slot.getStartTime());
        } else if (slot.getLegacyStartTime() != null) {
            Date d = slot.getLegacyStartTime().toDate();
            holder.tvDate.setText(dateFormat.format(d));
            holder.tvTime.setText(timeFormat.format(d));
        } else {
            holder.tvDate.setText("—");
            holder.tvTime.setText("—");
        }
        holder.itemView.setOnClickListener(v -> listener.onSlotClick(slot));
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    static class SlotViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvTime;

        SlotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
