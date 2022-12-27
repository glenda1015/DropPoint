package com.example.droppoint;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.example.droppoint.mapPostResources.DataDistModal;
import com.example.droppoint.mapPostResources.DataModal;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SearchFragment extends Fragment {

    private EditText editSearchBar;
    private LatLng coordinates;
    private String name;
    private FirebaseFirestore db;
    public ProgressDialog progressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_search, container,false);

        // Initialize Firebase database
        db = FirebaseFirestore.getInstance();

        editSearchBar = view.findViewById(R.id.searchBar);
        //Initialize Places
        com.google.android.libraries.places.api.Places.initialize(getActivity(), getActivity().getString(R.string.google_maps_key));

        //Set searchBar non focusable
        editSearchBar.setFocusable(false);

        getActivity().getSupportFragmentManager().addOnBackStackChangedListener(() -> editSearchBar.setText(null));

        ActivityResultLauncher searchLocGetContent = registerForActivityResult( new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode() == AutocompleteActivity.RESULT_OK && result.getData() != null){
                Place place = Autocomplete.getPlaceFromIntent(result.getData());
                editSearchBar.setText(place.getName());
                coordinates = place.getLatLng();
                name = place.getName();

                //load result of search
                searchResultOfLocation();
            } else if(result.getResultCode() == AutocompleteActivity.RESULT_ERROR){
                Status status = Autocomplete.getStatusFromIntent(result.getData());
                Toast.makeText(getActivity(), status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        editSearchBar.setOnClickListener(view1 -> {
            List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS,
                    Place.Field.LAT_LNG, Place.Field.NAME);
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fieldList).build(getActivity());
            searchLocGetContent.launch(intent);
        });
        return view;
    }

    private void searchResultOfLocation(){
        showProgress();
        // Find cities within 30km of location selected based on latitude and longitude
        final GeoLocation center = new GeoLocation(coordinates.latitude, coordinates.longitude);
        final double radiusInM = 30 * 1000;

        // Each item in 'bounds' represents a startAt/endAt pair. We have to issue
        // a separate query for each pair. There can be up to 9 pairs of bounds
        // depending on overlap, but in most cases there are 4.
        List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM);
        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (GeoQueryBounds b : bounds) {
            Query q = db.collectionGroup("posts")
                    .orderBy("geohash")
                    .startAt(b.startHash)
                    .endAt(b.endHash);
            tasks.add(q.get());
        }


        Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> {
            ArrayList<DataDistModal> postsDist = new ArrayList<>();
            ArrayList<DataModal> posts = new ArrayList<>();

            for (Task<QuerySnapshot> task : tasks) {
                QuerySnapshot snap = task.getResult();
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    double lat = doc.getDouble("Latitude");
                    double lng = doc.getDouble("Longitude");

                    // We have to filter out a few false positives due to GeoHash accuracy, but most will match
                    GeoLocation docLocation = new GeoLocation(lat, lng);
                    double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center);

                    if (distanceInM <= radiusInM) {
                        DataModal dataModal = doc.toObject(DataModal.class);
                        DataDistModal dataDistModal = new DataDistModal(distanceInM, dataModal);
                        postsDist.add(dataDistModal);
                    }
                }
            }
            //sort array of posts from nearest to farthest from searched location
            Collections.sort(postsDist, (s1, s2) -> {
                double dist1 = s1.getDistance();
                double dist2 = s2.getDistance();
               return Double.compare(dist1, dist2);
            });

            //populate posts array with just the DataModal class
            for (DataDistModal post:postsDist) {
                posts.add(post.getDataModal());
            }

            if(posts.isEmpty()){
                ErrorSearchFragment errorSearchFragment = new ErrorSearchFragment();
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_search, errorSearchFragment).commit();
            }else{
                //send posts array to the MapPostsFragment
                Bundle bundle = new Bundle();
                bundle.putSerializable("postsArray", posts);
                MapPostsFragment mapPostsFragment = new MapPostsFragment();
                mapPostsFragment.setArguments(bundle);
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_search, mapPostsFragment).commit();
            }
            dismissLoading();
        });
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