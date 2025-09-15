package com.ifprcrpgtcc.feirajovem.baseclasses

data class Item(
    val titulo: String = "",
    val descricao: String = "",
    val preco: String = "",
    val endereco: String = "",
    val imagemBase64: String = "",
    val dataCriacao: Long = 0L,
    val dataExpiracao: Long = 0L,
    var avaliacao: Float = 0f,           // Média exibida no feed
    var mediaAvaliacao: Float = 0f,      // Média salva no Firebase
    var itemId: String? = null,          // preenchido ao carregar
    var userId: String? = null           // preenchido ao carregar
)
