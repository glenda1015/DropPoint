package com.example.droppoint;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.droppoint.mapPostResources.DataModal;
import com.example.droppoint.mapViewResources.CustomClusterRenderers;
import com.example.droppoint.mapViewResources.MapGetOrDefault;
import com.example.droppoint.mapViewResources.MyCluster;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MapsFragment extends Fragment implements GoogleMap.OnMyLocationClickListener, GoogleMap.OnMyLocationButtonClickListener, OnMapReadyCallback, OnMapsSdkInitializedCallback {

    private FirebaseFirestore db;
    private List<MyCluster> postsLocations;

    private GoogleMap map;

    // Declare variable for the cluster manager.
    private ClusterManager<MyCluster> clusterManager;

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        // Position the map.
        LatLng center = new LatLng(22, -82);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 5));

        // Initialize the manager with the context and the map.
        clusterManager = new ClusterManager<>(this.getActivity(), map);

        CustomClusterRenderers renderers  =  new CustomClusterRenderers(getActivity(),map,clusterManager);

        clusterManager.setRenderer(renderers);

        // Point the map's listeners at the listeners implemented by the cluster manager.
        map.setOnCameraIdleListener(clusterManager);
        map.setOnMarkerClickListener(clusterManager);

        enableMyLocation();

        for (MyCluster marker: postsLocations) {
            clusterManager.addItem(marker);
        }

        clusterManager.cluster();

        clusterManager.setOnClusterClickListener(cluster -> {
            Collection<MyCluster> markersArr = cluster.getItems();

            //put post ids inside an array of strings
            ArrayList<DataModal> posts = new ArrayList<>();
            for (MyCluster item:markersArr) {
                posts.add(item.getDataModal());
            }
            Collections.shuffle(posts); //right now there is no criteria for specific order of showing posts. This simulates likes/followers by randomizing array.

            //replace fragment with fragment that shows posts
            //pass post ids to the fragment
            Bundle bundle = new Bundle();
            bundle.putSerializable("postsArray", posts);
            MapPostsFragment mapPostsFragment = new MapPostsFragment();
            mapPostsFragment.setArguments(bundle);
            getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, mapPostsFragment, null).addToBackStack(null).commit();

           return true;
        });

        clusterManager.setOnClusterItemClickListener(item -> {
            //replace fragment with fragment that shows posts
            //pass post ids to the fragment
            Bundle bundle = new Bundle();
            bundle.putString("postID", item.getPostID());
            PostFragment postFragment = new PostFragment();
            postFragment.setArguments(bundle);
            getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, postFragment, null).addToBackStack(null).commit();
            return true;
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Initialize Firebase database
        db = FirebaseFirestore.getInstance();
        MapsInitializer.initialize(getActivity(), MapsInitializer.Renderer.LATEST, this);

        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        postsLocations = new ArrayList<>();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            loadAllLocations(mapFragment);
        }
    }

    private void loadAllLocations(SupportMapFragment mapFragment) {

        //Retrieve data from db
        db.collectionGroup("posts").get().addOnCompleteListener(task -> {
            for (QueryDocumentSnapshot document : task.getResult()) {
                double latitude = document.getDouble("Latitude");
                double longitude = document.getDouble("Longitude");
                String postID = document.getString("PostID");

                DataModal dataModal = document.toObject(DataModal.class);

                postsLocations.add(new MyCluster(longitude,latitude,postID,dataModal));
            }
            mapFragment.getMapAsync(this);
        }).addOnFailureListener(e -> Log.d(TAG, "Error getting documents: ", e));
    }


//     Enables the My Location layer if the fine location permission has been granted.
    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        // 1. Check if permissions are granted, if so, enable the my location layer
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            return;
        }

        locationPermissionRequest.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });    }

    ActivityResultLauncher<String[]> locationPermissionRequest =
        registerForActivityResult(new ActivityResultContracts
                        .RequestMultiplePermissions(), result -> {

            Boolean fineLocationGranted =
                            MapGetOrDefault.getOrDefault(result,
                            Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted =  MapGetOrDefault.getOrDefault(result,
                            Manifest.permission.ACCESS_COARSE_LOCATION,false);

            if (fineLocationGranted != null && fineLocationGranted) {
                // Precise location access granted.
                enableMyLocation();
            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                // Only approximate location access granted.
                enableMyLocation();
            }
        }
    );


    @Override
    public boolean onMyLocationButtonClick() {
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        //Handles click on location of user
    }

    @Override
    public void onMapsSdkInitialized(@NonNull MapsInitializer.Renderer renderer) {
        switch (renderer) {
            case LATEST:
                Log.d("MapsDemo", "The latest version of the renderer is used.");
                break;
            case LEGACY:
                Log.d("MapsDemo", "The legacy version of the renderer is used.");
                break;
        }
    }
}