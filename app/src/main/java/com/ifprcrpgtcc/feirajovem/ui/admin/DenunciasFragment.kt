package com.ifprcrpgtcc.feirajovem.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Denuncias
import androidx.recyclerview.widget.RecyclerView

class DenunciasFragment : Fragment() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DenunciasAdapter
    private val denunciasList = mutableListOf<Denuncias>()

    companion object {
        private const val TAG = "DenunciasFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_denuncias, container, false)
        recyclerView = view.findViewById(R.id.recyclerDenuncias)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = DenunciasAdapter(denunciasList, ::resolverDenuncia, ::ignorarDenuncia)
        recyclerView.adapter = adapter

        databaseReference = FirebaseDatabase.getInstance().getReference("denuncias")
        carregarDenuncias()

        return view
    }

    private fun carregarDenuncias() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                denunciasList.clear()
                for (denunciaSnapshot in snapshot.children) {
                    val denuncia = denunciaSnapshot.getValue(Denuncias::class.java)
                    if (denuncia != null) {
                        denunciasList.add(denuncia)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Erro ao carregar denúncias: ${error.message}")
                Toast.makeText(context, "Erro ao carregar denúncias", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun resolverDenuncia(denuncia: Denuncias) {
        denuncia.denunciaId?.let { id ->
            databaseReference.child(id).child("status").setValue("resolvido")
        }
    }

    private fun ignorarDenuncia(denuncia: Denuncias) {
        denuncia.denunciaId?.let { id ->
            databaseReference.child(id).child("status").setValue("ignorado")
        }
    }
}
