package com.example.bliss;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bliss.adapter.JournalAdapter;
import com.example.bliss.model.JournalEntry;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JournalListFragment extends Fragment {

    private RecyclerView rvJournalList;
    private EditText etSearch;
    private JournalAdapter adapter;
    private List<JournalEntry> journalList;
    private List<JournalEntry> allJournalList;
    private FirebaseFirestore db;
    private FloatingActionButton fabAddJournal;
    private ImageButton btnCalendar;
    private ListenerRegistration firestoreListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_journal_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        rvJournalList = view.findViewById(R.id.rvJournalList);
        etSearch = view.findViewById(R.id.etSearch);
        fabAddJournal = view.findViewById(R.id.fabAddJournal);
        btnCalendar = view.findViewById(R.id.btnCalendar);

        journalList = new ArrayList<>();
        allJournalList = new ArrayList<>();

        db = FirebaseFirestore.getInstance();

        // Setup RecyclerView
        adapter = new JournalAdapter(requireContext(), journalList, entry -> {
            Intent intent = new Intent(requireContext(), JournalDetailActivity.class);
            intent.putExtra("journal_entry", entry);
            startActivity(intent);
        });
        rvJournalList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvJournalList.setAdapter(adapter);

        // Click listeners
        fabAddJournal.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddJournalActivity.class);
            startActivity(intent);
        });

        btnCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CalendarActivity.class);
            startActivity(intent);
        });

        setupSearch();
    }

    @Override
    public void onStart() {
        super.onStart();
        startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (firestoreListener != null) {
            firestoreListener.remove();
            firestoreListener = null;
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterJournals(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
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
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) return;

        if (firestoreListener != null) {
            firestoreListener.remove();
        }

        firestoreListener = db.collection("journals")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("JournalFragment", "Listen failed.", error);
                        return;
                    }

                    allJournalList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value) {
                            JournalEntry entry = doc.toObject(JournalEntry.class);
                            if (entry != null) {
                                entry.setId(doc.getId());
                                allJournalList.add(entry);
                            }
                        }
                    }

                    // Sort newest first
                    Collections.sort(allJournalList, (e1, e2) -> {
                        if (e1.getDate() == null || e2.getDate() == null) return 0;
                        return e2.getDate().compareTo(e1.getDate());
                    });

                    filterJournals(etSearch.getText().toString());
                });
    }
}