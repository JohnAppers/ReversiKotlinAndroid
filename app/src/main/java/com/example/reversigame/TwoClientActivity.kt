package com.example.reversigame

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
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

    private lateinit var b : ActivityTwoclientBinding
    private lateinit var listaPosicoes : List<List<ImageView>>

    private var socket: Socket? = null
    private var jogadorUm = ""
    private var jogadorDois = ""
    private var buttonFlag = 0
    private var trocas = 0
    private var isConnected = false
    private var mostrarJogadas = true
    private var isMinhaVez = false
    private var isGameOver = false
    private var listaMudancas: MutableList<Posicao> = mutableListOf()
    private var listaJogadas: MutableList<Posicao> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTwoclientBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)

        jogadorUm = intent.getStringExtra(MainActivity.jogadorUm).toString()
        listaPosicoes = arrayOfNulls<List<ImageView>>(8).mapIndexed { fila, _ ->
            arrayOfNulls<ImageView>(8).mapIndexed { coluna, _ ->
                val posicao = layoutInflater.inflate(R.layout.posicao,null)
                posicao.setOnClickListener {
                    if(buttonFlag==0 && isConnected && isMinhaVez){
                        onClickPosicao(fila, coluna)
                    }
                    if(buttonFlag==1 && isConnected && isMinhaVez){
                        onClickBomba(fila,coluna)
                    }
                    if(buttonFlag==2 && isConnected && isMinhaVez){
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
        b.btnAtivarBomba.setOnClickListener {
            if(buttonFlag == 0 && isMinhaVez) {
                buttonFlag = 1
                it.visibility = View.GONE
            }
        }
        b.btnAtivarTroca.setOnClickListener {
            if(buttonFlag == 0 && isMinhaVez) {
                buttonFlag = 2
                it.visibility = View.GONE
            }
        }
        b.btnPassarVez.setOnClickListener {
            passarVez()
        }

        setupDialogo()
    }

    private fun passarVez() {
        try{
            val printStream = PrintStream(socket!!.getOutputStream())
            val mensagem = JSONObject()
            mensagem.put("assunto","skip")
            printStream.println(mensagem)
            printStream.flush()
        }
        catch (e: Exception){
            Log.d("TagError", "passarVez falhou: $e")
        }
    }

    private fun leMensagens() {
        thread{
            val bufferIn = socket!!.getInputStream().bufferedReader()
            var mensagem: JSONObject
            var mensagemString : String
            while(!isGameOver){
                mensagemString = bufferIn.readLine()
                mensagem = JSONObject(mensagemString)
                if(mensagem["assunto"].equals("setup")){
                    jogadorDois = mensagem["nome"].toString()
                    checkVez(mensagem)
                    checkMudancas(mensagem)
                    checkJogadas(mensagem)
                }
                else if(mensagem["assunto"].equals("jogada")){
                    checkVez(mensagem)
                    checkMudancas(mensagem)
                    checkJogadas(mensagem)
                }
                else if(mensagem["assunto"].equals("final")){
                    if(mensagem["vencedor"].equals("pretas"))
                        terminaJogo(jogadorDois)
                    else
                        terminaJogo(jogadorUm)
                }
                else if(mensagem["assunto"].equals("troca")){
                    if(mensagem["peca"].equals("preta")){
                        setJogada(Posicao(mensagem.getInt("fila"),mensagem.getInt("coluna"),Peca.PRETA))
                        trocas++
                    }
                    else
                        setJogada(Posicao(mensagem.getInt("fila"),mensagem.getInt("coluna"),Peca.BRANCA))
                }
                else if(mensagem["assunto"].equals("bomba")){
                    val jogadas = mensagem.getJSONArray("mudancas")
                    listaMudancas.clear()
                    for(pos in 0 until jogadas.length()){
                        val posicao = jogadas.getJSONObject(pos)
                        val filaNova = posicao.getInt("fila")
                        val colunaNova = posicao.getInt("coluna")
                        listaMudancas.add(Posicao(filaNova,colunaNova,Peca.VAZIA))
                    }
                    listaMudancas.forEach{ setJogada(it) }
                    if(mensagem["centropeca"].equals("preta")){
                        setJogada(Posicao(mensagem.getInt("centrofila"),mensagem.getInt("centrocoluna"),Peca.PRETA))
                    }
                    else{
                        setJogada(Posicao(mensagem.getInt("centrofila"),mensagem.getInt("centrocoluna"),Peca.BRANCA))
                    }
                    checkVez(mensagem)
                    checkJogadas(mensagem)
                }
                else if(mensagem["assunto"].equals("skip")){
                    checkVez(mensagem)
                    checkJogadas(mensagem)
                }
            }
        }
    }

    private fun terminaJogo(player: String){
        val intent = Intent(this,GameOverActivity::class.java)
        intent.putExtra(GameOverActivity.vencedor, player)
        startActivity(intent)
    }

    private fun checkJogadas(mensagem: JSONObject) {
        this@TwoClientActivity.runOnUiThread {
            if (mensagem["semjogadas"].equals("true")) {
                b.btnPassarVez.visibility = View.VISIBLE
            } else {
                b.btnPassarVez.visibility = View.GONE
                val jogadas = mensagem.getJSONArray("jogadas")
                listaJogadas.clear()
                for (pos in 0 until jogadas.length()) {
                    val posicao = jogadas.getJSONObject(pos)
                    val filaNova = posicao.getInt("fila")
                    val colunaNova = posicao.getInt("coluna")
                    listaJogadas.add(Posicao(filaNova, colunaNova, Peca.BRANCA))
                }
                if (mostrarJogadas) {
                    apagaJogadas()
                    mostraJogadas()
                }
            }
        }
    }

    private fun checkMudancas(mensagem: JSONObject) {
        val jogadas = mensagem.getJSONArray("mudancas")
        listaMudancas.clear()
        for(pos in 0 until jogadas.length()){
            val posicao = jogadas.getJSONObject(pos)
            val filaNova = posicao.getInt("fila")
            val colunaNova = posicao.getInt("coluna")
            val peca : Peca = if(posicao.getString("peca").equals("branca"))
                Peca.BRANCA
            else
                Peca.PRETA
            listaMudancas.add(Posicao(filaNova,colunaNova,peca))

        }
        listaMudancas.forEach{ setJogada(it) }
    }

    private fun checkVez(mensagem: JSONObject) {
        if(mensagem["vez"].equals("client")) {
            isMinhaVez = true
            Log.d("TagCheck", "Cliente a jogar.")
        }
        else {
            isMinhaVez = false
            Log.d("TagCheck", "Servidor a jogar.")
        }
        setStringJogador()
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
                            if (it.isDigit() || it == '.')
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
                iniciaCliente("10.0.2.2", SERVER_PORT-1)
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

    private fun iniciaCliente(ip: String, port: Int = SERVER_PORT-1) {
        try {
            thread{
                socket = Socket()
                Log.d("TagCheck","A conetar a servidor...")
                socket!!.connect(InetSocketAddress(ip, port), 5000)
                isConnected = true
                enviaNome()
            }
        } catch (e: Exception) {
            Log.e("TagSocketError", "Falha ao abrir socket: $e")
        }
    }

    private fun enviaNome(){
        socket!!.getOutputStream()?.run {
            try {
                val printStream = PrintStream(this)
                val json = JSONObject()
                json.put("nome", jogadorUm)
                printStream.println(json.toString())
                printStream.flush()
                Log.d("TagCheck","Nome enviado")
                leMensagens()
            } catch (e: Exception) {
                Log.d("TagSocketError", e.toString())
            }
        }
    }

    private fun apagaJogadas(){
        listaPosicoes.flatten().forEach{ it.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_700)) }
    }

    private fun mostraJogadas(){
        listaJogadas.forEach {
            listaPosicoes[it.fila][it.coluna].setBackgroundColor(ContextCompat.getColor(this, R.color.purple_500))
        }
    }

    private fun setStringJogador(){
        this@TwoClientActivity.runOnUiThread {
            if (isMinhaVez)
                findViewById<TextView>(R.id.tvJogador).text =
                    getString(R.string.jogador_atual, jogadorUm)
            else
                findViewById<TextView>(R.id.tvJogador).text =
                    getString(R.string.jogador_atual, jogadorDois)
        }
    }

    private fun setJogada(pos: Posicao){
        this@TwoClientActivity.runOnUiThread {
            when (pos.peca) {
                Peca.BRANCA -> listaPosicoes[pos.fila][pos.coluna].setImageResource(R.drawable.white_stone)
                Peca.PRETA -> listaPosicoes[pos.fila][pos.coluna].setImageResource(R.drawable.black_stone)
                else -> listaPosicoes[pos.fila][pos.coluna].setImageDrawable(null)
            }
        }
    }

    private fun onClickPosicao(fila: Int,coluna: Int){
        thread {
            try {
                val printStream = PrintStream(socket!!.getOutputStream())
                val json = JSONObject()
                json.put("assunto", "jogada")
                json.put("jogada","normal")
                json.put("fila", fila)
                json.put("coluna", coluna)
                printStream.println(json.toString())
                printStream.flush()
            } catch (e: Exception) {
                Log.d("TagError", "onClickPosicao falhou: $e")
            }
        }
    }

    private fun onClickBomba(fila: Int, coluna: Int){
        thread{
            try {
                val printStream = PrintStream(socket!!.getOutputStream())
                val json = JSONObject()
                json.put("assunto", "jogada")
                json.put("jogada","bomba")
                json.put("fila", fila)
                json.put("coluna", coluna)
                printStream.println(json.toString())
                printStream.flush()
                buttonFlag = 0
            } catch (e: Exception) {
                Log.d("TagError", "onClickBomba falhou: $e")
            }
        }
    }

    private fun onClickTroca(fila: Int, coluna: Int){
        thread{
            try {
                val printStream = PrintStream(socket!!.getOutputStream())
                val json = JSONObject()
                json.put("assunto", "jogada")
                json.put("jogada","troca")
                json.put("fila", fila)
                json.put("coluna", coluna)
                printStream.println(json.toString())
                printStream.flush()
                if(trocas == 2)
                    buttonFlag = 0
            } catch (e: Exception) {
                Log.d("TagError", "onClickTroca falhou: $e")
            }
        }
    }
}