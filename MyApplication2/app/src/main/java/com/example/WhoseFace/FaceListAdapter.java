package com.example.WhoseFace;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class FaceListAdapter extends RecyclerView.Adapter<FaceListAdapter.MyViewHolder>{
    private ArrayList<String> recognizedFaceName = null;
    private ArrayList<Bitmap> recognizedFace = null;
    private ArrayList<String> recognizedTimeStamp = null;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        View mView;
        MyViewHolder(View view) {
            super(view);
            mView = view;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public FaceListAdapter(ArrayList<String> recognizedFaceName, ArrayList<Bitmap> recognizedFace, ArrayList<String> recognizedTimeStamp) {
        this.recognizedFaceName = recognizedFaceName;
        this.recognizedFace = recognizedFace;
        this.recognizedTimeStamp = recognizedTimeStamp;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public FaceListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_view_item_layout, parent, false);
        return new MyViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        ImageView imageView = holder.mView.findViewById(R.id.face_container);
        Bitmap bitmap = recognizedFace.get(position);
        imageView.setImageBitmap(bitmap);
        TextView textView = holder.mView.findViewById(R.id.name_container_list);
        textView.setText(this.recognizedFaceName.get(position));
        TextView timestampTv = holder.mView.findViewById(R.id.timestamp_container);
        timestampTv.setText(this.recognizedTimeStamp.get(position));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (recognizedFace.size() == recognizedFaceName.size()){
            return recognizedFace.size();
        } else {
            Logger.i("your list is mismatching !!!");
            return 0;
        }
    }
}
