package com.example.droppoint.mapViewResources;

import androidx.annotation.Nullable;

import com.example.droppoint.mapPostResources.DataModal;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;


public class MyCluster implements ClusterItem {

    private LatLng mPosition;
    private String postID;
    private Double lat;
    private Double lng;
    private DataModal dataModal;

    public MyCluster(Double lng, Double lat, String postID, DataModal dataModal){
        this.lat  =  lat;
        this.lng  =  lng;
        this.postID = postID;
        this.dataModal = dataModal;
        mPosition  =  new LatLng(this.lat,this.lng);

    }

    @Nullable
    @Override
    public String getSnippet() {
        return null;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    @Nullable
    @Override
    public String getTitle() {
        return null;
    }

    public String getPostID() {
        return this.postID;
    }

    public DataModal getDataModal() {
        return this.dataModal;
    }

}