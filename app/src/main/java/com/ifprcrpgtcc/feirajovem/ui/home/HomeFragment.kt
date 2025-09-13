package com.ifprcrpgtcc.feirajovem.ui.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Item

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var databaseRef: DatabaseReference
    private val listaItens = mutableListOf<Item>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_feed, container, false)

        // Configura RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewFeed)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        feedAdapter = FeedAdapter(listaItens) { item ->
            Toast.makeText(requireContext(), "Ler mais de ${item.titulo}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = feedAdapter

        // Firebase
        databaseRef = FirebaseDatabase.getInstance().getReference("itens")

        carregarItensMarketplace()

        return view
    }

    /** Carregar produtos do Firebase */
    private fun carregarItensMarketplace() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val novaLista = mutableListOf<Item>()

                for (userSnapshot in snapshot.children) {
                    for (itemSnapshot in userSnapshot.children) {
                        val item = itemSnapshot.getValue(Item::class.java)
                        item?.let {
                            val agora = System.currentTimeMillis()
                            // Verifica se ainda não expirou
                            if (it.dataExpiracao == 0L || it.dataExpiracao > agora) {
                                novaLista.add(it)
                            } else {
                                // Remove do banco se expirou
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
}
