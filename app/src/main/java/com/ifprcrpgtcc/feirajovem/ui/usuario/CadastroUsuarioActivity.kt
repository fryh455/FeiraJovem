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
import com.google.firebase.messaging.FirebaseMessaging
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Usuario
import com.ifprcrpgtcc.feirajovem.ui.login.LoginActivity

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
        val dark = ContextCompat.getColor(this, R.color.gray_dark)
        val light = ContextCompat.getColor(this, R.color.gray_light)

        btnEntrarTab.setBackgroundColor(if (isEntrarSelected) dark else light)
        btnCadastrarTab.setBackgroundColor(if (isEntrarSelected) light else dark)
    }

    private fun createAccount() {
        val nome = registerNameEditText.text.toString().trim()
        val email = registerEmailEditText.text.toString().trim()
        val senha = registerPasswordEditText.text.toString().trim()
        val confirmar = registerConfirmPasswordEditText.text.toString().trim()
        val escola = registerSchoolSpinner.selectedItem.toString()

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() ||
            confirmar.isEmpty() || escola == "Selecione sua escola") {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
            return
        }

        if (senha != confirmar) {
            Toast.makeText(this, "As senhas não coincidem.", Toast.LENGTH_SHORT).show()
            return
        }

        if (senha.length < 6) {
            Toast.makeText(this, "A senha deve ter ao menos 6 caracteres.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, senha)
            .addOnSuccessListener {
                val user = auth.currentUser ?: return@addOnSuccessListener
                updateProfile(user, nome)

                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->

                    val usuario = Usuario(
                        key = user.uid,
                        nome = nome,
                        email = email,
                        escola = escola,
                        tipo = "user comum",
                        token = token
                    )

                    // Realtime DB
                    database.child("usuarios").child(user.uid).setValue(usuario)

                    // Firestore
                    firestore.collection("usuarios").document(user.uid)
                        .set(usuario)

                    sendEmailVerification(user)

                    Toast.makeText(this, "Cadastro concluído!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun sendEmailVerification(user: FirebaseUser) {
        user.sendEmailVerification()
    }

    private fun updateProfile(user: FirebaseUser, nome: String) {
        val upd = UserProfileChangeRequest.Builder()
            .setDisplayName(nome)
            .build()
        user.updateProfile(upd)
    }
}
