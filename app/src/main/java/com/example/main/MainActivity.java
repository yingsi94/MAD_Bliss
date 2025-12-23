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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
            Toast.makeText(this, "Opening AI Assistant...", Toast.LENGTH_SHORT).show();
            // 如果有 ChatActivity，可以使用 Intent 跳转
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