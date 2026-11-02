package com.nutriworks.app.ui.home;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nutriworks.app.R;
import com.nutriworks.app.data.db.MealEntry;
import com.nutriworks.app.databinding.DialogEditMealBinding;

import java.util.Locale;

/**
 * DialogFragment for editing portion grams and meal category of an already logged meal.
 */
public class EditMealDialogFragment extends DialogFragment {

    public interface OnMealUpdatedListener {
        void onMealUpdated(@NonNull MealEntry updatedMeal);
    }

    private MealEntry originalEntry;
    private OnMealUpdatedListener listener;

    public static EditMealDialogFragment newInstance(@NonNull MealEntry entry) {
        EditMealDialogFragment fragment = new EditMealDialogFragment();
        fragment.originalEntry = entry;
        return fragment;
    }

    public void setOnMealUpdatedListener(OnMealUpdatedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        DialogEditMealBinding binding = DialogEditMealBinding.inflate(LayoutInflater.from(requireContext()));

        if (originalEntry != null) {
            binding.textEditMealFoodName.setText(originalEntry.getFoodName());
            binding.editPortionGrams.setText(String.format(Locale.getDefault(), "%.0f", originalEntry.getGrams()));

            String type = originalEntry.getMealType();
            if ("Breakfast".equalsIgnoreCase(type)) {
                binding.chipEditBreakfast.setChecked(true);
            } else if ("Dinner".equalsIgnoreCase(type)) {
                binding.chipEditDinner.setChecked(true);
            } else if ("Snack".equalsIgnoreCase(type)) {
                binding.chipEditSnack.setChecked(true);
            } else {
                binding.chipEditLunch.setChecked(true);
            }
        }

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Logged Meal")
                .setView(binding.getRoot())
                .setPositiveButton("Save", (dialog, which) -> {
                    if (originalEntry == null) return;

                    String gramsStr = binding.editPortionGrams.getText() != null
                            ? binding.editPortionGrams.getText().toString().trim() : "";
                    double newGrams;
                    try {
                        newGrams = Double.parseDouble(gramsStr);
                    } catch (Exception e) {
                        newGrams = originalEntry.getGrams();
                    }

                    if (newGrams <= 0) {
                        Toast.makeText(requireContext(), "Invalid portion size", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Recalculate macros based on portion ratio
                    double ratio = originalEntry.getGrams() > 0 ? (newGrams / originalEntry.getGrams()) : 1.0;
                    int newCalories = (int) Math.round(originalEntry.getCalories() * ratio);
                    double newProtein = originalEntry.getProtein() * ratio;
                    double newCarbs = originalEntry.getCarbs() * ratio;
                    double newFat = originalEntry.getFat() * ratio;

                    String newType = "Lunch";
                    int checkedChipId = binding.chipGroupEditMealType.getCheckedChipId();
                    if (checkedChipId == R.id.chip_edit_breakfast) {
                        newType = "Breakfast";
                    } else if (checkedChipId == R.id.chip_edit_dinner) {
                        newType = "Dinner";
                    } else if (checkedChipId == R.id.chip_edit_snack) {
                        newType = "Snack";
                    }

                    originalEntry.setGrams(newGrams);
                    originalEntry.setCalories(newCalories);
                    originalEntry.setProtein(newProtein);
                    originalEntry.setCarbs(newCarbs);
                    originalEntry.setFat(newFat);
                    originalEntry.setMealType(newType);

                    if (listener != null) {
                        listener.onMealUpdated(originalEntry);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
    }
}
