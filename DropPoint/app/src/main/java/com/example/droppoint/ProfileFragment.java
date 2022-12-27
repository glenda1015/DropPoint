package com.example.droppoint;

import static android.content.ContentValues.TAG;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.droppoint.mapPostResources.DataModal;
import com.example.droppoint.mapViewResources.MyCluster;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {
    private TextView textEmail, textBio;
    private ImageView profileImage;

    private ListView tripsListView;
    private ImageView addTripIcon;
    private List<String> tripsList;
    private ArrayAdapter arrayAdapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private String userID;
    private String tripId;
    private ArrayList<String> bookmarkPostIds;
    private ArrayList<DataModal> posts;
    public ProgressDialog progressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firebase database
        db = FirebaseFirestore.getInstance();

        // Create a storage reference from our app
        storageRef = FirebaseStorage.getInstance().getReference();

        textEmail = view.findViewById(R.id.displayUserEmail);
        textBio = view.findViewById(R.id.displayUserBio);
        profileImage = view.findViewById(R.id.displayUserImg);
        tripsListView = view.findViewById(R.id.myTripsListView);
        addTripIcon = view.findViewById(R.id.addTripIcon);

        //Load the page with the user's data once
        showProgress();
        loadProfileData();

        //Handle Adding Trip Folders
        tripsList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, tripsList);
        tripsListView.setAdapter(arrayAdapter);


        tripsListView.setOnItemClickListener((adapterView, view1, i, l) -> {
            bookmarkPostIds = new ArrayList<>();
            posts = new ArrayList<>();
            String trip = (String) adapterView.getItemAtPosition(i);
            queryForTripID(trip);
        });

        addTripIcon.setOnClickListener(v -> enterFolderNameDialog());
        setHasOptionsMenu(true);

        return view;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.profile_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.map: {
                mAuth.signOut();
                startActivity(new Intent(getActivity().getApplicationContext(), LoginActivity.class));
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadProfileData() {
        //Get ID from the user
        userID = mAuth.getCurrentUser().getUid();

        //Retrieve data from db
        DocumentReference documentReference = db.collection("users").document(userID);
        documentReference.get().addOnCompleteListener(task -> {
            textEmail.setText(task.getResult().getString("Email"));
            textBio.setText(task.getResult().getString("Bio"));

            //fetch trip folders
            documentReference.collection("trips").get().addOnCompleteListener(task1 -> {
                for (QueryDocumentSnapshot document : task1.getResult()) {
                    tripsList.add(document.getString("TripName"));
                    arrayAdapter.notifyDataSetChanged();
                }
            }).addOnFailureListener(e ->
                    Log.d(TAG, "Error getting documents: ", task.getException()));

            //Specify the path in storage for the profile image to be fetched
            String profileImgPath = task.getResult().getString("ProfileImgURL");
            StorageReference imageRef = storageRef.child(profileImgPath);

            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Glide.with(this).load(uri).into(profileImage);
                dismissLoading();
            }).addOnFailureListener(exception -> Toast.makeText(getActivity(), "Error in displaying profile image", Toast.LENGTH_SHORT).show());
        });
    }

    private void enterFolderNameDialog() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(getActivity());
        final EditText input = new EditText(getActivity());

        dialogBuilder.setTitle("Add a new trip")
                .setMessage("Enter a name for your trip:")
                .setIcon(R.drawable.ic_baseline_airplane_ticket_24)
                .setBackground(getResources().getDrawable(R.drawable.alert_dialog_bg, null))
                .setView(input);

        dialogBuilder.setPositiveButton("OK", (dialogInterface, i) -> {
            if (input.length() != 0) {
                String tripName = input.getText().toString();
                addTrip(tripName);
            } else {
                Toast.makeText(getActivity(), "Trip name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        }).setNegativeButton("CANCEL", (dialogInterface, i) ->
                dialogInterface.cancel()).show();
    }

    private void addTrip(String tripName) {
        //capitalize first letter for consistency
        tripName = tripName.substring(0, 1).toUpperCase() + tripName.substring(1);

        //add to listview
        tripsList.add(tripName);

        arrayAdapter.notifyDataSetChanged();

        //store in db
        storeTripFolder(tripName);
    }

    private void storeTripFolder(String tripName) {
        //get user & store in db trips collection
        DocumentReference documentReference = db.collection("users").document(userID)
                .collection("trips").document();
        Map<String, Object> trip = new HashMap<>();

        //store to users collection in database
        trip.put("TripId", documentReference.getId());
        trip.put("TripName", tripName);
        documentReference.set(trip).addOnSuccessListener(unused ->
                        Log.d(TAG, "onSuccess: trip folder is created for " + userID))
                .addOnFailureListener(e -> Log.d(TAG, "onFailure: " + e));
    }

    //get reference to trip that has the condition specified
    private void queryForTripID(String trip) {
        CollectionReference tripsRef = db.collection("users").document(userID).collection("trips");

        tripsRef.whereEqualTo("TripName", trip).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    tripId = document.getString("TripId");
                }
                queryForBookmarks(tripsRef, tripId);
            } else {
                Log.d("err", "Error getting documents: ", task.getException());
            }
        });
    }

    //get all bookmarks within the trip folder reference
    private void queryForBookmarks(CollectionReference tripsRef, String tripId) {
        tripsRef.document(tripId).collection("bookmarks")
            .get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        bookmarkPostIds.add(document.getString("PostIdRef"));
                    }
                } else {
                    Log.d("err", "Error getting documents: ", task.getException());
                }
                if (bookmarkPostIds.isEmpty()) {
                    EmptyTripFolderFragment emptyTripFolderFragment = new EmptyTripFolderFragment();
                    getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, emptyTripFolderFragment, null).addToBackStack(null).commit();
                } else {
                    sendDataToFrag(bookmarkPostIds);
                }
            }
        );
    }

    private void sendDataToFrag(ArrayList<String> bookmarkPostIds) {
        db.collectionGroup("posts").whereIn("PostID", bookmarkPostIds)
            .get().addOnCompleteListener(task2 -> {
                if (task2.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task2.getResult()) {
                        DataModal dataModal = document.toObject(DataModal.class);
                        posts.add(dataModal);
                    }
                } else {
                    Log.d("err", "Error getting documents: ", task2.getException());
                }
                //send post ids to the MapPostsFragment
                Bundle bundle = new Bundle();
                bundle.putSerializable("postsArray", posts);
                MapPostsFragment mapPostsFragment = new MapPostsFragment();
                mapPostsFragment.setArguments(bundle);
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, mapPostsFragment, null).addToBackStack(null).commit();
            }
        );
    }

    public void showProgress() {
        progressDialog = new ProgressDialog(getActivity());
        if (progressDialog != null) {
            progressDialog.show();
            progressDialog.setCancelable(true);
            progressDialog.setContentView(R.layout.progress_dialog);
            progressDialog.getWindow().setBackgroundDrawableResource(
                    android.R.color.transparent
            );
        }
    }

    public void dismissLoading() {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }
}