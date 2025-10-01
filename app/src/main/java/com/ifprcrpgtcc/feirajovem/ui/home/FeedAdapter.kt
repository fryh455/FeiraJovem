package com.ifprcrpgtcc.feirajovem.ui.home

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Item
import com.ifprcrpgtcc.feirajovem.baseclasses.Usuario
import com.ifprcrpgtcc.feirajovem.ui.feed.ItemDetailsDialog
import java.util.concurrent.TimeUnit

class FeedAdapter(
    private val listaItens: MutableList<Item>,
    private val onLerMaisClick: (Item) -> Unit,
    private val onAvaliarClick: (String, String) -> Unit,
    private val onDeletarClick: (String, String) -> Unit,
    private val onDenunciarClick: (Item) -> Unit // callback do botão denunciar
) : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    private val itensExpandidos = mutableSetOf<String>()

    class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagem: ImageView = itemView.findViewById(R.id.imageAnuncio)
        val imagemUsuario: ImageView = itemView.findViewById(R.id.imageUser)
        val titulo: TextView = itemView.findViewById(R.id.textTitulo)
        val avaliacao: TextView = itemView.findViewById(R.id.textAvaliacao)
        val tempoRestante: TextView = itemView.findViewById(R.id.textTempoRestante)
        val preco: TextView = itemView.findViewById(R.id.textPreco)
        val descricao: TextView = itemView.findViewById(R.id.textDescricao)
        val lerMais: TextView = itemView.findViewById(R.id.textLerMais)
        val btnAvaliar: Button = itemView.findViewById(R.id.btnAvaliar)
        val btnDeletar: Button = itemView.findViewById(R.id.btnDeletar)
        val btnDenunciar: Button = itemView.findViewById(R.id.btnDenunciar) // botão denunciar
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_feed, parent, false)
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val item = listaItens[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Reset ImageViews
        holder.imagem.setImageDrawable(null)
        holder.imagemUsuario.setImageResource(R.drawable.ic_placeholder_user)

        holder.titulo.text = item.titulo ?: ""
        holder.avaliacao.text = "Avaliação: ★ ${item.mediaAvaliacao.toInt()} (${item.mediaAvaliacao})"
        holder.preco.text = "R$ ${item.preco ?: "0,00"}"
        holder.descricao.text = item.descricao ?: ""

        // TEMPO RESTANTE
        if (item.dataExpiracao > 0) {
            val restanteMillis = item.dataExpiracao - System.currentTimeMillis()
            holder.tempoRestante.text = when {
                restanteMillis > 0 -> {
                    val dias = TimeUnit.MILLISECONDS.toDays(restanteMillis)
                    val horas = TimeUnit.MILLISECONDS.toHours(restanteMillis) % 24
                    if (dias > 0) "Faltam $dias dia(s) e $horas hora(s)" else "Faltam $horas hora(s)"
                }
                else -> "Expirado"
            }
        } else holder.tempoRestante.text = ""

        // LER MAIS
        val estaExpandido = item.itemId?.let { itensExpandidos.contains(it) } ?: false
        if (estaExpandido) {
            holder.descricao.maxLines = Integer.MAX_VALUE
            holder.descricao.ellipsize = null
            holder.lerMais.text = "ler menos"
        } else {
            holder.descricao.maxLines = 2
            holder.descricao.ellipsize = TextUtils.TruncateAt.END
            holder.lerMais.text = "ler mais"
        }

        holder.lerMais.setOnClickListener {
            if (!item.itemId.isNullOrEmpty()) {
                if (estaExpandido) itensExpandidos.remove(item.itemId!!) else itensExpandidos.add(item.itemId!!)
                notifyItemChanged(position)
            }
            onLerMaisClick(item)
        }

        // BOTÃO AVALIAR
        holder.btnAvaliar.setOnClickListener {
            if (!item.itemId.isNullOrEmpty() && !item.userId.isNullOrEmpty())
                onAvaliarClick(item.itemId!!, item.userId!!)
        }

        // BOTÃO DELETAR (só para dono)
        if (item.userId == currentUserId) {
            holder.btnDeletar.visibility = View.VISIBLE
            holder.btnDeletar.setOnClickListener {
                if (!item.itemId.isNullOrEmpty() && !item.userId.isNullOrEmpty()) {
                    AlertDialog.Builder(holder.itemView.context)
                        .setTitle("Confirmar exclusão")
                        .setMessage("Tem certeza que deseja deletar este produto?")
                        .setPositiveButton("Sim") { _, _ -> onDeletarClick(item.itemId!!, item.userId!!) }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        } else holder.btnDeletar.visibility = View.GONE

        // BOTÃO DENUNCIAR
        holder.btnDenunciar.setOnClickListener {
            onDenunciarClick(item)
        }

        // CARREGAR IMAGEM DO PRODUTO
        item.imagemBase64.let {
            try {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                holder.imagem.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            } catch (_: Exception) {}
        }

        // MINIATURA USUÁRIO
        holder.imagemUsuario.setImageResource(R.drawable.ic_placeholder_user)
        item.userId?.let { userId ->
            val dbUser = FirebaseDatabase.getInstance().getReference("usuarios")
            dbUser.child(userId).get().addOnSuccessListener { snapshot ->
                val usuario = snapshot.getValue(Usuario::class.java)
                if (!usuario?.fotoBase64.isNullOrEmpty()) {
                    try {
                        val bytes = Base64.decode(usuario?.fotoBase64, Base64.DEFAULT)
                        holder.imagemUsuario.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                    } catch (_: Exception) {}
                }
            }
        }

        // CLIQUE NA IMAGEM
        holder.imagem.setOnClickListener {
            val dialog = ItemDetailsDialog(holder.itemView.context, item)
            dialog.show()
        }
    }

    override fun getItemCount(): Int = listaItens.size

    fun atualizarLista(novaLista: List<Item>) {
        listaItens.clear()
        listaItens.addAll(novaLista)
        notifyDataSetChanged()
    }
}
