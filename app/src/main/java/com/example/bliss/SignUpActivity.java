package com.example.bliss;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore store;
    String userId;
    private EditText signupEmail, signupPassword;
    private EditText signupFullName, signupPhone;
    private Button btnSignUp;
    private TextView tvBackToLogin;
    static final String TAG = "TAG";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        store = FirebaseFirestore.getInstance();

        signupEmail = findViewById(R.id.etEmailSignup);
        signupPassword = findViewById(R.id.etPasswordSignup);
        signupPhone = findViewById(R.id.etPhone);
        signupFullName = findViewById(R.id.etName);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Add registration logic
                String email = signupEmail.getText().toString().trim();
                String password = signupPassword.getText().toString().trim();
                String fullName = signupFullName.getText().toString();
                String phone = signupPhone.getText().toString();

                if(email.isEmpty()){
                    signupEmail.setError("Email required");
                }
                if(password.isEmpty()) {
                    signupPassword.setError("Password required");
                }else{
                    auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(SignUpActivity.this,"SignUp Successful", Toast.LENGTH_SHORT).show();
                                userId = auth.getCurrentUser().getUid();
                                DocumentReference documentReference = store.collection("users").document(userId);
                                //create data using hashmap
                                Map<String,Object> user = new HashMap<>();
                                user.put("fullName",fullName);
                                user.put("email",email);
                                user.put("phone",phone);
                                documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG,"onSuccess: user profile is created for "+userId);
                                    }
                                });
                                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                            }else{
                                Toast.makeText(SignUpActivity.this,"SignUp Failed"+task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

            }
        });

        tvBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            }
        });
    }
}
