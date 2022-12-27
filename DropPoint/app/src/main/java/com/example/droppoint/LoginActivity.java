package com.example.droppoint;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail, editPassword;
    private MaterialButton loginBtn;
    private TextView signUp;

    boolean isAllFieldsChecked = false;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        editEmail = findViewById(R.id.emailText);
        editPassword = findViewById(R.id.passwordText);
        loginBtn = findViewById(R.id.logInBtn);
        signUp = findViewById(R.id.signUpText);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Check if user is signed in (non-null) and update UI accordingly.
        if(mAuth.getCurrentUser()!=null){
            startActivity(new Intent(getApplication(), NavigationActivity.class));
        }

        loginBtn.setOnClickListener(v -> userLogin());

        signUp.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(i);
        });
    }

    private void userLogin() {
        String email = editEmail.getText().toString();
        String password = editPassword.getText().toString();
        isAllFieldsChecked = CheckAllFields();

        if (isAllFieldsChecked) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            startActivity(new Intent(getApplicationContext(), NavigationActivity.class));
                        } else {
                            Toast.makeText(LoginActivity.this, "Error! " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    //check that none of the fields are empty (validation)
    private boolean CheckAllFields() {
        if (editEmail.length() == 0) {
            editEmail.setError("Please enter email");
            return false;
        }

        if (editPassword.length() == 0) {
            editPassword.setError("Please enter password");
            return false;
        }

        // after all validation is done for each field return true.
        return true;
    }
}