package com.nutriworks.app.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nutriworks.app.databinding.ItemMealBinding;
import com.nutriworks.app.model.FoodItem;
import com.nutriworks.app.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying logged food items and meals.
 */
public class MealsAdapter extends RecyclerView.Adapter<MealsAdapter.MealViewHolder> {

    private final List<FoodItem> mealsList = new ArrayList<>();

    public MealsAdapter() {
    }

    public void setMeals(List<FoodItem> meals) {
        this.mealsList.clear();
        if (meals != null) {
            this.mealsList.addAll(meals);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMealBinding binding = ItemMealBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new MealViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        FoodItem meal = mealsList.get(position);
        holder.bind(meal);
    }

    @Override
    public int getItemCount() {
        return mealsList.size();
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {

        private final ItemMealBinding binding;

        public MealViewHolder(@NonNull ItemMealBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(@NonNull FoodItem meal) {
            binding.textMealName.setText(meal.getName());

            String timeString = DateTimeUtils.formatMealTime(meal.getTimestamp());
            String details = String.format(
                    Locale.getDefault(),
                    "P: %.0fg  •  C: %.0fg  •  F: %.0fg  •  %s (%s)",
                    meal.getProteinGrams(),
                    meal.getCarbsGrams(),
                    meal.getFatGrams(),
                    meal.getMealType(),
                    timeString
            );
            binding.textMealDetails.setText(details);

            String calorieString = String.format(
                    Locale.getDefault(),
                    "%d kcal",
                    meal.getCalories()
            );
            binding.textMealCalories.setText(calorieString);
        }
    }
}
