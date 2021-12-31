package com.example.reversigame.model

import java.io.Serializable

enum class Peca : Serializable{

    BRANCA, PRETA, VAZIA;
    fun proximo() = if(this == BRANCA) PRETA else BRANCA
    fun getPeca() = this.name
}