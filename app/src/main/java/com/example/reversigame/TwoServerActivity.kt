package com.example.reversigame

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.reversigame.databinding.ActivityTwoserverBinding
import com.example.reversigame.model.Peca
import com.example.reversigame.model.Posicao
import com.example.reversigame.model.Tabuleiro
import org.json.JSONArray
import org.json.JSONObject
import java.io.PrintStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

const val SERVER_PORT = 9999

class TwoServerActivity : AppCompatActivity() {

    private lateinit var b : ActivityTwoserverBinding
    private var dlg: AlertDialog? = null
    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null

    private val jogo: Tabuleiro = Tabuleiro()
    private lateinit var listaPosicoes: List<List<ImageView>>
    private var jogadorAtual = Peca.PRETA
    private var jogadorUm = ""
    private var jogadorDois = ""
    private val filasJogo = jogo.ROWS
    private val colunasJogo = jogo.COLUMNS
    private var buttonFlag = 0
    private var bombPecasPretas = 1
    private var bombPecasBrancas = 1
    private var trocaPecasPretas = 1
    private var trocaPecasBrancas = 3
    private var nTrocaPecas = 0
    private var mostrarJogadas = true
    private var isSemJogadas = false
    private var isGameOver = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTwoserverBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)
        jogadorUm = intent.getStringExtra(MainActivity.jogadorUm).toString()

        listaPosicoes = arrayOfNulls<List<ImageView>>(8).mapIndexed { fila, _ ->
            arrayOfNulls<ImageView>(8).mapIndexed { coluna, _ ->
                val posicao = layoutInflater.inflate(R.layout.posicao,null)
                posicao.setOnClickListener {
                    if(buttonFlag==0 && jogadorAtual == Peca.PRETA){
                        onClickPosicao(fila, coluna)
                    }
                    if(buttonFlag==1 && jogadorAtual == Peca.PRETA){
                        onClickBomba(fila,coluna)
                    }
                    if(buttonFlag==2 && jogadorAtual == Peca.PRETA){
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
        b.btnPassarVez.setOnClickListener {
            if(isSemJogadas && jogadorAtual == Peca.PRETA){
                apagaJogadas()
                proximoJogador()
                mostraJogadas()
                verificaJogadaPossivel(jogadorAtual)
                val resposta = JSONObject()
                resposta.put("assunto","skip")
                resposta.put("vez","client")
                resposta.put("jogadas",getPosJsonArray(jogo.getPosicoesValidas(Peca.BRANCA)))
                if(isSemJogadas)
                    resposta.put("semjogadas","true")
                else
                    resposta.put("semjogadas","false")
                enviaMensagem(resposta)
            }
        }
        b.btnAtivarBomba.setOnClickListener {
            if(buttonFlag == 0 && jogadorAtual == Peca.PRETA) {
                bombPecasPretas = 0
                buttonFlag = 1
                it.visibility = View.GONE
            }
        }
        b.btnAtivarTroca.setOnClickListener {
            if(buttonFlag == 0  && jogadorAtual == Peca.PRETA) {
                trocaPecasPretas = 0
                buttonFlag = 2
                it.visibility = View.GONE
            }
        }

        setupDialogo()
        iniciaServidor()
        getPosicoesInicio().forEach { setJogada(Posicao(it.fila,it.coluna,it.peca)) }
        if((0..1).random() == 1)
            proximoJogador()
        setStringJogador()
        mostraJogadas()
    }

    private fun setJogada(pos: Posicao){
        this@TwoServerActivity.runOnUiThread {
            jogo.setPosicao(pos)
            if (pos.peca == Peca.PRETA)
                listaPosicoes[pos.fila][pos.coluna].setImageResource(R.drawable.black_stone)
            else
                listaPosicoes[pos.fila][pos.coluna].setImageResource(R.drawable.white_stone)
        }
    }

    private fun proximoJogador(){
        this@TwoServerActivity.runOnUiThread {
            jogadorAtual = jogadorAtual.proximo()
            setStringJogador()
            if (jogadorAtual == Peca.PRETA) {
                if (bombPecasPretas == 0)
                    b.btnAtivarBomba.visibility = View.GONE
                else
                    b.btnAtivarBomba.visibility = View.VISIBLE
                if (trocaPecasPretas == 0)
                    b.btnAtivarTroca.visibility = View.GONE
                else
                    b.btnAtivarTroca.visibility = View.VISIBLE
            } else
                b.btnAtivarTroca.visibility = View.GONE
        }
    }

    private fun setStringJogador(){
        this@TwoServerActivity.runOnUiThread {
            if (jogadorAtual == Peca.PRETA)
                findViewById<TextView>(R.id.tvJogador).text =
                    getString(R.string.jogador_atual, jogadorUm)
            else
                findViewById<TextView>(R.id.tvJogador).text =
                    getString(R.string.jogador_atual, jogadorDois)
        }
    }

    private fun getPosicoesInicio():List<Posicao>{
        return listOf(Posicao(colunasJogo/2-1,filasJogo/2-1,Peca.BRANCA),
            Posicao(colunasJogo/2,filasJogo/2-1,Peca.PRETA),
            Posicao(colunasJogo/2-1,filasJogo/2,Peca.PRETA),
            Posicao(colunasJogo/2,filasJogo/2,Peca.BRANCA)
        )
    }

    private fun mostraJogadas(){
        this@TwoServerActivity.runOnUiThread {
            if (mostrarJogadas) {
                jogo.getPosicoesValidas(jogadorAtual).forEach {
                    listaPosicoes[it.fila][it.coluna].setBackgroundColor(
                        ContextCompat.getColor(
                            this,
                            R.color.purple_500
                        )
                    )
                }
            }
        }
    }

    private fun apagaJogadas(){
        this@TwoServerActivity.runOnUiThread {
            listaPosicoes.flatten()
                .forEach { it.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_700)) }
        }
    }

    private fun verificaJogadaPossivel(jogador:Peca){
        if(jogo.getPosicoesValidas(jogador).isNotEmpty()) {
            isSemJogadas = false
            if(jogador == Peca.PRETA)
                b.btnPassarVez.visibility = View.GONE
        }
        else{
            isSemJogadas = true
            if(jogador == Peca.PRETA)
                b.btnPassarVez.visibility = View.VISIBLE
        }
    }

    private fun setupDialogo(){
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress
        val strIPAddress = String.format("%d.%d.%d.%d",
            ip and 0xff,
            (ip shr 8) and 0xff,
            (ip shr 16) and 0xff,
            (ip shr 24) and 0xff
        )

        val ll = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            this.setPadding(50, 50, 50, 50)
            setBackgroundColor(Color.rgb(240, 224, 208))
            orientation = LinearLayout.HORIZONTAL

            addView(ProgressBar(context).apply {
                isIndeterminate = true
                val paramsPB = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                paramsPB.gravity = Gravity.CENTER_VERTICAL
                layoutParams = paramsPB
                indeterminateTintList = ColorStateList.valueOf(Color.rgb(96, 96, 32))
            })

            addView(TextView(context).apply {
                val paramsTV = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                layoutParams = paramsTV
                text = String.format(getString(R.string.msg_ip_address),strIPAddress)
                textSize = 20f
                setTextColor(Color.rgb(96, 96, 32))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            })
        }

        dlg = AlertDialog.Builder(this).run {
            setTitle(getString(R.string.modo_servidor))
            setView(ll)
            setOnCancelListener {
                finish()
            }
            create()
        }
        dlg?.show()
    }

    private fun iniciaServidor(){
        thread {
            serverSocket = ServerSocket(SERVER_PORT)
            serverSocket?.apply {
                try {
                    if(serverSocket == null)
                        Log.d("TagSocketError","Socket is null")
                    Log.d("TagCheck","A espera do cliente em "+serverSocket!!.inetAddress.toString())
                    socket = serverSocket!!.accept()
                    Log.d("TagSuccess", "Cliente conectado.")
                    leMensagem()
                    setupCliente()
                }
                catch (_: Exception) {
                    Log.d("Erro", "Conexao falhou")
                }
                finally {
                    serverSocket?.close()
                }
            }
        }
    }

    private fun setupCliente() {
        val mensagem = JSONObject()
        mensagem.put("assunto","setup")
        mensagem.put("nome",jogadorUm)
        if(jogadorAtual == Peca.PRETA)
            mensagem.put("vez","server")
        else
            mensagem.put("vez","client")
        mensagem.put("mudancas",getPosJsonArray(getPosicoesInicio()))
        mensagem.put("jogadas",getPosJsonArray(jogo.getPosicoesValidas(jogadorAtual)))
        mensagem.put("semjogadas","false")
        enviaMensagem(mensagem)
    }

    private fun getPosJsonArray(lista: List<Posicao>): JSONArray {
        val arrayPos = JSONArray()
        lista.forEach {
            val jsonObject = JSONObject()
            jsonObject.put("fila", it.fila)
            jsonObject.put("coluna", it.coluna)
            if(it.peca == Peca.BRANCA)
                jsonObject.put("peca","branca")
            else
                jsonObject.put("peca","preta")
            arrayPos.put(jsonObject)
        }
        return arrayPos
    }

    private fun leMensagem(){
        thread {
            while(!isGameOver) {
                try {
                    val bufferIn = socket!!.getInputStream().bufferedReader()
                    var mensagem: JSONObject
                    var mensagemString: String
                    if (jogadorDois == "") {
                        Log.d("TagCheck", "A espera de nome...")
                        mensagemString = bufferIn.readLine()
                        mensagem = JSONObject(mensagemString)
                        jogadorDois = mensagem["nome"].toString()
                        Log.d("TagCheck", "Nome recebido: $jogadorDois")
                        setStringJogador()
                        dlg?.dismiss()
                        continue
                    }
                    mensagemString = bufferIn.readLine()
                    mensagem = JSONObject(mensagemString)
                    Log.d("TagJson", "Mensagem recebida, assunto: "+mensagem["assunto"].toString())
                    if(mensagem["assunto"].equals("jogada")){
                        if (mensagem["jogada"].equals("normal"))
                            setJogadaClient(mensagem)
                        if (mensagem["jogada"].equals("bomba"))
                            setBombaClient(mensagem)
                        if (mensagem["jogada"].equals("troca"))
                            setTrocaClient(mensagem)
                    }
                    else if(mensagem["assunto"].equals("skip") && isSemJogadas){
                        apagaJogadas()
                        proximoJogador()
                        mostraJogadas()
                        verificaJogadaPossivel(jogadorAtual)
                        val resposta = JSONObject()
                        resposta.put("assunto","skip")
                        resposta.put("vez","server")
                        resposta.put("jogadas",getPosJsonArray(jogo.getPosicoesValidas(Peca.PRETA)))
                        if(isSemJogadas)
                            resposta.put("semjogadas","true")
                        else
                            resposta.put("semjogadas","false")
                        enviaMensagem(resposta)
                    }
                } catch (e: Exception) {
                    Log.d("Erro", "leMensagem falhou: $e")
                }
            }
        }
    }

    private fun enviaMensagem(mensagem: JSONObject){
        try{
            thread {
                val printStream = PrintStream(socket!!.getOutputStream())
                printStream.println(mensagem.toString())
                printStream.flush()
            }
        }catch(e: Exception){
            Log.d("TagError", "enviaMensagem falhou: $e")
        }
    }

    private fun setJogadaClient(mensagem: JSONObject) {
        val fila = mensagem["fila"].toString().toInt()
        val coluna = mensagem["coluna"].toString().toInt()
        val novaPosicao = Posicao(fila, coluna, Peca.BRANCA)

        if (!jogo.isPosicaoValida(novaPosicao)) {
            return
        }
        val lista = jogo.getListasValidas(novaPosicao).plus(novaPosicao)
        lista.forEach{ setJogada(Posicao(it.fila,it.coluna,jogadorAtual)) }

        if(jogo.isFinalJogo()){
            terminaJogo()
        }

        apagaJogadas()
        proximoJogador()
        mostraJogadas()
        verificaJogadaPossivel(jogadorAtual)

        val resposta = JSONObject()
        resposta.put("assunto","jogada")
        resposta.put("vez","server")
        resposta.put("mudancas",getPosJsonArray(lista))
        resposta.put("jogadas",getPosJsonArray(jogo.getPosicoesValidas(Peca.PRETA)))
        if(isSemJogadas)
            resposta.put("semjogadas","true")
        else
            resposta.put("semjogadas","false")
        enviaMensagem(resposta)
    }

    private fun setBombaClient(mensagem: JSONObject) {
        if(bombPecasBrancas == 0)
            return
        bombPecasBrancas = 0
        val novaPosicao = Posicao(mensagem.getInt("fila"), mensagem.getInt("coluna"), Peca.BRANCA)
        setJogada(novaPosicao)
        val jogadas = jogo.setBomba(novaPosicao)
        jogadas.forEach { listaPosicoes[it.fila][it.coluna].setImageDrawable(null) }

        apagaJogadas()
        proximoJogador()
        mostraJogadas()
        verificaJogadaPossivel(jogadorAtual)

        if(jogo.isFinalJogo()){
            terminaJogo()
        }

        val resposta = JSONObject()
        resposta.put("assunto","bomba")
        resposta.put("centrofila",novaPosicao.fila)
        resposta.put("centrocoluna",novaPosicao.coluna)
        resposta.put("centropeca","branca")
        resposta.put("vez","server")
        resposta.put("mudancas",getPosJsonArray(jogadas))
        resposta.put("jogadas",getPosJsonArray(jogo.getPosicoesValidas(Peca.PRETA)))
        if(isSemJogadas)
            resposta.put("semjogadas","true")
        else
            resposta.put("semjogadas","false")
        enviaMensagem(resposta)
    }

    private fun setTrocaClient(mensagem: JSONObject) {
        if(trocaPecasBrancas == 0)
            return
        val fila = mensagem.getInt("fila")
        val coluna = mensagem.getInt("coluna")

        if(trocaPecasBrancas == 1){
            if(jogo.getPeca(fila,coluna) == Peca.PRETA)
                return
            listaPosicoes[fila][coluna].setImageResource(R.drawable.black_stone)
            val resposta = JSONObject()
            resposta.put("assunto","troca")
            resposta.put("fila",fila)
            resposta.put("coluna",coluna)
            resposta.put("peca","preta")
            enviaMensagem(resposta)
        }
        else{
            if(jogo.getPeca(fila,coluna) == Peca.BRANCA)
                return
            listaPosicoes[fila][coluna].setImageResource(R.drawable.white_stone)
            apagaJogadas()
            proximoJogador()
            mostraJogadas()
            verificaJogadaPossivel(jogadorAtual)
            val jogada = List(1){Posicao(fila,coluna,Peca.BRANCA)}
            val resposta = JSONObject()
            resposta.put("assunto","jogada")
            resposta.put("vez","server")
            resposta.put("mudancas",getPosJsonArray(jogada))
            resposta.put("jogadas",getPosJsonArray(jogo.getPosicoesValidas(Peca.PRETA)))
            if(isSemJogadas)
                resposta.put("semjogadas","true")
            else
                resposta.put("semjogadas","false")
            enviaMensagem(resposta)
        }
        trocaPecasBrancas--
    }

    private fun onClickPosicao(fila:Int, coluna:Int){
        Log.d("TagCheck", "onClick em fila: $fila coluna: $coluna")
        val novaPosicao = Posicao(fila, coluna, jogadorAtual)
        if(!jogo.isPosicaoValida(novaPosicao))
            return

        val jogadas = jogo.getListasValidas(novaPosicao).plus(novaPosicao)
        jogadas.forEach{ setJogada(Posicao(it.fila,it.coluna,jogadorAtual)) }

        if(jogo.isFinalJogo()){
            terminaJogo()
        }
        apagaJogadas()
        proximoJogador()
        mostraJogadas()
        verificaJogadaPossivel(jogadorAtual)

        val mensagem = JSONObject()
        mensagem.put("assunto","jogada")
        mensagem.put("vez","client")
        mensagem.put("mudancas",getPosJsonArray(jogadas))
        mensagem.put("jogadas",getPosJsonArray(jogo.getPosicoesValidas(Peca.BRANCA)))
        if(isSemJogadas)
            mensagem.put("semjogadas","true")
        else
            mensagem.put("semjogadas","false")
        enviaMensagem(mensagem)
    }

    private fun terminaJogo() {
        val intent = Intent(this,GameOverActivity::class.java)
        val mensagem = JSONObject()
        mensagem.put("assunto","final")
        if(jogo.getVencedor() == Peca.PRETA) {
            intent.putExtra(GameOverActivity.vencedor, jogadorUm)
            mensagem.put("vencedor","pretas")
        }
        else {
            intent.putExtra(GameOverActivity.vencedor, jogadorDois)
            mensagem.put("vencedor","brancas")
        }
        enviaMensagem(mensagem)
        startActivity(intent)
    }

    private fun onClickBomba(fila:Int, coluna:Int){
        buttonFlag = 0
        val novaPosicao = Posicao(fila, coluna, Peca.PRETA)
        setJogada(novaPosicao)
        val jogadas = jogo.setBomba(novaPosicao)
        jogadas.forEach { listaPosicoes[it.fila][it.coluna].setImageDrawable(null) }

        apagaJogadas()
        proximoJogador()
        mostraJogadas()
        verificaJogadaPossivel(jogadorAtual)

        if(jogo.isFinalJogo()){
            terminaJogo()
        }

        val mensagem = JSONObject()
        mensagem.put("assunto","bomba")
        mensagem.put("centrofila",fila)
        mensagem.put("centrocoluna",coluna)
        mensagem.put("centropeca","preta")
        mensagem.put("vez","client")
        mensagem.put("mudancas",getPosJsonArray(jogadas))
        mensagem.put("jogadas",getPosJsonArray(jogo.getPosicoesValidas(Peca.BRANCA)))
        if(isSemJogadas)
            mensagem.put("semjogadas","true")
        else
            mensagem.put("semjogadas","false")
        enviaMensagem(mensagem)
    }

    private fun onClickTroca(fila: Int, coluna: Int){
        if(jogo.getPeca(fila,coluna) == Peca.PRETA && nTrocaPecas < 2){
            listaPosicoes[fila][coluna].setImageResource(R.drawable.white_stone)
            nTrocaPecas++
            val mensagem = JSONObject()
            mensagem.put("assunto","troca")
            mensagem.put("fila",fila)
            mensagem.put("coluna",coluna)
            mensagem.put("peca","branca")
            enviaMensagem(mensagem)
        }
        else if(jogo.getPeca(fila,coluna) == Peca.BRANCA && nTrocaPecas == 2){
            listaPosicoes[fila][coluna].setImageResource(R.drawable.black_stone)
            nTrocaPecas = 0
            apagaJogadas()
            proximoJogador()
            mostraJogadas()
            verificaJogadaPossivel(jogadorAtual)
            buttonFlag = 0
            val jogada = List(1){Posicao(fila,coluna,Peca.PRETA)}
            val mensagem = JSONObject()
            mensagem.put("assunto","jogada")
            mensagem.put("vez","client")
            mensagem.put("mudancas",getPosJsonArray(jogada))
            mensagem.put("jogadas",getPosJsonArray(jogo.getPosicoesValidas(Peca.BRANCA)))
            if(isSemJogadas)
                mensagem.put("semjogadas","true")
            else
                mensagem.put("semjogadas","false")
            enviaMensagem(mensagem)
        }
        if(jogo.isFinalJogo()){
            terminaJogo()
        }
    }
}