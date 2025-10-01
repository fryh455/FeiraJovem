package com.ifprcrpgtcc.feirajovem.baseclasses

data class Usuario(
    var key: String? = null,
    var nome: String? = null,
    var email: String? = null,
    var escola: String? = null,
    var tipo: String? = "user comum",
    var endereco: String? = null,
    var fotoBase64: String? = null
)