package com.ifprcrpgtcc.feirajovem.ui.produtos

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Item
import com.ifprcrpgtcc.feirajovem.databinding.FragmentCadastroProdutosBinding
import java.io.InputStream

class ProdutosFragment : Fragment() {

    private var _binding: FragmentCadastroProdutosBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri? = null
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var progressDialog: AlertDialog? = null

    companion object {
        private const val TAG = "ProdutosFragment"
    }

    // Novo launcher moderno
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            Glide.with(this).load(uri).into(binding.imageItem)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCadastroProdutosBinding.inflate(inflater, container, false)
        val view = binding.root

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference("itens")

        // Spinner
        val options = listOf("24 horas", "3 dias", "7 dias (máx)")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDuracao.adapter = adapter
        binding.spinnerDuracao.setSelection(2)

        // Botões
        binding.buttonSelectImage.setOnClickListener { openFileChooser() }
        binding.salvarItemButton.setOnClickListener { salvarItem() }

        return view
    }

    private fun openFileChooser() {
        pickImageLauncher.launch("image/*")
    }

    private fun mostrarProgress() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setCancelable(false)
        val view = layoutInflater.inflate(R.layout.dialog_progress, null)
        builder.setView(view)
        progressDialog = builder.create()
        progressDialog?.show()
    }

    private fun esconderProgress() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun salvarItem() {
        val titulo = binding.editTituloProduto.text.toString().trim()
        val descricao = binding.editDescricaoProduto.text.toString().trim()
        val preco = binding.editPrecoProduto.text.toString().trim()
        val endereco = binding.enderecoItemEditText.text.toString().trim()

        if (titulo.isEmpty() || descricao.isEmpty() || preco.isEmpty() || endereco.isEmpty() || imageUri == null) {
            Toast.makeText(context, "Preencha todos os campos e selecione uma imagem.", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarProgress()

        val agora = System.currentTimeMillis()
        val duracaoMillis = when (binding.spinnerDuracao.selectedItemPosition) {
            0 -> 24L * 60 * 60 * 1000
            1 -> 3L * 24 * 60 * 60 * 1000
            else -> 7L * 24 * 60 * 60 * 1000
        }
        val expiracao = agora + duracaoMillis

        val userId = auth.uid ?: run {
            Toast.makeText(context, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            esconderProgress()
            return
        }

        val newKey = databaseReference.push().key ?: run {
            Toast.makeText(context, "Erro ao gerar ID do item.", Toast.LENGTH_SHORT).show()
            esconderProgress()
            return
        }

        try {
            val inputStream: InputStream? = context?.contentResolver?.openInputStream(imageUri!!)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            val base64Image = if (bytes != null) Base64.encodeToString(bytes, Base64.DEFAULT) else ""

            val item = Item(
                itemId = newKey,
                userId = userId,
                titulo = titulo,
                descricao = descricao,
                preco = preco,
                endereco = endereco,
                imagemBase64 = base64Image,
                dataCriacao = agora,
                dataExpiracao = expiracao,
                avaliacao = 0f
            )

            databaseReference.child(userId).child(newKey).setValue(item)
                .addOnSuccessListener {
                    Toast.makeText(context, "Item cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                    limparCampos()
                    esconderProgress()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Falha ao cadastrar o item.", Toast.LENGTH_SHORT).show()
                    esconderProgress()
                }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar imagem", e)
            Toast.makeText(context, "Erro ao processar a imagem.", Toast.LENGTH_SHORT).show()
            esconderProgress()
        }
    }

    private fun limparCampos() {
        binding.editTituloProduto.text.clear()
        binding.editDescricaoProduto.text.clear()
        binding.editPrecoProduto.text.clear()
        binding.enderecoItemEditText.text.clear()
        binding.imageItem.setImageResource(android.R.color.transparent)
        imageUri = null
        binding.spinnerDuracao.setSelection(2)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
