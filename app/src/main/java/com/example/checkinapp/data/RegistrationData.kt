package com.example.checkinapp.data

data class RegistrationData(
    val fullname: String,
    val company: String,
    val phone: String,
    val email: String,
    val purpose: String,
    val contact: String,
    val department: String
)

object MockRegistrationRepository {
    private val mockData = mapOf(
        "123456" to RegistrationData(
            fullname = "สมชาย ใจดี",
            company = "บริษัท ABC จำกัด",
            phone = "081-234-5678",
            email = "somchai@abc.co.th",
            purpose = "ประชุมงาน",
            contact = "คุณสมหญิง",
            department = "แผนกขาย"
        ),
        "789012" to RegistrationData(
            fullname = "สมหญิง รักดี",
            company = "บริษัท XYZ จำกัด",
            phone = "082-345-6789",
            email = "somying@xyz.co.th",
            purpose = "นำเสนอผลิตภัณฑ์",
            contact = "คุณสมชาย",
            department = "แผนกการตลาด"
        )
    )
    
    fun getRegistrationData(pincode: String): RegistrationData? {
        return mockData[pincode]
    }
}