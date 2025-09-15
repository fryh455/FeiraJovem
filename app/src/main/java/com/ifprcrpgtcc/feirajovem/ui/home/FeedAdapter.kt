package com.ifprcrpgtcc.feirajovem.ui.home

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Item

class FeedAdapter(
    private val listaItens: MutableList<Item>,
    private val onLerMaisClick: (Item) -> Unit,
    private val onAvaliarClick: (String, String) -> Unit,
    private val onDeletarClick: (String, String) -> Unit
) : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagem: ImageView = itemView.findViewById(R.id.imageAnuncio)
        val titulo: TextView = itemView.findViewById(R.id.textTitulo)
        val avaliacao: TextView = itemView.findViewById(R.id.textAvaliacao)
        val preco: TextView = itemView.findViewById(R.id.textPreco)
        val descricao: TextView = itemView.findViewById(R.id.textDescricao)
        val lerMais: TextView = itemView.findViewById(R.id.textLerMais)
        val btnAvaliar: Button = itemView.findViewById(R.id.btnAvaliar)
        val btnDeletar: Button = itemView.findViewById(R.id.btnDeletar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_feed, parent, false)
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val item = listaItens[position]

        holder.titulo.text = item.titulo
        holder.avaliacao.text = "Avaliação: ★ ${item.avaliacao} (${item.avaliacao.toInt()})"
        holder.preco.text = "R$ ${item.preco}"
        holder.descricao.text = item.descricao

        holder.lerMais.setOnClickListener { onLerMaisClick(item) }
        holder.btnAvaliar.setOnClickListener {
            if (!item.itemId.isNullOrEmpty() && !item.userId.isNullOrEmpty()) {
                onAvaliarClick(item.itemId!!, item.userId!!)
            }
        }
        holder.btnDeletar.setOnClickListener {
            if (!item.itemId.isNullOrEmpty() && !item.userId.isNullOrEmpty()) {
                onDeletarClick(item.itemId!!, item.userId!!)
            }
        }

        if (!item.imagemBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(item.imagemBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.imagem.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getItemCount(): Int = listaItens.size

    fun atualizarLista(novaLista: List<Item>) {
        listaItens.clear()
        listaItens.addAll(novaLista)
        notifyDataSetChanged()
    }
}
