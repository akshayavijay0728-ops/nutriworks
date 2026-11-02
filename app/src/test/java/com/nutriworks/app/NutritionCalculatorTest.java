package com.nutriworks.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.nutriworks.app.model.NutritionInfo;
import com.nutriworks.app.util.NutritionCalculator;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for NutritionCalculator portion and macronutrient scaling.
 */
public class NutritionCalculatorTest {

    private NutritionInfo testBiryani;
    private NutritionInfo testIdli;

    @Before
    public void setUp() {
        // Sample Biryani: 185 kcal, 8.5g protein, 22.0g carbs, 6.8g fat, 1.4g fiber, 0.8g sugar per 100g. Default 250g.
        testBiryani = new NutritionInfo(
                "biryani",
                185.0,
                8.5,
                22.0,
                6.8,
                1.4,
                0.8,
                250.0,
                "1 plate"
        );

        // Sample Idli: 143 kcal, 4.0g protein, 30.0g carbs, 0.4g fat, 1.5g fiber, 0.5g sugar per 100g. Default 80g.
        testIdli = new NutritionInfo(
                "idli",
                143.0,
                4.0,
                30.0,
                0.4,
                1.5,
                0.5,
                80.0,
                "2 pieces"
        );
    }

    @Test
    public void testZeroGramsProducesZeroNutrients() {
        NutritionCalculator.CalculatedNutrition result =
                NutritionCalculator.calculateFromGrams(testBiryani, 0.0);

        assertNotNull(result);
        assertEquals(0, result.getCalories());
        assertEquals(0.0, result.getProteinGrams(), 0.001);
        assertEquals(0.0, result.getCarbsGrams(), 0.001);
        assertEquals(0.0, result.getFatGrams(), 0.001);
        assertEquals(0.0, result.getFiberGrams(), 0.001);
        assertEquals(0.0, result.getGrams(), 0.001);
    }

    @Test
    public void testNegativeGramsSafelyClampedToZero() {
        NutritionCalculator.CalculatedNutrition result =
                NutritionCalculator.calculateFromGrams(testBiryani, -150.0);

        assertNotNull(result);
        assertEquals(0, result.getCalories());
        assertEquals(0.0, result.getProteinGrams(), 0.001);
        assertEquals(0.0, result.getCarbsGrams(), 0.001);
        assertEquals(0.0, result.getGrams(), 0.001);
    }

    @Test
    public void testStandardGramsScaling() {
        // 200g of Biryani = 2.0x factor
        NutritionCalculator.CalculatedNutrition result =
                NutritionCalculator.calculateFromGrams(testBiryani, 200.0);

        assertNotNull(result);
        assertEquals(370, result.getCalories()); // 185 * 2.0
        assertEquals(17.0, result.getProteinGrams(), 0.001); // 8.5 * 2.0
        assertEquals(44.0, result.getCarbsGrams(), 0.001); // 22.0 * 2.0
        assertEquals(13.6, result.getFatGrams(), 0.001); // 6.8 * 2.0
        assertEquals(200.0, result.getGrams(), 0.001);
        assertEquals(0.8, result.getServings(), 0.001); // 200g / 250g
    }

    @Test
    public void testLargeValuesDoNotOverflowOrProduceNaN() {
        // Test massive portion: 10,000 grams (10 kg)
        NutritionCalculator.CalculatedNutrition result =
                NutritionCalculator.calculateFromGrams(testBiryani, 10000.0);

        assertNotNull(result);
        assertEquals(18500, result.getCalories());
        assertEquals(850.0, result.getProteinGrams(), 0.001);
        assertEquals(2200.0, result.getCarbsGrams(), 0.001);
        assertTrue(result.getCalories() > 0);
        assertTrue(!Double.isNaN(result.getFatGrams()));
    }

    @Test
    public void testRoundingToOneDecimal() {
        assertEquals(12.3, NutritionCalculator.roundToOneDecimal(12.34), 0.001);
        assertEquals(12.4, NutritionCalculator.roundToOneDecimal(12.36), 0.001);
        assertEquals(0.0, NutritionCalculator.roundToOneDecimal(0.04), 0.001);
        assertEquals(0.1, NutritionCalculator.roundToOneDecimal(0.06), 0.001);
        assertEquals(0.0, NutritionCalculator.roundToOneDecimal(Double.NaN), 0.001);
    }

    @Test
    public void testCalculateFromServings() {
        // 2.0 servings of Idli (default serving = 80g, so 160g total)
        NutritionCalculator.CalculatedNutrition result =
                NutritionCalculator.calculateFromServings(testIdli, 2.0);

        assertNotNull(result);
        assertEquals(160.0, result.getGrams(), 0.001);
        assertEquals(229, result.getCalories()); // 143 * 1.6 = 228.8 -> 229
        assertEquals(6.4, result.getProteinGrams(), 0.001); // 4.0 * 1.6 = 6.4
        assertEquals(48.0, result.getCarbsGrams(), 0.001); // 30.0 * 1.6 = 48.0
        assertEquals(0.6, result.getFatGrams(), 0.001); // 0.4 * 1.6 = 0.64 -> 0.6
        assertEquals(2.0, result.getServings(), 0.001);
    }
}
