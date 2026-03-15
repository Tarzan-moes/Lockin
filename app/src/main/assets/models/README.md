# AI Model Assets

This directory is for smaller TFLite models (exercise classifier, etc.).

## Large Model Files (Gemma LLM)

The Gemma 2B model (`gemma-2b-it-gpu-int4.bin`, ~1.4 GB) is **too large for APK assets**.
It must be placed in the app's private files directory at runtime.

### Setup via ADB (Development)

```bash
# Create the models directory on device
adb shell mkdir -p /data/data/com.lockin.app/files/models/

# Push the model file
adb push "C:\Users\Mustafa\Downloads\gemma-2b-it-gpu-int4.bin" /data/data/com.lockin.app/files/models/
```

### Setup via Downloads (Device)

1. Copy `gemma-2b-it-gpu-int4.bin` to the device's Download folder
2. The app will attempt to auto-import it on first launch of the AI Coach screen

## Smaller Models (Assets)

| File | Description | Size |
|------|-------------|------|
| `exercise_classifier.tflite` | Exercise classification from pose landmarks | ~50MB |

## Notes
- The app gracefully falls back to rule-based responses when model files are missing
- `.tflite` and `.bin` files are NOT committed to version control (see .gitignore)
- For Samsung S24 Ultra, INT4 GPU quantization provides the best speed/quality balance
- The MediaPipe LLM Inference API handles tokenization internally for `.bin` models
