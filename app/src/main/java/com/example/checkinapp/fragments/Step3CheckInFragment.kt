package com.example.checkinapp.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.checkinapp.R
import com.example.checkinapp.data.RegistrationData
import com.example.checkinapp.detection.DetectionResults
import java.text.SimpleDateFormat
import java.util.*

class Step3CheckInFragment : Fragment() {
    
    private lateinit var tvSummaryFullname: TextView
    private lateinit var tvSummaryCompany: TextView
    private lateinit var tvSummaryPhone: TextView
    private lateinit var tvSummaryEmail: TextView
    private lateinit var tvSummaryPurpose: TextView
    private lateinit var tvSummaryContact: TextView
    private lateinit var tvSummaryDepartment: TextView
    private lateinit var ivSummaryPhoto: ImageView
    private lateinit var tvNoPhoto: TextView
    private lateinit var tvCheckinTime: TextView
    private lateinit var cbConfirmCheckin: CheckBox
    private lateinit var btnFinalCheckin: Button
    
    // Detection info views
    private lateinit var layoutDetectionInfo: LinearLayout
    private lateinit var tvDetectionSummary: TextView
    private lateinit var tvDetectionDetails: TextView
    
    private var onCheckInCompleteListener: (() -> Unit)? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_step3_checkin, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupListeners()
        
        // Set current time
        updateCheckinTime()
    }
    
    private fun initViews(view: View) {
        tvSummaryFullname = view.findViewById(R.id.tv_summary_fullname)
        tvSummaryCompany = view.findViewById(R.id.tv_summary_company)
        tvSummaryPhone = view.findViewById(R.id.tv_summary_phone)
        tvSummaryEmail = view.findViewById(R.id.tv_summary_email)
        tvSummaryPurpose = view.findViewById(R.id.tv_summary_purpose)
        tvSummaryContact = view.findViewById(R.id.tv_summary_contact)
        tvSummaryDepartment = view.findViewById(R.id.tv_summary_department)
        ivSummaryPhoto = view.findViewById(R.id.iv_summary_photo)
        tvNoPhoto = view.findViewById(R.id.tv_no_photo)
        tvCheckinTime = view.findViewById(R.id.tv_checkin_time)
        cbConfirmCheckin = view.findViewById(R.id.cb_confirm_checkin)
        btnFinalCheckin = view.findViewById(R.id.btn_final_checkin)
        
        // Create detection info views programmatically since they don't exist in layout
        createDetectionInfoLayout(view)
    }
    
    private fun setupListeners() {
        btnFinalCheckin.setOnClickListener {
            performFinalCheckIn()
        }
    }
    
    private fun createDetectionInfoLayout(view: View) {
        // Find the main LinearLayout inside ScrollView
        val scrollView = view as? ScrollView
        val rootLayout = scrollView?.getChildAt(0) as? LinearLayout ?: return
        
        // Create detection info layout
        layoutDetectionInfo = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            visibility = View.GONE
            background = ContextCompat.getDrawable(context, R.drawable.summary_card_background)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 24)
            layoutParams = params
        }
        
        // Create header for detection info
        val headerText = TextView(context).apply {
            text = "🔍 ข้อมูลการตรวจจับวัตถุ"
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.teal_200))
            setTypeface(null, android.graphics.Typeface.BOLD)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 16)
            layoutParams = params
        }
        
        tvDetectionSummary = TextView(context).apply {
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setTypeface(null, android.graphics.Typeface.BOLD)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 8)
            layoutParams = params
        }
        
        tvDetectionDetails = TextView(context).apply {
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        }
        
        layoutDetectionInfo.addView(headerText)
        layoutDetectionInfo.addView(tvDetectionSummary)
        layoutDetectionInfo.addView(tvDetectionDetails)
        
        // Add to main layout before the confirmation checkbox (second to last child)
        val insertIndex = rootLayout.childCount - 2
        rootLayout.addView(layoutDetectionInfo, insertIndex)
    }
    
    fun updateSummary(registrationData: RegistrationData?, capturedPhoto: Bitmap?, detectionResults: DetectionResults? = null) {
        // Check if view is initialized
        if (!::tvSummaryFullname.isInitialized) {
            // If view is not ready, store data and update later
            view?.post {
                updateSummary(registrationData, capturedPhoto, detectionResults)
            }
            return
        }
        
        registrationData?.let { data ->
            tvSummaryFullname.text = data.fullname
            tvSummaryCompany.text = data.company
            tvSummaryPhone.text = data.phone
            tvSummaryEmail.text = data.email
            tvSummaryPurpose.text = data.purpose
            tvSummaryContact.text = data.contact
            tvSummaryDepartment.text = data.department
        }
        
        if (capturedPhoto != null) {
            ivSummaryPhoto.setImageBitmap(capturedPhoto)
            ivSummaryPhoto.visibility = View.VISIBLE
            tvNoPhoto.visibility = View.GONE
        } else {
            ivSummaryPhoto.visibility = View.GONE
            tvNoPhoto.visibility = View.VISIBLE
        }
        
        // Update detection info
        updateDetectionInfo(detectionResults)
        
        updateCheckinTime()
    }
    
    private fun updateDetectionInfo(detectionResults: DetectionResults?) {
        // Hide detection info completely - user should not see detection details
        if (::layoutDetectionInfo.isInitialized) {
            layoutDetectionInfo.visibility = View.GONE
        }
    }
    
    private fun updateCheckinTime() {
        val currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("th", "TH"))
            .format(Date())
        tvCheckinTime.text = currentTime
    }
    
    private fun performFinalCheckIn() {
        if (!cbConfirmCheckin.isChecked) {
            Toast.makeText(context, "กรุณายืนยันการ Check In", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Simulate check in process
        btnFinalCheckin.isEnabled = false
        btnFinalCheckin.text = "กำลัง Check In..."
        
        // Simulate network delay
        btnFinalCheckin.postDelayed({
            onCheckInCompleteListener?.invoke()
            
            // Reset UI
            btnFinalCheckin.isEnabled = true
            btnFinalCheckin.text = "Check In"
            cbConfirmCheckin.isChecked = false
        }, 1500)
    }
    
    fun setOnCheckInCompleteListener(listener: () -> Unit) {
        onCheckInCompleteListener = listener
    }
}