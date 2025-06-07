package com.example.checkinapp.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
    
    private var onPhotoChangedListener: ((Bitmap?) -> Unit)? = null
    
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
            createTempFile("photo", ".jpg", requireContext().cacheDir)
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
                            
                            // Update UI
                            ivCapturedPhoto.setImageBitmap(capturedBitmap)
                            ivCapturedPhoto.visibility = View.VISIBLE
                            cameraPreview.visibility = View.GONE
                            btnCapturePhoto.visibility = View.GONE
                            btnRetakePhoto.visibility = View.VISIBLE
                            
                            // Notify listener
                            onPhotoChangedListener?.invoke(capturedBitmap)
                            
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
        onPhotoChangedListener?.invoke(null)
        
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
    
    fun setOnPhotoChangedListener(listener: (Bitmap?) -> Unit) {
        onPhotoChangedListener = listener
    }
    
    fun resetCamera() {
        capturedBitmap = null
        
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
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}