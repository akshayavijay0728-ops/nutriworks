package com.nutriworks.app.ui.capture;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.nutriworks.app.data.db.MealEntry;
import com.nutriworks.app.data.repository.MealRepository;
import com.nutriworks.app.data.repository.NutritionRepository;
import com.nutriworks.app.ml.FoodClassifier;
import com.nutriworks.app.ml.Prediction;
import com.nutriworks.app.model.NutritionInfo;
import com.nutriworks.app.util.ImageUtils;
import com.nutriworks.app.util.NutritionCalculator;
import com.nutriworks.app.util.Resource;

import java.io.File;
import java.util.List;

/**
 * ViewModel managing food recognition state, nutrition lookup via NutritionRepository,
 * dynamic calorie / portion calculations, and persistence via MealRepository into Room.
 */
public class RecognitionViewModel extends AndroidViewModel {

    private static final String TAG = "RecognitionViewModel";

    private final FoodClassifier foodClassifier;
    private final NutritionRepository nutritionRepository;
    private final MealRepository mealRepository;

    private final MutableLiveData<Resource<List<Prediction>>> predictionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Long> inferenceTimeMsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> selectedFoodLabelLiveData = new MutableLiveData<>();

    private final MutableLiveData<Resource<NutritionInfo>> nutritionInfoLiveData = new MutableLiveData<>();
    private final MutableLiveData<NutritionCalculator.CalculatedNutrition> calculatedNutritionLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> portionGramsLiveData = new MutableLiveData<>(100.0);
    private final MutableLiveData<String> selectedMealTypeLiveData = new MutableLiveData<>("Lunch");

    private NutritionInfo currentNutritionInfo = null;

    public RecognitionViewModel(@NonNull Application application) {
        super(application);
        this.foodClassifier = new FoodClassifier(application);
        this.nutritionRepository = new NutritionRepository(application);
        this.mealRepository = new MealRepository(application);
    }

    @NonNull
    public LiveData<Resource<List<Prediction>>> getPredictions() {
        return predictionsLiveData;
    }

    @NonNull
    public LiveData<Long> getInferenceTimeMs() {
        return inferenceTimeMsLiveData;
    }

    @NonNull
    public LiveData<String> getSelectedFoodLabel() {
        return selectedFoodLabelLiveData;
    }

    @NonNull
    public LiveData<Resource<NutritionInfo>> getNutritionInfo() {
        return nutritionInfoLiveData;
    }

    @NonNull
    public LiveData<NutritionCalculator.CalculatedNutrition> getCalculatedNutrition() {
        return calculatedNutritionLiveData;
    }

    @NonNull
    public LiveData<Double> getPortionGrams() {
        return portionGramsLiveData;
    }

    @NonNull
    public LiveData<String> getSelectedMealType() {
        return selectedMealTypeLiveData;
    }

    public void selectFoodLabel(@NonNull String label) {
        selectedFoodLabelLiveData.setValue(label);
        loadNutritionForFood(label);
    }

    public void setMealType(@NonNull String mealType) {
        selectedMealTypeLiveData.setValue(mealType);
    }

    /**
     * Resolves nutrition data for the selected food label from the repository.
     */
    public void loadNutritionForFood(@NonNull String foodName) {
        nutritionInfoLiveData.setValue(Resource.loading());

        nutritionRepository.getNutritionInfo(foodName, new NutritionRepository.NutritionCallback() {
            @Override
            public void onLoaded(@NonNull NutritionInfo info) {
                currentNutritionInfo = info;
                nutritionInfoLiveData.postValue(Resource.success(info));

                // Default portion to the food's default serving size
                double defaultGrams = info.getDefaultServingGrams() > 0 ? info.getDefaultServingGrams() : 100.0;
                portionGramsLiveData.postValue(defaultGrams);

                NutritionCalculator.CalculatedNutrition calc =
                        NutritionCalculator.calculateFromGrams(info, defaultGrams);
                calculatedNutritionLiveData.postValue(calc);
            }

            @Override
            public void onNotFound(@NonNull String foodName) {
                currentNutritionInfo = null;
                nutritionInfoLiveData.postValue(Resource.error(
                        "No nutrition data found for '" + foodName + "'. Please enter manually.",
                        null
                ));
            }
        });
    }

