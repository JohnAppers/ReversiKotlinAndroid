package com.example.reversigame.model

import java.io.Serializable

data class Posicao(val fila: Int, val coluna: Int, var peca: Peca) : Serializable{}