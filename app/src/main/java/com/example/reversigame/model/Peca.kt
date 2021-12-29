package com.example.reversigame.model

enum class Peca {

    BRANCA, PRETA, VAZIA;
    fun proximo() = if(this == BRANCA) PRETA else BRANCA
}