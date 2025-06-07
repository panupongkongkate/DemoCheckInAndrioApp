package com.example.checkinapp.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.IOException
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

class YoloDetector(private val context: Context) {
    
    companion object {
        private const val TAG = "YoloDetector"
        private const val MODEL_PATH = "best.onnx"
        private const val METADATA_PATH = "metadata.yaml"
        private const val INPUT_SIZE = 640
        private const val MAX_DETECTIONS = 25
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val IOU_THRESHOLD = 0.3f
    }
    
    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false
    private var labels = listOf<String>()
    
    init {
        try {
            initializeModel()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing YOLO model", e)
        }
    }
    
    private fun initializeModel() {
        try {
            // Initialize ONNX Runtime environment
            ortEnvironment = OrtEnvironment.getEnvironment()
            
            // Load model from assets
            val modelBytes = context.assets.open(MODEL_PATH).use { inputStream ->
                inputStream.readBytes()
            }
            
            // Create ONNX Runtime session
            ortSession = ortEnvironment?.createSession(modelBytes)
            
            // Load labels (simplified - in real implementation, parse metadata.yaml)
            labels = loadLabels()
            
            isInitialized = true
            Log.d(TAG, "ONNX YOLO11 model initialized successfully")
            Log.d(TAG, "Using model: $MODEL_PATH (ONNX Runtime)")
            Log.d(TAG, "Model size: ${modelBytes.size} bytes")
            Log.d(TAG, "Confidence threshold: $CONFIDENCE_THRESHOLD")
            Log.d(TAG, "Loaded ${labels.size} classes: ${labels.joinToString()}")
            
            // Log input/output info
            ortSession?.let { session ->
                Log.d(TAG, "Input names: ${session.inputNames}")
                Log.d(TAG, "Output names: ${session.outputNames}")
                session.inputInfo.forEach { (name, info) ->
                    Log.d(TAG, "Input $name: ${info.info}")
                }
                session.outputInfo.forEach { (name, info) ->
                    Log.d(TAG, "Output $name: ${info.info}")
                }
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Error loading model file: $MODEL_PATH", e)
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ONNX model", e)
            isInitialized = false
        }
    }
    
    private fun loadLabels(): List<String> {
        return try {
            // Load from metadata.yaml if available, otherwise use default 4-class labels
            val assetManager = context.assets
            try {
                val inputStream = assetManager.open(METADATA_PATH)
                val yamlContent = inputStream.bufferedReader().use { it.readText() }
                inputStream.close()
                
                // Parse YAML format with numbered names (0: name1, 1: name2, ...)
                val names = mutableMapOf<Int, String>()
                val lines = yamlContent.lines()
                var inNamesSection = false
                
                for (line in lines) {
                    val trimmedLine = line.trim()
                    if (trimmedLine == "names:") {
                        inNamesSection = true
                        continue
                    }
                    if (inNamesSection) {
                        val nameMatch = """(\d+):\s*(.+)""".toRegex().find(trimmedLine)
                        if (nameMatch != null) {
                            val index = nameMatch.groupValues[1].toInt()
                            val name = nameMatch.groupValues[2].trim()
                            names[index] = name
                        } else if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith(" ")) {
                            // End of names section
                            break
                        }
                    }
                }
                
                if (names.isNotEmpty()) {
                    val sortedNames = (0 until names.size).map { names[it] ?: "Unknown_$it" }
                    Log.d(TAG, "Loaded ${sortedNames.size} class names from metadata: $sortedNames")
                    return sortedNames
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not load metadata.yaml: ${e.message}")
            }
            
            // Default labels for ID card detection (based on metadata.yaml)
            Log.d(TAG, "Using default ID card labels")
            listOf("Date_of_Birth", "First_Name", "ID_Number", "Last_Name")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading labels", e)
            listOf("Unknown")
        }
    }
    
    fun detect(bitmap: Bitmap): DetectionResults? {
        if (!isInitialized || ortSession == null || ortEnvironment == null) {
            Log.w(TAG, "ONNX model not initialized")
            return null
        }
        
        return try {
            val startTime = System.currentTimeMillis()
            
            // Preprocess image for ONNX
            val inputTensor = preprocessImageForOnnx(bitmap)
            
            // Run inference
            val inputs = mapOf("images" to inputTensor)
            val outputs = ortSession!!.run(inputs)
            
            // Get output tensor
            val outputTensor = outputs[0].value as Array<Array<FloatArray>>
            Log.d(TAG, "ONNX output shape: [${outputTensor.size}, ${outputTensor[0].size}, ${outputTensor[0][0].size}]")
            
            // Post-process results - YOLO11 ONNX format: [1, 8, 8400]
            val detections = postProcessOnnxOutput(outputTensor, bitmap.width, bitmap.height)
            
            val inferenceTime = System.currentTimeMillis() - startTime
            
            // ONNX Runtime manages tensor memory automatically
            
            DetectionResults(
                detections = detections,
                inferenceTime = inferenceTime,
                imageWidth = bitmap.width,
                imageHeight = bitmap.height
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during ONNX detection", e)
            null
        }
    }
    
    private fun preprocessImageForOnnx(bitmap: Bitmap): OnnxTensor {
        // Resize bitmap to 640x640
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        
        // Convert to float array [1, 3, 640, 640] format for ONNX
        val floatArray = FloatArray(1 * 3 * INPUT_SIZE * INPUT_SIZE)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        resizedBitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        // Convert ARGB to RGB and normalize to [0, 1]
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            
            // ONNX expects [batch, channel, height, width] format
            floatArray[i] = r                           // Red channel
            floatArray[INPUT_SIZE * INPUT_SIZE + i] = g // Green channel  
            floatArray[2 * INPUT_SIZE * INPUT_SIZE + i] = b // Blue channel
        }
        
        // Create ONNX tensor
        val shape = longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
        return OnnxTensor.createTensor(ortEnvironment!!, FloatBuffer.wrap(floatArray), shape)
    }
    
    private fun postProcessOnnxOutput(
        output: Array<Array<FloatArray>>, 
        imageWidth: Int, 
        imageHeight: Int
    ): List<DetectionResult> {
        val detections = mutableListOf<DetectionResult>()
        
        // YOLO11 ONNX output format: [1, 8, 8400]
        // Rows 0-3: bbox coordinates (x_center, y_center, width, height)  
        // Rows 4-7: class probabilities (no separate objectness in YOLO11)
        val batchOutput = output[0] // Get first batch [8, 8400]
        
        if (batchOutput.size < 8) {
            Log.w(TAG, "Invalid YOLO11 ONNX output size: ${batchOutput.size}")
            return emptyList()
        }
        
        val numDetections = batchOutput[0].size // 8400
        Log.d(TAG, "Processing $numDetections detections from YOLO11 ONNX")
        
        for (i in 0 until numDetections) {
            try {
                // Extract class probabilities first (rows 4-7 for 4 classes)
                var maxClassIndex = 0
                var maxClassScore = batchOutput[4][i]
                
                // Find highest class probability
                for (classIdx in 1 until 4) { // Check classes 1, 2, 3
                    val classScore = batchOutput[4 + classIdx][i]
                    if (classScore > maxClassScore) {
                        maxClassScore = classScore
                        maxClassIndex = classIdx
                    }
                }
                
                // YOLO11: Use class probability directly as confidence
                // Skip if confidence too low - be more strict for YOLO11
                if (maxClassScore < CONFIDENCE_THRESHOLD) continue
                
                // Only process if we have a reasonable confidence
                if (maxClassScore < 0.1f) continue // Additional filter for very low scores
                
                // Extract bbox coordinates (already normalized 0-1 in YOLO11)
                val centerX = batchOutput[0][i] * imageWidth
                val centerY = batchOutput[1][i] * imageHeight
                val width = batchOutput[2][i] * imageWidth
                val height = batchOutput[3][i] * imageHeight
                
                // Skip if coordinates are clearly invalid
                if (centerX < 0 || centerY < 0 || width <= 0 || height <= 0) continue
                if (centerX > imageWidth || centerY > imageHeight) continue
                
                // Calculate bounding box
                val left = centerX - width / 2
                val top = centerY - height / 2
                val right = centerX + width / 2
                val bottom = centerY + height / 2
                
                val boundingBox = RectF(
                    max(0f, left),
                    max(0f, top),
                    min(imageWidth.toFloat(), right),
                    min(imageHeight.toFloat(), bottom)
                )
                
                // Skip very small boxes (likely false positives)
                val minBoxSize = 50f // Increased to 50 pixels
                if (boundingBox.width() < minBoxSize || boundingBox.height() < minBoxSize) {
                    Log.v(TAG, "Skipping small box: ${boundingBox.width()}x${boundingBox.height()}")
                    continue
                }
                
                // Skip boxes that are too large (likely false positives)
                val maxBoxSize = min(imageWidth * 0.3f, imageHeight * 0.3f)
                if (boundingBox.width() > maxBoxSize || boundingBox.height() > maxBoxSize) {
                    Log.v(TAG, "Skipping large box: ${boundingBox.width()}x${boundingBox.height()}")
                    continue
                }
                
                val label = if (maxClassIndex < labels.size) {
                    labels[maxClassIndex]
                } else {
                    "Class_$maxClassIndex"
                }
                
                Log.v(TAG, "Valid detection: $label (${String.format("%.2f", maxClassScore)}) at (${centerX.toInt()}, ${centerY.toInt()}) size ${width.toInt()}x${height.toInt()}")
                
                detections.add(
                    DetectionResult(
                        boundingBox = boundingBox,
                        label = label,
                        confidence = maxClassScore
                    )
                )
                
            } catch (e: Exception) {
                Log.w(TAG, "Error processing detection $i: ${e.message}")
                continue
            }
        }
        
        Log.d(TAG, "Found ${detections.size} valid YOLO11 ONNX detections before NMS (from $numDetections total)")
        return applyNMS(detections)
    }
    
    private fun applyNMS(detections: List<DetectionResult>): List<DetectionResult> {
        if (detections.isEmpty()) return emptyList()
        
        val sortedDetections = detections.sortedByDescending { it.confidence }
        val finalDetections = mutableListOf<DetectionResult>()
        
        for (detection in sortedDetections) {
            var shouldAdd = true
            
            for (finalDetection in finalDetections) {
                val iou = calculateIoU(detection.boundingBox, finalDetection.boundingBox)
                if (iou > IOU_THRESHOLD) {
                    shouldAdd = false
                    break
                }
            }
            
            if (shouldAdd) {
                finalDetections.add(detection)
            }
        }
        
        return finalDetections
    }
    
    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectionLeft = max(box1.left, box2.left)
        val intersectionTop = max(box1.top, box2.top)
        val intersectionRight = min(box1.right, box2.right)
        val intersectionBottom = min(box1.bottom, box2.bottom)
        
        if (intersectionRight <= intersectionLeft || intersectionBottom <= intersectionTop) {
            return 0f
        }
        
        val intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop)
        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)
        val unionArea = box1Area + box2Area - intersectionArea
        
        return if (unionArea > 0f) intersectionArea / unionArea else 0f
    }
    
    fun close() {
        try {
            ortSession?.close()
            ortSession = null
            // Note: Do not close ortEnvironment as it's a singleton
            // ortEnvironment?.close()
            ortEnvironment = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ONNX session", e)
        }
    }
}