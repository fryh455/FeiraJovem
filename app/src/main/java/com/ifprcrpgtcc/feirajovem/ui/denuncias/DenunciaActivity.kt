package com.ifprcrpgtcc.feirajovem.ui.denuncias

import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Denuncias
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class DenunciaActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var motivoEditText: TextInputEditText
    private lateinit var enviarButton: MaterialButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DenunciaAdapter
    private val denunciasList = mutableListOf<Denuncias>()
    private var progressDialog: AlertDialog? = null

    companion object {
        private const val TAG = "DenunciaActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.usuario_denuncia_activity)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("denuncias")

        motivoEditText = findViewById(R.id.editMotivoDenuncia)
        enviarButton = findViewById(R.id.buttonEnviarDenuncia)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DenunciaAdapter(denunciasList)
        recyclerView.adapter = adapter

        enviarButton.setOnClickListener { enviarDenuncia() }

        carregarDenuncias()
    }

    private fun mostrarProgress() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_progress, null)
        builder.setView(view)
        progressDialog = builder.create()
        progressDialog?.show()
    }

    private fun esconderProgress() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun enviarDenuncia() {
        val motivo = motivoEditText.text.toString().trim()
        if (motivo.isEmpty()) {
            Toast.makeText(this, "Digite o motivo da denúncia", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.uid ?: run {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarProgress()
        val newKey = database.push().key ?: run {
            Toast.makeText(this, "Erro ao gerar ID da denúncia", Toast.LENGTH_SHORT).show()
            esconderProgress()
            return
        }

        val denuncia = Denuncias(
            denunciaId = newKey,
            denuncianteId = userId,
            motivo = motivo,
            status = "pendente",
            data = System.currentTimeMillis()
        )

        database.child(newKey).setValue(denuncia)
            .addOnSuccessListener {
                Toast.makeText(this, "Denúncia enviada com sucesso!", Toast.LENGTH_SHORT).show()
                motivoEditText.text?.clear()
                esconderProgress()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao enviar denúncia", e)
                Toast.makeText(this, "Falha ao enviar denúncia", Toast.LENGTH_SHORT).show()
                esconderProgress()
            }
    }

    private fun carregarDenuncias() {
        val userId = auth.uid ?: return

        database.orderByChild("denuncianteId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    denunciasList.clear()
                    for (ds in snapshot.children) {
                        val denuncia = ds.getValue(Denuncias::class.java)
                        if (denuncia != null) {
                            denunciasList.add(denuncia)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Erro ao carregar denúncias: ${error.message}")
                }
            })
    }
}
