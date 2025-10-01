package com.ifprcrpgtcc.feirajovem.ui.denuncias

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Denuncias
import com.ifprcrpgtcc.feirajovem.databinding.UsuarioDenunciaActivityBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class UsuarioDenunciaActivity {

    companion object {

        fun showDenunciaDialog(context: Context) {
            val binding = UsuarioDenunciaActivityBinding.inflate(LayoutInflater.from(context))
            val dialog = AlertDialog.Builder(context)
                .setView(binding.root)
                .setCancelable(true)
                .create()

            val auth = FirebaseAuth.getInstance()
            val database = FirebaseDatabase.getInstance().getReference("denuncias")
            var progressDialog: AlertDialog? = null

            fun mostrarProgress() {
                val builder = AlertDialog.Builder(context)
                builder.setCancelable(false)
                val view = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null)
                builder.setView(view)
                progressDialog = builder.create()
                progressDialog?.show()
            }

            fun esconderProgress() {
                progressDialog?.dismiss()
                progressDialog = null
            }

            binding.buttonEnviarDenuncia.setOnClickListener {
                val motivo = binding.editMotivoDenuncia.text.toString().trim()
                if (motivo.isEmpty()) {
                    Toast.makeText(context, "Digite o motivo da denúncia.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val usuarioId = auth.uid ?: run {
                    Toast.makeText(context, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                mostrarProgress()

                val novaKey = database.push().key ?: run {
                    Toast.makeText(context, "Erro ao gerar ID da denúncia.", Toast.LENGTH_SHORT).show()
                    esconderProgress()
                    return@setOnClickListener
                }

                val denuncia = Denuncias(
                    denunciaId = novaKey,
                    denuncianteId = usuarioId,
                    motivo = motivo,
                    status = "pendente",
                    data = System.currentTimeMillis()
                )

                database.child(novaKey).setValue(denuncia)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Denúncia enviada com sucesso!", Toast.LENGTH_SHORT).show()
                        binding.editMotivoDenuncia.text.clear()
                        esconderProgress()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Falha ao enviar denúncia.", Toast.LENGTH_SHORT).show()
                        esconderProgress()
                    }
            }

            dialog.show()
        }
    }
}
