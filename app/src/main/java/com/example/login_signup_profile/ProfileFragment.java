package com.example.login_signup_profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bliss.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.squareup.picasso.Picasso;

import com.google.firebase.firestore.ListenerRegistration;

public class ProfileFragment extends Fragment {

    private LinearLayout llChangePassword;
    private LinearLayout llLogout;
    private Button btnEditProfile;

    private TextView fullName, email, phone;
    private ImageView profilePicture;

    private FirebaseAuth auth;
    private FirebaseFirestore store;
    private String userId;
    private ListenerRegistration userListener;

    // Fragment 必须有一个空的构造函数
    public ProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载布局文件
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化视图 (注意使用 view.findViewById)
        fullName = view.findViewById(R.id.tvUserName);
        email = view.findViewById(R.id.tvUserEmail);
        phone = view.findViewById(R.id.tvUserPhone);
        profilePicture = view.findViewById(R.id.ivProfilePicture);

        llChangePassword = view.findViewById(R.id.llChangePassword);
        llLogout = view.findViewById(R.id.llLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        // 初始化 Firebase
        auth = FirebaseAuth.getInstance();
        store = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
            loadUserData();
        }

        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // User is logged out, redirect to Login
            Toast.makeText(requireContext(), "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }
        
        // Ensure userId is set
        if (userId == null) {
            userId = currentUser.getUid();
        }
        
        // Load user data - email change detection is now handled globally in MainActivity
        loadUserData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }

    private void loadUserData() {
        if (userId == null) return;
        DocumentReference documentReference = store.collection("users").document(userId);
        
        // Remove previous listener if exists
        if (userListener != null) {
            userListener.remove();
        }

        // Use manual listener management instead of Activity-scoped to avoid FragmentManager transaction issues
        userListener = documentReference.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    return;
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    if (fullName != null) fullName.setText(documentSnapshot.getString("fullName"));
                    if (email != null) email.setText(documentSnapshot.getString("email"));
                    if (phone != null) phone.setText(documentSnapshot.getString("phone"));

                    String imageUrl = documentSnapshot.getString("profileImageUrl");
                    if (imageUrl != null && !imageUrl.isEmpty() && profilePicture != null) {
                        Picasso.get().load(imageUrl)
                                .placeholder(R.mipmap.ic_launcher)
                                .into(profilePicture);
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        // 1. 跳转到修改密码 (Fragment 切换)
        llChangePassword.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, new ChangePasswordFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // 2. 退出登录 (Activity 跳转 - 保持现状)
        llLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // 3. 跳转到编辑资料 (Fragment 切换并传递数据)
        btnEditProfile.setOnClickListener(v -> {
            EditProfileFragment editFragment = new EditProfileFragment();

            // 封装数据到 Bundle
            Bundle args = new Bundle();
            args.putString("fullName", fullName.getText().toString());
            args.putString("email", email.getText().toString());
            args.putString("phone", phone.getText().toString());
            editFragment.setArguments(args);

            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, editFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }
}