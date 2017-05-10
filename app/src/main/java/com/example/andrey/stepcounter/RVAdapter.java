package com.example.andrey.stepcounter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Andrey on 17.04.2017.
 */

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.SensorsViewHolder> {

    public static class SensorsViewHolder extends RecyclerView.ViewHolder {

        public TextView manufacturer, model, datasource;

        public SensorsViewHolder(View itemView) {
            super(itemView);
            manufacturer = (TextView) itemView.findViewById(R.id.deviceManufacturer);
            model = (TextView) itemView.findViewById(R.id.deviceModel);
            datasource = (TextView) itemView.findViewById(R.id.dataSource);
        }
    }

    ArrayList<Item> sensors;

    RVAdapter(ArrayList<Item> sensors) {
        this.sensors = sensors;
    }

    @Override
    public SensorsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycle_view_item, parent, false);
        SensorsViewHolder svh = new SensorsViewHolder(v);
        return svh;
    }

    @Override
    public void onBindViewHolder(SensorsViewHolder holder, int position) {
        holder.manufacturer.setText(sensors.get(position).manuf);
        holder.model.setText(sensors.get(position).model);
        holder.datasource.setText(sensors.get(position).sens);

    }

    @Override
    public int getItemCount() {
        return sensors.size();
    }

}
