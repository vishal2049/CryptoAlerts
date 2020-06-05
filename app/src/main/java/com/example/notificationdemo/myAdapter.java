package com.example.notificationdemo;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

class myAdapter extends RecyclerView.Adapter<myAdapter.myViewHolder> {
    private List<model> alert_list;
    private RecyclerViewClickListner mlistner;
//    private String RpriceForDB;

    //creating constructor to get List to be inflate on layout
    public myAdapter(List<model> alert_list,RecyclerViewClickListner mylistner) {
        this.alert_list = alert_list;
        mlistner=mylistner;
    }

    //creating ViewHolder class
    public class myViewHolder extends RecyclerView.ViewHolder {
        private TextView Rsymbol, Rprice;
        private ImageButton Rdelete;

        public myViewHolder(@NonNull View itemView) {
           super(itemView);
            Rsymbol = itemView.findViewById(R.id.Rsymbol);
            Rprice = itemView.findViewById(R.id.Rprice);
            Rdelete = itemView.findViewById(R.id.Rdelete);

//            itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//
//                }
//            });
            Rdelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    model mod = alert_list.get(getAdapterPosition());
                    mlistner.onImageClick(getAdapterPosition(),mod.getrprice());
                }
            });
        }
    }

    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.alert_items_layout, parent, false);
        return new myViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull myViewHolder holder, int position) {
        model item=alert_list.get(position);
        holder.Rsymbol.setText(item.getrsymbol());
        holder.Rprice.setText(item.getrprice());
        holder.Rdelete.setImageResource(item.getrdelete());
  }

    @Override
    public int getItemCount() {
        return alert_list.size();
    }


}

