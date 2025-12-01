package com.ifprcrpgtcc.feirajovem

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.ifprcrpgtcc.feirajovem.baseclasses.Usuario
import com.ifprcrpgtcc.feirajovem.databinding.ActivityMainBinding
import com.ifprcrpgtcc.feirajovem.ui.admin.AdminPanelActivity
import com.ifprcrpgtcc.feirajovem.ui.login.LoginActivity
import java.util.Locale

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
        verificarTipoUsuario()
        configurarFCMToken() // pega token, salva e inscreve no tópico da escola
    }

    // verifica tipo de usuário (admin / comum) e configura menu
    private fun verificarTipoUsuario() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid

        if (uid == null) {
            goToLogin("Usuário não autenticado.")
            return
        }

        val ref = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)

        ref.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    goToLogin("Usuário não encontrado.")
                    return@addOnSuccessListener
                }

                val usuario = snapshot.getValue(Usuario::class.java)

                if (usuario?.tipo == "admin") {
                    val escolaAdmin = usuario.escola
                    binding.navView.menu.findItem(R.id.navigation_admin)?.isVisible = true

                    binding.navView.setOnItemSelectedListener { item ->
                        when (item.itemId) {
                            R.id.navigation_admin -> {
                                val intent = Intent(this, AdminPanelActivity::class.java)
                                intent.putExtra("escolaAdmin", escolaAdmin)
                                startActivity(intent)
                                true
                            }
                            else -> {
                                val navController = findNavController(R.id.nav_host_fragment_activity_main)
                                navController.navigate(item.itemId)
                                true
                            }
                        }
                    }
                } else {
                    configurarNavegacaoPadrao()
                }
            }
            .addOnFailureListener {
                goToLogin("Erro ao verificar usuário.")
            }
    }

    private fun goToLogin(message: String? = null) {
        try { FirebaseAuth.getInstance().signOut() } catch (_: Throwable) {}
        message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun configurarNavegacaoPadrao() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
        if (navHostFragment?.view != null) {
            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            val navView: BottomNavigationView = binding.navView
            val appBarConfiguration = AppBarConfiguration(
                setOf(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_profile)
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            navView.setupWithNavController(navController)
        }
    }

    /**
     * Obtém token FCM, salva em Firestore + Realtime DB e se possível inscreve no tópico da escola.
     * Executa sempre que o app abre.
     */
    private fun configurarFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e("FCM_TOKEN", "Erro ao pegar token", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result ?: run {
                Log.e("FCM_TOKEN", "Token retornou null")
                return@addOnCompleteListener
            }

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                // Usuário não autenticado — guardamos só localmente no log (ou poderia guardar para envio posterior)
                Log.w("FCM_TOKEN", "Usuário não autenticado — token obtido mas não salvo: $token")
                return@addOnCompleteListener
            }

            val firestore = FirebaseFirestore.getInstance()
            val rtdb = FirebaseDatabase.getInstance().getReference("tokens")

            // salva no Realtime Database (tokens/{uid} = token)
            rtdb.child(uid).setValue(token)
                .addOnSuccessListener { Log.d("FCM_TOKEN", "Token salvo no Realtime DB") }
                .addOnFailureListener { e -> Log.e("FCM_TOKEN", "Falha ao salvar token no Realtime DB: ${e.message}") }

            // salva no Firestore (usuarios/{uid}.token)
            val userRef = firestore.collection("usuarios").document(uid)
            userRef.get().addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    // cria documento mínimo com token (se usuário não tiver doc no Firestore)
                    userRef.set(mapOf("token" to token))
                        .addOnSuccessListener { Log.d("FCM_TOKEN", "Documento criado no Firestore com token") }
                        .addOnFailureListener { e -> Log.e("FCM_TOKEN", "Falha ao criar doc Firestore: ${e.message}") }
                } else {
                    // atualiza token
                    userRef.update("token", token)
                        .addOnSuccessListener { Log.d("FCM_TOKEN", "Token atualizado no Firestore") }
                        .addOnFailureListener { e -> Log.e("FCM_TOKEN", "Falha ao atualizar token no Firestore: ${e.message}") }
                }

                // tenta obter a escola para se inscrever no tópico
                val escola = doc.getString("escola")
                if (!escola.isNullOrBlank()) {
                    val topic = normalizeTopic(escola)
                    FirebaseMessaging.getInstance().subscribeToTopic(topic)
                        .addOnSuccessListener { Log.d("FCM_TOPIC", "Inscrito no tópico: $topic") }
                        .addOnFailureListener { e -> Log.e("FCM_TOPIC", "Falha ao inscrever no tópico: ${e.message}") }
                } else {
                    // fallback: busca no Realtime Database se Firestore não contém escola
                    FirebaseDatabase.getInstance().getReference("usuarios").child(uid).get()
                        .addOnSuccessListener { snap ->
                            val escolaRt = snap.child("escola").getValue(String::class.java)
                            if (!escolaRt.isNullOrBlank()) {
                                val topic = normalizeTopic(escolaRt)
                                FirebaseMessaging.getInstance().subscribeToTopic(topic)
                                    .addOnSuccessListener { Log.d("FCM_TOPIC", "Inscrito no tópico (RTDB): $topic") }
                                    .addOnFailureListener { e -> Log.e("FCM_TOPIC", "Falha tópico (RTDB): ${e.message}") }
                            } else {
                                Log.d("FCM_TOPIC", "Escola não encontrada para inscrição em tópico")
                            }
                        }
                }
            }.addOnFailureListener { e ->
                Log.e("FCM_TOKEN", "Falha ao ler doc usuario no Firestore: ${e.message}")
            }
        }
    }

    private fun normalizeTopic(escola: String): String {
        return escola.lowercase(Locale.ROOT).replace("[^a-z0-9]".toRegex(), "_")
    }

    // permissões e canal
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
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Canal Padrão",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Canal para notificações do app" }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
