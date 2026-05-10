package com.cy.kaptstudy

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cy.annotation.Builder
import com.cy.annotation.Optional
import com.cy.annotation.Required

@Builder
class SecondActivity : AppCompatActivity() {

    @Required
    var name: String? = null

    @Required
    var age: Int? = null

    @Optional(stringValue = "bytedance")
    var company: String? = null

    @Optional(stringValue = "infra")
    var title: String? = null

    @Optional(stringValue = "beijing")
    var workPlace: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        findViewById<TextView>(R.id.name).setText(name)
        findViewById<TextView>(R.id.age).setText(age.toString())
        findViewById<TextView>(R.id.company).setText(company)
        findViewById<TextView>(R.id.title).setText(title)
        findViewById<TextView>(R.id.workPlace).setText(workPlace)
    }
}