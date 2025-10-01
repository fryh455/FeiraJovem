package com.ifprcrpgtcc.feirajovem.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Usuario

class UsuariosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var usuariosAdapter: UsuariosAdapter
    private lateinit var databaseReference: DatabaseReference
    private val usuariosList = mutableListOf<Usuario>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_usuarios, container, false)

        recyclerView = view.findViewById(R.id.recyclerUsuarios)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        databaseReference = FirebaseDatabase.getInstance().getReference("usuarios")

        usuariosAdapter = UsuariosAdapter(
            usuariosList,
            onRemoverClick = { usuario -> removerUsuario(usuario) },
            onPromoverClick = { usuario -> promoverUsuario(usuario) }
        )

        recyclerView.adapter = usuariosAdapter

        carregarUsuarios()

        return view
    }

    private fun carregarUsuarios() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usuariosList.clear()
                for (userSnapshot in snapshot.children) {
                    val usuario = userSnapshot.getValue(Usuario::class.java)
                    usuario?.let {
                        it.key = userSnapshot.key
                        usuariosList.add(it)
                    }
                }
                usuariosAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Erro ao carregar usuários", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun removerUsuario(usuario: Usuario) {
        usuario.key?.let {
            databaseReference.child(it).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Usuário removido com sucesso", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Falha ao remover usuário", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun promoverUsuario(usuario: Usuario) {
        usuario.key?.let {
            databaseReference.child(it).child("tipo").setValue("admin")
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Usuário promovido a admin", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Falha ao promover usuário", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
