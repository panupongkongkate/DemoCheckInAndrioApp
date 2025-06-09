package com.example.checkinapp.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.checkinapp.R
import com.example.checkinapp.detection.YoloDetector
import com.example.checkinapp.detection.DetectionResults
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Step2CameraFragment : Fragment() {
    
    private lateinit var cameraPreview: PreviewView
    private lateinit var cameraPlaceholder: LinearLayout
    private lateinit var ivCapturedPhoto: ImageView
    private lateinit var btnStartCamera: Button
    private lateinit var layoutCameraActions: LinearLayout
    private lateinit var btnCapturePhoto: Button
    private lateinit var btnRetakePhoto: Button
    
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var capturedBitmap: Bitmap? = null
    
    // YOLO detection
    private var yoloDetector: YoloDetector? = null
    private var detectionResults: DetectionResults? = null
    
    private var onPhotoChangedListener: ((Bitmap?, DetectionResults?) -> Unit)? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_step2_camera, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupListeners()
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Initialize YOLO detector
        try {
            yoloDetector = YoloDetector(requireContext())
        } catch (e: Exception) {
            Log.e("Step2Camera", "Failed to initialize YOLO detector", e)
            Toast.makeText(context, "ไม่สามารถเริ่มต้นระบบตรวจจับวัตถุได้", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun initViews(view: View) {
        cameraPreview = view.findViewById(R.id.camera_preview)
        cameraPlaceholder = view.findViewById(R.id.camera_placeholder)
        ivCapturedPhoto = view.findViewById(R.id.iv_captured_photo)
        btnStartCamera = view.findViewById(R.id.btn_start_camera)
        layoutCameraActions = view.findViewById(R.id.layout_camera_actions)
        btnCapturePhoto = view.findViewById(R.id.btn_capture_photo)
        btnRetakePhoto = view.findViewById(R.id.btn_retake_photo)
    }
    
    private fun setupListeners() {
        btnStartCamera.setOnClickListener {
            startCamera()
        }
        
        btnCapturePhoto.setOnClickListener {
            takePhoto()
        }
        
        btnRetakePhoto.setOnClickListener {
            retakePhoto()
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(cameraPreview.surfaceProvider)
                }
            
            imageCapture = ImageCapture.Builder()
                .build()
            
            // Use back camera for check-in
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                // Check if back camera is available
                if (!cameraProvider.hasCamera(cameraSelector)) {
                    Toast.makeText(context, "กล้องหลังไม่พร้อมใช้งาน", Toast.LENGTH_SHORT).show()
                    return@addListener
                }
                
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                
                // Update UI
                cameraPlaceholder.visibility = View.GONE
                cameraPreview.visibility = View.VISIBLE
                btnStartCamera.visibility = View.GONE
                layoutCameraActions.visibility = View.VISIBLE
                btnCapturePhoto.visibility = View.VISIBLE
                btnRetakePhoto.visibility = View.GONE
                
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
                Toast.makeText(context, "ไม่สามารถเปิดกล้องได้", Toast.LENGTH_SHORT).show()
            }
            
        }, ContextCompat.getMainExecutor(requireContext()))
    }
    
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            java.io.File.createTempFile("photo", ".jpg", requireContext().cacheDir)
        ).build()
        
        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                    Toast.makeText(context, "ถ่ายรูปไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                }
                
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Convert captured image to bitmap
                    output.savedUri?.let { uri ->
                        try {
                            val inputStream = requireContext().contentResolver.openInputStream(uri)
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            inputStream?.close()
                            
                            // Use image as-is (back camera doesn't need mirroring)
                            capturedBitmap = bitmap
                            
                            // Run YOLO detection and apply censoring
                            processImageWithYolo(bitmap)
                            
                        } catch (e: Exception) {
                            Log.e("CameraX", "Error processing captured image", e)
                            Toast.makeText(context, "ประมวลผลรูปภาพไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
    
    private fun retakePhoto() {
        capturedBitmap = null
        detectionResults = null
        onPhotoChangedListener?.invoke(null, null)
        
        ivCapturedPhoto.visibility = View.GONE
        cameraPlaceholder.visibility = View.VISIBLE
        btnStartCamera.visibility = View.VISIBLE
        layoutCameraActions.visibility = View.GONE
        
        // Stop camera
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        }, ContextCompat.getMainExecutor(requireContext()))
    }
    
    fun setOnPhotoChangedListener(listener: (Bitmap?, DetectionResults?) -> Unit) {
        onPhotoChangedListener = listener
    }
    
    fun getDetectionResults(): DetectionResults? {
        return detectionResults
    }
    
    fun resetCamera() {
        capturedBitmap = null
        detectionResults = null
        
        // Check if fragment is attached before accessing views
        if (!isAdded || view == null) return
        
        cameraPreview.visibility = View.GONE
        ivCapturedPhoto.visibility = View.GONE
        cameraPlaceholder.visibility = View.VISIBLE
        btnStartCamera.visibility = View.VISIBLE
        layoutCameraActions.visibility = View.GONE
        
        // Stop camera only if context is available
        context?.let { ctx ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            }, ContextCompat.getMainExecutor(ctx))
        }
    }
    
    
    private fun processImageWithYolo(bitmap: Bitmap) {
        // Show loading indicator
        Toast.makeText(context, "กำลังตรวจจับวัตถุ...", Toast.LENGTH_SHORT).show()
        
        // Run YOLO detection in background thread
        cameraExecutor.execute {
            try {
                detectionResults = yoloDetector?.detect(bitmap)
                
                // Update UI on main thread
                activity?.runOnUiThread {
                    val annotatedBitmap = drawDetections(bitmap, detectionResults)
                    
                    // Update UI
                    ivCapturedPhoto.setImageBitmap(annotatedBitmap)
                    ivCapturedPhoto.visibility = View.VISIBLE
                    cameraPreview.visibility = View.GONE
                    btnCapturePhoto.visibility = View.GONE
                    btnRetakePhoto.visibility = View.VISIBLE
                    
                    // Show detection info
                    showDetectionInfo(detectionResults)
                    
                    // Notify listener with processed image (with bounding boxes)
                    onPhotoChangedListener?.invoke(annotatedBitmap, detectionResults)
                }
                
            } catch (e: Exception) {
                Log.e("Step2Camera", "Error running YOLO detection: ${e.message}", e)
                activity?.runOnUiThread {
                    val errorMsg = when {
                        e.message?.contains("bytes") == true -> "ขนาดข้อมูลภาพไม่ตรงกับโมเดล"
                        e.message?.contains("IllegalArgumentException") == true -> "รูปแบบข้อมูลไม่ถูกต้อง"
                        else -> "ตรวจจับวัตถุไม่สำเร็จ: ${e.message}"
                    }
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    
                    // Still show the image without annotations
                    ivCapturedPhoto.setImageBitmap(bitmap)
                    ivCapturedPhoto.visibility = View.VISIBLE
                    cameraPreview.visibility = View.GONE
                    btnCapturePhoto.visibility = View.GONE
                    btnRetakePhoto.visibility = View.VISIBLE
                    
                    // Notify listener with null detection results
                    onPhotoChangedListener?.invoke(bitmap, null)
                }
            }
        }
    }
    
    private fun drawDetections(bitmap: Bitmap, detectionResults: DetectionResults?): Bitmap {
        if (detectionResults == null || detectionResults.detections.isEmpty()) {
            return bitmap
        }
        
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        
        // Filter only high confidence detections (50%+ for green boxes only)
        val veryHighConfidenceDetections = detectionResults.detections.filter { 
            it.confidence >= 0.5f 
        }
        
        if (veryHighConfidenceDetections.isEmpty()) {
            return bitmap // Return original if no very high confidence detections
        }
        
        for (detection in veryHighConfidenceDetections) {
            // Different colors for each class
            val boxColor = when (detection.label) {
                "Date_of_Birth" -> Color.RED
                "First_Name" -> Color.GREEN
                "ID_Number" -> Color.BLUE
                "Last_Name" -> Color.MAGENTA
                else -> Color.YELLOW
            }
            
            // Paint for filled bounding boxes
            val paint = Paint().apply {
                style = Paint.Style.FILL
                color = boxColor
                alpha = 255 // Fully opaque filled box
            }
            
            // Paint for text
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 45f
                style = Paint.Style.FILL
                isAntiAlias = true
                setShadowLayer(3f, 2f, 2f, Color.BLACK)
            }
            
            // Paint for text background
            val textBackgroundPaint = Paint().apply {
                color = boxColor
                style = Paint.Style.FILL
                alpha = 220
            }
            
            // Draw filled bounding box
            canvas.drawRect(detection.boundingBox, paint)
            
            // Draw border outline for better visibility
            val borderPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                color = boxColor
                alpha = 255 // Full opacity for border
            }
            canvas.drawRect(detection.boundingBox, borderPaint)
            
            // Draw label with confidence
            val confidencePercent = String.format("%.0f%%", detection.confidence * 100)
            val label = "${detection.label}\n$confidencePercent"
            
            val lines = label.split("\n")
            val textBounds = android.graphics.Rect()
            textPaint.getTextBounds(lines[0], 0, lines[0].length, textBounds)
            
            val textWidth = maxOf(
                textPaint.measureText(lines[0]),
                textPaint.measureText(lines[1])
            )
            val textHeight = textBounds.height() * lines.size + 10f
            
            // Position text above the box
            val textX = detection.boundingBox.left
            val textY = detection.boundingBox.top - textHeight - 10f
            
            // Draw text background
            canvas.drawRoundRect(
                textX - 5f,
                textY - 5f,
                textX + textWidth + 10f,
                textY + textHeight + 5f,
                8f, 8f,
                textBackgroundPaint
            )
            
            // Draw text lines
            for (i in lines.indices) {
                canvas.drawText(
                    lines[i], 
                    textX, 
                    textY + (i + 1) * textBounds.height(), 
                    textPaint
                )
            }
        }
        
        return mutableBitmap
    }
    
    private fun showDetectionInfo(detectionResults: DetectionResults?) {
        // Log detection info for debugging only (no user-visible messages)
        if (detectionResults == null) {
            Log.d("Step2Camera", "No detection results")
            return
        }
        
        // Filter high confidence detections (50%+ after parameter update)
        val veryHighConfidenceDetections = detectionResults.detections.filter { 
            it.confidence >= 0.5f 
        }
        
        val totalCount = detectionResults.detections.size
        val veryHighConfidenceCount = veryHighConfidenceDetections.size
        val inferenceTime = detectionResults.inferenceTime
        
        Log.i("Step2Camera", "Detection results: $totalCount total, $veryHighConfidenceCount high-confidence (50%+) in ${inferenceTime}ms")
        
        // Log all detections
        for ((index, detection) in detectionResults.detections.withIndex()) {
            val confidencePercent = String.format("%.0f%%", detection.confidence * 100)
            val status = if (detection.confidence >= 0.5f) "🟢" else "⚠️"
            Log.i("Step2Camera", "Object ${index + 1}: $status ${detection.label} ($confidencePercent)")
        }
        
        // Log very high confidence summary (green boxes only)
        if (veryHighConfidenceDetections.isNotEmpty()) {
            Log.i("Step2Camera", "Very high confidence detections (green boxes):")
            veryHighConfidenceDetections.forEach { detection ->
                val confidencePercent = String.format("%.0f%%", detection.confidence * 100)
                Log.i("Step2Camera", "  🟢 ${detection.label}: $confidencePercent")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        yoloDetector?.close()
    }
}