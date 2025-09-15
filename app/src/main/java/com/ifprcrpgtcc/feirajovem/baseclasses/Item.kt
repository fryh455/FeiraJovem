package com.ifprcrpgtcc.feirajovem.baseclasses

data class Item(
    val titulo: String = "",
    val descricao: String = "",
    val preco: String = "",
    val endereco: String = "",
    val imagemBase64: String = "",
    val dataCriacao: Long = 0L,
    val dataExpiracao: Long = 0L,
    val avaliacao: Float = 0f,
    var itemId: String? = null, // preenchido ao carregar
    var userId: String? = null  // preenchido ao carregar
)
