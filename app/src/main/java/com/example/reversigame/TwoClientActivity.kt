package com.example.reversigame

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.reversigame.databinding.ActivityTwoclientBinding
import com.example.reversigame.model.Peca
import com.example.reversigame.model.Posicao
import org.json.JSONObject
import java.io.PrintStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

class TwoClientActivity : AppCompatActivity() {

    lateinit var b : ActivityTwoclientBinding
    lateinit var listaPosicoes : List<List<ImageView>>

    var socket: Socket? = null
    var jogadorDois = ""
    var buttonFlag = 0
    var connectFlag = false
    var mostrarJogadas = true
    var minhaVez = false
    var threadComm: Thread? = null
    var listaJogadas: MutableList<Posicao> = mutableListOf()
    var listaMudancas: MutableList<Posicao> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTwoclientBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)

        jogadorDois = intent.getStringExtra(MainActivity.jogadorUm).toString()
        listaPosicoes = arrayOfNulls<List<ImageView>>(8).mapIndexed { fila, lista ->
            arrayOfNulls<ImageView>(8).mapIndexed { coluna, imageButton ->
                val posicao = layoutInflater.inflate(R.layout.posicao,null)
                posicao.setOnClickListener {
                    if(buttonFlag==0 && connectFlag && minhaVez){
                        onClickPosicao(fila, coluna)
                    }
                    if(buttonFlag==1 && connectFlag && minhaVez){
                        onClickBomba(fila,coluna)
                    }
                    if(buttonFlag==2 && connectFlag && minhaVez){
                        onClickTroca(fila,coluna)
                    }
                }
                b.gridLayout.addView(posicao)
                posicao.findViewById(R.id.btnPosicao) as ImageView
            }
        }
        b.btnOptJogadas.setOnClickListener {
            if (mostrarJogadas) {
                mostrarJogadas = false
                apagaJogadas()
            } else {
                mostrarJogadas = true
                mostraJogadas()
            }
        }

        setupDialogo()
    }

    private fun setupDialogo(){
        val edtBox = EditText(this).apply {
            maxLines = 1
            filters = arrayOf(object : InputFilter {
                override fun filter(
                    source: CharSequence?,
                    start: Int,
                    end: Int,
                    dest: Spanned?,
                    dstart: Int,
                    dend: Int
                ): CharSequence? {
                    source?.run {
                        var ret = ""
                        forEach {
                            if (it.isDigit() || it.equals('.'))
                                ret += it
                        }
                        return ret
                    }
                    return null
                }

            })
        }

        val dlg = AlertDialog.Builder(this).run {
            setTitle(getString(R.string.client_mode))
            setMessage(getString(R.string.ask_ip))
            setPositiveButton(getString(R.string.button_connect)) { _: DialogInterface, _: Int ->
                val strIP = edtBox.text.toString()
                if (strIP.isEmpty() || !Patterns.IP_ADDRESS.matcher(strIP).matches()) {
                    Toast.makeText(this@TwoClientActivity, getString(R.string.error_address), Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    iniciaCliente(edtBox.text.toString())
                }
            }
            setNeutralButton(getString(R.string.btn_emulator)) { _: DialogInterface, _: Int ->
                iniciaCliente("10.0.2.16", SERVER_PORT-1)
            }
            setNegativeButton(getString(R.string.button_cancel)) { _: DialogInterface, _: Int ->
                finish()
            }
            setCancelable(false)
            setView(edtBox)
            create()
        }
        dlg.show()
    }

    private fun iniciaCliente(ip: String, port: Int = SERVER_PORT) {
        thread{
            try {
                socket = Socket()
                socket!!.connect(InetSocketAddress(ip,port),5000)
                connectFlag = true
            } catch (_: Exception) {
                //TODO close sockets
            }
        }
    }

    fun apagaJogadas(){
        listaPosicoes.flatMap { it }.forEach{ it.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_700)) }
    }

    fun mostraJogadas(){
        listaJogadas.forEach {
            listaPosicoes[it.fila][it.coluna].setBackgroundColor(ContextCompat.getColor(this, R.color.purple_500))
        }
    }

    fun verificaCondicoes(mensagem: JSONObject){
        //TODO jogadasPossiveis, semJogadas
    }

    fun setJogada(pos: Posicao){
         listaPosicoes[pos.fila][pos.coluna].setImageResource(R.drawable.white_stone)
    }

    fun onClickPosicao(fila: Int,coluna: Int){
        thread {
            try {
                val printStream = PrintStream(socket!!.getOutputStream())
                val json = JSONObject()
                json.put("jogada","normal")
                json.put("fila", fila)
                json.put("coluna", coluna)
                printStream.println(json.toString())
                printStream.flush()

                val bufferIn = socket!!.getInputStream().bufferedReader()
                val mensagem = JSONObject(bufferIn.readLine())
                if(mensagem["valida"].toString().equals("true")){
                    val jogadas = mensagem.getJSONArray("jogadas")
                    listaMudancas.clear()
                    for(pos in 0 until jogadas.length()){
                        val posicao = jogadas.getJSONObject(pos)
                        val filaNova = posicao.getInt("fila")
                        val colunaNova = posicao.getInt("coluna")
                        listaMudancas.add(Posicao(filaNova,colunaNova,Peca.BRANCA))
                    }
                    listaMudancas.forEach{ setJogada(it) }
                }
                verificaCondicoes(mensagem)
            } catch (_: Exception) {
                Log.d("Erro", "onClickPosicao falhou")
            }
        }
    }

    fun onClickBomba(fila: Int,coluna: Int){

    }

    fun onClickTroca(fila: Int,coluna: Int){

    }
}