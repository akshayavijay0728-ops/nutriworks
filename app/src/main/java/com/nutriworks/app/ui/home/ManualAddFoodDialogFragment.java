package com.nutriworks.app.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.nutriworks.app.R;
import com.nutriworks.app.data.db.FoodItem;
import com.nutriworks.app.data.db.MealEntry;
import com.nutriworks.app.data.repository.MealRepository;
import com.nutriworks.app.databinding.DialogManualAddFoodBinding;

import java.util.List;
import java.util.Locale;

/**
 * BottomSheetDialogFragment implementing the complete manual food add flow:
 * search foods with autocomplete, filter by favorites or recent, create custom foods,
 * and log portions to the user's selected date.
 */
public class ManualAddFoodDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "ManualAddFoodDialog";
    private static final String ARG_TARGET_TIMESTAMP = "target_timestamp";

    public interface OnMealAddedListener {
        void onMealAdded();
    }

    private DialogManualAddFoodBinding binding;
    private MealRepository mealRepository;
    private FoodSuggestionsAdapter adapter;

    private FoodItem selectedFoodItem = null;
    private long targetTimestamp = System.currentTimeMillis();
    private OnMealAddedListener onMealAddedListener;

    private LiveData<List<FoodItem>> currentFoodListLiveData;

    public static ManualAddFoodDialogFragment newInstance(long targetTimestamp) {
        ManualAddFoodDialogFragment fragment = new ManualAddFoodDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_TARGET_TIMESTAMP, targetTimestamp);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnMealAddedListener(OnMealAddedListener listener) {
        this.onMealAddedListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            targetTimestamp = getArguments().getLong(ARG_TARGET_TIMESTAMP, System.currentTimeMillis());
        }
        mealRepository = new MealRepository(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogManualAddFoodBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupSearchAndFilters();
        setupCustomFoodForm();
        setupConfirmButton();

        // Default query: load all foods
        loadFoodData(mealRepository.searchFoods(""));
    }

    private void setupRecyclerView() {
        adapter = new FoodSuggestionsAdapter(new FoodSuggestionsAdapter.OnFoodItemClickListener() {
            @Override
            public void onFoodSelected(@NonNull FoodItem item) {
                selectFood(item);
            }

            @Override
            public void onFavoriteToggle(@NonNull FoodItem item) {
                mealRepository.toggleFavoriteFood(item);
            }
        });

        binding.recyclerFoodSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerFoodSuggestions.setAdapter(adapter);
    }

    private void setupSearchAndFilters() {
        binding.autocompleteSearchFood.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString().trim() : "";
                loadFoodData(mealRepository.searchFoods(query));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.chipGroupQuickFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_filter_all) {
                binding.layoutCustomFoodForm.setVisibility(View.GONE);
                binding.recyclerFoodSuggestions.setVisibility(View.VISIBLE);
                String query = binding.autocompleteSearchFood.getText() != null
                        ? binding.autocompleteSearchFood.getText().toString().trim() : "";
                loadFoodData(mealRepository.searchFoods(query));
            } else if (checkedId == R.id.chip_filter_favorites) {
                binding.layoutCustomFoodForm.setVisibility(View.GONE);
                binding.recyclerFoodSuggestions.setVisibility(View.VISIBLE);
                loadFoodData(mealRepository.getFavorites());
            } else if (checkedId == R.id.chip_filter_recent) {
                binding.layoutCustomFoodForm.setVisibility(View.GONE);
                binding.recyclerFoodSuggestions.setVisibility(View.VISIBLE);
                loadFoodData(mealRepository.getRecentFoods(15));
            } else if (checkedId == R.id.chip_filter_custom) {
                binding.layoutCustomFoodForm.setVisibility(View.VISIBLE);
                binding.recyclerFoodSuggestions.setVisibility(View.GONE);
            }
        });
    }

    private void loadFoodData(LiveData<List<FoodItem>> liveData) {
        if (currentFoodListLiveData != null) {
            currentFoodListLiveData.removeObservers(getViewLifecycleOwner());
        }
        currentFoodListLiveData = liveData;
        currentFoodListLiveData.observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                adapter.setItems(items);
            }
        });
    }

    private void selectFood(@NonNull FoodItem item) {
        this.selectedFoodItem = item;
        binding.textSelectedFoodName.setText(item.getName());
        String info = String.format(Locale.getDefault(),
                "100g = %d kcal  •  P: %.1fg  •  C: %.1fg  •  F: %.1fg",
                item.getCalories(),
                item.getProteinGrams(),
                item.getCarbsGrams(),
                item.getFatGrams());
        binding.textSelectedFoodMacroInfo.setText(info);
    }

    private void setupCustomFoodForm() {
        binding.btnSaveCustomFood.setOnClickListener(v -> {
            String name = binding.editCustomFoodName.getText() != null
                    ? binding.editCustomFoodName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                binding.editCustomFoodName.setError("Enter food name");
                return;
            }

            int calories = (int) Math.round(parseDouble(binding.editCustomFoodKcal.getText()));
            double protein = parseDouble(binding.editCustomFoodProtein.getText());
            double carbs = parseDouble(binding.editCustomFoodCarbs.getText());
            double fat = parseDouble(binding.editCustomFoodFat.getText());

            FoodItem customItem = new FoodItem(
                    name,
                    calories,
                    protein,
                    carbs,
                    fat,
                    false,
                    System.currentTimeMillis()
            );

            mealRepository.insertFood(customItem, rowId -> {
                customItem.setId(rowId);
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (isAdded() && binding != null) {
                            selectFood(customItem);
                            Toast.makeText(requireContext(), "Saved '" + name + "' to library!", Toast.LENGTH_SHORT).show();
                            binding.chipFilterAll.setChecked(true);
                        }
                    });
                }
            });
        });
    }

    private void setupConfirmButton() {
        binding.btnConfirmAddMeal.setOnClickListener(v -> {
            if (selectedFoodItem == null) {
                Toast.makeText(requireContext(), "Please select a food item or save a custom food", Toast.LENGTH_SHORT).show();
                return;
            }

            String portionText = binding.editManualPortionGrams.getText() != null
                    ? binding.editManualPortionGrams.getText().toString().trim() : "";
            double grams = parseDouble(portionText);
            if (grams <= 0) {
                binding.editManualPortionGrams.setError("Enter valid portion in grams");
                return;
            }

            String mealType = getSelectedMealType();
            double factor = grams / 100.0;
            int totalCalories = (int) Math.round(selectedFoodItem.getCalories() * factor);
            double totalProtein = selectedFoodItem.getProteinGrams() * factor;
            double totalCarbs = selectedFoodItem.getCarbsGrams() * factor;
            double totalFat = selectedFoodItem.getFatGrams() * factor;

            MealEntry entry = new MealEntry(
                    selectedFoodItem.getName(),
                    grams,
                    totalCalories,
                    totalProtein,
                    totalCarbs,
                    totalFat,
                    mealType,
                    targetTimestamp,
                    null
            );

            mealRepository.insertMeal(entry, rowId -> {
                mealRepository.updateFoodLastUsed(selectedFoodItem);
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            if (onMealAddedListener != null) {
                                onMealAddedListener.onMealAdded();
                            }
                            dismiss();
                        }
                    });
                }
            });
        });
    }

    @NonNull
    private String getSelectedMealType() {
        int checkedId = binding.chipGroupManualMealType.getCheckedChipId();
        if (checkedId == R.id.chip_meal_breakfast) {
            return "Breakfast";
        } else if (checkedId == R.id.chip_meal_dinner) {
            return "Dinner";
        } else if (checkedId == R.id.chip_meal_snack) {
            return "Snack";
        }
        return "Lunch";
    }

    private double parseDouble(@Nullable Object input) {
        if (input == null) return 0.0;
        try {
            return Double.parseDouble(input.toString().trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
