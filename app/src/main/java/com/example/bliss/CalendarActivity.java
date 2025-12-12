package com.example.bliss;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bliss.model.JournalEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarActivity extends AppCompatActivity {

    private TextView tvMonthYear;
    private RecyclerView rvCalendar;
    private ImageButton btnPrevMonth, btnNextMonth, btnBack;
    private Calendar currentMonth;
    private CalendarAdapter adapter;
    private FirebaseFirestore db;
    private Map<String, List<JournalEntry>> journalMap = new HashMap<>(); // Key: "yyyy-MM-dd"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        tvMonthYear = findViewById(R.id.tvMonthYear);
        rvCalendar = findViewById(R.id.rvCalendar);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        btnBack = findViewById(R.id.btnBack);

        db = FirebaseFirestore.getInstance();
        currentMonth = Calendar.getInstance();
        currentMonth.set(Calendar.DAY_OF_MONTH, 1); // Start at 1st of month

        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        adapter = new CalendarAdapter();
        rvCalendar.setAdapter(adapter);

        btnPrevMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updateCalendar();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            updateCalendar();
        });

        btnBack.setOnClickListener(v -> finish());

        fetchJournals();
    }

    private void fetchJournals() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        if (userId == null) {
            return;
        }

        // Removed orderBy("date") to avoid requiring a composite index
        db.collection("journals")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    journalMap.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    
                    List<JournalEntry> allEntries = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        JournalEntry entry = doc.toObject(JournalEntry.class);
                        if (entry != null && entry.getDate() != null) {
                            entry.setId(doc.getId());
                            allEntries.add(entry);
                        }
                    }

                    // Sort by date ascending (oldest first)
                    Collections.sort(allEntries, (e1, e2) -> {
                        if (e1.getDate() == null || e2.getDate() == null) return 0;
                        return e1.getDate().compareTo(e2.getDate());
                    });

                    for (JournalEntry entry : allEntries) {
                        String dateKey = sdf.format(entry.getDate().toDate());
                        if (!journalMap.containsKey(dateKey)) {
                            journalMap.put(dateKey, new ArrayList<>());
                        }
                        journalMap.get(dateKey).add(entry);
                    }
                    updateCalendar();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load journals", Toast.LENGTH_SHORT).show();
                    Log.e("CalendarActivity", "Error fetching journals", e);
                });
    }

    private void updateCalendar() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentMonth.getTime()));

        List<Date> days = new ArrayList<>();
        Calendar calendar = (Calendar) currentMonth.clone();
        
        // Determine start of week offset
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // 1=Sunday
        int maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Add empty placeholders for days before 1st of month
        for (int i = 1; i < dayOfWeek; i++) {
            days.add(null);
        }

        // Add actual days
        for (int i = 1; i <= maxDays; i++) {
            days.add(calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        adapter.setDays(days);
    }

    private void showMultipleEntriesDialog(List<JournalEntry> entries) {
        String[] items = new String[entries.size()];
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        for (int i = 0; i < entries.size(); i++) {
            JournalEntry entry = entries.get(i);
            String title = entry.getTitle() != null && !entry.getTitle().isEmpty() ? entry.getTitle() : "Untitled";
            String time = entry.getDate() != null ? timeFormat.format(entry.getDate().toDate()) : "";
            items[i] = time + " - " + title;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Journal Entry")
                .setItems(items, (dialog, which) -> {
                    JournalEntry selectedEntry = entries.get(which);
                    Intent intent = new Intent(CalendarActivity.this, JournalDetailActivity.class);
                    intent.putExtra("journal_entry", selectedEntry);
                    startActivity(intent);
                })
                .show();
    }

    private class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {
        private List<Date> days = new ArrayList<>();
        private SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        private SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.getDefault());

        public void setDays(List<Date> days) {
            this.days = days;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
            return new DayViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
            Date date = days.get(position);
            if (date == null) {
                holder.tvDay.setText("");
                holder.ivMoodEmoji.setVisibility(View.INVISIBLE);
                holder.itemView.setOnClickListener(null);
            } else {
                holder.tvDay.setText(dayFormat.format(date));
                
                String key = keyFormat.format(date);
                List<JournalEntry> entries = journalMap.get(key);

                if (entries != null && !entries.isEmpty()) {
                    // Show mood of the latest entry
                    JournalEntry latestEntry = entries.get(entries.size() - 1);
                    holder.ivMoodEmoji.setVisibility(View.VISIBLE);
                    holder.ivMoodEmoji.setImageResource(getMoodIconResource(latestEntry.getMood()));
                    
                    holder.itemView.setOnClickListener(v -> {
                        if (entries.size() == 1) {
                            Intent intent = new Intent(CalendarActivity.this, JournalDetailActivity.class);
                            intent.putExtra("journal_entry", latestEntry);
                            startActivity(intent);
                        } else {
                            showMultipleEntriesDialog(entries);
                        }
                    });
                } else {
                    holder.ivMoodEmoji.setVisibility(View.INVISIBLE);
                    holder.itemView.setOnClickListener(null);
                }
            }
        }

        @Override
        public int getItemCount() {
            return days.size();
        }

        class DayViewHolder extends RecyclerView.ViewHolder {
            TextView tvDay;
            ImageView ivMoodEmoji;

            public DayViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDay = itemView.findViewById(R.id.tvDay);
                ivMoodEmoji = itemView.findViewById(R.id.ivMoodEmoji);
            }
        }

        private int getMoodIconResource(String mood) {
            if (mood == null) return R.drawable.neutral;
            String lowerMood = mood.toLowerCase();
            if (lowerMood.contains("happy")) {
                return R.drawable.happy;
            } else if (lowerMood.contains("sad")) {
                return R.drawable.sad;
            } else if (lowerMood.contains("angry")) {
                return R.drawable.angry;
            } else if (lowerMood.contains("anxious")) {
                return R.drawable.anxious;
            } else {
                return R.drawable.neutral;
            }
        }
    }
}
