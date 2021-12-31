package com.example.reversigame

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.reversigame.databinding.ActivityGameoverBinding

class GameOverActivity : AppCompatActivity() {

    companion object{
        var vencedor = ""
    }

    private lateinit var b : ActivityGameoverBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityGameoverBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)

        vencedor = intent.getStringExtra(vencedor).toString()
        b.tvVencedor.text = getString(R.string.frase_vencedor, vencedor)

        b.btBack.setOnClickListener {
            val intent = Intent(this, StartActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            //intent.putExtra(MainActivity.nomeParametro, MainActivity.parametro)
            startActivity(intent)
        }
    }
}