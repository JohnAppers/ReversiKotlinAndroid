package com.example.reversigame;

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.reversigame.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity(){

    private lateinit var b : ActivityStartBinding
    private var jogadorUm = "um"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityStartBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)

        b.btModo1.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra(MainActivity.jogadorUm, jogadorUm)
            startActivity(intent)
        }

        b.btModo2.setOnClickListener {
            b.btModo2.visibility = View.INVISIBLE
            b.btModo2.width = 1
            b.btModoCliente.visibility = View.VISIBLE
            b.btModoServer.visibility = View.VISIBLE
        }

        b.btModoServer.setOnClickListener {
            val intent = Intent(this, TwoServerActivity::class.java)
            intent.putExtra(MainActivity.jogadorUm, jogadorUm)
            startActivity(intent)
        }

        b.btModoCliente.setOnClickListener {
            val intent = Intent(this, TwoClientActivity::class.java)
            intent.putExtra(MainActivity.jogadorUm, jogadorUm)
            startActivity(intent)
        }
    }
}
