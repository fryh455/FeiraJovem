package com.ifprcrpgtcc.feirajovem.ui.produtos

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Item
import com.ifprcrpgtcc.feirajovem.databinding.FragmentCadastroProdutosBinding
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

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
        private const val WORKER_URL = "https://feirajovem.nickollas-v-mendonca.workers.dev/"
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri = it
                Glide.with(this).load(it).into(binding.imageItem)
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

        StrictMode.setThreadPolicy(ThreadPolicy.Builder().permitAll().build())

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

        if (titulo.isEmpty() || descricao.isEmpty() || preco.isEmpty() || endereco.isEmpty() || imageUri == null) {
            Toast.makeText(context, "Preencha tudo e selecione imagem.", Toast.LENGTH_SHORT).show()
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
                titulo = titulo,
                descricao = descricao,
                preco = preco,
                endereco = endereco,
                imagemBase64 = base64Image,
                dataCriacao = agora,
                dataExpiracao = expiracao,
                avaliacao = 0f,
                mediaAvaliacao = 0f,
                itemId = newKey,
                userId = userId
            )

            realtimeRef.child(userId).child(newKey).setValue(item)
                .addOnSuccessListener {
                    replicarParaFirestore(newKey, titulo, userId, agora)
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Erro ao salvar item.", Toast.LENGTH_SHORT).show()
                    esconderProgress()
                }

        } catch (e: Exception) {
            Log.e(TAG, "Erro imagem", e)
            Toast.makeText(context, "Erro ao processar imagem.", Toast.LENGTH_SHORT).show()
            esconderProgress()
        }
    }

    private fun converterImagemBase64(uri: Uri): String {
        val inputStream: InputStream? = context?.contentResolver?.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        return bytes?.let { Base64.encodeToString(it, Base64.DEFAULT) } ?: ""
    }

    private fun replicarParaFirestore(itemId: String, titulo: String, userId: String, dataCriacao: Long) {
        firestore.collection("usuarios").document(userId).get()
            .addOnSuccessListener { doc ->
                val escola = doc.getString("escola") ?: "desconhecida"
                val fsItem = mapOf(
                    "titulo" to titulo,
                    "userId" to userId,
                    "dataCriacao" to dataCriacao,
                    "escolaDoUsuario" to escola
                )

                firestore.collection("items").document(itemId)
                    .set(fsItem)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Item cadastrado!", Toast.LENGTH_SHORT).show()
                        limparCampos()
                        esconderProgress()

                        val topic = normalizeTopic(escola)
                        val title = "Novo produto: $titulo"
                        val body = "Novo item publicado na sua escola"

                        // chama Worker garantindo POST
                        sendNotificationToWorker(title, body, topic)

                        // mostra notificação local imediata
                        showLocalNotification(title, body)
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Erro Firestore.", Toast.LENGTH_SHORT).show()
                        limparCampos()
                        esconderProgress()
                    }
            }
            .addOnFailureListener {
                Log.e(TAG, "Falha ao buscar escola do usuário no Firestore.")
                esconderProgress()
            }
    }

    private fun normalizeTopic(escola: String): String {
        return escola.lowercase(Locale.ROOT).replace("[^a-z0-9]".toRegex(), "_")
    }

    private fun sendNotificationToWorker(title: String, body: String, topic: String) {
        Thread {
            try {
                val url = URL(WORKER_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("Content-Type", "application/json")

                val payload = JSONObject()
                payload.put("title", title)
                payload.put("body", body)
                payload.put("topic", topic)

                val out = BufferedOutputStream(conn.outputStream)
                out.write(payload.toString().toByteArray(Charsets.UTF_8))
                out.flush()
                out.close()

                val code = conn.responseCode
                val respStream = if (code in 200..299) conn.inputStream else conn.errorStream
                val resp = respStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Worker response ($code): $resp")
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao chamar Worker: ${e.message}", e)
            }
        }.start()
    }

    private fun showLocalNotification(title: String, body: String) {
        val intent = Intent(requireContext(), com.ifprcrpgtcc.feirajovem.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(requireContext(), "default_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(sound)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val manager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default_channel",
                "Notificações Gerais",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        manager.notify(System.currentTimeMillis().toInt(), builder.build())
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
