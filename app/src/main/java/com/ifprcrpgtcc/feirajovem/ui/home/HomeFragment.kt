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

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var databaseRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var editTextSearch: EditText
    private val listaItens = mutableListOf<Item>()
    private val listaFiltrada = mutableListOf<Item>()

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
                Toast.makeText(requireContext(), "Ler mais de ${item.titulo}", Toast.LENGTH_SHORT).show()
            },
            onAvaliarClick = { itemId, userId ->
                mostrarDialogAvaliacao(itemId, userId)
            },
            onDeletarClick = { itemId, userId ->
                confirmarExclusao(itemId, userId)
            }
        )

        recyclerView.adapter = feedAdapter

        configurarBarraDePesquisa()
        carregarItensMarketplace()

        return view
    }

    /**
     * Configura a barra de pesquisa para filtrar os produtos em tempo real
     */
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

    /**
     * Filtra os itens pelo texto digitado
     */
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

        // Ordena após filtrar → maior avaliação primeiro
        listaFiltrada.sortByDescending { it.avaliacao }

        feedAdapter.notifyDataSetChanged()
    }

    /**
     * Carrega os itens do Firebase e remove automaticamente os expirados
     */
    private fun carregarItensMarketplace() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaItens.clear()

                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    for (itemSnapshot in userSnapshot.children) {
                        val item = itemSnapshot.getValue(Item::class.java)
                        item?.let {
                            val agora = System.currentTimeMillis()
                            if (it.dataExpiracao == 0L || it.dataExpiracao > agora) {
                                it.itemId = itemSnapshot.key
                                it.userId = userId

                                val media = itemSnapshot.child("mediaAvaliacao").getValue(Float::class.java) ?: 0f
                                it.avaliacao = media

                                listaItens.add(it)
                            } else {
                                itemSnapshot.ref.removeValue()
                                Log.d("HomeFragment", "Item expirado removido: ${it.titulo}")
                            }
                        }
                    }
                }

                // Ordena lista principal antes de filtrar
                listaItens.sortByDescending { it.avaliacao }

                // Atualiza a lista filtrada
                filtrarItens(editTextSearch.text.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Erro ao carregar feed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Mostra dialog de confirmação antes de deletar produto
     */
    private fun confirmarExclusao(itemId: String, userId: String) {
        val currentUserId = auth.uid
        if (currentUserId != userId) {
            Toast.makeText(requireContext(), "Você só pode deletar seus próprios itens!", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Produto")
            .setMessage("Tem certeza que deseja excluir este produto?")
            .setPositiveButton("Sim") { _, _ ->
                databaseRef.child(userId).child(itemId).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Item deletado com sucesso!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Erro ao deletar item", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Abre o dialog com a RatingBar para avaliar o produto
     */
    private fun mostrarDialogAvaliacao(itemId: String, userId: String) {
        val currentUserId = auth.uid ?: return

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_avaliacao, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)

        AlertDialog.Builder(requireContext())
            .setTitle("Avaliar Produto")
            .setView(dialogView)
            .setPositiveButton("Confirmar") { dialog, _ ->
                val nota = ratingBar.rating

                if (nota < 1f) {
                    Toast.makeText(requireContext(), "Escolha pelo menos 1 estrela!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val avaliacaoRef = databaseRef.child(userId).child(itemId).child("avaliacoes")

                avaliacaoRef.child(currentUserId).setValue(nota)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Avaliação registrada!", Toast.LENGTH_SHORT).show()
                        calcularMedia(itemId, userId)
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Erro ao registrar avaliação!", Toast.LENGTH_SHORT).show()
                    }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Calcula a média das avaliações e atualiza no banco
     */
    private fun calcularMedia(itemId: String, userId: String) {
        val avaliacaoRef = databaseRef.child(userId).child(itemId).child("avaliacoes")

        avaliacaoRef.get().addOnSuccessListener { snapshot ->
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
            Toast.makeText(requireContext(), "Erro ao calcular média!", Toast.LENGTH_SHORT).show()
        }
    }
}
