"""
NutriWorks - MobileNetV2 Transfer Learning & TFLite Model Export
Google Colab Notebook Script
================================================================================
Datasets:
1. Food-101: 101 Western/Global food categories (101,000 images).
   Source: https://data.vision.ee.ethz.ch/cvl/datasets_extra/food-101/
   (Accessible via tfds.load('food101') or Kaggle: danaelifazelm/food-101)
2. Indian Food Images: Authentic regional Indian dishes (Biryani, Dosa, Idli,
   Samosa, Paneer Tikka, Dal Makhani, Naan, Chana Masala, Gulab Jamun, etc.).
   Source: Kaggle iamsouravbanerjee/indian-food-images-dataset

Pipeline Overview:
- Dataset unification & stratified split (70% train, 15% val, 15% test).
- Data augmentation (RandomFlip, RandomRotation, RandomZoom, RandomContrast).
- MobileNetV2 backbone (transfer learning + top-layer fine-tuning).
- Evaluation: Top-1 accuracy, Top-5 accuracy, classification report & confusion matrix.
- Post-training INT8 / Dynamic range quantization.
- TFLite metadata injection via tflite-support for Android Task Vision ImageClassifier.
================================================================================
"""

import os
import shutil
import pathlib
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.metrics import classification_report, confusion_matrix

import tensorflow as tf
from tensorflow.keras import layers, models, callbacks
from tensorflow.keras.applications.mobilenet_v2 import MobileNetV2, preprocess_input

# -----------------------------------------------------------------------------
# 1. SETUP & HARDWARE ACCELERATION
# -----------------------------------------------------------------------------
print("TensorFlow Version:", tf.__version__)
gpus = tf.config.list_physical_devices('GPU')
if gpus:
    print(f"GPU Detected: {gpus[0].name}. Using mixed precision.")
    tf.keras.mixed_precision.set_global_policy('mixed_float16')
else:
    print("No GPU detected; running on CPU.")

IMAGE_SIZE = (224, 224)
BATCH_SIZE = 32
RANDOM_SEED = 42

# -----------------------------------------------------------------------------
# 2. DATASET MERGING LOGIC
# -----------------------------------------------------------------------------
"""
To merge Food-101 and Indian Food Images:
1. Download Food-101 and Indian Food images to /content/raw_datasets/
2. Standardize all directory names to lowercase snake_case (e.g. 'Paneer Tikka' -> 'paneer_tikka').
3. Create unified directories: /content/unified_dataset/{train, val, test}/{class_name}/
"""

def prepare_merged_dataset(food101_dir: str, indian_food_dir: str, target_dir: str):
    """
    Merges Food-101 and Indian Food datasets into a single balanced dataset
    split into train (70%), val (15%), and test (15%).
    """
    os.makedirs(target_dir, exist_ok=True)
    all_source_dirs = [food101_dir, indian_food_dir]

    for source_dir in all_source_dirs:
        if not os.path.exists(source_dir):
            continue
        for class_name in os.listdir(source_dir):
            clean_class_name = class_name.strip().lower().replace(" ", "_").replace("-", "_")
            src_class_path = os.path.join(source_dir, class_name)
            if not os.path.isdir(src_class_path):
                continue

            images = [os.path.join(src_class_path, img) for img in os.listdir(src_class_path)
                      if img.lower().endswith(('.jpg', '.jpeg', '.png'))]
            np.random.seed(RANDOM_SEED)
            np.random.shuffle(images)

            n_total = len(images)
            n_train = int(n_total * 0.70)
            n_val = int(n_total * 0.15)

            splits = {
                'train': images[:n_train],
                'val': images[n_train:n_train + n_val],
                'test': images[n_train + n_val:]
            }

            for split_name, split_files in splits.items():
                split_class_dir = os.path.join(target_dir, split_name, clean_class_name)
                os.makedirs(split_class_dir, exist_ok=True)
                for f in split_files:
                    dest = os.path.join(split_class_dir, os.path.basename(f))
                    if not os.path.exists(dest):
                        shutil.copy2(f, dest)

    print(f"Unified dataset successfully prepared at: {target_dir}")

