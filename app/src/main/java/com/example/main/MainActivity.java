package com.example.main;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.bliss.R;
import com.example.bliss.JournalListFragment;
import com.example.login_signup_profile.ProfileFragment;
import com.example.relaxation.MeditationPlayerFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private FloatingActionButton fabChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNavigation);
        fabChat = findViewById(R.id.fabChat);

        // 1. 设置默认进入的 Fragment
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
        }

        // 2. 底部栏点击监听
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                replaceFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_journal) {
                replaceFragment(new JournalListFragment());
                return true;
            } else if (itemId == R.id.nav_relax) {
                replaceFragment(new MeditationPlayerFragment());
                return true;
            } else if (itemId == R.id.nav_profile) {
                replaceFragment(new ProfileFragment());
                return true;
            }
            return false;
        });

        // 3. FAB 聊天按钮（它不在 BottomNav 内部，是独立点击）
        fabChat.setOnClickListener(v -> {
            Toast.makeText(this, "Opening AI Assistant...", Toast.LENGTH_SHORT).show();
            // 可以在这里跳转到 ChatActivity
        });
    }

    // 封装一个简单的切换方法
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}