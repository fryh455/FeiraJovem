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
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.ui.login.LoginActivity

// Modelo de dados do usuário que será salvo no Realtime Database
data class Usuario(
    val nome: String = "",
    val email: String = "",
    val escola: String = ""
)

class CadastroUsuarioActivity : AppCompatActivity() {

    // Campos do formulário
    private lateinit var registerNameEditText: EditText
    private lateinit var registerEmailEditText: EditText
    private lateinit var registerPasswordEditText: EditText
    private lateinit var registerConfirmPasswordEditText: EditText
    private lateinit var registerSchoolSpinner: Spinner
    private lateinit var registerButton: Button
    private lateinit var sairButton: Button

    // Botões de navegação (tabs)
    private lateinit var btnEntrarTab: Button
    private lateinit var btnCadastrarTab: Button

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    companion object {
        private const val TAG = "CadastroUsuarioActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_usuario)

        // Inicializa Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Vincula elementos do layout
        registerNameEditText = findViewById(R.id.registerNameEditText)
        registerEmailEditText = findViewById(R.id.registerEmailEditText)
        registerPasswordEditText = findViewById(R.id.registerPasswordEditText)
        registerConfirmPasswordEditText = findViewById(R.id.registerConfirmPasswordEditText)
        registerSchoolSpinner = findViewById(R.id.registerSchoolSpinner)
        registerButton = findViewById(R.id.salvarButton)
        sairButton = findViewById(R.id.sairButton)

        btnEntrarTab = findViewById(R.id.btn_entrar_tab)
        btnCadastrarTab = findViewById(R.id.btn_cadastrar_tab)

        // Preenche o Spinner com as opções fixas
        val escolas = listOf("Selecione sua escola", "IFPR-PG", "IFPR-Jaguariaíva", "IFPR-CWB")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, escolas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        registerSchoolSpinner.adapter = adapter

        // Ações das abas
        btnEntrarTab.setOnClickListener {
            highlightTab(isEntrarSelected = true)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnCadastrarTab.setOnClickListener {
            highlightTab(isEntrarSelected = false)
            Toast.makeText(this, "Você já está na tela de cadastro", Toast.LENGTH_SHORT).show()
        }

        // Botão de cadastro
        registerButton.setOnClickListener {
            createAccount()
        }

        // Botão "Sair" apenas fecha a tela
        sairButton.setOnClickListener {
            finish()
        }

        // Começa com a aba "Cadastrar" destacada
        highlightTab(isEntrarSelected = false)
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

    /**
     * Cria uma conta no Firebase Authentication e salva dados adicionais no Realtime Database
     */
    private fun createAccount() {
        val nome = registerNameEditText.text.toString().trim()
        val email = registerEmailEditText.text.toString().trim()
        val senha = registerPasswordEditText.text.toString().trim()
        val confirmarSenha = registerConfirmPasswordEditText.text.toString().trim()
        val escola = registerSchoolSpinner.selectedItem.toString()

        // Validações
        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty() || escola == "Selecione sua escola") {
            Toast.makeText(this, "Por favor, preencha todos os campos corretamente", Toast.LENGTH_SHORT).show()
            return
        }

        if (senha != confirmarSenha) {
            Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
            return
        }

        if (senha.length < 6) {
            Toast.makeText(this, "A senha deve ter no mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        // Cria usuário no Firebase Authentication
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Atualiza nome no perfil
                        updateProfile(user, nome)

                        // Salva dados no Realtime Database
                        val usuario = Usuario(nome, email, escola)
                        database.child("usuarios").child(user.uid).setValue(usuario)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    sendEmailVerification(user)
                                    Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()

                                    // Vai para a tela de login
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this, "Erro ao salvar dados no banco", Toast.LENGTH_LONG).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(this, "Falha no cadastro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun sendEmailVerification(user: FirebaseUser) {
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Email de verificação enviado para ${user.email}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Falha ao enviar email de verificação", Toast.LENGTH_LONG).show()
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
