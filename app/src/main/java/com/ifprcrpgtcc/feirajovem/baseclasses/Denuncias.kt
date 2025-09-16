package com.ifprcrpgtcc.feirajovem.baseclasses

data class Denuncia(
    var denunciaId: String? = null,       // ID gerado no Firebase para a denúncia
    var denuncianteId: String? = null,    // Usuário que fez a denúncia
    var denunciadoId: String? = null,     // Usuário que está sendo denunciado
    var itemId: String? = null,           // ID do produto (opcional, se denúncia for de produto)
    var motivo: String? = null,           // Motivo da denúncia
    var status: String = "pendente",      // "pendente", "resolvido" ou "ignorado"
    var data: Long = System.currentTimeMillis() // Timestamp da criação
)
