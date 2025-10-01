package com.ifprcrpgtcc.feirajovem.ui.denuncias

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Denuncias

class DenunciaAdapter(
    private val denunciasList: List<Denuncias>
) : RecyclerView.Adapter<DenunciaAdapter.DenunciaViewHolder>() {

    inner class DenunciaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val motivoText: TextView = itemView.findViewById(R.id.textMotivoDenuncia)
        private val statusText: TextView = itemView.findViewById(R.id.textStatusDenuncia)

        fun bind(denuncia: Denuncias) {
            motivoText.text = denuncia.motivo ?: "Sem motivo"
            statusText.text = "Status: ${denuncia.status}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DenunciaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_denuncia, parent, false)
        return DenunciaViewHolder(view)
    }

    override fun onBindViewHolder(holder: DenunciaViewHolder, position: Int) {
        holder.bind(denunciasList[position])
    }

    override fun getItemCount(): Int = denunciasList.size
}
