package com.ifprcrpgtcc.feirajovem.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.ifprcrpgtcc.feirajovem.MainActivity
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Usuario
import com.ifprcrpgtcc.feirajovem.ui.usuario.CadastroUsuarioActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var btnGoogleSignIn: Button
    private lateinit var progressBar: ProgressBar

    // Botões das abas
    private lateinit var btnEntrarTab: Button
    private lateinit var btnCadastrarTab: Button

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializa o Firebase
        FirebaseApp.initializeApp(this)
        firebaseAuth = FirebaseAuth.getInstance()

        // Vincula elementos da UI
        emailEditText = findViewById(R.id.edit_text_email)
        passwordEditText = findViewById(R.id.edit_text_password)
        loginButton = findViewById(R.id.button_login)
        registerLink = findViewById(R.id.registerLink)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        progressBar = findViewById(R.id.progressBarLogin)

        // Vincula botões de tabs
        btnEntrarTab = findViewById(R.id.btn_entrar_tab)
        btnCadastrarTab = findViewById(R.id.btn_cadastrar_tab)

        progressBar.visibility = View.GONE

        // Configuração do Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // ======= LÓGICA DAS TABS =======
        btnEntrarTab.setOnClickListener {
            highlightTab(true)
        }

        btnCadastrarTab.setOnClickListener {
            // Abre a tela de cadastro
            startActivity(Intent(this, CadastroUsuarioActivity::class.java))
        }

        // ======= LOGIN =======
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Preencha email e senha", Toast.LENGTH_SHORT).show()
            } else {
                signIn(email, password)
            }
        }

        // Login com Google
        btnGoogleSignIn.setOnClickListener {
            signInGoogle()
        }

        // Se já existe usuário autenticado, verifica no banco e segue
        firebaseAuth.currentUser?.let {
            Log.d(TAG, "Usuário já autenticado previamente: ${it.uid}")
            verificarUsuario(it)
        }

        // Por padrão, seleciona a aba "Entrar"
        highlightTab(true)
    }

    /**
     * Destaque visual na aba selecionada
     */
    private fun highlightTab(isLoginSelected: Boolean) {
        if (isLoginSelected) {
            btnEntrarTab.setBackgroundColor(resources.getColor(R.color.gray_light))
            btnCadastrarTab.setBackgroundColor(resources.getColor(R.color.gray_dark))
        } else {
            btnEntrarTab.setBackgroundColor(resources.getColor(R.color.gray_dark))
            btnCadastrarTab.setBackgroundColor(resources.getColor(R.color.gray_light))
        }
    }

    /**
     * Mostrar / ocultar loader
     */
    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        loginButton.isEnabled = !isLoading
        btnGoogleSignIn.isEnabled = !isLoading
    }

    /**
     * Login com email e senha
     */
    private fun signIn(email: String, password: String) {
        setLoading(true)
        loginButton.text = "Entrando..."

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                loginButton.text = "Entrar"

                if (task.isSuccessful) {
                    firebaseAuth.currentUser?.let { verificarUsuario(it) }
                } else {
                    Toast.makeText(this, "Falha na autenticação: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * Checa se o usuário já existe no banco de dados
     */
    private fun verificarUsuario(user: FirebaseUser) {
        val uid = user.uid
        val ref = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)

        ref.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Usuário já cadastrado → segue
                goToMain()
            } else {
                // Usuário novo → pede escolha da escola
                mostrarDialogoEscolhaEscola(uid, user.email ?: "")
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Erro ao verificar usuário", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Pop-up para escolher a escola (não cancelável)
     */
    private fun mostrarDialogoEscolhaEscola(uid: String, email: String) {
        val escolas = arrayOf("IFPR-PG", "IFPR-CWB")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Selecione sua escola")
            .setSingleChoiceItems(escolas, -1) { dialog, which ->
                val escolaEscolhida = escolas[which]

                // **CORREÇÃO AQUI**: sua data class usa 'key', não 'id'
                val usuario = Usuario(
                    key = uid,
                    email = email,
                    escola = escolaEscolhida,
                    tipo = "user comum"
                )

                FirebaseDatabase.getInstance().getReference("usuarios")
                    .child(uid)
                    .setValue(usuario)
                    .addOnSuccessListener {
                        dialog.dismiss()
                        goToMain()
                    }
                    .addOnFailureListener { ex ->
                        Toast.makeText(this, "Erro ao salvar usuário: ${ex.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setCancelable(false) // não pode fechar sem escolher
            .create()
            .show()
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun signInGoogle() {
        setLoading(true)
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val idToken = account.idToken ?: return
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    firebaseAuth.currentUser?.let { verificarUsuario(it) }
                } else {
                    Toast.makeText(this, "Falha na autenticação com Google", Toast.LENGTH_LONG).show()
                }
            }
    }

    @Deprecated("Compatibilidade com templates antigos")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }
            } catch (e: ApiException) {
                setLoading(false)
                Toast.makeText(this, "Erro no login com Google: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
