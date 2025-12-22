package com.example.relaxation;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.bliss.R;

import java.util.Locale;

public class BreathingFragment extends Fragment {

    private static final String TAG = "BreathingFragment";

    private Button btnMeditation, btnBreathing, btnGoals, btnStart;
    private ImageButton btnSettings;
    private TextView tvBreathingAction, tvBreathingTimer, tvCycleCounter;
    private ImageView ivBreathingIcon;
    private CardView breathingCircle;
    private View outerRing1, outerRing2;
    private LinearLayout settingsPanel;
    private NumberPicker npCycles;
    private SwitchCompat switchSound;

    private CountDownTimer breathingTimer;
    private boolean isBreathing = false;
    private int currentCycle = 0;
    private int maxCycles = 4;
    private boolean soundEnabled = true;

    // Breathing phases
    private static final int PHASE_INHALE = 0;
    private static final int PHASE_HOLD = 1;
    private static final int PHASE_EXHALE = 2;
    private int currentPhase = PHASE_INHALE;

    // Text-to-Speech
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;

    // Animation
    private ValueAnimator circleAnimator;
    private ObjectAnimator ring1Animator, ring2Animator;

    public BreathingFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载布局文件
        return inflater.inflate(R.layout.fragment_breathing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        initializeViews(view);

        // Setup Text-to-Speech
        setupTextToSpeech();

        // Setup listeners
        setupListeners(view);

        // Setup NumberPicker
        setupNumberPicker();
    }

    private void initializeViews(View view) {
        btnMeditation = view.findViewById(R.id.btnMeditation);
        btnBreathing = view.findViewById(R.id.btnBreathing);
        btnGoals = view.findViewById(R.id.btnGoals);
        btnStart = view.findViewById(R.id.btnStart);
        btnSettings = view.findViewById(R.id.btnSettings);

        tvBreathingAction = view.findViewById(R.id.tvBreathingAction);
        tvBreathingTimer = view.findViewById(R.id.tvBreathingTimer);
        tvCycleCounter = view.findViewById(R.id.tvCycleCounter);
        ivBreathingIcon = view.findViewById(R.id.ivBreathingIcon);

        breathingCircle = view.findViewById(R.id.breathingCircle);
        outerRing1 = view.findViewById(R.id.outerRing1);
        outerRing2 = view.findViewById(R.id.outerRing2);

        settingsPanel = view.findViewById(R.id.settingsPanel);
        npCycles = view.findViewById(R.id.npCycles);
        switchSound = view.findViewById(R.id.switchSound);

        updateCycleCounter();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 确保 Tab 状态正确
        selectTab(btnBreathing);
    }

    private void selectTab(Button selectedButton) {
        resetTab(btnMeditation);
        resetTab(btnBreathing);
        resetTab(btnGoals);

        selectedButton.setBackgroundResource(R.drawable.tab_selected_white);
        selectedButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
    }

