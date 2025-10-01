package com.ifprcrpgtcc.feirajovem.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Item
import com.ifprcrpgtcc.feirajovem.ui.denuncias.UsuarioDenunciaActivity

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var databaseRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var editTextSearch: EditText
    private val listaItens = mutableListOf<Item>()
    private val listaFiltrada = mutableListOf<Item>()
    private val mapaEscolasUsuarios = mutableMapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_feed, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewFeed)
        editTextSearch = view.findViewById(R.id.editTextSearch)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().getReference("itens")

        feedAdapter = FeedAdapter(
            listaFiltrada,
            onLerMaisClick = { item ->
                context?.let { Toast.makeText(it, "Ler mais de ${item.titulo}", Toast.LENGTH_SHORT).show() }
            },
            onAvaliarClick = { itemId, userId ->
                mostrarDialogAvaliacao(itemId, userId)
            },
            onDeletarClick = { itemId, userId ->
                confirmarExclusao(itemId, userId)
            },
            onDenunciarClick = { item ->
                // Abre pop-up de denúncia (usar context? para segurança)
                context?.let { UsuarioDenunciaActivity.showDenunciaDialog(it) }
            }
        )

        recyclerView.adapter = feedAdapter
        configurarBarraDePesquisa()

        val currentUserId = auth.currentUser?.uid
        if (currentUserId.isNullOrEmpty()) {
            context?.let { Toast.makeText(it, "Usuário não autenticado", Toast.LENGTH_SHORT).show() }
            return view
        }

        val userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(currentUserId)

        userRef.get().addOnSuccessListener { snapshot ->
            // Se fragment já não estiver anexado, não tenta continuar
            if (!isAdded) return@addOnSuccessListener

            val escolaUsuarioLogado = snapshot.child("escola").getValue(String::class.java)
            if (!escolaUsuarioLogado.isNullOrEmpty()) {
                // carregar mapa e depois itens, mas só se fragment ainda estiver anexado
                carregarMapaEscolas {
                    if (!isAdded) return@carregarMapaEscolas
                    carregarItensMarketplace(escolaUsuarioLogado)
                }
            } else {
                context?.let { Toast.makeText(it, "Não foi possível identificar sua escola", Toast.LENGTH_SHORT).show() }
            }
        }.addOnFailureListener {
            if (!isAdded) return@addOnFailureListener
            context?.let { Toast.makeText(it, "Erro ao carregar informações do usuário", Toast.LENGTH_SHORT).show() }
        }

        return view
    }

    private fun carregarMapaEscolas(onComplete: () -> Unit) {
        val usuariosRef = FirebaseDatabase.getInstance().getReference("usuarios")
        usuariosRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // fragment pode ter sido removido entre a chamada e o retorno
                if (!isAdded) return
                mapaEscolasUsuarios.clear()
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    val escola = userSnapshot.child("escola").getValue(String::class.java) ?: ""
                    mapaEscolasUsuarios[userId] = escola
                }
                onComplete()
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                context?.let { Toast.makeText(it, "Erro ao carregar escolas dos usuários", Toast.LENGTH_SHORT).show() }
            }
        })
    }

    private fun configurarBarraDePesquisa() {
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val textoBusca = s.toString().trim().lowercase()
                filtrarItens(textoBusca)
            }
        })
    }

    private fun filtrarItens(query: String) {
        listaFiltrada.clear()
        if (query.isEmpty()) {
            listaFiltrada.addAll(listaItens)
        } else {
            listaFiltrada.addAll(
                listaItens.filter {
                    it.titulo.lowercase().contains(query) ||
                            it.descricao.lowercase().contains(query)
                }
            )
        }
        listaFiltrada.sortByDescending { it.avaliacao }
        if (isAdded) feedAdapter.notifyDataSetChanged()
    }

    private fun carregarItensMarketplace(escolaUsuarioLogado: String) {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // evita operar se fragment não estiver anexado
                if (!isAdded) return

                listaItens.clear()
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    val escolaDonoDoItem = mapaEscolasUsuarios[userId] ?: ""
                    if (escolaDonoDoItem != escolaUsuarioLogado) continue

                    for (itemSnapshot in userSnapshot.children) {
                        val item = itemSnapshot.getValue(Item::class.java)
                        item?.let {
                            val agora = System.currentTimeMillis()
                            if (it.dataExpiracao == 0L || it.dataExpiracao > agora) {
                                it.itemId = itemSnapshot.key
                                it.userId = userId
                                it.avaliacao = itemSnapshot.child("mediaAvaliacao").getValue(Float::class.java) ?: 0f
                                listaItens.add(it)
                            } else {
                                itemSnapshot.ref.removeValue()
                                Log.d("HomeFragment", "Item expirado removido: ${it.titulo}")
                            }
                        }
                    }
                }
                listaItens.sortByDescending { it.avaliacao }
                if (isAdded) filtrarItens(editTextSearch.text.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                context?.let { Toast.makeText(it, "Erro ao carregar feed", Toast.LENGTH_SHORT).show() }
            }
        })
    }

    private fun confirmarExclusao(itemId: String, userId: String) {
        if (!isAdded) return
        val currentUserId = auth.uid
        if (currentUserId != userId) {
            context?.let { Toast.makeText(it, "Você só pode deletar seus próprios itens!", Toast.LENGTH_SHORT).show() }
            return
        }

        context?.let { ctx ->
            AlertDialog.Builder(ctx)
                .setTitle("Excluir Produto")
                .setMessage("Tem certeza que deseja excluir este produto?")
                .setPositiveButton("Sim") { _, _ ->
                    databaseRef.child(userId).child(itemId).removeValue()
                        .addOnSuccessListener {
                            if (!isAdded) return@addOnSuccessListener
                            Toast.makeText(ctx, "Item deletado com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            if (!isAdded) return@addOnFailureListener
                            Toast.makeText(ctx, "Erro ao deletar item", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun mostrarDialogAvaliacao(itemId: String, userId: String) {
        if (!isAdded) return
        val currentUserId = auth.uid ?: return
        val ctx = context ?: return
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_avaliacao, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)

        AlertDialog.Builder(ctx)
            .setTitle("Avaliar Produto")
            .setView(dialogView)
            .setPositiveButton("Confirmar") { dialog, _ ->
                if (!isAdded) return@setPositiveButton
                val nota = ratingBar.rating
                if (nota < 1f) {
                    Toast.makeText(ctx, "Escolha pelo menos 1 estrela!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val avaliacaoRef = databaseRef.child(userId).child(itemId).child("avaliacoes")
                avaliacaoRef.child(currentUserId).setValue(nota)
                    .addOnSuccessListener {
                        if (!isAdded) return@addOnSuccessListener
                        Toast.makeText(ctx, "Avaliação registrada!", Toast.LENGTH_SHORT).show()
                        calcularMedia(itemId, userId)
                    }
                    .addOnFailureListener {
                        if (!isAdded) return@addOnFailureListener
                        Toast.makeText(ctx, "Erro ao registrar avaliação!", Toast.LENGTH_SHORT).show()
                    }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun calcularMedia(itemId: String, userId: String) {
        if (!isAdded) return
        val avaliacaoRef = databaseRef.child(userId).child(itemId).child("avaliacoes")
        avaliacaoRef.get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener
            var soma = 0f
            var total = 0
            for (child in snapshot.children) {
                val valor = child.getValue(Float::class.java) ?: 0f
                soma += valor
                total++
            }
            val media = if (total > 0) soma / total else 0f
            databaseRef.child(userId).child(itemId).child("mediaAvaliacao").setValue(media)
        }.addOnFailureListener {
            if (!isAdded) return@addOnFailureListener
            context?.let { Toast.makeText(it, "Erro ao calcular média!", Toast.LENGTH_SHORT).show() }
        }
    }
}
