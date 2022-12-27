package com.example.droppoint;

import static android.content.ContentValues.TAG;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.droppoint.postDialogResources.HorizontalItemAdapter;
import com.example.droppoint.postDialogResources.TripDataModal;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PostFragment extends Fragment {
    private TextView textViewPostName, textViewLocation, textViewHours, textViewTips, textViewOther;
    private ImageView locationImage, bookmarkBtn;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userID;
    private StorageReference storageRef;
    private String postID;
    public ProgressDialog progressDialog;
    private BottomSheetDialog bottomSheetDialog;
    private ArrayList<TripDataModal> dataModalArrayList;
    private RecyclerView recyclerView;
    private HorizontalItemAdapter.RecyclerViewClickListener listener;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_post, container,false);

        //get data transferred from map fragment
        postID  = getArguments().getString("postID");

        // Initialize Firebase database
        db = FirebaseFirestore.getInstance();

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        //getting current users logged in
        userID = mAuth.getCurrentUser().getUid();

        // Create a storage reference from our app
        storageRef = FirebaseStorage.getInstance().getReference();

        textViewPostName = view.findViewById(R.id.postNameTextView);
        textViewLocation = view.findViewById(R.id.locationTextView);
        textViewHours = view.findViewById(R.id.hoursTextView);
        textViewTips = view.findViewById(R.id.tipsTextView);
        textViewOther = view.findViewById(R.id.otherTextView);
        locationImage = view.findViewById(R.id.postImg);
        bookmarkBtn = view.findViewById(R.id.bookmarkBtnImg);

        //Load the page with the user's data once
        showProgress();
        loadPostData();

        //handle click on bookmark icon
        dataModalArrayList = new ArrayList<>();

        bookmarkBtn.setOnClickListener(view1 -> {
            if(dataModalArrayList.isEmpty()){
                db.collection("users").document(userID)
                .collection("trips").get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    TripDataModal tripDataModal = document.toObject(TripDataModal.class);
                                    dataModalArrayList.add(tripDataModal);
                                }
                            } else {
                                Log.d("err", "Error getting documents: ", task.getException());
                            }
                            showBottomDialog();
                        }
                );
            }
            else{
                showBottomDialog();
            }
        });

        return view;
    }

    private void loadPostData() {
        //Retrieve data from db
        db.collectionGroup("posts").whereEqualTo("PostID", postID).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            textViewPostName.setText(document.getString("Name"));
                            textViewLocation.setText(document.getString("Location"));

                            if(document.getString("Hours").isEmpty()){
                                textViewHours.setVisibility(View.GONE);
                            }else{
                                textViewHours.setText(document.getString("Hours"));
                            }

                            if(document.getString("Tips").isEmpty()){
                                textViewTips.setVisibility(View.GONE);
                            }else{
                                textViewTips.setText(document.getString("Tips"));
                            }

                            if(document.getString("Other").isEmpty()){
                                textViewOther.setVisibility(View.GONE);
                            }else{
                                textViewOther.setText(document.getString("Other"));
                            }

                            String postImgPath = document.getString("PostImageURL");
                            StorageReference imageRef = storageRef.child(postImgPath);

                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                    Glide.with(this).load(uri).into(locationImage);

                            }).addOnFailureListener(exception -> Toast.makeText(getActivity(), "Error in displaying profile image", Toast.LENGTH_SHORT).show());
                        }

                        dismissLoading();

                    } else {
                        Log.d("err", "Error getting document: ", task.getException());
                    }
                }
        );
    }

    private void showBottomDialog(){
        View dialogView = getLayoutInflater().inflate(R.layout.bottom_trips_dialog, null);
        bottomSheetDialog = new BottomSheetDialog(getActivity(), R.style.BottomSheetDialogTheme);
        bottomSheetDialog.setContentView(dialogView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        setOnCLickListener();
        recyclerView = dialogView.findViewById(R.id.rvItem);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(new HorizontalItemAdapter(dataModalArrayList, listener));
        bottomSheetDialog.show();
    }

    private void setOnCLickListener(){
        listener = (v, position) -> {
            String tripId = dataModalArrayList.get(position).getTripId();
            storeTripFolder(tripId);
        };
    }

    private void storeTripFolder(String tripId){
        //get user & store in db bookmarks collection
        db.collection("users").document(userID)
                .collection("trips").document(tripId).collection("bookmarks").whereEqualTo("PostIdRef", postID)
                .get().addOnCompleteListener(task -> {
                    if(task.getResult().isEmpty()){
                        DocumentReference documentReference = db.collection("users").document(userID)
                                .collection("trips").document(tripId).collection("bookmarks").document();

                        Map<String,Object> bookmark = new HashMap<>();

                        //store to users collection in database
                        bookmark.put("BookmarkId", documentReference.getId());
                        bookmark.put("PostIdRef", postID);

                        documentReference.set(bookmark).addOnSuccessListener(unused ->{
                                        Log.d(TAG, "onSuccess: trip folder is created for " + userID);
                                        bottomSheetDialog.dismiss();
                                    bookmarkBtn.setImageResource(R.drawable.ic_baseline_bookmark_24);
                                })
                                .addOnFailureListener(e -> Log.d(TAG, "onFailure: " + e));
                    }
                    else{
                        Toast.makeText(getActivity(), "This post was already added to this trip", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void showProgress() {
        progressDialog = new ProgressDialog(getActivity());
        if (progressDialog != null) {
            progressDialog.show();
            progressDialog.setCancelable(true);
            progressDialog.setContentView(R.layout.progress_dialog);
            progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
    public void dismissLoading() {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }
}