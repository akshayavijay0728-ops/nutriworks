package com.nutriworks.app.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.nutriworks.app.R;
import com.nutriworks.app.data.db.UserProfile;
import com.nutriworks.app.databinding.FragmentProfileBinding;
import com.nutriworks.app.util.GoalCalculator;

import java.util.Locale;

/**
 * ProfileFragment displaying the user's metabolic stats, demographic metrics,
 * dietary goals, and recalculating personalized nutritional targets upon update.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private UserProfile currentProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupGoalToggleListener();
        setupObservers();
        setupSaveButton();
    }

    private void setupGoalToggleListener() {
        binding.chipGroupProfileGoal.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = checkedIds.isEmpty() ? R.id.chip_p_goal_maintain : checkedIds.get(0);
            if (checkedId == R.id.chip_p_goal_lose || checkedId == R.id.chip_p_goal_gain) {
                binding.layoutProfileWeeklyRate.setVisibility(View.VISIBLE);
            } else {
                binding.layoutProfileWeeklyRate.setVisibility(View.GONE);
            }
        });
    }

    private void setupObservers() {
        viewModel.getProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                this.currentProfile = profile;
                populateUi(profile);
            }
        });
    }

    private void populateUi(@NonNull UserProfile profile) {
        // Overview Card
        binding.textProfileCaloriesVal.setText(String.format(Locale.getDefault(), "%,d", profile.getTargetCalories()));

        double bmr = GoalCalculator.calculateBmr(profile.getAge(), profile.getGender(), profile.getHeightCm(), profile.getWeightKg());
        double tdee = GoalCalculator.calculateTdee(bmr, profile.getActivityLevel());
        binding.textProfileMetabolicBreakdown.setText(String.format(Locale.getDefault(),
                "Estimated BMR: %,.0f kcal  •  TDEE: %,.0f kcal", bmr, tdee));

        binding.textProfileTargetProtein.setText(String.format(Locale.getDefault(), "%dg", profile.getTargetProteinGrams()));
        binding.textProfileTargetCarbs.setText(String.format(Locale.getDefault(), "%dg", profile.getTargetCarbsGrams()));
        binding.textProfileTargetFat.setText(String.format(Locale.getDefault(), "%dg", profile.getTargetFatGrams()));

        // Input Fields (only if not currently focused to avoid typing disruption)
        if (!binding.editProfileAge.hasFocus()) {
            binding.editProfileAge.setText(String.valueOf(profile.getAge()));
        }
        if (!binding.editProfileHeight.hasFocus()) {
            binding.editProfileHeight.setText(String.format(Locale.getDefault(), "%.1f", profile.getHeightCm()));
        }
        if (!binding.editProfileWeight.hasFocus()) {
            binding.editProfileWeight.setText(String.format(Locale.getDefault(), "%.1f", profile.getWeightKg()));
        }

        // Gender
        if (UserProfile.GENDER_FEMALE.equalsIgnoreCase(profile.getGender())) {
            binding.chipProfileFemale.setChecked(true);
        } else if (UserProfile.GENDER_OTHER.equalsIgnoreCase(profile.getGender())) {
            binding.chipProfileOther.setChecked(true);
        } else {
            binding.chipProfileMale.setChecked(true);
        }

        // Activity
        String act = profile.getActivityLevel();
        if (UserProfile.ACTIVITY_SEDENTARY.equalsIgnoreCase(act)) {
            binding.chipPActSedentary.setChecked(true);
        } else if (UserProfile.ACTIVITY_LIGHT.equalsIgnoreCase(act)) {
            binding.chipPActLight.setChecked(true);
        } else if (UserProfile.ACTIVITY_ACTIVE.equalsIgnoreCase(act)) {
            binding.chipPActActive.setChecked(true);
        } else {
            binding.chipPActModerate.setChecked(true);
        }

        // Goal
        String goal = profile.getGoalType();
        if (UserProfile.GOAL_LOSE.equalsIgnoreCase(goal)) {
            binding.chipPGoalLose.setChecked(true);
            binding.layoutProfileWeeklyRate.setVisibility(View.VISIBLE);
        } else if (UserProfile.GOAL_GAIN.equalsIgnoreCase(goal)) {
            binding.chipPGoalGain.setChecked(true);
            binding.layoutProfileWeeklyRate.setVisibility(View.VISIBLE);
        } else {
            binding.chipPGoalMaintain.setChecked(true);
            binding.layoutProfileWeeklyRate.setVisibility(View.GONE);
        }

        // Rate
        double rate = profile.getWeeklyRateKg();
        if (rate <= 0.3) {
            binding.chipPRate025.setChecked(true);
        } else if (rate >= 0.7) {
            binding.chipPRate075.setChecked(true);
        } else {
            binding.chipPRate050.setChecked(true);
        }

        // Macro Split
        String split = profile.getMacroSplit();
        if (UserProfile.SPLIT_HIGH_PROTEIN.equalsIgnoreCase(split)) {
            binding.chipPSplitHighProtein.setChecked(true);
        } else if (UserProfile.SPLIT_LOW_CARB.equalsIgnoreCase(split)) {
            binding.chipPSplitLowCarb.setChecked(true);
        } else {
            binding.chipPSplitBalanced.setChecked(true);
        }
    }

    private void setupSaveButton() {
        binding.btnSaveProfile.setOnClickListener(v -> {
            String ageStr = binding.editProfileAge.getText() != null ? binding.editProfileAge.getText().toString().trim() : "";
            String heightStr = binding.editProfileHeight.getText() != null ? binding.editProfileHeight.getText().toString().trim() : "";
            String weightStr = binding.editProfileWeight.getText() != null ? binding.editProfileWeight.getText().toString().trim() : "";

            int age;
            double height, weight;

            try {
                age = Integer.parseInt(ageStr);
                if (age < 13 || age > 120) {
                    binding.editProfileAge.setError("Age must be between 13 and 120");
                    return;
                }
            } catch (Exception e) {
                binding.editProfileAge.setError("Valid age required");
                return;
            }

            try {
                height = Double.parseDouble(heightStr);
                if (height < 80 || height > 250) {
                    binding.editProfileHeight.setError("Height must be 80 - 250 cm");
                    return;
                }
            } catch (Exception e) {
                binding.editProfileHeight.setError("Valid height required");
                return;
            }

            try {
                weight = Double.parseDouble(weightStr);
                if (weight < 30 || weight > 300) {
                    binding.editProfileWeight.setError("Weight must be 30 - 300 kg");
                    return;
                }
            } catch (Exception e) {
                binding.editProfileWeight.setError("Valid weight required");
                return;
            }

            UserProfile profileToSave = currentProfile != null ? currentProfile : new UserProfile();
            profileToSave.setAge(age);
            profileToSave.setHeightCm(height);
            profileToSave.setWeightKg(weight);

            // Gender
            int checkedGenderId = binding.chipGroupProfileGender.getCheckedChipId();
            if (checkedGenderId == R.id.chip_profile_female) {
                profileToSave.setGender(UserProfile.GENDER_FEMALE);
            } else if (checkedGenderId == R.id.chip_profile_other) {
                profileToSave.setGender(UserProfile.GENDER_OTHER);
            } else {
                profileToSave.setGender(UserProfile.GENDER_MALE);
            }

            // Activity
            int checkedActId = binding.chipGroupProfileActivity.getCheckedChipId();
            if (checkedActId == R.id.chip_p_act_sedentary) {
                profileToSave.setActivityLevel(UserProfile.ACTIVITY_SEDENTARY);
            } else if (checkedActId == R.id.chip_p_act_light) {
                profileToSave.setActivityLevel(UserProfile.ACTIVITY_LIGHT);
            } else if (checkedActId == R.id.chip_p_act_active) {
                profileToSave.setActivityLevel(UserProfile.ACTIVITY_ACTIVE);
            } else {
                profileToSave.setActivityLevel(UserProfile.ACTIVITY_MODERATE);
            }

            // Goal & Rate
            int checkedGoalId = binding.chipGroupProfileGoal.getCheckedChipId();
            if (checkedGoalId == R.id.chip_p_goal_lose) {
                profileToSave.setGoalType(UserProfile.GOAL_LOSE);
                profileToSave.setWeeklyRateKg(getSelectedRate());
            } else if (checkedGoalId == R.id.chip_p_goal_gain) {
                profileToSave.setGoalType(UserProfile.GOAL_GAIN);
                profileToSave.setWeeklyRateKg(getSelectedRate());
            } else {
                profileToSave.setGoalType(UserProfile.GOAL_MAINTAIN);
                profileToSave.setWeeklyRateKg(0.0);
            }

            // Macro Split
            int checkedSplitId = binding.chipGroupProfileSplit.getCheckedChipId();
            if (checkedSplitId == R.id.chip_p_split_high_protein) {
                profileToSave.setMacroSplit(UserProfile.SPLIT_HIGH_PROTEIN);
            } else if (checkedSplitId == R.id.chip_p_split_low_carb) {
                profileToSave.setMacroSplit(UserProfile.SPLIT_LOW_CARB);
            } else {
                profileToSave.setMacroSplit(UserProfile.SPLIT_BALANCED);
            }

            viewModel.saveAndRecalculateProfile(profileToSave, () -> {
                requireActivity().runOnUiThread(() -> {
                    Snackbar.make(binding.getRoot(), "Goals recalculated & profile updated!", Snackbar.LENGTH_SHORT).show();
                });
            });
        });
    }

    private double getSelectedRate() {
        int checkedId = binding.chipGroupProfileRate.getCheckedChipId();
        if (checkedId == R.id.chip_p_rate_025) {
            return 0.25;
        } else if (checkedId == R.id.chip_p_rate_075) {
            return 0.75;
        }
        return 0.50;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
