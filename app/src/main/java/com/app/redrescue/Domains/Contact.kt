package com.app.redrescue.Domains

data class Contact(
    val name: String = "",
    val phone: String = "",
    var isSelected: Boolean = false,
    var fcmToken: String? = null
)
