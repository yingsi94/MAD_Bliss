package com.example.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bliss.R;
import com.example.music.TrackMoodFragment;
import com.example.weeklysummary.WeeklySummaryFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvWelcome;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvWelcome = view.findViewById(R.id.tvWelcome);
        TextView tvDate = view.findViewById(R.id.tvDate);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        String currentDate = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(new Date());
        tvDate.setText(currentDate);

        fetchUserName();

        setupCard(view.findViewById(R.id.cardMood),
                "Track Mood", "How are you feeling?",
                R.drawable.ic_track_mood,
                new TrackMoodFragment());

        /*setupCard(view.findViewById(R.id.cardAchievement),
                "Achievement", "Unlock badges as you grow.",
                R.drawable.ic_achievement,
                new AchievementFragment());

        setupCard(view.findViewById(R.id.cardSupport),
                "Support", "Find help and comforting resources.",
                R.drawable.ic_support,
                new SupportFragment()); */

        setupCard(view.findViewById(R.id.cardSummary),
                "Summary", "View your progress.",
                R.drawable.ic_summary,
                new WeeklySummaryFragment());

    }

    private void fetchUserName() {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();

            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (isAdded() && documentSnapshot.exists()) {

                            String name = documentSnapshot.getString("name");
                            if (name != null && !name.isEmpty()) {
                                tvWelcome.setText("Welcome back, " + name + "!");
                            } else {
                                tvWelcome.setText("Welcome back!");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            tvWelcome.setText("Welcome back!");
                        }
                    });
        } else {
            tvWelcome.setText("Welcome back!");
        }
    }

    private void setupCard(View cardView, String title, String subtitle, int iconRes,Fragment targetFragment) {
        if (cardView == null) return;

        TextView tvTitle = cardView.findViewById(R.id.itemTitle);
        TextView tvSub = cardView.findViewById(R.id.itemSubtitle);
        ImageView imgIcon = cardView.findViewById(R.id.itemIcon);

        tvTitle.setText(title);
        tvSub.setText(subtitle);
        imgIcon.setImageResource(iconRes);

        cardView.setOnClickListener(v -> {
            if (targetFragment != null) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, targetFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(getContext(), title + " coming soon", Toast.LENGTH_SHORT).show();
            }
        });
    }


}