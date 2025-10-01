package com.ifprcrpgtcc.feirajovem.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Item

class ProdutosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var produtosAdapter: ProdutosAdapter
    private val produtosList = mutableListOf<Item>()

    private lateinit var databaseReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_produtos, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewProdutosAdmin)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        produtosAdapter = ProdutosAdapter(produtosList) { produto ->
            // Aqui você pode abrir detalhes do produto ou permitir ações
        }
        recyclerView.adapter = produtosAdapter

        databaseReference = FirebaseDatabase.getInstance().getReference("itens")
        carregarProdutos()

        return view
    }

    private fun carregarProdutos() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                produtosList.clear()
                snapshot.children.forEach { userSnap ->
                    userSnap.children.forEach { itemSnap ->
                        val produto = itemSnap.getValue(Item::class.java)
                        produto?.let { produtosList.add(it) }
                    }
                }
                produtosAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
