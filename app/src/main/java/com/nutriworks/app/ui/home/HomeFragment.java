package com.nutriworks.app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.nutriworks.app.R;
import com.nutriworks.app.data.db.DailyMacroSummary;
import com.nutriworks.app.data.db.MealEntry;
import com.nutriworks.app.data.db.UserProfile;
import com.nutriworks.app.databinding.FragmentHomeBinding;
import com.nutriworks.app.util.DateTimeUtils;

import java.util.Locale;

/**
 * HomeFragment destination displaying personalized daily calorie and macro progress rings,
 * over-goal visual status indicators, date navigation, and logged meals grouped by meal type
 * with swipe-to-delete (and undo) and tap-to-edit interactions.
 */
public class HomeFragment extends Fragment implements GroupedMealsAdapter.OnMealActionListener {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private GroupedMealsAdapter mealsAdapter;

    private int goalCalories = 2000;
    private int goalProtein = 130;
    private int goalCarbs = 220;
    private int goalFat = 65;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupDateNavigator();
        setupMealsRecyclerView();
        setupObservers();
        setupManualAddFab();
    }

    private void setupDateNavigator() {
        binding.btnPrevDay.setOnClickListener(v -> viewModel.previousDay());
        binding.btnNextDay.setOnClickListener(v -> viewModel.nextDay());

        binding.btnDatePicker.setOnClickListener(v -> {
            Long currentMillis = viewModel.getSelectedDateMillis().getValue();
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date")
                    .setSelection(currentMillis != null ? currentMillis : System.currentTimeMillis())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                if (selection != null) {
                    viewModel.setDate(selection);
                }
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });
    }

    private void setupMealsRecyclerView() {
        mealsAdapter = new GroupedMealsAdapter(this);
        binding.recyclerTodaysMeals.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTodaysMeals.setAdapter(mealsAdapter);

        // Attach ItemTouchHelper for swipe-to-delete with undo
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                if (mealsAdapter.getMealEntryAt(position) == null) {
                    return 0; // Section headers cannot be swiped
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                MealEntry entryToDelete = mealsAdapter.getMealEntryAt(position);
                if (entryToDelete != null) {
                    viewModel.deleteMeal(entryToDelete);

                    Snackbar.make(binding.getRoot(), "Deleted " + entryToDelete.getFoodName(), Snackbar.LENGTH_LONG)
                            .setAction("UNDO", v -> viewModel.undoDelete(entryToDelete))
                            .show();
                }
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerTodaysMeals);
    }

    private void setupObservers() {
        // Observe Personalized UserProfile Goals
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), this::updatePersonalizedGoals);

        // Observe Selected Date
        viewModel.getSelectedDateMillis().observe(getViewLifecycleOwner(), timestamp -> {
            if (timestamp != null) {
                String relative = DateTimeUtils.getRelativeDateString(timestamp);
                String friendly = DateTimeUtils.formatFriendlyDate(timestamp);
                binding.textDateDisplay.setText(String.format("%s (%s)", relative, friendly));
            }
        });

        // Observe Daily Summary
        viewModel.getSummaryForDate().observe(getViewLifecycleOwner(), this::updateNutritionSummary);

        // Observe Grouped Meal List
        viewModel.getGroupedMealsForDate().observe(getViewLifecycleOwner(), groupedItems -> {
            if (groupedItems == null || groupedItems.isEmpty()) {
                binding.layoutEmptyState.setVisibility(View.VISIBLE);
                binding.recyclerTodaysMeals.setVisibility(View.GONE);
            } else {
                binding.layoutEmptyState.setVisibility(View.GONE);
                binding.recyclerTodaysMeals.setVisibility(View.VISIBLE);
                mealsAdapter.setItems(groupedItems);
            }
        });
    }

    private void updatePersonalizedGoals(@Nullable UserProfile profile) {
        if (profile != null) {
            this.goalCalories = profile.getTargetCalories();
            this.goalProtein = profile.getTargetProteinGrams();
            this.goalCarbs = profile.getTargetCarbsGrams();
            this.goalFat = profile.getTargetFatGrams();

            binding.progressCalories.setMax(goalCalories);
            binding.textGoalCalories.setText(String.format(Locale.getDefault(), "/ %,d kcal", goalCalories));

            binding.progressMacroProtein.setMax(goalProtein);
            binding.progressMacroCarbs.setMax(goalCarbs);
            binding.progressMacroFat.setMax(goalFat);

            // Re-render summary with new denominators
            updateNutritionSummary(viewModel.getSummaryForDate().getValue());
        }
    }

    private void updateNutritionSummary(@Nullable DailyMacroSummary summary) {
        int consumedCals = summary != null ? summary.getTotalCalories() : 0;
        double protein = summary != null ? summary.getTotalProtein() : 0.0;
        double carbs = summary != null ? summary.getTotalCarbs() : 0.0;
        double fat = summary != null ? summary.getTotalFat() : 0.0;

        binding.progressCalories.setProgress(Math.min(consumedCals, goalCalories));
        binding.textCurrentCalories.setText(String.format(Locale.getDefault(), "%,d", consumedCals));

        // Over-goal color change and status calculation
        int errorColor = ContextCompat.getColor(requireContext(), R.color.md_theme_light_error);
        int normalRingColor = ContextCompat.getColor(requireContext(), R.color.macro_calories);
        int primaryGreen = ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary);

        if (consumedCals > goalCalories) {
            // Over-goal state: highlight ring and remaining text in red
            int overAmount = consumedCals - goalCalories;
            binding.progressCalories.setIndicatorColor(errorColor);
            binding.imgCaloriesIcon.setColorFilter(errorColor);
            binding.textRemainingCalories.setTextColor(errorColor);
            binding.textRemainingCalories.setText(String.format(Locale.getDefault(), "+%d kcal over goal", overAmount));
        } else {
            // Within goal state: normal vibrant orange/green
            int remainingCals = goalCalories - consumedCals;
            binding.progressCalories.setIndicatorColor(normalRingColor);
            binding.imgCaloriesIcon.setColorFilter(normalRingColor);
            binding.textRemainingCalories.setTextColor(primaryGreen);
            binding.textRemainingCalories.setText(String.format(Locale.getDefault(), "%d kcal remaining", remainingCals));
        }

        // Macro Progress Bars
        binding.progressMacroProtein.setProgress((int) Math.round(protein));
        binding.textMacroProteinVal.setText(String.format(Locale.getDefault(), "%.0fg / %dg", protein, goalProtein));

        binding.progressMacroCarbs.setProgress((int) Math.round(carbs));
        binding.textMacroCarbsVal.setText(String.format(Locale.getDefault(), "%.0fg / %dg", carbs, goalCarbs));

        binding.progressMacroFat.setProgress((int) Math.round(fat));
        binding.textMacroFatVal.setText(String.format(Locale.getDefault(), "%.0fg / %dg", fat, goalFat));
    }

    private void setupManualAddFab() {
        binding.fabAddMeal.setOnClickListener(v -> {
            Long date = viewModel.getSelectedDateMillis().getValue();
            long timestamp = date != null ? date : System.currentTimeMillis();
            ManualAddFoodDialogFragment dialog = ManualAddFoodDialogFragment.newInstance(timestamp);
            dialog.show(getParentFragmentManager(), ManualAddFoodDialogFragment.TAG);
        });
    }

    @Override
    public void onMealClick(@NonNull MealEntry meal) {
        openEditMealDialog(meal);
    }

    @Override
    public void onMealEdit(@NonNull MealEntry meal) {
        openEditMealDialog(meal);
    }

    private void openEditMealDialog(@NonNull MealEntry meal) {
        EditMealDialogFragment dialog = EditMealDialogFragment.newInstance(meal);
        dialog.setOnMealUpdatedListener(updatedMeal -> {
            viewModel.updateMeal(updatedMeal);
            Snackbar.make(binding.getRoot(), "Updated " + updatedMeal.getFoodName(), Snackbar.LENGTH_SHORT).show();
        });
        dialog.show(getParentFragmentManager(), "EDIT_MEAL");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
