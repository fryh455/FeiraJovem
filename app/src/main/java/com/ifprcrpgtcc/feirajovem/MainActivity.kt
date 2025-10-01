package com.ifprcrpgtcc.feirajovem

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.ifprcrpgtcc.feirajovem.databinding.ActivityMainBinding
import android.app.NotificationChannel
import android.app.NotificationManager
import com.ifprcrpgtcc.feirajovem.baseclasses.Usuario
import com.ifprcrpgtcc.feirajovem.ui.admin.AdminPanelFragment
import com.ifprcrpgtcc.feirajovem.ui.login.LoginActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val NOTIFICATION_PERMISSION_CODE = 1001
    private val CHANNEL_ID = "default"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()
        requestNotificationPermission()
        configurarFCMToken()
        verificarTipoUsuario()
    }

    private fun verificarTipoUsuario() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid

        if (uid == null) {
            goToLogin("Usuário não autenticado.")
            return
        }

        val ref = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)
        ref.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val usuario = snapshot.getValue(Usuario::class.java)
                val navView: BottomNavigationView = binding.navView

                if (usuario?.tipo == "admin") {
                    // Ativa item admin no menu (garante que o ID exista no menu XML)
                    navView.menu.findItem(R.id.navigation_admin)?.isVisible = true

                    // Configura listener da BottomNavigationView
                    navView.setOnItemSelectedListener { item ->
                        when (item.itemId) {
                            R.id.navigation_admin -> {
                                // Mostra o fragment admin
                                supportFragmentManager.beginTransaction()
                                    .replace(R.id.admin_feed_container, AdminPanelFragment())
                                    .commit()
                                binding.adminFeedContainer.visibility = View.VISIBLE

                                // Oculta fragment normal do feed
                                supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)?.view?.visibility = View.GONE
                                true
                            }
                            else -> {
                                // Exibe feed normal
                                val navController = findNavController(R.id.nav_host_fragment_activity_main)
                                navController.navigate(item.itemId)
                                binding.adminFeedContainer.visibility = View.GONE
                                supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)?.view?.visibility = View.VISIBLE
                                true
                            }
                        }
                    }

                } else {
                    // Usuário comum → nav padrão
                    configurarNavegacaoPadrao()
                }
            } else {
                goToLogin("Usuário não encontrado. Faça login novamente.")
            }
        }.addOnFailureListener { exception ->
            Log.e("MainActivity", "Erro ao verificar tipo de usuário", exception)
            goToLogin("Erro ao verificar usuário. Tente logar novamente.")
        }
    }

    private fun goToLogin(message: String? = null) {
        try { FirebaseAuth.getInstance().signOut() } catch (t: Throwable) { Log.w("MainActivity", "Erro ao deslogar", t) }
        message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun configurarNavegacaoPadrao() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
        if (navHostFragment != null && navHostFragment.isAdded && navHostFragment.view != null) {
            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            val navView: BottomNavigationView = binding.navView
            val appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.navigation_home,
                    R.id.navigation_dashboard,
                    R.id.navigation_profile
                )
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            navView.setupWithNavController(navController)
        } else {
            Log.d("MainActivity", "NavHostFragment não disponível — pulando configuração de navegação")
        }
    }

    private fun configurarFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Erro ao pegar token", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM_TOKEN", "Token gerado: $token")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            } else {
                Log.d("FCM", "Permissão para notificações já concedida")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("FCM", "Permissão para notificações concedida")
            } else {
                Log.e("FCM", "Permissão para notificações NEGADA")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Canal Padrão",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para notificações do app"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
