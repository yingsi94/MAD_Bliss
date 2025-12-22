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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bliss.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.squareup.picasso.Picasso;

public class ProfileFragment extends Fragment {

    private LinearLayout llChangePassword;
    private LinearLayout llLogout;
    private Button btnEditProfile;

    private TextView fullName, email, phone;
    private ImageView profilePicture;

    private FirebaseAuth auth;
    private FirebaseFirestore store;
    private String userId;

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

    private void loadUserData() {
        DocumentReference documentReference = store.collection("users").document(userId);
        // 使用 getViewLifecycleOwner() 替代 this，确保 Fragment 销毁时自动取消监听
        documentReference.addSnapshotListener(requireActivity(), new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    fullName.setText(documentSnapshot.getString("fullName"));
                    email.setText(documentSnapshot.getString("email"));
                    phone.setText(documentSnapshot.getString("phone"));

                    String imageUrl = documentSnapshot.getString("profileImageUrl");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Picasso.get().load(imageUrl)
                                .placeholder(R.mipmap.ic_launcher)
                                .into(profilePicture);
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        llChangePassword.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), ChangePasswordFragment.class));
        });

        llLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        btnEditProfile.setOnClickListener(v -> {
            Intent i = new Intent(getContext(), EditProfileFragment.class);
            i.putExtra("fullName", fullName.getText().toString());
            i.putExtra("email", email.getText().toString());
            i.putExtra("phone", phone.getText().toString());
            startActivity(i);
        });
    }
}