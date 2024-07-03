#Module Quant{Actions}
---
 
[![Release](https://jitpack.io/v/QuantActions/QA-Android-SDK.svg)]
(https://jitpack.io/private#QuantActions/QA-Android-SDK)
 
## Upload to bintray
```shell
./gradlew install -x javadoc
./gradlew bintrayUpload -x javadoc
```

## Extract apk from bundle
```shell
bundletool build-apks --bundle=tc-release.aab --output=tc-release.apks --ks=./../../../KEY_STORE/key_store_qa.jks  --ks-key-alias=quantactions``
bundletool install-apks --apks=tc-release.apks --device-id=emulator-1234
```

# Pipelines AZURE
Follow https://medium.com/@barcelos.ds/install-android-sdk-in-the-ubuntu-20-04-lts-without-android-studio-1b629924d6c5
- Download latest tools from https://developer.android.com/studio/index.html#command-tools
- Install gcloud tools https://cloud.google.com/sdk/docs/install#deb
```bash
mkdir Android
mkdir Android/cmdline-tools
unzip commandlinetools-linux-7583922_latest.zip
mv cmdline-tools/ tools/
mv tools ./Android/cmdline-tools/
ls -l ./Android/cmdline-tools/tools
export ANDROID_SDK_ROOT=/var/home_azureagent/Android
export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/tools/bin"

# check installation
sdkmanager --version
sdkmanager "platform-tools" "platforms;android-31"
sdkmanager "build-tools;29.0.3"
sdkmanager --licenses


```

# Create docs for SDK
I need to check if we want to do the multi-module docs, to include the example app in the docs or not 

```bash
./gradlew :qa_sdk:dokkaHtml
```

This produces the docs in the relative path `../../QA-Android-SDK-docs/docs`
use something like `python -m http.server` to serve the docs locally
This production overwrites so spec file that should be reverted (for icons and so on) 




