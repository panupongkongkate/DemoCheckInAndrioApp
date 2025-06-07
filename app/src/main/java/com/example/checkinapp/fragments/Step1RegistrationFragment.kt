package com.example.checkinapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.checkinapp.R
import com.example.checkinapp.data.MockRegistrationRepository
import com.example.checkinapp.data.RegistrationData

class Step1RegistrationFragment : Fragment() {
    
    private lateinit var etPincode: EditText
    private lateinit var btnCheckData: Button
    private lateinit var layoutRegistrationData: LinearLayout
    
    private lateinit var tvFullname: TextView
    private lateinit var tvCompany: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPurpose: TextView
    private lateinit var tvContact: TextView
    private lateinit var tvDepartment: TextView
    
    private var onDataLoadedListener: ((RegistrationData) -> Unit)? = null
    private var onDataClearedListener: (() -> Unit)? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_step1_registration, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupListeners()
    }
    
    private fun initViews(view: View) {
        etPincode = view.findViewById(R.id.et_pincode)
        btnCheckData = view.findViewById(R.id.btn_check_data)
        layoutRegistrationData = view.findViewById(R.id.layout_registration_data)
        
        tvFullname = view.findViewById(R.id.tv_fullname)
        tvCompany = view.findViewById(R.id.tv_company)
        tvPhone = view.findViewById(R.id.tv_phone)
        tvEmail = view.findViewById(R.id.tv_email)
        tvPurpose = view.findViewById(R.id.tv_purpose)
        tvContact = view.findViewById(R.id.tv_contact)
        tvDepartment = view.findViewById(R.id.tv_department)
    }
    
    private fun setupListeners() {
        btnCheckData.setOnClickListener {
            loadRegistrationData()
        }
    }
    
    private fun loadRegistrationData() {
        val pincode = etPincode.text.toString().trim()
        
        if (pincode.isEmpty()) {
            Toast.makeText(context, "กรุณากรอก PIN Code", Toast.LENGTH_SHORT).show()
            return
        }
        
        val registrationData = MockRegistrationRepository.getRegistrationData(pincode)
        
        if (registrationData != null) {
            displayRegistrationData(registrationData)
            onDataLoadedListener?.invoke(registrationData)
        } else {
            Toast.makeText(
                context, 
                "ไม่พบข้อมูลการลงทะเบียน กรุณาตรวจสอบ PIN Code", 
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun displayRegistrationData(data: RegistrationData) {
        tvFullname.text = data.fullname
        tvCompany.text = data.company
        tvPhone.text = data.phone
        tvEmail.text = data.email
        tvPurpose.text = data.purpose
        tvContact.text = data.contact
        tvDepartment.text = data.department
        
        layoutRegistrationData.visibility = View.VISIBLE
    }
    
    fun setOnDataLoadedListener(listener: (RegistrationData) -> Unit) {
        onDataLoadedListener = listener
    }
    
    fun setOnDataClearedListener(listener: () -> Unit) {
        onDataClearedListener = listener
    }
    
    fun resetData() {
        // Check if fragment is attached before accessing views
        if (!isAdded || view == null) return
        
        etPincode.text.clear()
        layoutRegistrationData.visibility = View.GONE
        
        tvFullname.text = ""
        tvCompany.text = ""
        tvPhone.text = ""
        tvEmail.text = ""
        tvPurpose.text = ""
        tvContact.text = ""
        tvDepartment.text = ""
        
        // Notify that data was cleared
        onDataClearedListener?.invoke()
    }
}