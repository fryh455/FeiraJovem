package com.ifprcrpgtcc.feirajovem.ui.admin

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ifprcrpgtcc.feirajovem.R

class AdminPanelActivity : AppCompatActivity() {

    private lateinit var navView: BottomNavigationView
    private var escolaAdmin: String? = null
    private val TAG = "AdminPanel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_panel)

        navView = findViewById(R.id.nav_view_admin)

        // Recebe escolaAdmin da intent
        escolaAdmin = intent.getStringExtra("escolaAdmin")
        Log.d(TAG, "Escola do admin recebida: $escolaAdmin")

        if (escolaAdmin.isNullOrEmpty()) {
            Toast.makeText(this, "Erro: usuário sem escola", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fragment inicial: usuários
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.admin_panel_fragment_container,
                UsuariosFragment().apply { this.escolaAdmin = this@AdminPanelActivity.escolaAdmin }
            )
            .commit()

        // Configura BottomNavigation
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_usuarios -> {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.admin_panel_fragment_container,
                            UsuariosFragment().apply { this.escolaAdmin = this@AdminPanelActivity.escolaAdmin }
                        )
                        .commit()
                    true
                }
                R.id.navigation_produtos -> {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.admin_panel_fragment_container,
                            ProdutosFragment().apply { this.escolaAdmin = this@AdminPanelActivity.escolaAdmin }
                        )
                        .commit()
                    true
                }
                R.id.navigation_denuncias -> {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.admin_panel_fragment_container,
                            DenunciasFragment().apply { this.escolaAdmin = this@AdminPanelActivity.escolaAdmin }
                        )
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}
