package com.example.reversigame.model

class Tabuleiro {

    val ROWS = 8
    val COLUMNS = 8

    private val pecas = arrayOfNulls<List<Posicao>>(ROWS).mapIndexed { fila, list -> arrayOfNulls<Posicao>(COLUMNS).mapIndexed { coluna, posicao -> Posicao(fila, coluna, Peca.VAZIA) } }

    fun isListaValida(lista:List<Posicao>, pos:Posicao):List<Posicao>{
        val posPecaValida = lista.indexOfFirst { it.peca == pos.peca }
        if(posPecaValida == -1)
            return emptyList()
        val subLista = lista.take(posPecaValida)
        if(subLista.all { it.peca == pos.peca.proximo() })
            return subLista;
        else
            return emptyList()
    }

    fun getPeca(fila:Int, coluna:Int):Peca{
        return pecas[fila][coluna].peca
    }

    fun getListaEsquerda(pos:Posicao):List<Posicao>{
        if(pos.coluna==0) return emptyList()
        val subLista = pecas.flatMap { it }.filter { it.coluna < pos.coluna && it.fila == pos.fila }
        return isListaValida(subLista.reversed(),pos)
    }

    fun getListaDireita(pos:Posicao):List<Posicao>{
        if(pos.coluna+1>=COLUMNS) return emptyList()
        val subLista = pecas.flatMap { it }.filter { it.coluna > pos.coluna && it.fila == pos.fila }
        return isListaValida(subLista,pos)
    }

    fun getListaCima(pos:Posicao):List<Posicao>{
        if(pos.fila==0) return emptyList()
        val subLista = pecas.flatMap { it }.filter { it.coluna == pos.coluna && it.fila < pos.fila }
        return isListaValida(subLista.reversed(),pos)
    }

    fun getListaBaixo(pos:Posicao):List<Posicao>{
        if(pos.fila+1>=ROWS) return emptyList()
        val subLista = pecas.flatMap { it }.filter { it.coluna == pos.coluna && it.fila > pos.fila }
        return isListaValida(subLista,pos)
    }

    fun getListaEsqCima(pos:Posicao):List<Posicao>{
        if(pos.coluna==0 || pos.fila==0) return emptyList()
        val subLista = pecas.flatMap { it }.filter { it.coluna < pos.coluna && it.fila < pos.fila
                && pos.coluna-it.coluna == pos.fila-it.fila}
        return isListaValida(subLista.reversed(),pos)
    }

    fun getListaDirCima(pos:Posicao):List<Posicao>{
        if(pos.coluna+1>=COLUMNS || pos.fila==0) return emptyList()
        val subLista = pecas.flatMap { it }.filter { it.coluna > pos.coluna && it.fila < pos.fila
                && it.coluna-pos.coluna == pos.fila-it.fila}
        return isListaValida(subLista.reversed(),pos)
    }

    fun getListaEsqBaixo(pos:Posicao):List<Posicao>{
        if(pos.coluna==0 || pos.fila+1>=ROWS) return emptyList()
        val subLista = pecas.flatMap { it }.filter { it.coluna < pos.coluna && it.fila > pos.fila
                && pos.coluna-it.coluna == it.fila-pos.fila}
        return isListaValida(subLista,pos)
    }

    fun getListaDirBaixo(pos:Posicao):List<Posicao>{
        if(pos.coluna+1>=COLUMNS || pos.fila+1>=ROWS) return emptyList()
        val subLista = pecas.flatMap { it }.filter { it.coluna > pos.coluna && it.fila > pos.fila
                && it.coluna-pos.coluna == it.fila-pos.fila}
        return isListaValida(subLista,pos)
    }

    fun getListasValidas(pos:Posicao):List<Posicao>{
        return getListaEsquerda(pos).plus(getListaDireita(pos))
            .plus(getListaCima(pos))
            .plus(getListaBaixo(pos))
            .plus(getListaEsqCima(pos))
            .plus(getListaDirCima(pos))
            .plus(getListaEsqBaixo(pos))
            .plus(getListaDirBaixo(pos))
    }

    fun isPosicaoValida(pos:Posicao):Boolean{
        return pecas[pos.fila][pos.coluna].peca == Peca.VAZIA && getListasValidas(pos).isNotEmpty()
    }

    fun getPosicoesValidas(peca:Peca):List<Posicao>{
        return pecas.flatMap { it }.filter { isPosicaoValida(Posicao(it.fila,it.coluna,peca)) }
    }

    fun getPosicoesInicio():List<Posicao>{
        return listOf(Posicao(COLUMNS/2-1,ROWS/2-1,Peca.BRANCA),
            Posicao(COLUMNS/2,ROWS/2-1,Peca.PRETA),
            Posicao(COLUMNS/2-1,ROWS/2,Peca.PRETA),
            Posicao(COLUMNS/2,ROWS/2,Peca.BRANCA)
        )
    }

    fun getPecasPretas():Int{
        return pecas.flatMap { it }.filter { it.peca == Peca.PRETA }.count()
    }

    fun getPecasBrancas():Int{
        return pecas.flatMap { it }.filter { it.peca == Peca.BRANCA }.count()
    }

    fun getVencedor():Peca{
        return if(getPecasBrancas()>getPecasPretas())
            Peca.BRANCA
        else
            Peca.PRETA
    }

    fun setPosicao(pos:Posicao){
        pecas[pos.fila][pos.coluna].peca = pos.peca
    }

    fun setBomba(pos:Posicao):List<Posicao>{
        val lista:List<Posicao> = pecas.flatMap { it }.filter {
            (it.fila == pos.fila && it.coluna == pos.coluna-1) ||
            (it.fila == pos.fila && it.coluna == pos.coluna+1) ||
            (it.fila == pos.fila-1 && it.coluna == pos.coluna-1) ||
            (it.fila == pos.fila-1 && it.coluna == pos.coluna) ||
            (it.fila == pos.fila-1 && it.coluna == pos.coluna+1) ||
            (it.fila == pos.fila+1 && it.coluna == pos.coluna-1) ||
            (it.fila == pos.fila+1 && it.coluna == pos.coluna) ||
            (it.fila == pos.fila+1 && it.coluna == pos.coluna+1)
        }
        lista.forEach{ it.peca = Peca.VAZIA }
        return lista
    }

    fun isFinalJogo():Boolean{
        return getPosicoesValidas(Peca.BRANCA).isEmpty() && getPosicoesValidas(Peca.PRETA).isEmpty()
    }
}