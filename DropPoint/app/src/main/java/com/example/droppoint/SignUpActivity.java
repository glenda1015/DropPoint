package com.example.droppoint;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {
    private EditText editEmail, editPassword, editBio;
    private MaterialButton signUpBtn;
    private TextView login;
    private ImageView profileImage;
    private Uri imageURI;
    private static final int PICK_IMAGE=1;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageReference; //cloud storage for images

    private String userID;

    boolean isAllFieldsChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firebase database
        db = FirebaseFirestore.getInstance();

        // Create a storage reference from our app
        storageReference = FirebaseStorage.getInstance().getReference();

        // Check if user is signed in (non-null) and update UI accordingly.
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(getApplication(), NavigationActivity.class));
        }

        profileImage = findViewById(R.id.postImg);
        editEmail = findViewById(R.id.emailSignUpText);
        editPassword = findViewById(R.id.passwordSignUpText);
        editBio = findViewById(R.id.bioSignUpText);
        signUpBtn = findViewById(R.id.signUpBtn);
        login = findViewById(R.id.clickLoginText);

        signUpBtn.setOnClickListener(v -> createAccount());

        login.setOnClickListener(v -> {
            Intent i = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(i);
        });


        profileImage.setOnClickListener(view -> {
            Intent i = new Intent();
            i.setType("image/*");
            i.setAction(Intent.ACTION_GET_CONTENT);

            startActivityIfNeeded(Intent.createChooser(i,"Select Image"), PICK_IMAGE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            imageURI = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageURI);
                profileImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void createAccount() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String bio = editBio.getText().toString();

        isAllFieldsChecked = CheckAllFields();

        if (isAllFieldsChecked) {
            //store user data in database
            storeUserData(email, password, bio);
        }
    }

    private void storeUserData(String email, String password, String bio){
        //register the user in firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        //User created
                        Toast.makeText(SignUpActivity.this, "Account Created", Toast.LENGTH_SHORT).show();

                        //get user & store in db
                        userID = mAuth.getCurrentUser().getUid();
                        DocumentReference documentReference = db.collection("users").document(userID);
                        Map<String,Object> user = new HashMap<>();

                        //Specify the file path and name of profile image
                        String path = "profImages/" + UUID.randomUUID().toString() + ".jpg";
                        StorageReference profileRef = storageReference.child(path);

                        //Upload image to Firebase Storage, if success, store user data.
                        profileRef.putFile(imageURI).addOnCompleteListener(task1 -> {
                            //store to users collection is database
                            user.put("Email", email);
                            user.put("Bio", bio);
                            user.put("ProfileImgURL", path);
                            documentReference.set(user).addOnSuccessListener(unused ->
                                            Log.d(TAG, "onSuccess: user profile is created for " + userID))
                                    .addOnFailureListener(e -> Log.d(TAG, "onFailure: " + e));

                            //redirect to profile page
                            startActivity(new Intent(getApplication(), NavigationActivity.class));
                        }).addOnFailureListener(e -> Toast.makeText(SignUpActivity.this, "Error uploading image. Please try again", Toast.LENGTH_SHORT).show());
                    } else {
                        // If user creation fails, display a message to the user.
                        Toast.makeText(SignUpActivity.this, "Error! " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    //check that none of the fields are empty (validation)
    private boolean CheckAllFields() {
        if (imageURI == null) {
            Toast.makeText(SignUpActivity.this, "Please upload an image",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (editEmail.length() == 0) {
            editEmail.setError("Please enter an email");
            return false;
        }

        String email = editEmail.getText().toString();
        if(!isValidEmail(email)){
            editEmail.setError("Please enter a valid email");
            return false;
        }

        if (editPassword.length() == 0) {
            editPassword.setError("Please enter a password");
            return false;
        }

        //check password meets criteria
        String password = editPassword.getText().toString();
        if(!isValidPassword(password)){
            editPassword.setError("Please have at least 1 Upper Case, 1 Lower Case, 1 number, 6-20 characters, no spaces");
            return false;
        }

        if (editBio.length() == 0) {
            editBio.setError("We want to know about you!");
            return false;
        }

        // after all validation is done for each field return true.
        return true;
    }

    private boolean isValidEmail(String email){
        return (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches());
    }

    private boolean isValidPassword(String password){
        // Regex to check valid password.
        String regex = "^(?=.*[0-9])"
                + "(?=.*[a-z])(?=.*[A-Z])"
                + "(?=\\S+$).{6,20}$";

        // Compile the ReGex
        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(password);

        return m.matches();
    }
}