    /**
     * Updates portion by grams and recalculates live macronutrients.
     */
    public void updatePortionGrams(double grams) {
        portionGramsLiveData.setValue(grams);
        if (currentNutritionInfo != null) {
            NutritionCalculator.CalculatedNutrition calc =
                    NutritionCalculator.calculateFromGrams(currentNutritionInfo, grams);
            calculatedNutritionLiveData.setValue(calc);
        }
    }

    /**
     * Updates portion by serving count (e.g. 1.0, 2.0).
     */
    public void updatePortionServings(double servings) {
        if (currentNutritionInfo != null) {
            NutritionCalculator.CalculatedNutrition calc =
                    NutritionCalculator.calculateFromServings(currentNutritionInfo, servings);
            portionGramsLiveData.setValue(calc.getGrams());
            calculatedNutritionLiveData.setValue(calc);
        }
    }

    /**
     * Saves user-entered manual nutrition values when a food is not in the database.
     */
    public void applyManualNutrition(@NonNull String foodName,
                                     double kcalPer100g,
                                     double proteinPer100g,
                                     double carbsPer100g,
                                     double fatPer100g) {
        NutritionInfo customInfo = new NutritionInfo(
                foodName,
                kcalPer100g,
                proteinPer100g,
                carbsPer100g,
                fatPer100g,
                0.0,
                0.0,
                100.0,
                "1 serving"
        );
        nutritionRepository.saveCustomFood(customInfo);
        currentNutritionInfo = customInfo;
        nutritionInfoLiveData.setValue(Resource.success(customInfo));

        Double currentGrams = portionGramsLiveData.getValue();
        double grams = currentGrams != null ? currentGrams : 100.0;
        NutritionCalculator.CalculatedNutrition calc =
                NutritionCalculator.calculateFromGrams(customInfo, grams);
        calculatedNutritionLiveData.setValue(calc);
    }

    /**
     * Persists the recognized meal with portion, calculated macros, and photo into Room.
     */
    public void saveMealEntry(@NonNull String foodName,
                              double grams,
                              int calories,
                              double protein,
                              double carbs,
                              double fat,
                              @NonNull String mealType,
                              @Nullable String imagePath,
                              @Nullable MealRepository.OnOperationCompleteCallback callback) {
        long timestamp = System.currentTimeMillis();
        MealEntry entry = new MealEntry(
                foodName,
                grams,
                calories,
                protein,
                carbs,
                fat,
                mealType,
                timestamp,
                imagePath
        );
        mealRepository.insertMeal(entry, callback);
    }

    /**
     * Submits image to FoodClassifier.
     */
    public void classifyImage(@NonNull String imagePath) {
        predictionsLiveData.setValue(Resource.loading());

        new Thread(() -> {
            File imageFile = new File(imagePath);
            if (!imageFile.exists() || !imageFile.canRead()) {
                predictionsLiveData.postValue(Resource.error("Image file not accessible.", null));
                return;
            }

            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), boundsOptions);

            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = ImageUtils.calculateInSampleSize(boundsOptions, 224, 224);
            decodeOptions.inJustDecodeBounds = false;

            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), decodeOptions);
            if (bitmap == null) {
                predictionsLiveData.postValue(Resource.error("Failed to decode image for recognition.", null));
                return;
            }

            Bitmap modelInputBitmap = ImageUtils.createModelInputBitmap(bitmap, 224);
            if (bitmap != modelInputBitmap && !bitmap.isRecycled()) {
                bitmap.recycle();
            }

            foodClassifier.classify(modelInputBitmap, new FoodClassifier.ClassificationCallback() {
                @Override
                public void onSuccess(@NonNull List<Prediction> predictions, long inferenceTimeMs) {
                    inferenceTimeMsLiveData.postValue(inferenceTimeMs);
                    predictionsLiveData.postValue(Resource.success(predictions));

                    if (!predictions.isEmpty()) {
                        String topLabel = predictions.get(0).getFormattedDisplayName();
                        selectedFoodLabelLiveData.postValue(topLabel);
                        loadNutritionForFood(topLabel);
                    }

                    if (!modelInputBitmap.isRecycled()) {
                        modelInputBitmap.recycle();
                    }
                }

                @Override
                public void onError(@NonNull String errorMessage) {
                    predictionsLiveData.postValue(Resource.error(errorMessage, null));
                    if (!modelInputBitmap.isRecycled()) {
                        modelInputBitmap.recycle();
                    }
                }
            });
        }).start();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        foodClassifier.close();
    }
}