# Example invocation path (customize based on your Google Drive / Kaggle download location):
DATASET_ROOT = "/content/unified_food_dataset"
TRAIN_DIR = os.path.join(DATASET_ROOT, "train")
VAL_DIR = os.path.join(DATASET_ROOT, "val")
TEST_DIR = os.path.join(DATASET_ROOT, "test")

# -----------------------------------------------------------------------------
# 3. TF.DATA PIPELINE WITH DATA AUGMENTATION
# -----------------------------------------------------------------------------
data_augmentation = tf.keras.Sequential([
    layers.RandomFlip("horizontal"),
    layers.RandomRotation(0.15),
    layers.RandomZoom(0.10),
    layers.RandomContrast(0.10),
], name="data_augmentation")

def build_data_pipeline(data_dir: str, is_training: bool = False):
    dataset = tf.keras.preprocessing.image_dataset_from_directory(
        data_dir,
        image_size=IMAGE_SIZE,
        batch_size=BATCH_SIZE,
        label_mode='categorical',
        shuffle=is_training,
        seed=RANDOM_SEED
    )

    class_names = dataset.class_names
    autotune = tf.data.AUTOTUNE

    if is_training:
        dataset = dataset.map(lambda x, y: (data_augmentation(x, training=True), y),
                              num_parallel_calls=autotune)

    dataset = dataset.map(lambda x, y: (preprocess_input(x), y), num_parallel_calls=autotune)
    dataset = dataset.prefetch(buffer_size=autotune)
    return dataset, class_names

# When testing without full dataset directories, fallback to a dummy dataset generator:
if os.path.exists(TRAIN_DIR) and os.path.exists(VAL_DIR):
    train_ds, class_names = build_data_pipeline(TRAIN_DIR, is_training=True)
    val_ds, _ = build_data_pipeline(VAL_DIR, is_training=False)
    test_ds, _ = build_data_pipeline(TEST_DIR, is_training=False)
    num_classes = len(class_names)
else:
    print("Dataset directory not found. Creating simulated dataset for demonstration...")
    num_classes = 105
    class_names = [f"food_class_{i}" for i in range(num_classes)]
    # Simulated pipeline
    dummy_x = tf.random.uniform((64, 224, 224, 3), 0, 255)
    dummy_y = tf.one_hot(tf.random.uniform((64,), 0, num_classes, dtype=tf.int32), num_classes)
    train_ds = tf.data.Dataset.from_tensor_slices((dummy_x, dummy_y)).batch(BATCH_SIZE)
    val_ds = train_ds
    test_ds = train_ds

# -----------------------------------------------------------------------------
# 4. MODEL ARCHITECTURE (TRANSFER LEARNING)
# -----------------------------------------------------------------------------
def build_food_model(num_classes: int):
    base_model = MobileNetV2(
        input_shape=(224, 224, 3),
        include_top=False,
        weights='imagenet'
    )
    base_model.trainable = False  # Freeze base during phase 1

    inputs = tf.keras.Input(shape=(224, 224, 3))
    x = base_model(inputs, training=False)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dropout(0.25)(x)
    outputs = layers.Dense(num_classes, activation='softmax', dtype='float32')(x)

    model = models.Model(inputs, outputs, name="NutriWorks_Food_Classifier")
    return model, base_model

model, base_model = build_food_model(num_classes)
model.summary()

# -----------------------------------------------------------------------------
# 5. TWO-PHASE TRAINING (FROZEN -> FINE-TUNING)
# -----------------------------------------------------------------------------
top5_acc = tf.keras.metrics.TopKCategoricalAccuracy(k=5, name="top_5_accuracy")

model.compile(
    optimizer=tf.keras.optimizers.Adam(learning_rate=1e-3),
    loss='categorical_crossentropy',
    metrics=['accuracy', top5_acc]
)

early_stopping = callbacks.EarlyStopping(
    monitor='val_loss',
    patience=4,
    restore_best_weights=True,
    verbose=1
)

reduce_lr = callbacks.ReduceLROnPlateau(
    monitor='val_loss',
    factor=0.2,
    patience=2,
    min_lr=1e-6,
    verbose=1
)

