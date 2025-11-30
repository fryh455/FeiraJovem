package com.ifprcrpgtcc.feirajovem.ui.usuario

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Usuario
import java.io.ByteArrayOutputStream

class EditarPerfilActivity : AppCompatActivity() {

    private lateinit var imageProfile: ImageView
    private lateinit var textChangePhoto: TextView
    private lateinit var editNome: EditText
    private lateinit var editEmail: EditText
    private lateinit var editNovaSenha: EditText
    private lateinit var editConfirmarSenha: EditText
    private lateinit var buttonSalvar: Button
    private lateinit var buttonCancelar: Button

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().getReference("usuarios")
    private val firestore = FirebaseFirestore.getInstance()

    private var imageUri: Uri? = null
    private var imageBase64: String? = null // Guardar a imagem como Base64
    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_perfil)

        auth = FirebaseAuth.getInstance()

        // Inicializa os componentes
        imageProfile = findViewById(R.id.userProfileImageView)
        textChangePhoto = findViewById(R.id.changePhotoTextView)
        editNome = findViewById(R.id.editTextNomeCompleto)
        editEmail = findViewById(R.id.editTextEmail)
        editNovaSenha = findViewById(R.id.editTextNovaSenha)
        editConfirmarSenha = findViewById(R.id.editTextConfirmarSenha)
        buttonSalvar = findViewById(R.id.buttonSalvarAlteracoes)
        buttonCancelar = findViewById(R.id.buttonCancelar)

        val user = auth.currentUser
        if (user != null) {
            carregarDadosUsuario(user)
        }

        // Selecionar nova foto
        textChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Cancelar edição
        buttonCancelar.setOnClickListener {
            finish()
        }

        // Salvar alterações
        buttonSalvar.setOnClickListener {
            salvarAlteracoes()
        }
    }

    /**
     * Carrega os dados atuais do usuário, incluindo a foto do perfil
     */
    private fun carregarDadosUsuario(user: FirebaseUser) {
        editNome.setText(user.displayName ?: "")
        editEmail.setText(user.email ?: "")

        // Carregar imagem salva no Firebase (Base64) do Realtime Database
        database.child(user.uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val usuario = snapshot.getValue(Usuario::class.java)
                usuario?.fotoBase64?.let { foto ->
                    if (foto.isNotEmpty()) {
                        val decodedBytes = Base64.decode(foto, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        imageProfile.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

    /**
     * Salva as alterações feitas no perfil
     */
    private fun salvarAlteracoes() {
        val nome = editNome.text.toString().trim()
        val novaSenha = editNovaSenha.text.toString().trim()
        val confirmarSenha = editConfirmarSenha.text.toString().trim()
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        if (nome.isEmpty()) {
            editNome.error = "Informe seu nome completo"
            return
        }

        if (novaSenha.isNotEmpty() && novaSenha != confirmarSenha) {
            editConfirmarSenha.error = "As senhas não coincidem"
            return
        }

        // Atualizar nome no Firebase Authentication
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(nome)
            .build()

        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                atualizarUsuarioNoDatabase(user.uid, nome, user.email)
            } else {
                Toast.makeText(this, "Erro ao atualizar perfil", Toast.LENGTH_SHORT).show()
            }
        }

        // Atualiza senha, se fornecida
        if (novaSenha.isNotEmpty()) {
            user.updatePassword(novaSenha).addOnCompleteListener { senhaTask ->
                if (senhaTask.isSuccessful) {
                    Toast.makeText(this, "Senha atualizada com sucesso!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Erro ao atualizar senha: ${senhaTask.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Atualiza os dados do usuário no Realtime Database e no Firestore (minimizado)
     */
    private fun atualizarUsuarioNoDatabase(uid: String, nome: String, email: String?) {
        val usuarioRef = database.child(uid)

        usuarioRef.get().addOnSuccessListener { snapshot ->
            val usuarioAtual = snapshot.getValue(Usuario::class.java)

            // Se não tiver imagem nova, mantém a antiga
            val fotoFinal = imageBase64 ?: usuarioAtual?.fotoBase64

            val usuarioAtualizado = Usuario(
                key = uid,
                nome = nome,
                email = email,
                endereco = usuarioAtual?.endereco,
                fotoBase64 = fotoFinal
            )

            usuarioRef.setValue(usuarioAtualizado)
                .addOnSuccessListener {
                    // Atualiza também no Firestore a versão mínima (nome, email, escola, fotoBase64)
                    val fsMap = hashMapOf<String, Any?>(
                        "nome" to nome,
                        "email" to email,
                        "fotoBase64" to fotoFinal,
                        // mantenha "escola" no Firestore se já existir, caso contrário não sobrescreve
                    )

                    firestore.collection("usuarios")
                        .document(uid)
                        .set(fsMap, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener {
                            // Mesmo que Firestore falhe, já atualizamos o Realtime DB
                            Toast.makeText(this, "Perfil atualizado (Realtime DB). Falha ao atualizar Firestore.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao salvar alterações no banco", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Recebe a imagem selecionada, exibe e converte para Base64
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            imageProfile.setImageBitmap(bitmap)

            // Converter imagem para Base64
            imageBase64 = converterBitmapParaBase64(bitmap)
            Log.d("EditarPerfilActivity", "Imagem convertida para Base64: ${imageBase64?.take(50)}...")
        }
    }

    private fun converterBitmapParaBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}
