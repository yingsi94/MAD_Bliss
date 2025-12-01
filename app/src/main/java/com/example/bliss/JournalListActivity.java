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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class JournalListActivity extends AppCompatActivity {

    private RecyclerView rvJournalList;
    private JournalAdapter adapter;
    private List<JournalEntry> journalList;
    private FirebaseFirestore db;
    private FloatingActionButton fabAddJournal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_list);

        rvJournalList = findViewById(R.id.rvJournalList);
        fabAddJournal = findViewById(R.id.fabAddJournal);
        
        journalList = new ArrayList<>();
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

        listenForUpdates();
    }

    private void listenForUpdates() {
        db.collection("journals")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("JournalListActivity", "Listen failed.", error);
                            return;
                        }
                        
                        Log.d("JournalListActivity", "Connected to Firestore! Document count: " + (value != null ? value.size() : 0));

                        journalList.clear();
                        for (DocumentSnapshot doc : value) {
                            JournalEntry entry = doc.toObject(JournalEntry.class);
                            if (entry != null) {
                                entry.setId(doc.getId());
                                journalList.add(entry);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
