package com.example.bliss;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    EditText fullName, email, phone;
    FirebaseAuth auth;
    FirebaseFirestore store;
    String userId;
    Button btnChange;
    Button btnCancel;
    Button btnSave;
    FirebaseUser user;
    ImageView profileImage;
    StorageReference storageReference;

    private static int IMAGE_REQUEST_CODE = 1;

    public static final String TAG = "TAG";
    private Uri imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);


        //--Initialize Views--
        btnChange = findViewById(R.id.btnChangePhoto);
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);
        profileImage = findViewById(R.id.ivEditProfilePicture);
        phone = findViewById(R.id.etMobile);
        fullName = findViewById(R.id.etFullName);
        email = findViewById(R.id.etEmailEdit);

        //--Firebase & Cloudinary Init--
        auth = FirebaseAuth.getInstance();
        store = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();
        userId = user.getUid();
        storageReference = FirebaseStorage.getInstance().getReference();
        initCongif();

        //--Load user data from Intent--
        Intent data = getIntent();
        String fullNameData = data.getStringExtra("fullName");
        String emailData = data.getStringExtra("email");
        String phoneData = data.getStringExtra("phone");
        fullName.setText(fullNameData);
        email.setText(emailData);
        phone.setText(phoneData);

        //--Load existing profile image from Firestore--
        loadProfileImageFromFirestore();

        // Log the data for debugging
        Log.d("TAG","onCreate:"+fullNameData+" "+emailData+" "+phoneData);



        btnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open gallery
                /*Intent openGalleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(openGalleryIntent,1000);*/
                selectImage();
            }

        });


        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Add save logic here
                if(fullName.getText().toString().isEmpty() || email.getText().toString().isEmpty() || phone.getText().toString().isEmpty()){
                    Toast.makeText(EditProfileActivity.this,"Fill all fields",Toast.LENGTH_SHORT).show();
                    return;
                }

                String originalEmail = user.getEmail();
                String newEmail = email.getText().toString();
                String newFullName = fullName.getText().toString();
                String newPhone = phone.getText().toString();

                // Check if the email has actually changed.
                if (!originalEmail.equals(newEmail)) {
                    // If the email is different, we must re-authenticate before updating.
                    reauthenticateAndupdateEmail(newEmail,newFullName,newPhone);
                } else {
                    // If the email is the same, just update the other Firestore data.
                    updateFirestoreDataOnly(newFullName, newPhone);
                }
            }


        });

    }

    private void initCongif() {
        Map<String,String> config = new HashMap<>();

        config.put("cloud_name", "ddzraiu5y");
        config.put("api_key","847458557797165");
        config.put("api_secret","VoH55kkWxVHl7D4FjqPhP6AQHVo");
        //config.put("secure", true);
        MediaManager.init(this, config);
    }

    /*private void requestPermission() {
        if(ContextCompat.checkSelfPermission(EditProfileActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            selectImage();
        }else{
            ActivityCompat.requestPermissions(EditProfileActivity.this,new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            },IMAGE_REQUEST_CODE);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == IMAGE_REQUEST_CODE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                selectImage(); // Permission granted, open gallery
            } else {
                Toast.makeText(this, "Permission denied to access storage", Toast.LENGTH_SHORT).show();
            }
        }
    } */

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*"); //if wanted,can use pdf/gif/video
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,IMAGE_REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null){

                imagePath = data.getData();
                Picasso.get().load(imagePath).into(profileImage);

                //uploadImageToFirebase(imageUri);
                MediaManager.get().upload(imagePath).callback(new UploadCallback(){
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG,"onStart: "+"started");
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        Log.d(TAG,"onStart: "+"uploading");
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        Log.d(TAG,"onStart: "+"success");
                        //Save URL to Firestore
                        String imageUrl = resultData.get("secure_url").toString();
                        DocumentReference docRef = store.collection("users").document(user.getUid());
                        Map<String,Object> map = new HashMap<>();
                        map.put("profileImageUrl", imageUrl);
                        docRef.update(map)
                                .addOnSuccessListener(aVoid -> Log.d(TAG,"Profile image URL saved"))
                                .addOnFailureListener(e -> Log.e(TAG,"Failed to save image URL", e));

                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.d(TAG,"onStart: "+error);
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.d(TAG,"onStart: "+error);
                    }


                }).dispatch();

        }
    }

    private void loadProfileImageFromFirestore() {
        DocumentReference docRef = store.collection("users").document(user.getUid());
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if(documentSnapshot.exists()){
                String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                if(profileImageUrl != null){
                    Picasso.get().load(profileImageUrl).into(profileImage);
                }
            }
        }).addOnFailureListener(e ->{
            Log.e(TAG,"Error loading profile image",e);
        });
    }

    // Method to re-authenticate the user before a sensitive operation
    private void reauthenticateAndupdateEmail(String newEmail, String newFullName, String newPhone) {
        FirebaseUser user = auth.getCurrentUser();

        // Create a dialog to get the user's password
        final EditText passwordEditText = new EditText(this);
        passwordEditText.setHint("Enter your current password");
        passwordEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Re-authentication Required")
                .setMessage("Please enter your password to change your email.")
                .setView(passwordEditText)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String password = passwordEditText.getText().toString().trim();
                        if (password.isEmpty()) {
                            Toast.makeText(EditProfileActivity.this, "Password is required.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (user.getEmail() == null) {
                            Toast.makeText(EditProfileActivity.this, "User email not found.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Get the credential from the user's email and entered password
                        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

                        // Re-authenticate the user with the credential
                        user.reauthenticate(credential)
                                .addOnSuccessListener(aVoid -> {

                                        Log.d(TAG, "User re-authenticated successfully.");
                                        // If re-authentication is successful, proceed with updating the email
                                        updateEmailInAuthAndFirestore(newEmail,newFullName,newPhone);

                                })
                                .addOnFailureListener(e-> {

                                        Log.e(TAG, "Re-authentication failed.", e);
                                        Toast.makeText(EditProfileActivity.this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show();

                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // This method runs *after* the user has been successfully re-authenticated
    private void updateEmailInAuthAndFirestore(String newEmail,String newFullName, String newPhone) {

        if (newEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(this, "Enter a valid email.", Toast.LENGTH_SHORT).show();
            return;
        }

        user.updateEmail(newEmail).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                // Now update the data in Firestore
                DocumentReference documentRef = store.collection("users").document(user.getUid());
                Map<String, Object> edited = new HashMap<>();
                edited.put("email", newEmail);
                edited.put("fullName", newFullName);
                edited.put("phone", newPhone);

                documentRef.update(edited).addOnSuccessListener(aVoid -> {
                        Toast.makeText(EditProfileActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(EditProfileActivity.this, ProfileActivity.class));
                        finish(); // Close this activity

                }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating Firestore.", e);
                        Toast.makeText(EditProfileActivity.this, "Error updating profile data.", Toast.LENGTH_SHORT).show();
                });
            }
        }).addOnFailureListener(e -> {
                Log.e(TAG, "Error updating email in Firebase Auth.", e);
                Toast.makeText(EditProfileActivity.this, "Failed to update email. Please try again.", Toast.LENGTH_SHORT).show();

        });
    }

    // Use this method when only non-sensitive data (like name or phone) is changed.
    private void updateFirestoreDataOnly(String newFullName, String newPhone) {
        DocumentReference documentRef = store.collection("users").document(user.getUid());
        Map<String, Object> edited = new HashMap<>();
        edited.put("fullName", newFullName);
        edited.put("phone", newPhone);

        documentRef.update(edited).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(EditProfileActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(EditProfileActivity.this, ProfileActivity.class));
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Error updating Firestore.", e);
                Toast.makeText(EditProfileActivity.this, "Error updating profile data.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    /*private void uploadImageToFirebase(Uri imageUri){
        StorageReference fileRef = storageReference.child("profile.jpg");
        fileRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()  {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(EditProfileActivity.this,"Image Uploaded",Toast.LENGTH_SHORT).show();
            }

        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(EditProfileActivity.this,"Failed",Toast.LENGTH_SHORT).show();
            }
        });
    } */
}
