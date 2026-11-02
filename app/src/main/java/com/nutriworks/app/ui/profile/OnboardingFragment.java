package com.nutriworks.app.ui.profile;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.nutriworks.app.R;
import com.nutriworks.app.data.db.UserProfile;
import com.nutriworks.app.data.repository.ProfileRepository;
import com.nutriworks.app.databinding.FragmentOnboardingBinding;
import com.nutriworks.app.util.GoalCalculator;

import java.util.Locale;

/**
 * OnboardingFragment collecting user physical measurements, metabolic rate parameters,
 * and fitness goals during first app launch, with live calculation preview and input validation.
 */
public class OnboardingFragment extends Fragment {

    private FragmentOnboardingBinding binding;
    private ProfileRepository profileRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileRepository = new ProfileRepository(requireContext());

        setupListeners();
        recalculatePreview();
    }

    private void setupListeners() {
        TextWatcher recalculateWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculatePreview();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.editOnboardingAge.addTextChangedListener(recalculateWatcher);
        binding.editOnboardingHeight.addTextChangedListener(recalculateWatcher);
        binding.editOnboardingWeight.addTextChangedListener(recalculateWatcher);

        binding.chipGroupOnboardingGender.setOnCheckedStateChangeListener((group, checkedIds) -> recalculatePreview());
        binding.chipGroupOnboardingActivity.setOnCheckedStateChangeListener((group, checkedIds) -> recalculatePreview());
        binding.chipGroupMacroSplit.setOnCheckedStateChangeListener((group, checkedIds) -> recalculatePreview());
        binding.chipGroupWeeklyRate.setOnCheckedStateChangeListener((group, checkedIds) -> recalculatePreview());

        binding.chipGroupOnboardingGoal.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = checkedIds.isEmpty() ? R.id.chip_goal_maintain : checkedIds.get(0);
            if (checkedId == R.id.chip_goal_lose || checkedId == R.id.chip_goal_gain) {
                binding.layoutWeeklyRateContainer.setVisibility(View.VISIBLE);
            } else {
                binding.layoutWeeklyRateContainer.setVisibility(View.GONE);
            }
            recalculatePreview();
        });

        binding.btnCompleteOnboarding.setOnClickListener(v -> submitProfile());
    }

    private UserProfile extractProfileFromInputs() {
        String ageStr = binding.editOnboardingAge.getText() != null ? binding.editOnboardingAge.getText().toString().trim() : "";
        String heightStr = binding.editOnboardingHeight.getText() != null ? binding.editOnboardingHeight.getText().toString().trim() : "";
        String weightStr = binding.editOnboardingWeight.getText() != null ? binding.editOnboardingWeight.getText().toString().trim() : "";

        int age = parsePositiveInt(ageStr, 28);
        double height = parsePositiveDouble(heightStr, 175.0);
        double weight = parsePositiveDouble(weightStr, 75.0);

        String gender = UserProfile.GENDER_MALE;
        int checkedGenderId = binding.chipGroupOnboardingGender.getCheckedChipId();
        if (checkedGenderId == R.id.chip_gender_female) {
            gender = UserProfile.GENDER_FEMALE;
        } else if (checkedGenderId == R.id.chip_gender_other) {
            gender = UserProfile.GENDER_OTHER;
        }

        String activity = UserProfile.ACTIVITY_MODERATE;
        int checkedActId = binding.chipGroupOnboardingActivity.getCheckedChipId();
        if (checkedActId == R.id.chip_act_sedentary) {
            activity = UserProfile.ACTIVITY_SEDENTARY;
        } else if (checkedActId == R.id.chip_act_light) {
            activity = UserProfile.ACTIVITY_LIGHT;
        } else if (checkedActId == R.id.chip_act_active) {
            activity = UserProfile.ACTIVITY_ACTIVE;
        }

        String goal = UserProfile.GOAL_MAINTAIN;
        int checkedGoalId = binding.chipGroupOnboardingGoal.getCheckedChipId();
        double weeklyRate = 0.0;
        if (checkedGoalId == R.id.chip_goal_lose) {
            goal = UserProfile.GOAL_LOSE;
            weeklyRate = getSelectedWeeklyRate();
        } else if (checkedGoalId == R.id.chip_goal_gain) {
            goal = UserProfile.GOAL_GAIN;
            weeklyRate = getSelectedWeeklyRate();
        }

        String split = UserProfile.SPLIT_BALANCED;
        int checkedSplitId = binding.chipGroupMacroSplit.getCheckedChipId();
        if (checkedSplitId == R.id.chip_split_high_protein) {
            split = UserProfile.SPLIT_HIGH_PROTEIN;
        } else if (checkedSplitId == R.id.chip_split_low_carb) {
            split = UserProfile.SPLIT_LOW_CARB;
        }

        UserProfile profile = new UserProfile();
        profile.setAge(age);
        profile.setGender(gender);
        profile.setHeightCm(height);
        profile.setWeightKg(weight);
        profile.setActivityLevel(activity);
        profile.setGoalType(goal);
        profile.setWeeklyRateKg(weeklyRate);
        profile.setMacroSplit(split);

        return GoalCalculator.calculateAndApplyGoals(profile);
    }

    private double getSelectedWeeklyRate() {
        int checkedId = binding.chipGroupWeeklyRate.getCheckedChipId();
        if (checkedId == R.id.chip_rate_025) {
            return 0.25;
        } else if (checkedId == R.id.chip_rate_075) {
            return 0.75;
        }
        return 0.50;
    }

    private void recalculatePreview() {
        UserProfile calculated = extractProfileFromInputs();

        binding.textPreviewDailyCalories.setText(String.format(Locale.getDefault(), "%,d", calculated.getTargetCalories()));

        double bmr = GoalCalculator.calculateBmr(
                calculated.getAge(),
                calculated.getGender(),
                calculated.getHeightCm(),
                calculated.getWeightKg()
        );
        double tdee = GoalCalculator.calculateTdee(bmr, calculated.getActivityLevel());

        binding.textPreviewMetabolicStats.setText(String.format(Locale.getDefault(),
                "Estimated BMR: %,.0f kcal  •  TDEE: %,.0f kcal", bmr, tdee));

        binding.textPreviewMacroGrams.setText(String.format(Locale.getDefault(),
                "Carbs: %dg  •  Protein: %dg  •  Fat: %dg",
                calculated.getTargetCarbsGrams(),
                calculated.getTargetProteinGrams(),
                calculated.getTargetFatGrams()));
    }

    private void submitProfile() {
        // Validate inputs
        String ageStr = binding.editOnboardingAge.getText() != null ? binding.editOnboardingAge.getText().toString().trim() : "";
        String heightStr = binding.editOnboardingHeight.getText() != null ? binding.editOnboardingHeight.getText().toString().trim() : "";
        String weightStr = binding.editOnboardingWeight.getText() != null ? binding.editOnboardingWeight.getText().toString().trim() : "";

        int age;
        double height, weight;

        try {
            age = Integer.parseInt(ageStr);
            if (age < 13 || age > 120) {
                binding.editOnboardingAge.setError("Age must be between 13 and 120");
                return;
            }
        } catch (Exception e) {
            binding.editOnboardingAge.setError("Valid age required");
            return;
        }

        try {
            height = Double.parseDouble(heightStr);
            if (height < 80 || height > 250) {
                binding.editOnboardingHeight.setError("Height must be between 80 and 250 cm");
                return;
            }
        } catch (Exception e) {
            binding.editOnboardingHeight.setError("Valid height required");
            return;
        }

        try {
            weight = Double.parseDouble(weightStr);
            if (weight < 30 || weight > 300) {
                binding.editOnboardingWeight.setError("Weight must be between 30 and 300 kg");
                return;
            }
        } catch (Exception e) {
            binding.editOnboardingWeight.setError("Valid weight required");
            return;
        }

        UserProfile profile = extractProfileFromInputs();
        profile.setOnboardingCompleted(true);

        profileRepository.saveProfile(profile, () -> {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Profile created! Welcome to NutriWorks.", Toast.LENGTH_SHORT).show();

                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(R.id.navigation_onboarding, true)
                        .build();

                Navigation.findNavController(requireView()).navigate(R.id.navigation_home, null, navOptions);
            });
        });
    }

    private int parsePositiveInt(String s, int fallback) {
        try {
            int val = Integer.parseInt(s);
            return val > 0 ? val : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private double parsePositiveDouble(String s, double fallback) {
        try {
            double val = Double.parseDouble(s);
            return val > 0 ? val : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
