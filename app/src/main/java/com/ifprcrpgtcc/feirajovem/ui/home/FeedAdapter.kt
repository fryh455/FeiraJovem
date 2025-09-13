package com.ifprcrpgtcc.feirajovem.ui.home

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Item

class FeedAdapter(
    private val listaItens: MutableList<Item>,
    private val onLerMaisClick: (Item) -> Unit
) : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagem: ImageView = itemView.findViewById(R.id.imageAnuncio)
        val titulo: TextView = itemView.findViewById(R.id.textTitulo)
        val avaliacao: TextView = itemView.findViewById(R.id.textAvaliacao)
        val preco: TextView = itemView.findViewById(R.id.textPreco)
        val descricao: TextView = itemView.findViewById(R.id.textDescricao)
        val lerMais: TextView = itemView.findViewById(R.id.textLerMais)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_feed, parent, false)
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val item = listaItens[position]

        // Título
        holder.titulo.text = item.titulo

        // Avaliação
        holder.avaliacao.text = "Avaliação: ★ ${item.avaliacao}"

        // Preço
        holder.preco.text = "R$ ${item.preco}"

        // Descrição resumida
        holder.descricao.text = item.descricao

        // Ler mais
        holder.lerMais.setOnClickListener { onLerMaisClick(item) }

        // Decodificar imagem Base64
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
