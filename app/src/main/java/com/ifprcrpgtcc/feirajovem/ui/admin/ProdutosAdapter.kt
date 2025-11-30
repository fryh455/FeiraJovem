package com.ifprcrpgtcc.feirajovem.ui.admin

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

class ProdutosAdapter(
    private val produtos: List<Item>,
    private val onDeleteClick: (Item) -> Unit
) : RecyclerView.Adapter<ProdutosAdapter.ProdutoViewHolder>() {

    class ProdutoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView = view.findViewById(R.id.textTituloProdutoAdmin)
        val descricao: TextView = view.findViewById(R.id.textDescricaoProdutoAdmin)
        val preco: TextView = view.findViewById(R.id.textPrecoProdutoAdmin)
        val imagem: ImageView = view.findViewById(R.id.imageProduto)
        val btnDeletar: Button = view.findViewById(R.id.buttonRemoverProdutoAdmin)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProdutoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_produto_admin, parent, false)
        return ProdutoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProdutoViewHolder, position: Int) {
        val produto = produtos[position]

        holder.titulo.text = produto.titulo
        holder.descricao.text = produto.descricao
        holder.preco.text = "R$ ${produto.preco}"

        if (!produto.imagemBase64.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(produto.imagemBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.imagem.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.imagem.setImageResource(R.drawable.ic_placeholder_user)
            }
        } else {
            holder.imagem.setImageResource(R.drawable.ic_placeholder_user)
        }

        holder.btnDeletar.setOnClickListener { onDeleteClick(produto) }
    }

    override fun getItemCount(): Int = produtos.size
}
