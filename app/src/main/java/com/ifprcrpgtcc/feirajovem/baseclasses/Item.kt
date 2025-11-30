package com.ifprcrpgtcc.feirajovem.baseclasses

data class Item(
    var titulo: String = "",
    var descricao: String = "",
    var preco: String = "",
    var endereco: String = "",
    var imagemBase64: String = "",
    var dataCriacao: Long = 0L,
    var dataExpiracao: Long = 0L,
    var avaliacao: Float = 0f,           // Média exibida no feed
    var mediaAvaliacao: Float = 0f,      // Média salva no Firebase
    var itemId: String? = null,          // preenchido ao carregar
    var userId: String? = null           // preenchido ao carregar
)