print("\n--- Phase 1: Training Classification Head ---")
history_phase1 = model.fit(
    train_ds,
    validation_data=val_ds,
    epochs=10,
    callbacks=[early_stopping, reduce_lr]
)

print("\n--- Phase 2: Fine-Tuning Top Layers of MobileNetV2 ---")
base_model.trainable = True
# Freeze the first 100 layers and unfreeze the top layers
for layer in base_model.layers[:100]:
    layer.trainable = False

model.compile(
    optimizer=tf.keras.optimizers.Adam(learning_rate=1e-5),
    loss='categorical_crossentropy',
    metrics=['accuracy', top5_acc]
)

history_phase2 = model.fit(
    train_ds,
    validation_data=val_ds,
    epochs=15,
    callbacks=[early_stopping, reduce_lr]
)

# -----------------------------------------------------------------------------
# 6. EVALUATION & CONFUSION MATRIX
# -----------------------------------------------------------------------------
print("\n--- Evaluating Model on Test Set ---")
results = model.evaluate(test_ds)
print(f"Test Loss: {results[0]:.4f}")
print(f"Test Top-1 Accuracy: {results[1] * 100:.2f}%")
print(f"Test Top-5 Accuracy: {results[2] * 100:.2f}%")

# Generate Predictions for Confusion Matrix
y_true = []
y_pred = []
for images, labels in test_ds:
    preds = model.predict(images, verbose=0)
    y_true.extend(np.argmax(labels.numpy(), axis=1))
    y_pred.extend(np.argmax(preds, axis=1))

y_true = np.array(y_true)
y_pred = np.array(y_pred)

cm = confusion_matrix(y_true, y_pred)
plt.figure(figsize=(14, 12))
# Plot first 20 classes for visualization clarity
sns.heatmap(cm[:20, :20], annot=True, fmt='d', cmap='Greens',
            xticklabels=class_names[:20], yticklabels=class_names[:20])
plt.xlabel('Predicted Label')
plt.ylabel('True Label')
plt.title('Food Classification Confusion Matrix (Sample 20 Classes)')
plt.tight_layout()
plt.savefig("confusion_matrix.png", dpi=300)
print("Saved confusion matrix plot to confusion_matrix.png")

# -----------------------------------------------------------------------------
# 7. POST-TRAINING QUANTIZATION & TFLITE EXPORT
# -----------------------------------------------------------------------------
print("\n--- Converting to TensorFlow Lite with Post-Training Quantization ---")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# Representative dataset for optimal INT8 calibration
def representative_dataset_gen():
    for images, _ in train_ds.take(50):
        yield [tf.cast(images, tf.float32)]

converter.representative_dataset = representative_dataset_gen
tflite_model_quant = converter.convert()

tflite_filename = "model.tflite"
with open(tflite_filename, "wb") as f:
    f.write(tflite_model_quant)
print(f"Successfully exported {tflite_filename} ({len(tflite_model_quant) / (1024 * 1024):.2f} MB)")

labels_filename = "labels.txt"
with open(labels_filename, "w") as f:
    for label in class_names:
        f.write(f"{label}\n")
print(f"Successfully exported {labels_filename} ({len(class_names)} classes)")

# -----------------------------------------------------------------------------
# 8. INJECT TFLITE METADATA FOR ANDROID TASK VISION
# -----------------------------------------------------------------------------
try:
    from tflite_support.metadata_writers import image_classifier
    from tflite_support.metadata_writers import writer_utils

    writer = image_classifier.MetadataWriter.create_for_inference(
        writer_utils.load_file(tflite_filename),
        input_norm_mean=[127.5, 127.5, 127.5],
        input_norm_std=[127.5, 127.5, 127.5],
        label_file_paths=[labels_filename]
    )

    tflite_with_metadata = writer.populate()
    with open(tflite_filename, "wb") as f:
        f.write(tflite_with_metadata)
    print("TFLite Task Vision metadata attached successfully.")
except ImportError:
    print("tflite-support library not found. Run 'pip install tflite-support' to embed metadata.")

print("\nModel training and packaging completed. Copy model.tflite and labels.txt into Android app/src/main/assets/")
