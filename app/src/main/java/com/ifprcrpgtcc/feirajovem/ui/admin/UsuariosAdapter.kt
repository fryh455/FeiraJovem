package com.ifprcrpgtcc.feirajovem.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Usuario

class UsuariosAdapter(
    private val usuariosList: List<Usuario>,
    private val onRemoverClick: (Usuario) -> Unit,
    private val onPromoverClick: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder>() {

    inner class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nomeText: TextView = itemView.findViewById(R.id.textNomeUsuarioAdmin)
        private val emailText: TextView = itemView.findViewById(R.id.textEmailUsuarioAdmin)
        private val removerButton: Button = itemView.findViewById(R.id.buttonRemoverUsuarioAdmin)
        private val promoverButton: Button = itemView.findViewById(R.id.buttonPromoverUsuarioAdmin)

        fun bind(usuario: Usuario) {
            nomeText.text = usuario.nome ?: "Sem nome"
            emailText.text = usuario.email ?: "Sem email"

            removerButton.setOnClickListener { onRemoverClick(usuario) }
            promoverButton.setOnClickListener { onPromoverClick(usuario) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario_admin, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        holder.bind(usuariosList[position])
    }

    override fun getItemCount(): Int = usuariosList.size
}
