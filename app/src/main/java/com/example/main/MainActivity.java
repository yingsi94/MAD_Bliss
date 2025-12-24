package com.example.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.bliss.R;
import com.example.bliss.JournalListFragment;
import com.example.login_signup_profile.ProfileFragment;
import com.example.relaxation.RelaxationFragment;
import com.example.chatbox.AIChatbotFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private FloatingActionButton fabChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 确保布局包含 fragment_container 和 bottomNavigation

        bottomNav = findViewById(R.id.bottomNavigation);
        fabChat = findViewById(R.id.fabChat);

        // 1. 设置默认进入的 Fragment (HomeFragment)
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment(),false);
            // 确保底部菜单的 Home 图标被手动选中
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        // 2. 底部导航点击逻辑
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                replaceFragment(new HomeFragment(),false);
                return true;
            } else if (itemId == R.id.nav_journal) {
                replaceFragment(new JournalListFragment(),false);
                return true;
            } else if (itemId == R.id.nav_relax) {
                replaceFragment(new RelaxationFragment(),false);
                return true;
            } else if (itemId == R.id.nav_profile) {
                replaceFragment(new ProfileFragment(),false);
                return true;
            }
            return false;
        });

        // 3. FAB 聊天按钮逻辑
        fabChat.setOnClickListener(v -> {
            replaceFragment(new AIChatbotFragment(), true);
            // Deselect bottom nav items since we are in a special fragment
            bottomNav.getMenu().setGroupCheckable(0, false, true);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Skip email change detection if we just logged in
        if (getIntent().getBooleanExtra("from_login", false)) {
            getIntent().removeExtra("from_login"); // Clear the flag
            return;
        }
        
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        
        if (currentUser == null) {
            // User is logged out, redirect to Login
            Intent intent = new Intent(this, com.example.login_signup_profile.LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }
        
        // Check if email was verified and updated in Auth
        currentUser.reload().addOnSuccessListener(aVoid -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                String authEmail = user.getEmail();
                
                // Get current email from Firestore to compare
                FirebaseFirestore store = FirebaseFirestore.getInstance();
                String userId = user.getUid();
                store.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firestoreEmail = documentSnapshot.getString("email");
                        
                        // If Auth email is different from Firestore, it means verification happened
                        if (authEmail != null && firestoreEmail != null && !authEmail.equals(firestoreEmail)) {
                             // Sync to Firestore and Logout
                             store.collection("users").document(userId).update("email", authEmail)
                                 .addOnSuccessListener(unused -> {
                                     auth.signOut();
                                     Toast.makeText(this, "Email verified! Please log in again.", Toast.LENGTH_LONG).show();
                                     Intent intent = new Intent(this, com.example.login_signup_profile.LoginActivity.class);
                                     intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                     startActivity(intent);
                                 });
                        }
                    }
                });
            }
        }).addOnFailureListener(e -> {
            // If reload fails (e.g. session invalid), redirect to login
            if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                Toast.makeText(this, "Security Update: Please log in with your new email.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, com.example.login_signup_profile.LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    // 封装跳转方法
    private void replaceFragment(Fragment fragment, boolean addToStack) {
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment);

        if (addToStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }
}