from google.colab import drive
drive.mount('/content/drive')



dataset_path = '/content/drive/My Drive/puzzledataset'



!cp -r '/content/drive/My Drive/puzzledataset' /content/



!pip install tensorflow
!pip install numpy
!pip install matplotlib



import tensorflow as tf
from tensorflow.keras import datasets, layers, models
import numpy as np
import matplotlib.pyplot as plt
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from tensorflow.keras.applications.mobilenet import preprocess_input
import cv2
import imagehash
from PIL import Image




def custom_preprocessing(img):

    img = preprocess_input(img)  # MobileNet's standard preprocessing

    # Add sharpening to emphasize numbers/differences
    kernel = np.array([[0, -1, 0], [-1, 5, -1], [0, -1, 0]])
    img = cv2.filter2D(img, -1, kernel)

    return img




datagen = ImageDataGenerator(
    preprocessing_function=custom_preprocessing,
    shear_range=0.2,  # Better for puzzles than shear
    rotation_range= 15,
    zoom_range=0.15,    # Reduced zoom to preserve numbers
    horizontal_flip=False,  # Often bad for puzzles
    vertical_flip=False,
    brightness_range=[0.9, 1.1],
    validation_split=0.2
)
train_data = datagen.flow_from_directory(
    '/content/puzzledataset/',
    target_size=(224, 224),
    batch_size=32,
    class_mode='categorical',
    subset='training',
    shuffle=True,
    seed=42
)
val_data = datagen.flow_from_directory(
    '/content/puzzledataset/',
    target_size=(224, 224),
    batch_size=32,
    class_mode='categorical',
    subset='validation',
    shuffle=True,
    seed=42
)



print("Training class counts:", np.bincount(train_data.classes))
print("Validation class counts:", np.bincount(val_data.classes))



x_batch, y_batch = next(train_data)
plt.figure(figsize=(12, 6))
for i in range(6):
    plt.subplot(2, 3, i+1)
    plt.imshow((x_batch[i] + 1) / 2)  # Undo MobileNet preprocessing for visualisation
    plt.title(f"Class: {np.argmax(y_batch[i])}")
    plt.axis('off')
plt.show()




from tensorflow.keras.models import Model
from tensorflow.keras.layers import Dropout, BatchNormalization

base_model = MobileNet(weights='imagenet', include_top=False, input_shape=(224, 224, 3))

# Fine-tune more layers for subtle feature detection
base_model.trainable = True
for layer in base_model.layers[:-8]:
    layer.trainable = False

# Add attention to important features
x = base_model.output
x = GlobalAveragePooling2D()(x)
x = Dense(256, activation='relu')(x)
x = Dropout(0.3)(x)  # Prevent overfitting to minor differences
x = BatchNormalization()(x)
predictions = Dense(3, activation='softmax')(x)

model = Model(inputs=base_model.input, outputs=predictions)

# Custom learning rate schedule
initial_learning_rate = 1e-4
lr_schedule = tf.keras.optimizers.schedules.ExponentialDecay(
    initial_learning_rate,
    decay_steps=100,
    decay_rate=0.96,
    staircase=True)

model.compile(optimizer=Adam(learning_rate=lr_schedule),
              loss='categorical_crossentropy',
              metrics=['accuracy',
                      tf.keras.metrics.TopKCategoricalAccuracy(k=2, name='top2_accuracy')])





from tensorflow.keras.callbacks import EarlyStopping

callbacks = [
    EarlyStopping(monitor='val_top2_accuracy', patience=7, mode='max'),
    tf.keras.callbacks.ModelCheckpoint('best_model.h5',
                                     save_best_only=True,
                                     monitor='val_top2_accuracy'),
    tf.keras.callbacks.TensorBoard(log_dir='./logs')
]

# Training with validation
history = model.fit(
    train_data,
    epochs=10,  # Increased for fine-tuning
    validation_data=val_data,
    steps_per_epoch=train_data.samples // 32,
    validation_steps=val_data.samples // 32,
    callbacks=callbacks
)




plt.figure(figsize=(12, 5))
plt.subplot(1, 2, 1)
plt.plot(history.history['top2_accuracy'], label='Train Top2 Acc')
plt.plot(history.history['val_top2_accuracy'], label='Val Top2 Acc')
plt.title('Top-2 Accuracy')
plt.legend()

plt.subplot(1, 2, 2)
plt.plot(history.history['loss'], label='Train Loss')
plt.plot(history.history['val_loss'], label='Val Loss')
plt.title('Loss')
plt.legend()
plt.show()




from tensorflow.keras.models import load_model

model = load_model('best_model.h5')




import tensorflow as tf

# Convert to TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# Save the TFLite model to a file
with open('model.tflite', 'wb') as f:
    f.write(tflite_model)