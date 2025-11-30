package com.ifprcrpgtcc.feirajovem.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Item
import com.ifprcrpgtcc.feirajovem.baseclasses.Usuario

class ProdutosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var produtosAdapter: ProdutosAdapter
    private val produtosList = mutableListOf<Item>()
    private lateinit var databaseReference: DatabaseReference
    var escolaAdmin: String? = null  // público para ser setado pelo AdminPanel

    companion object {
        private const val TAG = "ProdutosFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_produtos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewProdutosAdmin)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        produtosAdapter = ProdutosAdapter(produtosList) { produto -> deletarProduto(produto) }
        recyclerView.adapter = produtosAdapter

        databaseReference = FirebaseDatabase.getInstance().getReference("itens")
        carregarProdutos()
    }

    private fun carregarProdutos() {
        escolaAdmin?.let { escola ->
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    produtosList.clear()
                    Log.d(TAG, "Snapshot de produtos recebido, total=${snapshot.childrenCount}")
                    snapshot.children.forEach { ds ->
                        val produto = ds.getValue(Item::class.java)
                        produto?.itemId = ds.key
                        Log.d(TAG, "Produto parseado: $produto")

                        val userId = produto?.userId
                        if (userId.isNullOrEmpty()) {
                            Log.w(TAG, "Produto ignorado porque userId é nulo: ${produto?.itemId}")
                            return@forEach
                        }

                        FirebaseDatabase.getInstance().getReference("usuarios")
                            .child(userId)
                            .get()
                            .addOnSuccessListener { userSnap ->
                                val usuario = userSnap.getValue(Usuario::class.java)
                                if (usuario?.escola == escola) {
                                    produto?.let {
                                        produtosList.add(it)
                                        produtosAdapter.notifyDataSetChanged()
                                        Log.d(TAG, "Produto adicionado: ${it.itemId}")
                                    }
                                } else {
                                    Log.d(TAG, "Produto ignorado (escola mismatch): ${produto?.itemId}")
                                }
                            }
                            .addOnFailureListener {
                                Log.e(TAG, "Falha ao buscar usuário $userId: ${it.message}")
                            }
                    }
                    Log.d(TAG, "Finalizado processamento do snapshot, produtos visíveis até agora: ${produtosList.size}")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Erro ao carregar produtos: ${error.message}")
                    Toast.makeText(requireContext(), "Erro ao carregar produtos", Toast.LENGTH_SHORT).show()
                }
            })
        } ?: Log.d(TAG, "escolaAdmin é nulo, não foi possível carregar produtos")
    }

    private fun deletarProduto(produto: Item) {
        produto.itemId?.let { id ->
            databaseReference.child(id).removeValue()
                .addOnSuccessListener { Log.d(TAG, "Produto deletado: $id") }
                .addOnFailureListener { Log.e(TAG, "Falha ao deletar produto: ${it.message}") }
        }
    }
}
