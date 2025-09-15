package com.ifprcrpgtcc.feirajovem.ui.home

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.text.TextUtils
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Item
import java.util.concurrent.TimeUnit

class FeedAdapter(
    private val listaItens: MutableList<Item>,
    private val onLerMaisClick: (Item) -> Unit,
    private val onAvaliarClick: (String, String) -> Unit,
    private val onDeletarClick: (String, String) -> Unit
) : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    // Lista para controlar quais descrições estão expandidas
    private val itensExpandidos = mutableSetOf<String>()

    class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagem: ImageView = itemView.findViewById(R.id.imageAnuncio)
        val titulo: TextView = itemView.findViewById(R.id.textTitulo)
        val avaliacao: TextView = itemView.findViewById(R.id.textAvaliacao)
        val tempoRestante: TextView = itemView.findViewById(R.id.textTempoRestante)
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
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        holder.titulo.text = item.titulo
        holder.avaliacao.text = "Avaliação: ★ ${item.mediaAvaliacao.toInt()} (${item.mediaAvaliacao})"
        holder.preco.text = "R$ ${item.preco}"
        holder.descricao.text = item.descricao

        // ===== CALCULA O TEMPO RESTANTE =====
        if (item.dataExpiracao > 0) {
            val agora = System.currentTimeMillis()
            val restanteMillis = item.dataExpiracao - agora

            if (restanteMillis > 0) {
                val dias = TimeUnit.MILLISECONDS.toDays(restanteMillis)
                val horas = TimeUnit.MILLISECONDS.toHours(restanteMillis) % 24

                holder.tempoRestante.text = when {
                    dias > 0 -> "Faltam $dias dia(s) e $horas hora(s)"
                    horas > 0 -> "Faltam $horas hora(s)"
                    else -> "Menos de 1 hora restante"
                }
            } else {
                holder.tempoRestante.text = "Expirado"
            }
        } else {
            holder.tempoRestante.text = ""
        }

        // ===== LER MAIS / LER MENOS =====
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
                if (estaExpandido) {
                    itensExpandidos.remove(item.itemId!!)
                } else {
                    itensExpandidos.add(item.itemId!!)
                }
                notifyItemChanged(position)
            }
        }

        // ===== Botão Avaliar =====
        holder.btnAvaliar.setOnClickListener {
            if (!item.itemId.isNullOrEmpty() && !item.userId.isNullOrEmpty()) {
                onAvaliarClick(item.itemId!!, item.userId!!)
            }
        }

        // ===== Botão Deletar → só aparece para o dono =====
        if (item.userId == currentUserId) {
            holder.btnDeletar.visibility = View.VISIBLE
            holder.btnDeletar.setOnClickListener {
                if (!item.itemId.isNullOrEmpty() && !item.userId.isNullOrEmpty()) {
                    // Mostra alerta antes de deletar
                    AlertDialog.Builder(holder.itemView.context)
                        .setTitle("Confirmar exclusão")
                        .setMessage("Tem certeza que deseja deletar este produto?")
                        .setPositiveButton("Sim") { _, _ ->
                            onDeletarClick(item.itemId!!, item.userId!!)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        } else {
            holder.btnDeletar.visibility = View.GONE
        }

        // ===== Carregar imagem =====
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
