package com.example.droppoint.mapPostResources;

public class DataModal {
    // variables for storing our image and name.
    private String PostID;
    private String PostImageURL;

    public DataModal() {
        // empty constructor required for firebase.
    }

    // constructor for our object class.
    public DataModal(String PostID, String PostImageURL) {
        this.PostID = PostID;
        this.PostImageURL = PostImageURL;
    }

    // getter and setter methods
    public String getPostID() {
        return PostID;
    }

    public String getPostImageURL() {
        return PostImageURL;
    }
}
