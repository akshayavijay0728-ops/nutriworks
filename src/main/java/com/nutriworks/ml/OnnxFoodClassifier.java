package com.nutriworks.ml;

import ai.onnxruntime.*;
import com.nutriworks.model.Prediction;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * High-performance on-device ONNX Runtime food image classifier.
 * Preprocesses input images to 224x224 RGB tensors, executes MobileNetV2 graph,
 * applies softmax normalization, and returns top-K predictions.
 */
public class OnnxFoodClassifier implements FoodClassifier {

    private static final int INPUT_SIZE = 224;
    private OrtEnvironment env;
    private OrtSession session;
    private final List<String> labels = new ArrayList<>();
    private final StubFoodClassifier fallbackStub = new StubFoodClassifier();

    public OnnxFoodClassifier(File modelFile) {
        loadLabels();
        if (modelFile != null && modelFile.exists() && modelFile.length() > 1024) {
            try {
                this.env = OrtEnvironment.getEnvironment();
                OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
                opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);
                this.session = env.createSession(modelFile.getAbsolutePath(), opts);
            } catch (Exception e) {
                System.err.println("Could not initialize ONNX session: " + e.getMessage() + ". Using stub fallback.");
                this.session = null;
            }
        }
    }

    private void loadLabels() {
        InputStream is = getClass().getResourceAsStream("/labels.txt");
        if (is != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        labels.add(line.replace("_", " "));
                    }
                }
            } catch (IOException ignored) {
            }
        }

        if (labels.isEmpty()) {
            labels.addAll(Arrays.asList("idli", "dosa", "biryani", "samosa", "pizza", "caesar salad"));
        }
    }

    @Override
    public List<Prediction> classify(BufferedImage image, int topK) {
        if (session == null || env == null) {
            return fallbackStub.classify(image, topK);
        }

        try {
            // Resize to 224x224
            BufferedImage resized = new BufferedImage(INPUT_SIZE, INPUT_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.drawImage(image.getScaledInstance(INPUT_SIZE, INPUT_SIZE, Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();

            // Prepare NCHW float tensor (1, 3, 224, 224)
            float[][][][] inputTensor = new float[1][3][INPUT_SIZE][INPUT_SIZE];
            for (int y = 0; y < INPUT_SIZE; y++) {
                for (int x = 0; x < INPUT_SIZE; x++) {
                    int rgb = resized.getRGB(x, y);
                    float r = ((rgb >> 16) & 0xFF) / 255.0f;
                    float gVal = ((rgb >> 8) & 0xFF) / 255.0f;
                    float b = (rgb & 0xFF) / 255.0f;

                    // ImageNet normalization
                    inputTensor[0][0][y][x] = (r - 0.485f) / 0.229f;
                    inputTensor[0][1][y][x] = (gVal - 0.456f) / 0.224f;
                    inputTensor[0][2][y][x] = (b - 0.406f) / 0.225f;
                }
            }

            String inputName = session.getInputNames().iterator().next();
            try (OnnxTensor tensor = OnnxTensor.createTensor(env, inputTensor);
                 OrtSession.Result result = session.run(Collections.singletonMap(inputName, tensor))) {

                float[][] output = (float[][]) result.get(0).getValue();
                float[] logits = output[0];
                float[] probs = softmax(logits);

                PriorityQueue<Prediction> pq = new PriorityQueue<>(Collections.reverseOrder());
                for (int i = 0; i < Math.min(probs.length, labels.size()); i++) {
                    pq.add(new Prediction(labels.get(i), probs[i]));
                }

                List<Prediction> list = new ArrayList<>();
                for (int i = 0; i < topK && !pq.isEmpty(); i++) {
                    list.add(pq.poll());
                }
                return list;
            }
        } catch (Exception e) {
            System.err.println("ONNX inference exception: " + e.getMessage() + ". Falling back to stub.");
            return fallbackStub.classify(image, topK);
        }
    }

    private float[] softmax(float[] logits) {
        float max = Float.NEGATIVE_INFINITY;
        for (float v : logits) {
            if (v > max) max = v;
        }

        float sum = 0.0f;
        float[] exp = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exp[i] = (float) Math.exp(logits[i] - max);
            sum += exp[i];
        }

        if (sum == 0.0f) sum = 1.0f;
        for (int i = 0; i < exp.length; i++) {
            exp[i] /= sum;
        }
        return exp;
    }

    @Override
    public void close() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (Exception ignored) {
        }
    }
}
