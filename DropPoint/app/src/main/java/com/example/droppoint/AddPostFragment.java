package com.example.droppoint;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AddPostFragment extends Fragment {

    private EditText editLocation, editHours, editTips, editOther;
    private TextView postText;
    private ShapeableImageView uploadImage;
    private Uri imageURI;
    private LatLng coordinates;
    private String name;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageReference; //cloud storage for images
    private String userID;

    boolean isAllFieldsChecked = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_add_post, container,false);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firebase database
        db = FirebaseFirestore.getInstance();

        //Initialize Places
        Places.initialize(getActivity(), getActivity().getString(R.string.google_maps_key));

        // Create a storage reference from our app
        storageReference = FirebaseStorage.getInstance().getReference();

        editLocation = view.findViewById(R.id.locationEditText);
        editHours = view.findViewById(R.id.hoursEditText);
        editTips = view.findViewById(R.id.tipsEditText);
        editOther = view.findViewById(R.id.otherEditText);
        postText = view.findViewById(R.id.postBtn);
        uploadImage = view.findViewById(R.id.uploadPostImg);

        editLocation.setFocusable(false);

        ActivityResultLauncher locationGetContent = registerForActivityResult( new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode() == AutocompleteActivity.RESULT_OK && result.getData() != null){
                Place place = Autocomplete.getPlaceFromIntent(result.getData());
                editLocation.setText(place.getAddress());
                coordinates = place.getLatLng();
                name = place.getName();
            } else if(result.getResultCode() == AutocompleteActivity.RESULT_ERROR){
                Status status = Autocomplete.getStatusFromIntent(result.getData());
                Toast.makeText(getActivity(), status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });


        editLocation.setOnClickListener(view1 -> {
            List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS,
                    Place.Field.LAT_LNG, Place.Field.NAME);
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fieldList).build(getActivity());
            locationGetContent.launch(intent);
        });

        ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if(uri != null){
                        uploadImage.setImageURI(uri);
                        imageURI = uri;
                    }
                }
        );

        uploadImage.setOnClickListener(v -> mGetContent.launch("image/*"));

        postText.setOnClickListener(v -> publish());

        // Inflate the layout for this fragment
        return view;
    }

    private void publish(){
        String location = editLocation.getText().toString().trim();
        String hours = editHours.getText().toString().trim();
        String tips = editTips.getText().toString();
        String other = editOther.getText().toString();

        isAllFieldsChecked = CheckAllFields();

        if (isAllFieldsChecked) {
            //store user data in database
            storeUserData(location, hours, tips, other);
        }
    }

    private boolean CheckAllFields() {
        if (imageURI == null) {
            Toast.makeText(getActivity(), "Please upload an image",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (editLocation.length() == 0) {
            editLocation.setError("Please enter the location of the place");
            return false;
        }

        // after all validation is done for each field return true.
        return true;
    }

    private void storeUserData(String location, String hours, String tips,String other ) {
        //get user & store in db posts collection
        userID = mAuth.getCurrentUser().getUid();
        DocumentReference documentReference = db.collection("users").document(userID)
                .collection("posts").document();

        //Specify the file path and name of profile image
        String path = "postImages/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference postRef = storageReference.child(path);

        Map<String,Object> trip = new HashMap<>();

        // Compute the GeoHash for a lat/lng point
        String hash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(coordinates.latitude, coordinates.longitude));

        postRef.putFile(imageURI).addOnCompleteListener(task ->{
            //store to users collection in database
            trip.put("PostID", documentReference.getId());
            trip.put("Location", location);
            trip.put("Name", name);
            trip.put("Latitude", coordinates.latitude);
            trip.put("Longitude", coordinates.longitude);
            trip.put("geohash", hash);
            trip.put("Hours", hours);
            trip.put("Tips", tips);
            trip.put("Other", other);
            trip.put("PostImageURL", path);

            documentReference.set(trip).addOnSuccessListener(u ->{
                Toast.makeText(getActivity(), "Post was shared!", Toast.LENGTH_SHORT).show();
                clearFields();
            }).addOnFailureListener(e -> Log.d(TAG, "onFailure: " + e));

        }).addOnFailureListener(e -> Toast.makeText(getActivity(), "Error storing image. Please try again", Toast.LENGTH_SHORT).show());

    }

    private void clearFields() {
        editHours.getText().clear();
        editLocation.getText().clear();
        editTips.getText().clear();
        editOther.getText().clear();
        uploadImage.setImageResource(R.drawable.upload_photo);
    }
}