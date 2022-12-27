package com.example.droppoint.postDialogResources;

public class TripDataModal {
    // variables for storing our image and name.
    private String TripId;
    private String TripName;

    public TripDataModal() {
        // empty constructor required for firebase.
    }

    // constructor for our object class.
    public TripDataModal(String TripId, String TripName) {
        this.TripId = TripId;
        this.TripName = TripName;
    }

    // getter and setter methods
    public String getTripId() {
        return TripId;
    }

    public String getTripName() {
        return TripName;
    }
}
