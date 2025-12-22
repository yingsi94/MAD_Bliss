package com.example.bliss;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.squareup.picasso.Picasso;

public class ProfileActivity extends AppCompatActivity {

    private LinearLayout llChangePassword;
    private LinearLayout llLogout;
    private Button btnEditProfile;

    TextView fullName,email,phone;
    ImageView profilePicture;

    FirebaseAuth auth;
    FirebaseFirestore store;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        fullName = findViewById(R.id.tvUserName);
        email = findViewById(R.id.tvUserEmail);
        phone = findViewById(R.id.tvUserPhone);

        profilePicture = findViewById(R.id.ivProfilePicture);



        auth = FirebaseAuth.getInstance();
        store = FirebaseFirestore.getInstance();

        userId = auth.getCurrentUser().getUid();

        DocumentReference documentReference = store.collection("users").document(userId);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {

                if (documentSnapshot != null && documentSnapshot.exists()) {

                    fullName.setText(documentSnapshot.getString("fullName"));
                    email.setText(documentSnapshot.getString("email"));
                    phone.setText(documentSnapshot.getString("phone"));

                    // Load Profile Image
                    String imageUrl = documentSnapshot.getString("profileImageUrl");

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Picasso.get().load(imageUrl)
                                .placeholder(R.mipmap.ic_launcher) // Optional — default image
                                .into(profilePicture);
                    }
                }
            }
        });


        llChangePassword = findViewById(R.id.llChangePassword);
        llLogout = findViewById(R.id.llLogout);
        btnEditProfile = findViewById(R.id.btnEditProfile);

        llChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ProfileActivity.this, ChangePasswordActivity.class));
            }
        });

        llLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                auth.signOut();
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent(ProfileActivity.this,EditProfileActivity.class);
                i.putExtra("fullName",fullName.getText().toString());
                i.putExtra("email",email.getText().toString());
                i.putExtra("phone",phone.getText().toString());
                startActivity(i);

            }
        });
    }
}
