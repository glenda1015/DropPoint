package com.example.droppoint.postDialogResources;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droppoint.R;

import java.util.ArrayList;

public class HorizontalItemAdapter extends RecyclerView.Adapter<HorizontalItemAdapter.MyHolder> {
    private ArrayList<TripDataModal> data;
    private RecyclerViewClickListener listener;
    public HorizontalItemAdapter(ArrayList<TripDataModal> data, RecyclerViewClickListener listener) {
        this.data = data;
        this.listener = listener;
    }
    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_trips_item, parent, false);
        return new MyHolder(view);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        TripDataModal tripDataModal = data.get(position);
        holder.tripTitle.setText(tripDataModal.getTripName());
    }

    public interface RecyclerViewClickListener{
        void onClick(View v, int position);
    }

    class MyHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView tripTitle;
        public MyHolder(@Nullable View itemView){
            super(itemView);
            tripTitle = itemView.findViewById(R.id.tripTitle);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onClick(view,getAdapterPosition());
        }
    }
}
