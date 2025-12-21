package com.example.bliss;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddEditGoalActivity extends AppCompatActivity {

    private TextInputEditText etGoalTitle, etCustomCategory, etCustomEmoji;
    private Spinner spinnerCategory;
    private RadioGroup rgGoalType;
    private RadioButton rbDaily, rbWeekly;
    private TextView tvTargetCount, tvToolbarTitle, tvGoalInfo;
    private ImageButton btnIncrement, btnDecrement;
    private Button btnSaveGoal;
    private ImageView btnBack;
    private ProgressBar loadingProgress;
    private LinearLayout layoutCustomCategory;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String userId;
    private int targetCount = 1;
    private String mode = "add";
    private String editingGoalId = null;

    private List<String> categoryList;
    private Map<String, String> categoryEmojiMap;
    private boolean isCustomCategorySelected = false;
    private Goal goalToEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_goal);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Get current user ID
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadCategories();
        setupListeners();
        checkEditMode();
    }

    private void initializeViews() {
        etGoalTitle = findViewById(R.id.etGoalTitle);
        etCustomCategory = findViewById(R.id.etCustomCategory);
        etCustomEmoji = findViewById(R.id.etCustomEmoji);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        rgGoalType = findViewById(R.id.rgGoalType);
        rbDaily = findViewById(R.id.rbDaily);
        rbWeekly = findViewById(R.id.rbWeekly);
        tvTargetCount = findViewById(R.id.tvTargetCount);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvGoalInfo = findViewById(R.id.tvGoalInfo);
        btnIncrement = findViewById(R.id.btnIncrement);
        btnDecrement = findViewById(R.id.btnDecrement);
        btnSaveGoal = findViewById(R.id.btnSaveGoal);
        btnBack = findViewById(R.id.btnBack);
        loadingProgress = findViewById(R.id.loadingProgress);
        layoutCustomCategory = findViewById(R.id.layoutCustomCategory);

        categoryEmojiMap = new HashMap<>();
        categoryList = new ArrayList<>();
    }

    private Map<String, String> getDefaultCategoryEmojis() {
        Map<String, String> map = new HashMap<>();
        map.put("Mindfulness", "🧘");
        map.put("Sleep", "😴");
        map.put("Journaling", "📔");
        map.put("Fitness", "💪");
        map.put("Gratitude", "🙏");
        return map;
    }

    private void loadCategories() {
        loadingProgress.setVisibility(View.VISIBLE);

        // Load categories from user's personal category collection
        firestore.collection("userCategories")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    categoryList.clear();
                    categoryEmojiMap.clear();

                    // Start with default categories
                    Map<String, String> defaultCategories = getDefaultCategoryEmojis();
                    categoryList.addAll(defaultCategories.keySet());
                    categoryEmojiMap.putAll(defaultCategories);

                    // If user has custom categories, add them
                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        Map<String, Object> customCategories = documentSnapshot.getData();
                        for (Map.Entry<String, Object> entry : customCategories.entrySet()) {
                            String categoryName = entry.getKey();
                            String emoji = entry.getValue().toString();

                            // Don't override default categories
                            if (!categoryList.contains(categoryName)) {
                                categoryList.add(categoryName);
                                categoryEmojiMap.put(categoryName, emoji);
                            }
                        }
                    }

                    // Add option to create custom category at the end
                    categoryList.add("+ Add New Category");

                    setupCategorySpinner();
                    if ("edit".equals(mode)) {
                        loadGoalData();
                    } else {
                        loadingProgress.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    // If loading fails, just use defaults
                    categoryList.clear();
                    categoryEmojiMap.clear();

                    Map<String, String> defaultCategories = getDefaultCategoryEmojis();
                    categoryList.addAll(defaultCategories.keySet());
                    categoryEmojiMap.putAll(defaultCategories);
                    categoryList.add("+ Add New Category");

                    setupCategorySpinner();
                    loadingProgress.setVisibility(View.GONE);
                });
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categoryList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = categoryList.get(position);
                if (selected.equals("+ Add New Category")) {
                    layoutCustomCategory.setVisibility(View.VISIBLE);
                    isCustomCategorySelected = true;
                } else {
                    layoutCustomCategory.setVisibility(View.GONE);
                    isCustomCategorySelected = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                isCustomCategorySelected = false;
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnIncrement.setOnClickListener(v -> {
            if (targetCount < 99) {
                targetCount++;
                updateTargetCount();
            }
        });

        btnDecrement.setOnClickListener(v -> {
            if (targetCount > 1) {
                targetCount--;
                updateTargetCount();
            }
        });

        rgGoalType.setOnCheckedChangeListener((group, checkedId) -> updateInfoText());

        btnSaveGoal.setOnClickListener(v -> saveGoal());
    }

    private void checkEditMode() {
        if (getIntent().hasExtra("goal_id")) {
            mode = "edit";
            editingGoalId = getIntent().getStringExtra("goal_id");
            tvToolbarTitle.setText("Edit Goal");
            btnSaveGoal.setText("Update Goal");
        } else {
            mode = "add";
            tvToolbarTitle.setText("Add New Goal");
            btnSaveGoal.setText("Save Goal");
        }
    }

    private void loadGoalData() {
        if (editingGoalId == null) return;
        loadingProgress.setVisibility(View.VISIBLE);

        firestore.collection("goals")
                .document(editingGoalId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    loadingProgress.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        goalToEdit = documentSnapshot.toObject(Goal.class);
                        if (goalToEdit != null) {
                            // Verify that this goal belongs to the current user
                            if (!userId.equals(goalToEdit.getUserId())) {
                                Toast.makeText(this, "Unauthorized access", Toast.LENGTH_SHORT).show();
                                finish();
                                return;
                            }

                            etGoalTitle.setText(goalToEdit.getTitle());

                            String category = goalToEdit.getCategory();
                            int categoryIndex = categoryList.indexOf(category);
                            if (categoryIndex != -1 && categoryIndex < categoryList.size() - 1) {
                                // Category exists in the list (and it's not the "+ Add New Category" option)
                                spinnerCategory.setSelection(categoryIndex);
                            } else {
                                // Category doesn't exist - it might be a custom one that was added
                                // Show it in custom fields
                                spinnerCategory.setSelection(categoryList.indexOf("+ Add New Category"));
                                etCustomCategory.setText(category);
                                etCustomEmoji.setText(goalToEdit.getCategoryEmoji());
                            }

                            if ("Daily".equals(goalToEdit.getGoalType())) {
                                rbDaily.setChecked(true);
                            } else {
                                rbWeekly.setChecked(true);
                            }

                            targetCount = goalToEdit.getTargetCount();
                            updateTargetCount();
                        }
                    } else {
                        Toast.makeText(this, "Goal not found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    loadingProgress.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load goal data.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void updateTargetCount() {
        tvTargetCount.setText(String.valueOf(targetCount));
    }

    private void updateInfoText() {
        if (rbDaily.isChecked()) {
            tvGoalInfo.setText("Daily goals reset every day at 12 AM.");
        } else {
            tvGoalInfo.setText("Weekly goals reset every seven days at 12 AM.");
        }
    }

    private void saveGoal() {
        String title = etGoalTitle.getText().toString().trim();
        if (title.isEmpty()) {
            etGoalTitle.setError("Title is required");
            etGoalTitle.requestFocus();
            return;
        }

        String category;
        String emoji;

        if (isCustomCategorySelected) {
            category = etCustomCategory.getText().toString().trim();
            emoji = etCustomEmoji.getText().toString().trim();

            if (category.isEmpty()) {
                etCustomCategory.setError("Category name is required");
                etCustomCategory.requestFocus();
                return;
            }
            if (emoji.isEmpty()) {
                etCustomEmoji.setError("Emoji is required");
                etCustomEmoji.requestFocus();
                return;
            }

            // Save the new custom category to user's categories
            saveCustomCategory(category, emoji);
        } else {
            category = spinnerCategory.getSelectedItem().toString();
            emoji = categoryEmojiMap.get(category);
            if (emoji == null || emoji.isEmpty()) {
                emoji = "📌"; // Fallback
            }
        }

        String goalType = rbDaily.isChecked() ? "Daily" : "Weekly";

        loadingProgress.setVisibility(View.VISIBLE);
        btnSaveGoal.setEnabled(false);

        if ("edit".equals(mode)) {
            updateExistingGoal(title, category, emoji, goalType);
        } else {
            createNewGoal(title, category, emoji, goalType);
        }
    }

    private void saveCustomCategory(String category, String emoji) {
        // Save custom category to user's category collection
        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put(category, emoji);

        firestore.collection("userCategories")
                .document(userId)
                .set(categoryData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Category saved successfully
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save category", Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewGoal(String title, String category, String emoji, String goalType) {
        String newGoalId = firestore.collection("goals").document().getId();

        Goal goal = new Goal(
                newGoalId,
                userId, // Set the userId
                title,
                category,
                emoji,
                goalType,
                targetCount,
                0,
                Timestamp.now(),
                false,
                Timestamp.now()
        );

        firestore.collection("goals").document(newGoalId)
                .set(goal)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Goal created!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    loadingProgress.setVisibility(View.GONE);
                    btnSaveGoal.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateExistingGoal(String title, String category, String emoji, String goalType) {
        firestore.collection("goals").document(editingGoalId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Goal existingGoal = documentSnapshot.toObject(Goal.class);
                        if (existingGoal != null) {
                            // Verify ownership
                            if (!userId.equals(existingGoal.getUserId())) {
                                Toast.makeText(this, "Unauthorized access", Toast.LENGTH_SHORT).show();
                                finish();
                                return;
                            }

                            existingGoal.setTitle(title);
                            existingGoal.setCategory(category);
                            existingGoal.setCategoryEmoji(emoji);
                            existingGoal.setGoalType(goalType);
                            existingGoal.setTargetCount(targetCount);
                            existingGoal.setCompleted(existingGoal.getCurrentCount() >= targetCount);

                            firestore.collection("goals").document(editingGoalId)
                                    .set(existingGoal)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Goal updated!", Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        loadingProgress.setVisibility(View.GONE);
                                        btnSaveGoal.setEnabled(true);
                                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    loadingProgress.setVisibility(View.GONE);
                    btnSaveGoal.setEnabled(true);
                    Toast.makeText(this, "Error fetching goal for update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}