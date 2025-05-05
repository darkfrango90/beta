package com.darkfrango.despesas.model

import java.util.Date

data class Despesa(
    val valor: Double = 0.0,
    val dataHora: Date? = null,
    val fotoUrl: String? = null,
    val userId: String? = null,
    val email: String? = null,
    val nome: String? = null // ❌ aqui faltava a vírgula se tiver quebra de linha abaixo
)