package com.example.reversigame

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.reversigame.databinding.ActivityMainBinding
import com.example.reversigame.model.Peca
import com.example.reversigame.model.Posicao
import com.example.reversigame.model.Tabuleiro

class MainActivity : AppCompatActivity() {

    companion object{
        var jogadorUm = ""
    }

    private val jogo:Tabuleiro = Tabuleiro()
    private val filasTabuleiro = jogo.ROWS
    private val colunasTabuleiro = jogo.COLUMNS

    private var jogadorAtual = Peca.PRETA
    private var mostrarJogadas = true
    private var semJogadas = false
    private var buttonFlag = 0
    private var bombPecasPretas = 1
    private var bombPecasBrancas = 1
    private var trocaPecasPretas = 1
    private var trocaPecasBrancas = 1
    private var nTrocaPecas = 0

    private lateinit var listaPosicoes: List<List<ImageView>>
    private lateinit var b : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)

        jogadorUm = intent.getStringExtra(jogadorUm).toString()

        listaPosicoes = arrayOfNulls<List<ImageView>>(filasTabuleiro).mapIndexed { fila, _ ->
                arrayOfNulls<ImageView>(colunasTabuleiro).mapIndexed { coluna, _ ->
                    val posicao = layoutInflater.inflate(R.layout.posicao,null)
                    posicao.setOnClickListener {
                        if(buttonFlag==0){
                            onClickPosicao(fila, coluna)
                        }
                        if(buttonFlag==1){
                            onClickBomba(fila,coluna)
                        }
                        if(buttonFlag==2){
                            onClickTroca(fila,coluna)
                        }
                    }
                    b.gridLayout.addView(posicao)
                    posicao.findViewById(R.id.btnPosicao) as ImageView
                }
        }
        b.btnOptJogadas.setOnClickListener {
            if(mostrarJogadas){
                mostrarJogadas = false
                apagaJogadas()
            }
            else{
                mostrarJogadas = true
                mostraJogadas()
            }
        }
        b.btnPassarVez.setOnClickListener {
            if(semJogadas){
                apagaJogadas()
                proximoJogador()
                mostraJogadas()
                verificaJogadaPossivel(jogadorAtual)
            }
        }
        b.btnAtivarBomba.setOnClickListener {
            if(buttonFlag == 0) {
                if (jogadorAtual == Peca.PRETA)
                    bombPecasPretas = 0
                else
                    bombPecasBrancas = 0
                buttonFlag = 1
                it.visibility = View.GONE
            }
        }
        b.btnAtivarTroca.setOnClickListener {
            if(buttonFlag == 0) {
                if (jogadorAtual == Peca.PRETA)
                    trocaPecasPretas = 0
                else
                    trocaPecasBrancas = 0
                buttonFlag = 2
                it.visibility = View.GONE
            }
        }

        jogo.getPosicoesInicio().forEach{ setJogada(it) }
        if((0..1).random() == 1)
            proximoJogador()
        setStringJogador()

        mostraJogadas()
    }

    private fun setJogada(pos: Posicao){
        jogo.setPosicao(pos)
        if(pos.peca == Peca.PRETA)
            listaPosicoes[pos.fila][pos.coluna].setImageResource(R.drawable.black_stone)
        else
            listaPosicoes[pos.fila][pos.coluna].setImageResource(R.drawable.white_stone)
    }

    private fun setStringJogador(){
        if(jogadorAtual == Peca.PRETA)
            findViewById<TextView>(R.id.tvJogador).text = getString(R.string.jogador_atual, jogadorUm)
        else
            findViewById<TextView>(R.id.tvJogador).text = getString(R.string.jogador_atual,"convidado")
    }

    private fun proximoJogador(){
        jogadorAtual = jogadorAtual.proximo()
        setStringJogador()
        if(jogadorAtual == Peca.PRETA){
            if(bombPecasPretas == 0)
                b.btnAtivarBomba.visibility = View.GONE
            else
                b.btnAtivarBomba.visibility = View.VISIBLE
            if(trocaPecasPretas == 0)
                b.btnAtivarTroca.visibility = View.GONE
            else
                b.btnAtivarTroca.visibility = View.VISIBLE
        }
        if(jogadorAtual == Peca.BRANCA){
            if(trocaPecasBrancas == 0)
                b.btnAtivarTroca.visibility = View.GONE
            else
                b.btnAtivarTroca.visibility = View.VISIBLE
            if(bombPecasBrancas == 0)
                b.btnAtivarBomba.visibility = View.GONE
            else
                b.btnAtivarBomba.visibility = View.VISIBLE
        }
    }

    private fun mostraJogadas(){
        if(!mostrarJogadas) return
        jogo.getPosicoesValidas(jogadorAtual).forEach{
            listaPosicoes[it.fila][it.coluna].setBackgroundColor(ContextCompat.getColor(this, R.color.purple_500))
        }
    }

    private fun apagaJogadas(){
        listaPosicoes.flatten().forEach{ it.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_700)) }
    }

    private fun verificaJogadaPossivel(jogador:Peca){
        if(jogo.getPosicoesValidas(jogador).isNotEmpty()) {
            semJogadas = false
            b.btnPassarVez.visibility = View.GONE
        }
        else{
            semJogadas = true
            b.btnPassarVez.visibility = View.VISIBLE
        }
    }

    private fun onClickPosicao(fila:Int, coluna:Int){
        val novaPosicao = Posicao(fila, coluna, jogadorAtual)

        if(!jogo.isPosicaoValida(novaPosicao))
            return

        jogo.getListasValidas(novaPosicao).plus(novaPosicao).forEach{ setJogada(Posicao(it.fila,it.coluna,jogadorAtual)) }

        if(jogo.isFinalJogo()){
            val intent = Intent(this,GameOverActivity::class.java)
            if(jogo.getVencedor() == Peca.PRETA)
                intent.putExtra(GameOverActivity.vencedor, jogadorUm)
            else
                intent.putExtra(GameOverActivity.vencedor, "convidado")
            startActivity(intent)
        }
        apagaJogadas()
        proximoJogador()
        mostraJogadas()
        verificaJogadaPossivel(jogadorAtual)
    }

    private fun onClickBomba(fila:Int, coluna:Int){
        buttonFlag = 0
        val novaPosicao = Posicao(fila, coluna, jogadorAtual)
        setJogada(novaPosicao)
        jogo.setBomba(novaPosicao).forEach { listaPosicoes[it.fila][it.coluna].setImageDrawable(null) }

        apagaJogadas()
        proximoJogador()
        mostraJogadas()
        verificaJogadaPossivel(jogadorAtual)
    }

    private fun onClickTroca(fila: Int, coluna: Int){
        if(jogo.getPeca(fila,coluna) == jogadorAtual && nTrocaPecas < 2){
            if(jogo.getPeca(fila,coluna)==Peca.BRANCA)
                listaPosicoes[fila][coluna].setImageResource(R.drawable.black_stone)
            else
                listaPosicoes[fila][coluna].setImageResource(R.drawable.white_stone)
            nTrocaPecas++
        }
        if(jogo.getPeca(fila,coluna) == jogadorAtual.proximo() && nTrocaPecas == 2){
            if(jogo.getPeca(fila,coluna)==Peca.BRANCA)
                listaPosicoes[fila][coluna].setImageResource(R.drawable.black_stone)
            else
                listaPosicoes[fila][coluna].setImageResource(R.drawable.white_stone)
            nTrocaPecas = 0
            apagaJogadas()
            proximoJogador()
            mostraJogadas()
            verificaJogadaPossivel(jogadorAtual)
            buttonFlag = 0
        }
    }
}