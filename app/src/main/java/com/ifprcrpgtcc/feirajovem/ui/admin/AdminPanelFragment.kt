package com.ifprcrpgtcc.feirajovem.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ifprcrpgtcc.feirajovem.R

class AdminPanelFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla o XML do painel de admin
        return inflater.inflate(R.layout.activity_admin_panel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnGerenciarUsuarios = view.findViewById<Button>(R.id.buttonGerenciarUsuarios)
        val btnGerenciarProdutos = view.findViewById<Button>(R.id.buttonGerenciarProdutos)
        val btnGerenciarDenuncias = view.findViewById<Button>(R.id.buttonGerenciarDenuncias)

        btnGerenciarUsuarios.setOnClickListener {
            Toast.makeText(requireContext(), "Gerenciar Usuários", Toast.LENGTH_SHORT).show()
            // Abre fragment de usuários dentro do container da activity
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.admin_feed_container, UsuariosFragment())
                .addToBackStack(null)
                .commit()
        }

        btnGerenciarProdutos.setOnClickListener {
            Toast.makeText(requireContext(), "Gerenciar Produtos", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.admin_feed_container, ProdutosFragment())
                .addToBackStack(null)
                .commit()
        }

        btnGerenciarDenuncias.setOnClickListener {
            Toast.makeText(requireContext(), "Gerenciar Denúncias", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.admin_feed_container, DenunciasFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
