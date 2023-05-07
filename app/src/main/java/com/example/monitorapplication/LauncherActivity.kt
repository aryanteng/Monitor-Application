package com.example.monitorapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.monitorapplication.databinding.ActivityLauncherBinding
import com.example.monitorapplication.databinding.ActivityMainBinding

class LauncherActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLauncherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnStart.setOnClickListener{
            if(binding.etHeight.text.isNotBlank()){
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("height", binding.etHeight.text.toString())
                startActivity(intent)
            }
            else{
                Toast.makeText(this, "Enter Height!", Toast.LENGTH_SHORT).show()
            }
        }

    }
}