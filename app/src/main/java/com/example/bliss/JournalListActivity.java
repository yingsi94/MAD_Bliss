package com.example.bliss;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bliss.adapter.JournalAdapter;
import com.example.bliss.model.JournalEntry;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class JournalListActivity extends AppCompatActivity {

    private RecyclerView rvJournalList;
    private android.widget.EditText etSearch;
    private JournalAdapter adapter;
    private List<JournalEntry> journalList;
    private List<JournalEntry> allJournalList;
    private FirebaseFirestore db;
    private FloatingActionButton fabAddJournal;
    private android.widget.ImageButton btnCalendar;
    private ListenerRegistration firestoreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_list);

        rvJournalList = findViewById(R.id.rvJournalList);
        etSearch = findViewById(R.id.etSearch);
        fabAddJournal = findViewById(R.id.fabAddJournal);
        btnCalendar = findViewById(R.id.btnCalendar);
        
        journalList = new ArrayList<>();
        allJournalList = new ArrayList<>();
        
        adapter = new JournalAdapter(this, journalList, entry -> {
            Intent intent = new Intent(JournalListActivity.this, JournalDetailActivity.class);
            intent.putExtra("journal_entry", entry);
            startActivity(intent);
        });

        rvJournalList.setLayoutManager(new LinearLayoutManager(this));
        rvJournalList.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        fabAddJournal.setOnClickListener(v -> {
            Intent intent = new Intent(JournalListActivity.this, AddJournalActivity.class);
            startActivity(intent);
        });
        
        btnCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(JournalListActivity.this, CalendarActivity.class);
            startActivity(intent);
        });
        
        setupSearch();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (firestoreListener != null) {
            firestoreListener.remove();
            firestoreListener = null;
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterJournals(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void filterJournals(String query) {
        journalList.clear();
        if (query.isEmpty()) {
            journalList.addAll(allJournalList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (JournalEntry entry : allJournalList) {
                boolean matchesTitle = entry.getTitle() != null && entry.getTitle().toLowerCase().contains(lowerCaseQuery);
                boolean matchesContent = entry.getContent() != null && entry.getContent().toLowerCase().contains(lowerCaseQuery);
                boolean matchesMood = entry.getMood() != null && entry.getMood().toLowerCase().contains(lowerCaseQuery);
                
                if (matchesTitle || matchesContent || matchesMood) {
                    journalList.add(entry);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void startListening() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            return;
        }

        if (firestoreListener != null) {
            firestoreListener.remove();
        }

        firestoreListener = db.collection("journals")
                .whereEqualTo("userId", userId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("JournalListActivity", "Listen failed.", error);
                            return;
                        }
                        
                        Log.d("JournalListActivity", "Connected to Firestore! Document count: " + (value != null ? value.size() : 0));

                        allJournalList.clear();
                        for (DocumentSnapshot doc : value) {
                            JournalEntry entry = doc.toObject(JournalEntry.class);
                            if (entry != null) {
                                entry.setId(doc.getId());
                                allJournalList.add(entry);
                            }
                        }

                        // Sort by date descending (newest first)
                        Collections.sort(allJournalList, (e1, e2) -> {
                            if (e1.getDate() == null || e2.getDate() == null) return 0;
                            return e2.getDate().compareTo(e1.getDate()); // Descending
                        });
                        
                        // Re-apply filter if search text exists
                        String currentSearch = etSearch.getText().toString();
                        filterJournals(currentSearch);
                    }
                });
    }
}
