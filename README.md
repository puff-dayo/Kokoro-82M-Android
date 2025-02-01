# Kokoro-82M-Android
A minimal Android demo app for **Kokoro-82M** TTS model in int8 quantization.

Kokoro-82M origin model: https://huggingface.co/hexgrad/Kokoro-82M<br>
Kokoro-82M int8-quant model: https://github.com/thewh1teagle/kokoro-onnx

Many features of the original model have not been implemented. This is just a simple Android on-device inference demo and also the first Android app I have developed. :)

A huge thank you to the open-source projects and communities that made this project possible. Your contributions are invaluable.

## Screenshot(s)
<p align="center">
  <img src="https://github.com/user-attachments/assets/81736bf6-cc51-4aae-aeb2-daae648fb7f9" width="25%" />
</p>


## How to Build
Android Studio Ladybug
Gradle Sync and Build

## Prebuilt apk files
See [release](https://github.com/puff-dayo/Kokoro-82M-Android/releases/).

## TODO

- Add a voice style mixer [Done]
- Update to [kokoro-onnx v1](https://github.com/thewh1teagle/kokoro-onnx/releases/tag/model-files-v1.0)
- Multi language support (might need a bit refactoring...)
