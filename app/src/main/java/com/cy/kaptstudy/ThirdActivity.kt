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
class ThirdActivity : AppCompatActivity() {

    @Required
    var name: String? = null

    @Required
    var owner: String? = null

    @Optional
    var url: String? = null

    @Optional
    var createAt: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third)
        findViewById<TextView>(R.id.name).setText(name)
        findViewById<TextView>(R.id.owner).setText(owner)
        findViewById<TextView>(R.id.url).setText(url)
        findViewById<TextView>(R.id.createAt).setText(createAt.toString())
    }
}