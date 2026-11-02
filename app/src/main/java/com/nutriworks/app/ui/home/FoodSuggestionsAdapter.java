package com.nutriworks.app.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.nutriworks.app.R;
import com.nutriworks.app.data.db.FoodItem;
import com.nutriworks.app.databinding.ItemFoodSuggestionBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying searchable, favorite, and recent food items
 * with favorite toggling support, optimized with DiffUtil.
 */
public class FoodSuggestionsAdapter extends RecyclerView.Adapter<FoodSuggestionsAdapter.ViewHolder> {

    public interface OnFoodItemClickListener {
        void onFoodSelected(@NonNull FoodItem item);
        void onFavoriteToggle(@NonNull FoodItem item);
    }

    private final List<FoodItem> items = new ArrayList<>();
    private final OnFoodItemClickListener listener;

    public FoodSuggestionsAdapter(@Nullable OnFoodItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<FoodItem> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return items.size();
            }

            @Override
            public int getNewListSize() {
                return newItems.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                FoodItem oldItem = items.get(oldItemPosition);
                FoodItem newItem = newItems.get(newItemPosition);
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                FoodItem oldItem = items.get(oldItemPosition);
                FoodItem newItem = newItems.get(newItemPosition);
                return oldItem.isFavorite() == newItem.isFavorite()
                        && oldItem.getCalories() == newItem.getCalories()
                        && oldItem.getName().equals(newItem.getName());
            }
        });

        this.items.clear();
        this.items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFoodSuggestionBinding binding = ItemFoodSuggestionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemFoodSuggestionBinding binding;

        ViewHolder(@NonNull ItemFoodSuggestionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull FoodItem item, @Nullable OnFoodItemClickListener listener) {
            binding.textFoodSuggestionName.setText(item.getName());

            String nutritionStr = String.format(Locale.getDefault(),
                    "%d kcal/100g  •  P: %.1fg  •  C: %.1fg  •  F: %.1fg",
                    item.getCalories(),
                    item.getProteinGrams(),
                    item.getCarbsGrams(),
                    item.getFatGrams());
            binding.textFoodSuggestionNutrition.setText(nutritionStr);

            binding.btnFoodFavoriteToggle.setImageResource(
                    item.isFavorite() ? R.drawable.ic_star : R.drawable.ic_star_border
            );

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFoodSelected(item);
                }
            });

            binding.btnFoodFavoriteToggle.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFavoriteToggle(item);
                }
            });
        }
    }
}
