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
import com.ifprcrpgtcc.feirajovem.baseclasses.Usuario

class UsuariosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var usuariosAdapter: UsuariosAdapter
    private val usuariosList = mutableListOf<Usuario>()
    private lateinit var databaseReference: DatabaseReference
    var escolaAdmin: String? = null  // Público

    companion object {
        private const val TAG = "UsuariosFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_usuarios, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerUsuarios)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        usuariosAdapter = UsuariosAdapter(usuariosList) { usuario -> deletarUsuario(usuario) }
        recyclerView.adapter = usuariosAdapter

        // Pega usuário logado e escola direto
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.d(TAG, "Usuário não logado")
            return
        }

        FirebaseDatabase.getInstance().getReference("usuarios")
            .child(currentUser.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val usuario = snapshot.getValue(Usuario::class.java)
                escolaAdmin = usuario?.escola
                Log.d(TAG, "escolaAdmin='$escolaAdmin'")

                if (!escolaAdmin.isNullOrEmpty()) {
                    databaseReference = FirebaseDatabase.getInstance().getReference("usuarios")
                    carregarUsuarios()
                } else {
                    Log.d(TAG, "Usuário sem escola definida")
                    Toast.makeText(requireContext(), "Erro: usuário sem escola", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Erro ao buscar dados do usuário: ${it.message}")
            }
    }

    private fun carregarUsuarios() {
        Log.d(TAG, "carregarUsuarios - escolaFiltro='$escolaAdmin'")
        escolaAdmin?.let { escola ->
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    usuariosList.clear()
                    snapshot.children.forEach { ds ->
                        val usuario = ds.getValue(Usuario::class.java)
                        usuario?.key = ds.key
                        if (usuario?.escola == escola) {
                            usuario?.let { usuariosList.add(it) }
                            usuariosAdapter.notifyDataSetChanged()
                            Log.d(TAG, "Usuário adicionado: ${usuario.key}")
                        } else {
                            Log.d(TAG, "Usuário ignorado (escola mismatch): id=${usuario?.key}")
                        }
                    }
                    Log.d(TAG, "Total usuários visíveis: ${usuariosList.size}")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Erro ao carregar usuários: ${error.message}")
                    Toast.makeText(context, "Erro ao carregar usuários", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun deletarUsuario(usuario: Usuario) {
        usuario.key?.let { id ->
            databaseReference.child(id).removeValue()
                .addOnSuccessListener { Log.d(TAG, "Usuário deletado: $id") }
                .addOnFailureListener { Log.e(TAG, "Falha ao deletar usuário: ${it.message}") }
        }
    }
}
