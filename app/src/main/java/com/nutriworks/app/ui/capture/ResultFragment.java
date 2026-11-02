package com.nutriworks.app.ui.capture;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.nutriworks.app.R;
import com.nutriworks.app.databinding.DialogManualNutritionBinding;
import com.nutriworks.app.databinding.FragmentResultBinding;
import com.nutriworks.app.ml.Prediction;
import com.nutriworks.app.model.NutritionInfo;
import com.nutriworks.app.util.ImageUtils;
import com.nutriworks.app.util.NutritionCalculator;
import com.nutriworks.app.util.Resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ResultFragment displaying food recognition predictions, interactive portion adjustment
 * (grams slider and serving stepper), live nutrition calculation, and MPAndroidChart macro breakdown.
 */
public class ResultFragment extends Fragment {

    private static final String TAG = "ResultFragment";
    public static final String ARG_IMAGE_PATH = "image_path";
    private static final float CONFIDENCE_THRESHOLD = 0.60f;

    private FragmentResultBinding binding;
    private RecognitionViewModel viewModel;
    private String imagePath;
    private Bitmap displayBitmap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentResultBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RecognitionViewModel.class);

        if (getArguments() != null) {
            imagePath = getArguments().getString(ARG_IMAGE_PATH);
        }

        loadImagePreview();
        setupPieChart();
        setupManualSearchDropdown();
        setupPortionControls();
        setupMealTypeSelector();
        setupObservers();
        setupActionButtons();

        if (imagePath != null && !imagePath.isEmpty()) {
            viewModel.classifyImage(imagePath);
        }
    }

    private void loadImagePreview() {
        if (imagePath == null) {
            return;
        }

        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            return;
        }

        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), boundsOptions);

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = ImageUtils.calculateInSampleSize(boundsOptions, 600, 600);
        decodeOptions.inJustDecodeBounds = false;

        displayBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), decodeOptions);
        if (displayBitmap != null) {
            binding.imgResultPreview.setImageBitmap(displayBitmap);
        }
    }

    private void setupPieChart() {
        PieChart chart = binding.chartMacros;
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawEntryLabels(false);
        chart.setHoleRadius(62f);
        chart.setTransparentCircleRadius(67f);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setTouchEnabled(false);
    }

    private void setupManualSearchDropdown() {
        List<String> foodLabels = loadLabelsFromAssets();
        if (getContext() != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    foodLabels
            );
            binding.autoCompleteFoodSearch.setAdapter(adapter);

            binding.autoCompleteFoodSearch.setOnItemClickListener((parent, view, position, id) -> {
                String selected = (String) parent.getItemAtPosition(position);
                viewModel.selectFoodLabel(selected);
                binding.chipGroupPredictions.clearCheck();
            });
        }
    }

    private List<String> loadLabelsFromAssets() {
        List<String> labels = new ArrayList<>();
        try (InputStream is = requireContext().getAssets().open("labels.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    Prediction p = new Prediction(line, 1.0f);
                    labels.add(p.getFormattedDisplayName());
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "Could not load labels.txt: " + e.getMessage());
        }
        return labels;
    }

    private void setupPortionControls() {
        // Grams Slider listener
        binding.sliderGrams.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                viewModel.updatePortionGrams(value);
            }
        });

        // Serving Stepper listeners
        binding.btnMinusServing.setOnClickListener(v -> {
            NutritionCalculator.CalculatedNutrition current = viewModel.getCalculatedNutrition().getValue();
            if (current != null) {
                double newServings = Math.max(0.5, current.getServings() - 0.5);
                viewModel.updatePortionServings(newServings);
            }
        });

        binding.btnPlusServing.setOnClickListener(v -> {
            NutritionCalculator.CalculatedNutrition current = viewModel.getCalculatedNutrition().getValue();
            if (current != null) {
                double newServings = current.getServings() + 0.5;
                viewModel.updatePortionServings(newServings);
            }
        });

        // Manual entry trigger
        binding.btnManualEntry.setOnClickListener(v -> {
            String currentFood = binding.textNutritionFoodName.getText().toString();
            showManualNutritionDialog(currentFood);
        });
    }

    private void setupMealTypeSelector() {
        binding.chipGroupMealType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_breakfast) {
                viewModel.setMealType("Breakfast");
            } else if (checkedId == R.id.chip_lunch) {
                viewModel.setMealType("Lunch");
            } else if (checkedId == R.id.chip_dinner) {
                viewModel.setMealType("Dinner");
            } else if (checkedId == R.id.chip_snack) {
                viewModel.setMealType("Snack");
            }
        });
    }

    private void setupObservers() {
        viewModel.getPredictions().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null || binding == null) {
                return;
            }

            switch (resource.getStatus()) {
                case LOADING:
                    binding.progressRecognition.setVisibility(View.VISIBLE);
                    break;

                case SUCCESS:
                    binding.progressRecognition.setVisibility(View.GONE);
                    populatePredictionChips(resource.getData());
                    break;

                case ERROR:
                    binding.progressRecognition.setVisibility(View.GONE);
                    Snackbar.make(binding.getRoot(),
                            resource.getMessage() != null ? resource.getMessage() : "Classification failed",
                            Snackbar.LENGTH_LONG
                    ).show();
                    break;
            }
        });

        viewModel.getInferenceTimeMs().observe(getViewLifecycleOwner(), timeMs -> {
            if (binding != null && timeMs != null) {
                String latencyText = String.format(Locale.getDefault(), getString(R.string.result_inference_format), timeMs);
                binding.chipInferenceTime.setText(latencyText);
                binding.chipInferenceTime.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getSelectedFoodLabel().observe(getViewLifecycleOwner(), label -> {
            if (binding != null && label != null) {
                binding.textNutritionFoodName.setText(label);
            }
        });

        viewModel.getNutritionInfo().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null || binding == null) {
                return;
            }

            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                NutritionInfo info = resource.getData();
                binding.layoutLiveNutrition.setVisibility(View.VISIBLE);
                binding.layoutMissingNutrition.setVisibility(View.GONE);
                binding.btnMinusServing.setEnabled(true);
                binding.btnPlusServing.setEnabled(true);
                binding.sliderGrams.setEnabled(true);
                binding.btnConfirmFood.setEnabled(true);

                String servingText = String.format(Locale.getDefault(), "%s (%.0fg)",
                        info.getServingLabel(), info.getDefaultServingGrams());
                binding.textServingLabel.setText(servingText);

            } else if (resource.getStatus() == Resource.Status.ERROR) {
                binding.layoutLiveNutrition.setVisibility(View.GONE);
                binding.layoutMissingNutrition.setVisibility(View.VISIBLE);
                binding.textServingLabel.setText("Custom Entry");
                binding.btnMinusServing.setEnabled(false);
                binding.btnPlusServing.setEnabled(false);
                binding.sliderGrams.setEnabled(false);
            }
        });

        viewModel.getCalculatedNutrition().observe(getViewLifecycleOwner(), calculated -> {
            if (calculated == null || binding == null) {
                return;
            }

            // Update live labels
            binding.textCalculatedCalories.setText(String.format(Locale.getDefault(), "%d kcal", calculated.getCalories()));
            binding.textMacroProtein.setText(String.format(Locale.getDefault(), "Protein: %.1fg", calculated.getProteinGrams()));
            binding.textMacroCarbs.setText(String.format(Locale.getDefault(), "Carbs: %.1fg", calculated.getCarbsGrams()));
            binding.textMacroFat.setText(String.format(Locale.getDefault(), "Fat: %.1fg", calculated.getFatGrams()));
            binding.textMacroFiberSugar.setText(String.format(Locale.getDefault(), "Fiber: %.1fg  •  Sugar: %.1fg",
                    calculated.getFiberGrams(), calculated.getSugarGrams()));

            binding.textServingStepperValue.setText(String.format(Locale.getDefault(), "%.1f serving", calculated.getServings()));
            binding.textGramsLabel.setText(String.format(Locale.getDefault(), "%.0fg", calculated.getGrams()));

            // Update slider without re-triggering listener loops
            float clampedGrams = (float) Math.min(800.0, Math.max(10.0, calculated.getGrams()));
            binding.sliderGrams.setValue(clampedGrams);

            // Update PieChart
            updateMacroPieChart(calculated.getProteinGrams(), calculated.getCarbsGrams(), calculated.getFatGrams());
        });
    }

    private void updateMacroPieChart(double protein, double carbs, double fat) {
        List<PieEntry> entries = new ArrayList<>();
        float p = (float) Math.max(0.0, protein);
        float c = (float) Math.max(0.0, carbs);
        float f = (float) Math.max(0.0, fat);

        if (p + c + f == 0) {
            entries.add(new PieEntry(1f, ""));
        } else {
            entries.add(new PieEntry(p, "Protein"));
            entries.add(new PieEntry(c, "Carbs"));
            entries.add(new PieEntry(f, "Fat"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Macronutrients");
        dataSet.setDrawValues(false);

        List<Integer> colors = new ArrayList<>();
        colors.add(ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary));   // Green
        colors.add(ContextCompat.getColor(requireContext(), R.color.md_theme_light_secondary)); // Orange
        colors.add(ContextCompat.getColor(requireContext(), R.color.md_theme_light_tertiary));  // Teal/Blue
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        binding.chartMacros.setData(data);
        binding.chartMacros.invalidate();
    }

    private void populatePredictionChips(@Nullable List<Prediction> predictions) {
        if (binding == null || predictions == null || predictions.isEmpty()) {
            return;
        }

        binding.chipGroupPredictions.removeAllViews();

        float topConfidence = predictions.get(0).getConfidence();
        boolean isLowConfidence = topConfidence < CONFIDENCE_THRESHOLD;
        binding.cardLowConfidence.setVisibility(isLowConfidence ? View.VISIBLE : View.GONE);

        for (int i = 0; i < predictions.size(); i++) {
            Prediction prediction = predictions.get(i);
            Chip chip = new Chip(requireContext());
            chip.setText(String.format(Locale.getDefault(), "%s  •  %s",
                    prediction.getFormattedDisplayName(),
                    prediction.getConfidencePercentage()
            ));
            chip.setCheckable(true);
            chip.setClickable(true);

            final String displayName = prediction.getFormattedDisplayName();
            chip.setOnClickListener(v -> viewModel.selectFoodLabel(displayName));

            binding.chipGroupPredictions.addView(chip);

            if (i == 0) {
                chip.setChecked(true);
                viewModel.selectFoodLabel(displayName);
            }
        }
    }

    private void showManualNutritionDialog(@NonNull String foodName) {
        DialogManualNutritionBinding dialogBinding =
                DialogManualNutritionBinding.inflate(getLayoutInflater());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_manual_nutrition_title))
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.dialog_btn_save, (dialog, which) -> {
                    double kcal = parseDoubleOrZero(dialogBinding.editInputKcal.getText() != null ? dialogBinding.editInputKcal.getText().toString() : "");
                    double protein = parseDoubleOrZero(dialogBinding.editInputProtein.getText() != null ? dialogBinding.editInputProtein.getText().toString() : "");
                    double carbs = parseDoubleOrZero(dialogBinding.editInputCarbs.getText() != null ? dialogBinding.editInputCarbs.getText().toString() : "");
                    double fat = parseDoubleOrZero(dialogBinding.editInputFat.getText() != null ? dialogBinding.editInputFat.getText().toString() : "");

                    viewModel.applyManualNutrition(foodName, kcal, protein, carbs, fat);
                })
                .setNegativeButton(R.string.permission_btn_cancel, null)
                .show();
    }

    private double parseDoubleOrZero(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void setupActionButtons() {
        binding.btnAddToLog.setOnClickListener(v -> {
            NutritionCalculator.CalculatedNutrition calc = viewModel.getCalculatedNutrition().getValue();
            String mealType = viewModel.getSelectedMealType().getValue();
            String food = binding.textNutritionFoodName.getText().toString();

            if (calc != null) {
                viewModel.saveMealEntry(
                        food,
                        calc.getGrams(),
                        calc.getCalories(),
                        calc.getProtein(),
                        calc.getCarbs(),
                        calc.getFat(),
                        mealType != null ? mealType : "Lunch",
                        imagePath,
                        rowId -> {
                            if (getActivity() != null) {
                                requireActivity().runOnUiThread(() -> {
                                    if (isAdded() && getView() != null) {
                                        Toast.makeText(requireContext(),
                                                String.format(Locale.getDefault(), "Logged %.0fg %s (%d kcal) as %s!",
                                                        calc.getGrams(), food, calc.getCalories(), mealType != null ? mealType : "Meal"),
                                                Toast.LENGTH_SHORT).show();
                                        Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
                                    }
                                });
                            }
                        }
                );
            }
        });

        binding.btnRetakePhoto.setOnClickListener(v -> {
            if (isAdded() && getView() != null) {
                Navigation.findNavController(requireView()).popBackStack(R.id.navigation_capture, false);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.imgFoodPreview.setImageBitmap(null);
            binding.chartMacroPie.clear();
        }
        if (displayBitmap != null && !displayBitmap.isRecycled()) {
            displayBitmap.recycle();
            displayBitmap = null;
        }
        binding = null;
    }
}
