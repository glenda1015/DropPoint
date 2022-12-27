package com.example.droppoint.mapPostResources;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.droppoint.PostFragment;
import com.example.droppoint.R;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class PostsAdapter extends ArrayAdapter<DataModal> {

    private StorageReference storageRef;
    String imageURL;
    // constructor for our list view adapter.
    public PostsAdapter(@NonNull Context context, ArrayList<DataModal> dataModalArrayList) {
        super(context, 0, dataModalArrayList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Create a storage reference from our app
        storageRef = FirebaseStorage.getInstance().getReference();

        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_map_item, parent, false);
        }

        // get data from array list inside
        DataModal dataModal = getItem(position);

        ImageView postIV = listItemView.findViewById(R.id.idIVimage);

        // load image from URL in our Image View.
        //Specify the path in storage for the profile image to be fetched
        String profileImgPath = dataModal.getPostImageURL();
        StorageReference imageRef = storageRef.child(profileImgPath);

        imageRef.getDownloadUrl().addOnCompleteListener(task -> {
            Glide.with(getContext()).load(task.getResult()).into(postIV);

            imageURL = task.getResult().toString();
        });

        //on item click, switch to individual post view
        listItemView.setOnClickListener(view -> {
            Bundle bundle = new Bundle();
            bundle.putString("postID", dataModal.getPostID());
            PostFragment postFragment = new PostFragment();
            postFragment.setArguments(bundle);
            AppCompatActivity activity = (AppCompatActivity)  view.getContext();
            activity.getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, postFragment, null).addToBackStack(null).commit();
        });

        return listItemView;
    }
}
