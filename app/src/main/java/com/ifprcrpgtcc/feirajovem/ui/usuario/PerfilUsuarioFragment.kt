package com.ifprcrpgtcc.feirajovem.ui.usuario

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.*
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Item
import com.ifprcrpgtcc.feirajovem.baseclasses.Usuario
import com.ifprcrpgtcc.feirajovem.databinding.FragmentPerfilUsuarioBinding
import com.ifprcrpgtcc.feirajovem.ui.home.FeedAdapter
import com.ifprcrpgtcc.feirajovem.ui.login.LoginActivity

class PerfilUsuarioFragment : Fragment() {

    private var _binding: FragmentPerfilUsuarioBinding? = null
    private val binding get() = _binding!!

    private lateinit var userProfileImageView: ImageView
    private lateinit var registerNameEditText: TextView
    private lateinit var registerEmailEditText: TextView
    private lateinit var registerEnderecoEditText: TextView
    private lateinit var registerPasswordEditText: EditText
    private lateinit var registerConfirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var sairButton: Button
    private lateinit var usersReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // === COMPONENTES PARA LISTA DE PRODUTOS ===
    private lateinit var recyclerViewMeusProdutos: RecyclerView
    private lateinit var meusProdutosAdapter: FeedAdapter
    private val listaMeusProdutos = mutableListOf<Item>()
    private lateinit var itensRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_perfil_usuario, container, false)

        // Inicializa Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Vincula elementos do layout
        userProfileImageView = view.findViewById(R.id.userProfileImageView)
        registerNameEditText = view.findViewById(R.id.registerNameEditText)
        registerEmailEditText = view.findViewById(R.id.registerEmailEditText)
        registerEnderecoEditText = view.findViewById(R.id.registerEnderecoEditText)
        registerPasswordEditText = view.findViewById(R.id.registerPasswordEditText)
        registerConfirmPasswordEditText = view.findViewById(R.id.registerConfirmPasswordEditText)
        registerButton = view.findViewById(R.id.salvarButton)
        sairButton = view.findViewById(R.id.sairButton)

        // === Configuração do RecyclerView ===
        recyclerViewMeusProdutos = view.findViewById(R.id.recyclerViewMeusProdutos)
        recyclerViewMeusProdutos.layoutManager = GridLayoutManager(requireContext(), 2)

        meusProdutosAdapter = FeedAdapter(
            listaMeusProdutos,
            onLerMaisClick = { item ->
                Toast.makeText(requireContext(), "Ler mais de ${item.titulo}", Toast.LENGTH_SHORT).show()
            },
            onAvaliarClick = { _, _ ->
                Toast.makeText(requireContext(), "Avaliar desativado no perfil.", Toast.LENGTH_SHORT).show()
            },
            onDeletarClick = { itemId, userId ->
                confirmarExclusao(itemId, userId)
            }
        )
        recyclerViewMeusProdutos.adapter = meusProdutosAdapter

        itensRef = FirebaseDatabase.getInstance().getReference("itens")

        try {
            usersReference = FirebaseDatabase.getInstance().getReference("users")
        } catch (e: Exception) {
            Log.e("DatabaseReference", "Erro ao obter referência para o Firebase DatabaseReference", e)
            Toast.makeText(context, "Erro ao acessar o Firebase DatabaseReference", Toast.LENGTH_SHORT).show()
        }

        val user = auth.currentUser
        if (user != null) {
            sairButton.visibility = View.VISIBLE
            registerPasswordEditText.visibility = View.GONE
            registerConfirmPasswordEditText.visibility = View.GONE
            registerEmailEditText.isEnabled = false
        }

        user?.let {
            Glide.with(this).load(it.photoUrl).into(userProfileImageView)
        }

        registerButton.setOnClickListener {
            updateUser()
        }

        sairButton.setOnClickListener {
            signOut()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userFirebase = auth.currentUser
        if (userFirebase != null) {
            registerNameEditText.text = userFirebase.displayName
            registerEmailEditText.text = userFirebase.email
            recuperarDadosUsuario(userFirebase.uid)
            carregarMeusProdutos(userFirebase.uid) // Carrega os produtos do usuário
        }
    }

    /**
     * Busca os produtos postados pelo usuário logado
     */
    private fun carregarMeusProdutos(userId: String) {
        itensRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val novaLista = mutableListOf<Item>()
                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(Item::class.java)
                    item?.let {
                        it.itemId = itemSnapshot.key
                        it.userId = userId
                        novaLista.add(it)
                    }
                }

                listaMeusProdutos.clear()
                listaMeusProdutos.addAll(novaLista)
                meusProdutosAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Erro ao carregar produtos", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Confirma e deleta produto do perfil do usuário
     */
    private fun confirmarExclusao(itemId: String, userId: String) {
        itensRef.child(userId).child(itemId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Produto deletado!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao deletar produto", Toast.LENGTH_SHORT).show()
            }
    }

    private fun signOut() {
        auth.signOut()
        Toast.makeText(context, "Logout realizado com sucesso!", Toast.LENGTH_SHORT).show()

        // Redireciona para a tela de login
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        requireActivity().finish()
    }


    private fun recuperarDadosUsuario(usuarioKey: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("users")

        databaseReference.child(usuarioKey).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val usuario = snapshot.getValue(Usuario::class.java)
                    usuario?.let {
                        registerEnderecoEditText.text = it.endereco ?: ""
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Erro ao recuperar dados: ${error.message}")
            }
        })
    }

    private fun updateUser() {
        val name = registerNameEditText.text.toString().trim()
        val endereco = registerEnderecoEditText.text.toString().trim()

        val user = auth.currentUser
        if (user != null) {
            updateProfile(user, name, endereco)
        } else {
            Toast.makeText(context, "Não foi possível encontrar o usuário logado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProfile(user: FirebaseUser?, displayName: String, endereco: String) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()

        val usuario = Usuario(
            key = user?.uid.toString(),
            nome = displayName,
            email = user?.email,
            endereco = endereco
        )

        user?.updateProfile(profileUpdates)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserToDatabase(usuario)
                    Toast.makeText(context, "Nome do usuario alterado com sucesso.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Não foi possivel alterar o nome do usuario.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToDatabase(usuario: Usuario) {
        if (usuario.key != null) {
            usersReference.child(usuario.key.toString()).setValue(usuario)
                .addOnSuccessListener {
                    Toast.makeText(context, "Usuario atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Falha ao atualizar o usuario", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "ID invalido", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
