package com.ifprcrpgtcc.feirajovem.ui.dashboard

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Item
import com.ifprcrpgtcc.feirajovem.databinding.FragmentDashboardBinding
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.InputStream

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // Views
    private lateinit var itemImageView: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var salvarButton: Button
    private lateinit var tituloEditText: EditText
    private lateinit var descricaoEditText: EditText
    private lateinit var precoEditText: EditText
    private lateinit var enderecoEditText: EditText
    private var duracaoSpinner: Spinner? = null

    private var imageUri: Uri? = null

    // Firebase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val TAG = "DashboardFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val view = binding.root

        // Inicializa Firebase
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference("itens")

        // Inicializa componentes (mantendo os mesmos ids)
        itemImageView = view.findViewById(R.id.image_item)
        selectImageButton = view.findViewById(R.id.button_select_image)
        salvarButton = view.findViewById(R.id.salvarItemButton)
        tituloEditText = view.findViewById(R.id.editTituloProduto)
        descricaoEditText = view.findViewById(R.id.editDescricaoProduto)
        precoEditText = view.findViewById(R.id.editPrecoProduto)
        enderecoEditText = view.findViewById(R.id.enderecoItemEditText)

        // Spinner (pode ser null se não adicionado ao layout)
        duracaoSpinner = view.findViewById(R.id.spinnerDuracao)
        if (duracaoSpinner != null) {
            val options = listOf("24 horas", "3 dias", "7 dias (máx)")
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            duracaoSpinner!!.adapter = adapter
            duracaoSpinner!!.setSelection(options.indexOf("7 dias (máx)"))
        } else {
            Log.w(TAG, "spinnerDuracao não encontrado no layout. Usarei 7 dias como padrão.")
        }

        // Botão selecionar imagem
        selectImageButton.setOnClickListener { openFileChooser() }

        // Botão salvar item
        salvarButton.setOnClickListener { salvarItem() }

        return view
    }

    private fun openFileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            imageUri = data.data
            // mostra preview com glide
            Glide.with(this).load(imageUri).into(itemImageView)
        }
    }

    private fun salvarItem() {
        val titulo = tituloEditText.text.toString().trim()
        val descricao = descricaoEditText.text.toString().trim()
        val preco = precoEditText.text.toString().trim()
        val endereco = enderecoEditText.text.toString().trim()

        if (titulo.isEmpty() || descricao.isEmpty() || preco.isEmpty() || endereco.isEmpty() || imageUri == null) {
            Toast.makeText(context, "Por favor, preencha todos os campos e selecione uma imagem.", Toast.LENGTH_SHORT).show()
            return
        }

        val agora = System.currentTimeMillis()
        val duracaoMillis = when (duracaoSpinner?.selectedItemPosition ?: 2) {
            0 -> 24L * 60 * 60 * 1000
            1 -> 3L * 24 * 60 * 60 * 1000
            else -> 7L * 24 * 60 * 60 * 1000
        }
        val expiracao = agora + duracaoMillis

        // Gerar chave antes para usar como id
        val userId = auth.uid ?: run {
            Toast.makeText(context, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }
        val newKey = databaseReference.push().key
        if (newKey == null) {
            Toast.makeText(context, "Erro ao gerar ID do item.", Toast.LENGTH_SHORT).show()
            return
        }

        // Converter imagem para base64 (mantendo sua estratégia)
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

            // salva em /itens/{ownerId}/{itemId}
            databaseReference.child(userId).child(newKey).setValue(item)
                .addOnSuccessListener {
                    Toast.makeText(context, "Item cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                    limparCampos()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Falha ao cadastrar o item.", Toast.LENGTH_SHORT).show()
                }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar imagem", e)
            Toast.makeText(context, "Erro ao processar a imagem.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun limparCampos() {
        tituloEditText.text.clear()
        descricaoEditText.text.clear()
        precoEditText.text.clear()
        enderecoEditText.text.clear()
        itemImageView.setImageResource(android.R.color.transparent)
        imageUri = null
        duracaoSpinner?.setSelection(2)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
