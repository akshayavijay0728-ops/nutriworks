package com.nutriworks.app.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.nutriworks.app.data.db.MealEntry;
import com.nutriworks.app.databinding.ItemMealBinding;
import com.nutriworks.app.databinding.ItemMealHeaderBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Multi-view-type RecyclerView adapter that displays logged meals grouped by
 * meal type (Breakfast, Lunch, Dinner, Snack) with section calorie totals,
 * optimized with DiffUtil for smooth animations and efficient view holder reuse.
 */
public class GroupedMealsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnMealActionListener {
        void onMealClick(@NonNull MealEntry meal);
        void onMealEdit(@NonNull MealEntry meal);
    }

    private final List<GroupedMealItem> items = new ArrayList<>();
    private final OnMealActionListener actionListener;

    public GroupedMealsAdapter(@Nullable OnMealActionListener actionListener) {
        this.actionListener = actionListener;
    }

    /**
     * Updates adapter data using DiffUtil to dispatch granular change notifications
     * without full list invalidation.
     */
    public void setItems(@NonNull List<GroupedMealItem> newItems) {
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
                GroupedMealItem oldItem = items.get(oldItemPosition);
                GroupedMealItem newItem = newItems.get(newItemPosition);

                if (oldItem.getType() != newItem.getType()) {
                    return false;
                }

                if (oldItem.getType() == GroupedMealItem.TYPE_HEADER) {
                    String oldTitle = oldItem.getHeaderTitle();
                    String newTitle = newItem.getHeaderTitle();
                    return oldTitle != null && oldTitle.equals(newTitle);
                } else {
                    MealEntry oldEntry = oldItem.getMealEntry();
                    MealEntry newEntry = newItem.getMealEntry();
                    if (oldEntry != null && newEntry != null) {
                        return oldEntry.getId() == newEntry.getId();
                    }
                    return false;
                }
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                GroupedMealItem oldItem = items.get(oldItemPosition);
                GroupedMealItem newItem = newItems.get(newItemPosition);

                if (oldItem.getType() == GroupedMealItem.TYPE_HEADER) {
                    return oldItem.getHeaderCalories() == newItem.getHeaderCalories();
                } else {
                    MealEntry oldEntry = oldItem.getMealEntry();
                    MealEntry newEntry = newItem.getMealEntry();
                    if (oldEntry == null || newEntry == null) return false;

                    return oldEntry.getCalories() == newEntry.getCalories()
                            && Double.compare(oldEntry.getGrams(), newEntry.getGrams()) == 0
                            && oldEntry.getFoodName().equals(newEntry.getFoodName())
                            && oldEntry.getMealType().equals(newEntry.getMealType());
                }
            }
        });

        this.items.clear();
        this.items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    @Nullable
    public MealEntry getMealEntryAt(int position) {
        if (position >= 0 && position < items.size()) {
            GroupedMealItem item = items.get(position);
            if (item.getType() == GroupedMealItem.TYPE_MEAL) {
                return item.getMealEntry();
            }
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position).getType();
        }
        return GroupedMealItem.TYPE_MEAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == GroupedMealItem.TYPE_HEADER) {
            ItemMealHeaderBinding binding = ItemMealHeaderBinding.inflate(inflater, parent, false);
            return new HeaderViewHolder(binding);
        } else {
            ItemMealBinding binding = ItemMealBinding.inflate(inflater, parent, false);
            return new MealViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        GroupedMealItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item);
        } else if (holder instanceof MealViewHolder) {
            ((MealViewHolder) holder).bind(item.getMealEntry(), actionListener);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ItemMealHeaderBinding binding;

        HeaderViewHolder(@NonNull ItemMealHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull GroupedMealItem item) {
            binding.textMealGroupTitle.setText(item.getHeaderTitle());
            binding.textMealGroupCalories.setText(String.format(Locale.getDefault(), "%d kcal", item.getHeaderCalories()));
        }
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        private final ItemMealBinding binding;

        MealViewHolder(@NonNull ItemMealBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@Nullable MealEntry entry, @Nullable OnMealActionListener listener) {
            if (entry == null) {
                return;
            }

            binding.textMealName.setText(entry.getFoodName());
            binding.textMealCalories.setText(String.format(Locale.getDefault(), "%d kcal", entry.getCalories()));

            String details = String.format(Locale.getDefault(),
                    "%.0fg  •  P: %.0fg  •  C: %.0fg  •  F: %.0fg",
                    entry.getGrams(),
                    entry.getProtein(),
                    entry.getCarbs(),
                    entry.getFat());
            binding.textMealDetails.setText(details);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMealClick(entry);
                }
            });

            binding.btnEditMeal.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMealEdit(entry);
                }
            });
        }
    }
}
