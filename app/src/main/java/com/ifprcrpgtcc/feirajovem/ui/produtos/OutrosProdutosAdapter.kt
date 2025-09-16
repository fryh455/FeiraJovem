package com.ifprcrpgtcc.feirajovem.ui.feed

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

class OutrosProdutosAdapter(
    private val itens: List<Item>
) : RecyclerView.Adapter<OutrosProdutosAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagem: ImageView = view.findViewById(R.id.produtoExtraImagem)
        val titulo: TextView = view.findViewById(R.id.produtoExtraTitulo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_produto_extra, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = itens[position]
        holder.titulo.text = item.titulo ?: ""
        item.imagemBase64?.let {
            val bytes = Base64.decode(it, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            holder.imagem.setImageBitmap(bitmap)
        }
    }

    override fun getItemCount(): Int = itens.size
}
