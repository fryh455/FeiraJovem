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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Denuncias
import com.ifprcrpgtcc.feirajovem.baseclasses.Usuario

class DenunciasFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DenunciasAdapter
    private val denunciasList = mutableListOf<Denuncias>()
    private lateinit var databaseReference: DatabaseReference
    var escolaAdmin: String? = null  // Mantido público

    companion object {
        private const val TAG = "DenunciasFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_denuncias, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerDenuncias)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = DenunciasAdapter(denunciasList,
            onResolverClick = { denuncia -> resolverDenuncia(denuncia) },
            onIgnorarClick = { denuncia -> ignorarDenuncia(denuncia) }
        )
        recyclerView.adapter = adapter

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.d(TAG, "Usuário não logado")
            return
        }

        // Busca a escola do admin
        FirebaseDatabase.getInstance().getReference("usuarios")
            .child(currentUser.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val usuario = snapshot.getValue(Usuario::class.java)
                escolaAdmin = usuario?.escola
                Log.d(TAG, "escolaAdmin='$escolaAdmin'")

                if (!escolaAdmin.isNullOrEmpty()) {
                    databaseReference = FirebaseDatabase.getInstance().getReference("denuncias")
                    carregarDenuncias()
                } else {
                    Log.d(TAG, "Usuário sem escola definida")
                    Toast.makeText(context, "Erro: usuário sem escola", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Erro ao buscar dados do usuário: ${it.message}")
            }
    }

    private fun carregarDenuncias() {
        Log.d(TAG, "carregarDenuncias - escolaFiltro='$escolaAdmin'")
        escolaAdmin?.let { escola ->
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    denunciasList.clear()
                    snapshot.children.forEach { ds ->
                        val denuncia = ds.getValue(Denuncias::class.java)
                        denuncia?.denunciaId = ds.key
                        val denuncianteId = denuncia?.denuncianteId ?: return@forEach

                        FirebaseDatabase.getInstance().getReference("usuarios")
                            .child(denuncianteId)
                            .get()
                            .addOnSuccessListener { userSnap ->
                                val usuario = userSnap.getValue(Usuario::class.java)
                                if (usuario?.escola == escolaAdmin) {
                                    denuncia?.let { denunciasList.add(it) }
                                    adapter.notifyDataSetChanged()
                                    Log.d(TAG, "Denúncia adicionada: ${denuncia.denunciaId}")
                                } else {
                                    Log.d(TAG, "Denúncia ignorada (escola mismatch): id=${denuncia?.denunciaId}")
                                }
                            }
                    }
                    Log.d(TAG, "Total denúncias visíveis: ${denunciasList.size}")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Erro ao carregar denúncias: ${error.message}")
                    Toast.makeText(context, "Erro ao carregar denúncias", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun resolverDenuncia(denuncia: Denuncias) {
        denuncia.denunciaId?.let { id ->
            databaseReference.child(id).removeValue()
                .addOnSuccessListener { Log.d(TAG, "Denúncia resolvida: $id") }
                .addOnFailureListener { Log.e(TAG, "Erro ao resolver denúncia: ${it.message}") }
        }
    }

    private fun ignorarDenuncia(denuncia: Denuncias) {
        denuncia.denunciaId?.let { id ->
            databaseReference.child(id).child("status").setValue("ignorado")
                .addOnSuccessListener { Log.d(TAG, "Denúncia ignorada: $id") }
                .addOnFailureListener { Log.e(TAG, "Erro ao ignorar denúncia: ${it.message}") }
        }
    }
}
