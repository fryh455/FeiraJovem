package com.ifprcrpgtcc.feirajovem.ui.usuario

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.ui.login.LoginActivity

// Modelo usado no Realtime Database
data class Usuario(
    val nome: String = "",
    val email: String = "",
    val escola: String = ""
)

class CadastroUsuarioActivity : AppCompatActivity() {

    private lateinit var registerNameEditText: EditText
    private lateinit var registerEmailEditText: EditText
    private lateinit var registerPasswordEditText: EditText
    private lateinit var registerConfirmPasswordEditText: EditText
    private lateinit var registerSchoolSpinner: Spinner
    private lateinit var registerButton: Button
    private lateinit var sairButton: Button

    private lateinit var btnEntrarTab: Button
    private lateinit var btnCadastrarTab: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "CadastroUsuarioActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_usuario)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        registerNameEditText = findViewById(R.id.registerNameEditText)
        registerEmailEditText = findViewById(R.id.registerEmailEditText)
        registerPasswordEditText = findViewById(R.id.registerPasswordEditText)
        registerConfirmPasswordEditText = findViewById(R.id.registerConfirmPasswordEditText)
        registerSchoolSpinner = findViewById(R.id.registerSchoolSpinner)
        registerButton = findViewById(R.id.salvarButton)
        sairButton = findViewById(R.id.sairButton)

        btnEntrarTab = findViewById(R.id.btn_entrar_tab)
        btnCadastrarTab = findViewById(R.id.btn_cadastrar_tab)

        val escolas = listOf("Selecione sua escola", "IFPR-PG", "IFPR-Jaguariaíva", "IFPR-CWB")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, escolas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        registerSchoolSpinner.adapter = adapter

        btnEntrarTab.setOnClickListener {
            highlightTab(true)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnCadastrarTab.setOnClickListener {
            highlightTab(false)
            Toast.makeText(this, "Você já está na tela de cadastro", Toast.LENGTH_SHORT).show()
        }

        registerButton.setOnClickListener { createAccount() }
        sairButton.setOnClickListener { finish() }

        highlightTab(false)
    }

    private fun highlightTab(isEntrarSelected: Boolean) {
        val darkGray = ContextCompat.getColor(this, R.color.gray_dark)
        val lightGray = ContextCompat.getColor(this, R.color.gray_light)

        if (isEntrarSelected) {
            btnEntrarTab.setBackgroundColor(darkGray)
            btnCadastrarTab.setBackgroundColor(lightGray)
        } else {
            btnEntrarTab.setBackgroundColor(lightGray)
            btnCadastrarTab.setBackgroundColor(darkGray)
        }
    }

    private fun createAccount() {
        val nome = registerNameEditText.text.toString().trim()
        val email = registerEmailEditText.text.toString().trim()
        val senha = registerPasswordEditText.text.toString().trim()
        val confirmarSenha = registerConfirmPasswordEditText.text.toString().trim()
        val escola = registerSchoolSpinner.selectedItem.toString()

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() ||
            confirmarSenha.isEmpty() || escola == "Selecione sua escola") {
            Toast.makeText(this, "Preencha todos os campos corretamente.", Toast.LENGTH_SHORT).show()
            return
        }

        if (senha != confirmarSenha) {
            Toast.makeText(this, "As senhas não coincidem.", Toast.LENGTH_SHORT).show()
            return
        }

        if (senha.length < 6) {
            Toast.makeText(this, "A senha deve ter no mínimo 6 caracteres.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        updateProfile(user, nome)

                        val usuario = Usuario(nome, email, escola)

                        // Salva no Realtime Database
                        database.child("usuarios").child(user.uid).setValue(usuario)
                            .addOnSuccessListener {
                                // Versão mínima para Firestore (para notificações)
                                val fsMap = hashMapOf(
                                    "nome" to nome,
                                    "email" to email,
                                    "escola" to escola
                                )

                                firestore.collection("usuarios")
                                    .document(user.uid)
                                    .set(fsMap)
                                    .addOnSuccessListener { /* opcional */ }
                                    .addOnFailureListener { }

                                sendEmailVerification(user)
                                Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()

                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Erro ao salvar no banco.", Toast.LENGTH_LONG).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Erro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun sendEmailVerification(user: FirebaseUser) {
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Verificação enviada para ${user.email}.", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun updateProfile(user: FirebaseUser, nome: String) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(nome)
            .build()

        user.updateProfile(profileUpdates)
    }
}