    private void resetTab(Button button) {
        button.setBackgroundResource(android.R.color.transparent);
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_dark));
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported");
                } else {
                    ttsReady = true;
                    textToSpeech.setPitch(1.0f);
                    textToSpeech.setSpeechRate(0.9f);
                }
            }
        });
    }

    private void setupNumberPicker() {
        npCycles.setMinValue(1);
        npCycles.setMaxValue(10);
        npCycles.setValue(4);
        npCycles.setWrapSelectorWheel(false);

        npCycles.setOnValueChangedListener((picker, oldVal, newVal) -> {
            maxCycles = newVal;
            updateCycleCounter();
        });
    }

    private void setupListeners(View rootView) {
        // 跳转回 Meditation (现在应该是 RelaxationFragment 或其 Activity)
        btnMeditation.setOnClickListener(v -> {
            // 如果你的主界面也是用 Fragment 切换，请联系我更换此处逻辑
            Intent intent = new Intent(getActivity(), RelaxationFragment.class);
            startActivity(intent);
        });

        btnGoals.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GoalsFragment.class);
            startActivity(intent);
        });

        btnStart.setOnClickListener(v -> {
            if (!isBreathing) {
                startBreathingExercise();
            } else {
                stopBreathingExercise();
            }
        });

        btnSettings.setOnClickListener(v -> {
            if (settingsPanel.getVisibility() == View.VISIBLE) {
                settingsPanel.setVisibility(View.GONE);
            } else {
                settingsPanel.setVisibility(View.VISIBLE);
                final ScrollView scrollView = rootView.findViewById(R.id.scrollView);
                scrollView.post(() -> scrollView.smoothScrollTo(0, settingsPanel.getBottom()));
            }
        });

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> soundEnabled = isChecked);
    }

    // --- 呼吸逻辑核心 (保持不变) ---

    private void startBreathingExercise() {
        isBreathing = true;
        currentCycle = 0;
        currentPhase = PHASE_INHALE;
        btnStart.setText("Stop");
        settingsPanel.setVisibility(View.GONE);
        updateCycleCounter();
        startPhase();
    }

    private void stopBreathingExercise() {
        isBreathing = false;
        if (breathingTimer != null) breathingTimer.cancel();
        cancelAnimations();
        btnStart.setText("Start");
        tvBreathingAction.setText("Ready");
        tvBreathingTimer.setText("Press Start");
        resetCircleAndRings();
        updateCycleCounter();
    }

    private void startPhase() {
        if (!isBreathing || !isAdded()) return;

        switch (currentPhase) {
            case PHASE_INHALE:
                tvBreathingAction.setText("Inhale");
                ivBreathingIcon.setImageResource(R.drawable.ic_arrow_up);
                speak("Breathe in");
                startTimer(4, () -> {
                    currentPhase = PHASE_HOLD;
                    startPhase();
                });
                animateCircle(240, 280, 4000);
                animateRings(300, 340, 270, 310, 4000);
                break;

            case PHASE_HOLD:
                tvBreathingAction.setText("Hold");
                ivBreathingIcon.setImageResource(R.drawable.ic_pause);
                speak("Hold");
                startTimer(7, () -> {
                    currentPhase = PHASE_EXHALE;
                    startPhase();
                });
                break;

            case PHASE_EXHALE:
                tvBreathingAction.setText("Exhale");
                ivBreathingIcon.setImageResource(R.drawable.ic_arrow_down);
                speak("Breathe out");
                startTimer(8, () -> {
                    currentCycle++;
                    updateCycleCounter();
                    if (currentCycle >= maxCycles) {
                        stopBreathingExercise();
                        Toast.makeText(getContext(), "Great job! Exercise completed", Toast.LENGTH_LONG).show();
                        speak("Breathing exercise completed. Well done!");
                    } else {
                        currentPhase = PHASE_INHALE;
                        startPhase();
                    }
                });
                animateCircle(280, 240, 8000);
                animateRings(340, 300, 310, 270, 8000);
                break;
        }
    }

    private void startTimer(int seconds, Runnable onComplete) {
        if (breathingTimer != null) breathingTimer.cancel();

        breathingTimer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000) + 1;
                tvBreathingTimer.setText(secondsLeft + " sec");
            }

            @Override
            public void onFinish() {
                if (onComplete != null) onComplete.run();
            }
        }.start();
    }

    // --- 动画逻辑 (适配 getResources 和 Density) ---

    private void animateCircle(int fromSize, int toSize, long duration) {
        if (circleAnimator != null && circleAnimator.isRunning()) circleAnimator.cancel();

        circleAnimator = ValueAnimator.ofInt(fromSize, toSize);
        circleAnimator.setDuration(duration);
        circleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        circleAnimator.addUpdateListener(animation -> {
            if (!isAdded()) return;
            int size = (int) animation.getAnimatedValue();
            float density = getResources().getDisplayMetrics().density;
            int sizePx = (int) (size * density);

            ViewGroup.LayoutParams lp = breathingCircle.getLayoutParams();
            lp.width = sizePx;
            lp.height = sizePx;
            breathingCircle.setRadius(sizePx / 2f);
            breathingCircle.setLayoutParams(lp);
        });
        circleAnimator.start();
    }

    private void animateRings(int fromSize1, int toSize1, int fromSize2, int toSize2, long duration) {
        if (ring1Animator != null && ring1Animator.isRunning()) ring1Animator.cancel();
        if (ring2Animator != null && ring2Animator.isRunning()) ring2Animator.cancel();

        ring1Animator = ObjectAnimator.ofFloat(outerRing1, "scaleX", fromSize1 / 300f, toSize1 / 300f);
        ring1Animator.setDuration(duration);
        ring1Animator.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator ring1ScaleY = ObjectAnimator.ofFloat(outerRing1, "scaleY", fromSize1 / 300f, toSize1 / 300f);
        ring1ScaleY.setDuration(duration);

        ring2Animator = ObjectAnimator.ofFloat(outerRing2, "scaleX", fromSize2 / 270f, toSize2 / 270f);
        ring2Animator.setDuration(duration);

        ObjectAnimator ring2ScaleY = ObjectAnimator.ofFloat(outerRing2, "scaleY", fromSize2 / 270f, toSize2 / 270f);
        ring2ScaleY.setDuration(duration);

        ring1Animator.start(); ring1ScaleY.start();
        ring2Animator.start(); ring2ScaleY.start();

        ObjectAnimator.ofFloat(outerRing1, "alpha", 0.3f, 0.6f, 0.3f).setDuration(duration).start();
        ObjectAnimator.ofFloat(outerRing2, "alpha", 0.4f, 0.7f, 0.4f).setDuration(duration).start();
    }

    private void cancelAnimations() {
        if (circleAnimator != null) circleAnimator.cancel();
        if (ring1Animator != null) ring1Animator.cancel();
        if (ring2Animator != null) ring2Animator.cancel();
    }

    private void resetCircleAndRings() {
        if (!isAdded()) return;
        float density = getResources().getDisplayMetrics().density;
        int sizePx = (int) (240 * density);

        ViewGroup.LayoutParams lp = breathingCircle.getLayoutParams();
        lp.width = sizePx; lp.height = sizePx;
        breathingCircle.setRadius(sizePx / 2f);
        breathingCircle.setLayoutParams(lp);

        outerRing1.setScaleX(1.0f); outerRing1.setScaleY(1.0f); outerRing1.setAlpha(0.3f);
        outerRing2.setScaleX(1.0f); outerRing2.setScaleY(1.0f); outerRing2.setAlpha(0.4f);
        ivBreathingIcon.setImageResource(R.drawable.ic_lungs);
    }

    private void updateCycleCounter() {
        tvCycleCounter.setText("Cycle " + currentCycle + " / " + maxCycles);
    }

    private void speak(String text) {
        if (soundEnabled && ttsReady && textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isBreathing) stopBreathingExercise();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (breathingTimer != null) breathingTimer.cancel();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        cancelAnimations();
    }
}