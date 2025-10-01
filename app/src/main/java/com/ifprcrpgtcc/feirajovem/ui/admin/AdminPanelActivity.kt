package com.ifprcrpgtcc.feirajovem.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.ifprcrpgtcc.feirajovem.R

class AdminPanelActivity : AppCompatActivity() {

    private lateinit var btnUsuarios: Button
    private lateinit var btnProdutos: Button
    private lateinit var btnDenuncias: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_panel)

        // Inicializa os botões
        btnUsuarios = findViewById(R.id.buttonGerenciarUsuarios)
        btnProdutos = findViewById(R.id.buttonGerenciarProdutos)
        btnDenuncias = findViewById(R.id.buttonGerenciarDenuncias)

        // Abre a tela de gerenciamento de usuários
        btnUsuarios.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, UsuariosFragment())
                .addToBackStack(null)
                .commit()
        }

        // Abre a tela de gerenciamento de produtos
        btnProdutos.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, ProdutosFragment())
                .addToBackStack(null)
                .commit()
        }

        // Abre a tela de gerenciamento de denúncias
        btnDenuncias.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, DenunciasFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
