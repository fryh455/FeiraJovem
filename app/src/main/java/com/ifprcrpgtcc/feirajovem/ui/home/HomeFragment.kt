package com.ifprcrpgtcc.feirajovem.ui.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private val listaItens = mutableListOf<Item>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_feed, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewFeed)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().getReference("itens")

        feedAdapter = FeedAdapter(
            listaItens,
            onLerMaisClick = { item ->
                Toast.makeText(requireContext(), "Ler mais de ${item.titulo}", Toast.LENGTH_SHORT).show()
            },
            onAvaliarClick = { itemId, userId ->
                avaliarProduto(itemId, userId)
            },
            onDeletarClick = { itemId, userId ->
                deletarProduto(itemId, userId)
            }
        )

        recyclerView.adapter = feedAdapter
        carregarItensMarketplace()

        return view
    }

    private fun carregarItensMarketplace() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val novaLista = mutableListOf<Item>()

                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    for (itemSnapshot in userSnapshot.children) {
                        val item = itemSnapshot.getValue(Item::class.java)
                        item?.let {
                            val agora = System.currentTimeMillis()
                            if (it.dataExpiracao == 0L || it.dataExpiracao > agora) {
                                // adiciona o ID do item e do usuário
                                it.itemId = itemSnapshot.key
                                it.userId = userId
                                novaLista.add(it)
                            } else {
                                itemSnapshot.ref.removeValue()
                                Log.d("HomeFragment", "Item expirado removido: ${it.titulo}")
                            }
                        }
                    }
                }

                feedAdapter.atualizarLista(novaLista)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Erro ao carregar feed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deletarProduto(itemId: String, userId: String) {
        val currentUserId = auth.uid
        if (currentUserId != userId) {
            Toast.makeText(requireContext(), "Você só pode deletar seus próprios itens!", Toast.LENGTH_SHORT).show()
            return
        }

        databaseRef.child(userId).child(itemId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Item deletado com sucesso!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao deletar item", Toast.LENGTH_SHORT).show()
            }
    }

    private fun avaliarProduto(itemId: String, userId: String) {
        val produtoRef = databaseRef.child(userId).child(itemId).child("avaliacao")

        produtoRef.get().addOnSuccessListener { snapshot ->
            val avaliacaoAtual = snapshot.getValue(Float::class.java) ?: 0f
            val novaAvaliacao = if (avaliacaoAtual < 5f) avaliacaoAtual + 1f else 5f
            produtoRef.setValue(novaAvaliacao)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Produto avaliado com sucesso!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Erro ao avaliar produto", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
