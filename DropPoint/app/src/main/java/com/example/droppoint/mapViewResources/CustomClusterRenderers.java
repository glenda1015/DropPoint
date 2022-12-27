package com.example.droppoint.mapViewResources;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

public class CustomClusterRenderers extends DefaultClusterRenderer<MyCluster> {
    public CustomClusterRenderers(Context context, GoogleMap map, ClusterManager<MyCluster> clusterManager) {
        super(context, map, clusterManager);
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster<MyCluster> cluster) {
        //start clustering if at least 2 items overlap
        return cluster.getSize() > 1;
    }
}