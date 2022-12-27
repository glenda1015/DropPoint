package com.example.droppoint.mapPostResources;

public class DataDistModal {
    // variables for storing our image and name.
    private double distance;
    private DataModal dataModal;

    // constructor for our object class.
    public DataDistModal(double distance, DataModal dataModal) {
        this.distance = distance;
        this.dataModal = dataModal;
    }

    public DataModal getDataModal() {
        return dataModal;
    }

    public void setDataModal(DataModal dataModal) {
        this.dataModal = dataModal;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

}
