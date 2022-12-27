package com.example.droppoint;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.fragment.app.Fragment;

import com.example.droppoint.mapPostResources.DataModal;
import com.example.droppoint.mapPostResources.PostsAdapter;

import java.util.ArrayList;

public class MapPostsFragment extends Fragment{

    // creating a variable for our grid view, arraylist and firebase Firestore.
    private GridView postsGV;
    ArrayList<DataModal> postsArr;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_map_posts, container,false);

        //get data transferred from map fragment
        postsArr= (ArrayList<DataModal>) getArguments().getSerializable("postsArray");

        postsGV = view.findViewById(R.id.idPosts);

        PostsAdapter adapter = new PostsAdapter(getActivity(), postsArr);
        postsGV.setAdapter(adapter);
        return view;
    }
}