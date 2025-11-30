package com.ifprcrpgtcc.feirajovem.ui.produtos

import android.app.AlertDialog
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
import com.google.firebase.firestore.FirebaseFirestore
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Item
import com.ifprcrpgtcc.feirajovem.databinding.FragmentCadastroProdutosBinding
import java.io.InputStream

class ProdutosFragment : Fragment() {

    private var _binding: FragmentCadastroProdutosBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri? = null
    private lateinit var realtimeRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()

    private var progressDialog: AlertDialog? = null

    companion object {
        private const val TAG = "ProdutosFragment"
    }

    // Image Picker
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

        auth = FirebaseAuth.getInstance()
        realtimeRef = FirebaseDatabase.getInstance().getReference("itens")

        setupDuracaoSpinner()

        binding.buttonSelectImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.salvarItemButton.setOnClickListener { salvarItem() }

        return binding.root
    }

    private fun setupDuracaoSpinner() {
        val options = listOf("24 horas", "3 dias", "7 dias (máx)")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDuracao.adapter = adapter
        binding.spinnerDuracao.setSelection(2)
    }

    private fun mostrarProgress() {
        val view = layoutInflater.inflate(R.layout.dialog_progress, null)
        progressDialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()

        progressDialog?.show()
    }

    private fun esconderProgress() {
        progressDialog?.dismiss()
    }

    private fun salvarItem() {
        val titulo = binding.editTituloProduto.text.toString().trim()
        val descricao = binding.editDescricaoProduto.text.toString().trim()
        val preco = binding.editPrecoProduto.text.toString().trim()
        val endereco = binding.enderecoItemEditText.text.toString().trim()

        if (titulo.isEmpty() || descricao.isEmpty() || preco.isEmpty() ||
            endereco.isEmpty() || imageUri == null
        ) {
            Toast.makeText(context, "Preencha todos os campos e selecione uma imagem.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.uid ?: run {
            Toast.makeText(context, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarProgress()

        val agora = System.currentTimeMillis()
        val expiracao = calcularExpiracao(binding.spinnerDuracao.selectedItemPosition)

        val newKey = realtimeRef.push().key ?: run {
            Toast.makeText(context, "Erro ao gerar ID do item.", Toast.LENGTH_SHORT).show()
            esconderProgress()
            return
        }

        try {
            val base64Image = converterImagemBase64(imageUri!!)
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

            // Salva no Realtime DB
            realtimeRef.child(userId).child(newKey).setValue(item)
                .addOnSuccessListener {
                    replicarParaFirestore(newKey, titulo, userId, agora)
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

    private fun converterImagemBase64(uri: Uri): String {
        val inputStream: InputStream? =
            context?.contentResolver?.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        return if (bytes != null) Base64.encodeToString(bytes, Base64.DEFAULT) else ""
    }

    private fun replicarParaFirestore(itemId: String, titulo: String, userId: String, dataCriacao: Long) {
        // Puxa escola do usuário direto do Firestore (onde ele já está salvo desde o cadastro)
        firestore.collection("usuarios").document(userId).get()
            .addOnSuccessListener { doc ->
                val escola = doc.getString("escola") ?: "desconhecida"

                val fsItem = hashMapOf(
                    "titulo" to titulo,
                    "userId" to userId,
                    "dataCriacao" to dataCriacao,
                    "escolaDoUsuario" to escola // otimiza trigger
                )

                firestore.collection("items")
                    .document(itemId)
                    .set(fsItem)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Item cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                        limparCampos()
                        esconderProgress()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Falha ao gravar no Firestore: ${e.message}")
                        Toast.makeText(context, "Item salvo, mas falhou ao sincronizar com Firestore.", Toast.LENGTH_SHORT).show()
                        limparCampos()
                        esconderProgress()
                    }
            }
            .addOnFailureListener {
                Log.e(TAG, "Falha ao buscar escola do usuário no Firestore.")
                esconderProgress()
            }
    }

    private fun calcularExpiracao(option: Int): Long {
        val agora = System.currentTimeMillis()
        return when (option) {
            0 -> agora + 24L * 60 * 60 * 1000
            1 -> agora + 3L * 24 * 60 * 60 * 1000
            else -> agora + 7L * 24 * 60 * 60 * 1000
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
