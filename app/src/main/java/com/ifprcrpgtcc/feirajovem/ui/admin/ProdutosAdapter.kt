package com.ifprcrpgtcc.feirajovem.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Item

class ProdutosAdapter(
    private val produtosList: List<Item>,
    private val onClick: (Item) -> Unit
) : RecyclerView.Adapter<ProdutosAdapter.ProdutoViewHolder>() {

    inner class ProdutoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tituloText: TextView = itemView.findViewById(R.id.textTituloProdutoAdmin)
        private val descricaoText: TextView = itemView.findViewById(R.id.textDescricaoProdutoAdmin)
        private val precoText: TextView = itemView.findViewById(R.id.textPrecoProdutoAdmin)

        fun bind(produto: Item) {
            tituloText.text = produto.titulo
            descricaoText.text = produto.descricao
            precoText.text = "R$ ${produto.preco}"
            itemView.setOnClickListener { onClick(produto) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProdutoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_produto_admin, parent, false)
        return ProdutoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProdutoViewHolder, position: Int) {
        holder.bind(produtosList[position])
    }

    override fun getItemCount(): Int = produtosList.size
}
