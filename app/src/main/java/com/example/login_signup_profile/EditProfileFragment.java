package com.example.login_signup_profile;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.bliss.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class EditProfileFragment extends Fragment {

    TextInputEditText fullName, email, phone;
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

    public EditProfileFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载布局
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //--Initialize Views--
        btnChange = view.findViewById(R.id.btnChangePhoto);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);
        profileImage = view.findViewById(R.id.ivEditProfilePicture);
        phone = view.findViewById(R.id.etMobile);
        fullName = view.findViewById(R.id.etFullName);
        email = view.findViewById(R.id.etEmailEdit);

        //--Firebase & Cloudinary Init--
        auth = FirebaseAuth.getInstance();
        store = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }
        storageReference = FirebaseStorage.getInstance().getReference();
        initConfig();

        //--Load user data from Arguments--
        if (getArguments() != null) {
            String fullNameData = getArguments().getString("fullName");
            String emailData = getArguments().getString("email");
            String phoneData = getArguments().getString("phone");
            fullName.setText(fullNameData);
            email.setText(emailData);
            phone.setText(phoneData);
            Log.d("TAG", "onViewCreated:" + fullNameData + " " + emailData + " " + phoneData);
        }

        //--Load existing profile image--
        loadProfileImageFromFirestore();

        btnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getFragmentManager() != null) {
                    getFragmentManager().popBackStack();
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (fullName.getText().toString().isEmpty() || email.getText().toString().isEmpty() || phone.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                String originalEmail = user.getEmail();
                String newEmail = email.getText().toString();
                String newFullName = fullName.getText().toString();
                String newPhone = phone.getText().toString();

                if (!originalEmail.equals(newEmail)) {
                    reauthenticateAndupdateEmail(newEmail, newFullName, newPhone);
                } else {
                    updateFirestoreDataOnly(newFullName, newPhone);
                }
            }
        });
    }

    private void initConfig() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "ddzraiu5y");
            config.put("api_key", "847458557797165");
            config.put("api_secret", "VoH55kkWxVHl7D4FjqPhP6AQHVo");
            MediaManager.init(requireContext(), config);
        } catch (Exception e) {
            Log.d(TAG, "MediaManager already initialized");
        }
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imagePath = data.getData();
            Picasso.get().load(imagePath).into(profileImage);

            MediaManager.get().upload(imagePath).callback(new UploadCallback() {
                @Override
                public void onStart(String requestId) { Log.d(TAG, "onStart: started"); }

                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) { }

                @Override
                public void onSuccess(String requestId, Map resultData) {
                    String imageUrl = resultData.get("secure_url").toString();
                    DocumentReference docRef = store.collection("users").document(userId);
                    Map<String, Object> map = new HashMap<>();
                    map.put("profileImageUrl", imageUrl);
                    docRef.update(map).addOnSuccessListener(aVoid -> Log.d(TAG, "Profile image URL saved"));
                }

                @Override
                public void onError(String requestId, ErrorInfo error) { Log.e(TAG, "Error: " + error.getDescription()); }

                @Override
                public void onReschedule(String requestId, ErrorInfo error) { }
            }).dispatch();
        }
    }

    private void loadProfileImageFromFirestore() {
        DocumentReference docRef = store.collection("users").document(userId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && isAdded()) {
                String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                if (profileImageUrl != null) {
                    Picasso.get().load(profileImageUrl).into(profileImage);
                }
            }
        });
    }

    private void reauthenticateAndupdateEmail(String newEmail, String newFullName, String newPhone) {
        final TextInputEditText passwordEditText = new TextInputEditText(requireContext());
        passwordEditText.setHint("Enter your current password");
        passwordEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(requireContext())
                .setTitle("Re-authentication Required")
                .setMessage("Please enter your password to change your email.")
                .setView(passwordEditText)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String password = passwordEditText.getText().toString().trim();
                        if (password.isEmpty()) {
                            Toast.makeText(getContext(), "Password is required.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // Check if user has a password provider (Email/Password)
                        boolean isEmailProvider = false;
                        for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
                            if (EmailAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                                isEmailProvider = true;
                                break;
                            }
                        }

                        if (isEmailProvider) {
                            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
                            user.reauthenticate(credential).addOnSuccessListener(aVoid -> {
                                // Re-authentication successful
                                user.verifyBeforeUpdateEmail(newEmail).addOnSuccessListener(unused -> {
                                    Toast.makeText(getContext(), "Verification email sent to " + newEmail + ". Please verify to complete the update.", Toast.LENGTH_LONG).show();
                                    
                                    // Do NOT update email in Firestore yet. Wait for verification.
                                    // Only update name and phone for now.
                                    updateFirestoreDataOnly(newFullName, newPhone);
                                }).addOnFailureListener(e -> {
                                    Log.w(TAG, "verifyBeforeUpdateEmail failed", e);
                                    // Fallback: If verification flow fails, try direct update
                                    updateEmailInAuthAndFirestore(newEmail, newFullName, newPhone);
                                });
                            }).addOnFailureListener(e -> {
                                Log.e(TAG, "Re-authentication failed", e);
                                Toast.makeText(getContext(), "Authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            // For Google/Facebook sign-in users, re-auth flow is different or email update might not be supported directly
                            // Try verifyBeforeUpdateEmail if available or just update Firestore if Auth update fails
                             user.verifyBeforeUpdateEmail(newEmail).addOnSuccessListener(aVoid -> {
                                 Toast.makeText(getContext(), "Verification email sent to " + newEmail + ". Please verify to update.", Toast.LENGTH_LONG).show();
                                 // Optionally update Firestore pending verification or wait
                                 updateFirestoreDataOnly(newFullName, newPhone);
                             }).addOnFailureListener(e -> {
                                 Log.e(TAG, "verifyBeforeUpdateEmail failed", e);
                                 // Fallback: Just update Firestore if Auth update is strictly not supported/allowed
                                 Toast.makeText(getContext(), "Email update not supported for this account type. Updating profile info only.", Toast.LENGTH_LONG).show();
                                 updateFirestoreDataOnly(newFullName, newPhone);
                             });
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateEmailInAuthAndFirestore(String newEmail, String newFullName, String newPhone) {
        user.updateEmail(newEmail).addOnSuccessListener(unused -> {
            DocumentReference documentRef = store.collection("users").document(userId);
            Map<String, Object> edited = new HashMap<>();
            edited.put("email", newEmail);
            edited.put("fullName", newFullName);
            edited.put("phone", newPhone);

            documentRef.update(edited).addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                if (getFragmentManager() != null) getFragmentManager().popBackStack();
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Firestore update failed", e);
                Toast.makeText(getContext(), "Failed to update profile data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Email update failed", e);
            Toast.makeText(getContext(), "Failed to update email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateFirestoreDataOnly(String newFullName, String newPhone) {
        DocumentReference documentRef = store.collection("users").document(userId);
        Map<String, Object> edited = new HashMap<>();
        edited.put("fullName", newFullName);
        edited.put("phone", newPhone);

        documentRef.update(edited).addOnSuccessListener(unused -> {
            Toast.makeText(getContext(), "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
            if (getFragmentManager() != null) getFragmentManager().popBackStack();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Firestore update failed", e);
            Toast.makeText(getContext(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}