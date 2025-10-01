package com.ifprcrpgtcc.feirajovem.ui.usuario

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Item
import com.ifprcrpgtcc.feirajovem.baseclasses.Usuario
import com.ifprcrpgtcc.feirajovem.databinding.FragmentPerfilUsuarioBinding
import com.ifprcrpgtcc.feirajovem.ui.denuncias.UsuarioDenunciaActivity
import com.ifprcrpgtcc.feirajovem.ui.home.FeedAdapter
import com.ifprcrpgtcc.feirajovem.ui.login.LoginActivity


class PerfilUsuarioFragment : Fragment() {

    private var _binding: FragmentPerfilUsuarioBinding? = null
    private val binding get() = _binding!!

    private lateinit var userProfileImageView: ImageView
    private lateinit var registerNameEditText: TextView
    private lateinit var registerEmailEditText: TextView
    private lateinit var registerEnderecoEditText: TextView
    private lateinit var sairButton: Button
    private lateinit var editarPerfilButton: Button
    private lateinit var usersReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // Lista de produtos do usuário
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

        auth = FirebaseAuth.getInstance()

        // Vincula elementos do layout
        userProfileImageView = view.findViewById(R.id.userProfileImageView)
        registerNameEditText = view.findViewById(R.id.registerNameEditText)
        registerEmailEditText = view.findViewById(R.id.registerEmailEditText)
        registerEnderecoEditText = view.findViewById(R.id.registerEnderecoEditText)
        sairButton = view.findViewById(R.id.sairButton)
        editarPerfilButton = view.findViewById(R.id.salvarButton)

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
            },
            onDenunciarClick = { item ->
                // Abre pop-up de denúncia
                val denunciaDialog = UsuarioDenunciaActivity.showDenunciaDialog(requireContext())
            }
        )
        recyclerViewMeusProdutos.adapter = meusProdutosAdapter

        itensRef = FirebaseDatabase.getInstance().getReference("itens")
        usersReference = FirebaseDatabase.getInstance().getReference("users")

        val user = auth.currentUser
        if (user != null) {
            sairButton.visibility = View.VISIBLE
            registerEmailEditText.isEnabled = false
        }

        sairButton.setOnClickListener { signOut() }

        editarPerfilButton.setOnClickListener {
            val intent = Intent(requireContext(), EditarPerfilActivity::class.java)
            startActivity(intent)
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
            carregarMeusProdutos(userFirebase.uid)
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
     * Recupera os dados do usuário, incluindo a foto em Base64
     */
    private fun recuperarDadosUsuario(usuarioKey: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("users")

        databaseReference.child(usuarioKey).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val usuario = snapshot.getValue(Usuario::class.java)
                    usuario?.let {
                        registerEnderecoEditText.text = it.endereco ?: ""

                        // Log para depuração
                        Log.d("PerfilUsuarioFragment", "Base64 recebida: ${it.fotoBase64?.take(50)}")

                        // Foto de perfil
                        if (!it.fotoBase64.isNullOrEmpty()) {
                            try {
                                val decodedBytes = Base64.decode(it.fotoBase64, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                userProfileImageView.setImageBitmap(bitmap)
                            } catch (e: Exception) {
                                Log.e("PerfilUsuarioFragment", "Erro ao decodificar imagem: ${e.message}")
                                userProfileImageView.setImageResource(R.drawable.ic_placeholder_user)
                            }
                        } else {
                            userProfileImageView.setImageResource(R.drawable.ic_placeholder_user)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Erro ao recuperar dados: ${error.message}")
            }
        })
    }

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

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
