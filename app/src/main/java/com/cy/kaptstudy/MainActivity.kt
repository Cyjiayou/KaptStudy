package com.cy.kaptstudy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


//        findViewById<Button>(R.id.jumpToSecondActivity).setOnClickListener {
//            SecondActivityBuilder.startWithoutOptional(this, 18, "caoyang")
//        }
//
//        findViewById<Button>(R.id.jumpToThirdActivity).setOnClickListener {
//            ThirdActivityBuilder.start(this, "hsm", "cy", 0L, "www.baidu.com")
//        }
    }
}