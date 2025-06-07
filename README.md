# CheckInApp - Android Check-in Application

แอปพลิเคชัน Android สำหรับระบบ Check-in ที่มีฟีเจอร์การลงทะเบียน การถ่ายภาพ และการ Check-in แบบขั้นตอน

## ความต้องการของระบบ

- **Android Studio**: Arctic Fox (2020.3.1) หรือใหม่กว่า
- **Android SDK**: API Level 24 (Android 7.0) ขึ้นไป
- **Target SDK**: API Level 35 (Android 15)
- **Java**: JDK 11
- **Kotlin**: เวอร์ชัน 2.0.21
- **Gradle**: เวอร์ชัน 8.10.1

## ฟีเจอร์หลัก

- 📝 **Step 1**: ลงทะเบียนข้อมูลผู้ใช้
- 📷 **Step 2**: ถ่ายภาพผ่านกล้อง + YOLO Object Detection
- ✅ **Step 3**: Check-in และสรุปข้อมูล (พร้อมผลการตรวจจับวัตถุ)

## Libraries และ Dependencies

### Core Android Libraries
```kotlin
androidx.core:core-ktx:1.10.1
androidx.appcompat:appcompat:1.6.1
com.google.android.material:material:1.10.0
androidx.constraintlayout:constraintlayout:2.1.4
```

### Architecture Components
```kotlin
androidx.lifecycle:lifecycle-livedata-ktx:2.6.1
androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1
```

### Navigation Components
```kotlin
androidx.navigation:navigation-fragment-ktx:2.6.0
androidx.navigation:navigation-ui-ktx:2.6.0
```

### Camera Functionality
```kotlin
androidx.camera:camera-core:1.3.1
androidx.camera:camera-camera2:1.3.1
androidx.camera:camera-lifecycle:1.3.1
androidx.camera:camera-view:1.3.1
androidx.camera:camera-extensions:1.3.1
```

### YOLO Object Detection (ONNX Runtime)
```kotlin
com.microsoft.onnxruntime:onnxruntime-android:1.17.1
com.microsoft.onnxruntime:onnxruntime-extensions-android:0.10.0
```

### Testing Libraries
```kotlin
junit:junit:4.13.2
androidx.test.ext:junit:1.1.5
androidx.test.espresso:espresso-core:3.5.1
```

## การติดตั้งและการใช้งาน

### 1. Clone หรือ Download โปรเจค
```bash
git clone [repository-url]
cd DemoApp
```

### 2. เปิดโปรเจคใน Android Studio
1. เปิด Android Studio
2. เลือก "Open an existing Android Studio project"
3. เลือกโฟลเดอร์ของโปรเจค

### 3. Sync โปรเจค
- Android Studio จะทำการ sync dependencies อัตโนมัติ
- หากไม่ sync ให้กด "Sync Now" ใน notification bar

### 4. การ Build โปรเจค
```bash
# สำหรับ Debug Build
./gradlew assembleDebug

# สำหรับ Release Build
./gradlew assembleRelease
```

### 5. การรันแอป
- เชื่อมต่ออุปกรณ์ Android หรือใช้ Emulator
- กด Run (▶️) ใน Android Studio หรือใช้คำสั่ง:
```bash
./gradlew installDebug
```

## Permissions ที่ใช้

แอปนี้ต้องการ permissions ดังนี้:
- `CAMERA`: สำหรับการใช้งานกล้องถ่ายภาพ
- `android.hardware.camera`: ต้องการฮาร์ดแวร์กล้อง

## โครงสร้างโปรเจค

```
app/
├── src/main/
│   ├── assets/
│   │   ├── best.onnx            # YOLO11 model (ONNX format)
│   │   └── metadata.yaml        # Model metadata
│   ├── java/com/example/checkinapp/
│   │   ├── MainActivity.kt
│   │   ├── data/
│   │   │   └── RegistrationData.kt
│   │   ├── detection/           # YOLO detection classes
│   │   │   ├── YoloDetector.kt
│   │   │   └── DetectionResult.kt
│   │   └── fragments/
│   │       ├── Step1RegistrationFragment.kt
│   │       ├── Step2CameraFragment.kt (with YOLO)
│   │       └── Step3CheckInFragment.kt
│   ├── res/
│   │   ├── layout/
│   │   ├── drawable/
│   │   └── values/
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## การพัฒนาและแก้ไข

### การเพิ่ม Dependencies ใหม่
1. เปิดไฟล์ `app/build.gradle.kts`
2. เพิ่ม dependency ใน block `dependencies`
3. Sync โปรเจค

### การเพิ่ม Permissions
1. เปิดไฟล์ `AndroidManifest.xml`
2. เพิ่ม `<uses-permission>` tag
3. อัปเดต logic ในโค้ดสำหรับ permission handling

## การแก้ไขปัญหาที่พบบ่อย

### 1. Build Error
```bash
# Clear build cache
./gradlew clean
./gradlew build
```

### 2. Dependency Conflict
- ตรวจสอบเวอร์ชันของ libraries ใน `gradle/libs.versions.toml`
- อัปเดตเวอร์ชันให้สอดคล้องกัน

### 3. Camera Permission
- ตรวจสอบให้แน่ใจว่าได้เพิ่ม camera permission ใน AndroidManifest.xml
- ทดสอบบนอุปกรณ์จริงสำหรับฟีเจอร์กล้อง

### 4. YOLO Model Issues
- ตรวจสอบว่าไฟล์ `best.onnx` อยู่ในโฟลเดอร์ `assets/`
- หากการตรวจจับไม่แม่นยำ สามารถปรับค่า confidence threshold ใน YoloDetector.kt
- หากมีปัญหาด้านประสิทธิภาพ ลองลด IOU_THRESHOLD หรือ minBoxSize
- ตรวจสอบ logcat สำหรับข้อผิดพลาดของ ONNX Runtime

## YOLO Object Detection

### ฟีเจอร์ YOLO
- **Object Detection**: ตรวจจับและระบุวัตถุในภาพแบบ real-time
- **Bounding Box**: แสดงกรอบล้อมรอบวัตถุที่ตรวจพบ
- **Confidence Score**: แสดงระดับความมั่นใจของการตรวจจับ
- **Multiple Objects**: สามารถตรวจจับวัตถุหลายชิ้นในภาพเดียว

### การตั้งค่า YOLO
```kotlin
// ใน YoloDetector.kt (ONNX Runtime)
private const val CONFIDENCE_THRESHOLD = 0.99f  // ระดับความมั่นใจขั้นต่ำ (99%)
private const val IOU_THRESHOLD = 0.3f           // Non-Maximum Suppression (30%)
private const val INPUT_SIZE = 640               // ขนาด input ของโมเดล
```

### Model Files
- **best.onnx**: โมเดล YOLO11 หลัก (ONNX format - ประสิทธิภาพสูง)
- **metadata.yaml**: ข้อมูล metadata ของโมเดล

## Version Information

- **App Version**: 1.0
- **Package Name**: com.example.checkinapp
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)

## การสนับสนุน

หากพบปัญหาหรือต้องการความช่วยเหลือ:
1. ตรวจสอบ Issues ในโปรเจค
2. สร้าง Issue ใหม่พร้อมรายละเอียดปัญหา
3. ดู Log ใน Android Studio Logcat สำหรับการแก้ไขปัญหา