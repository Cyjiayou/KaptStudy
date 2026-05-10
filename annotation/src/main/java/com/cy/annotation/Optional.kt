package com.cy.annotation

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Optional(
    val stringValue: String = "",
    val intValue: Int = 0,
    val floatValue: Float = 0f,
    val booleanValue: Boolean = false
)
