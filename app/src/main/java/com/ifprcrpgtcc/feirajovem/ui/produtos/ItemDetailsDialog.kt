package com.ifprcrpgtcc.feirajovem.ui.feed

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ifprcrpgtcc.feirajovem.R
import com.ifprcrpgtcc.feirajovem.baseclasses.Item
import com.ifprcrpgtcc.feirajovem.baseclasses.Usuario
import com.google.firebase.database.*

class ItemDetailsDialog(
    context: Context,
    private val item: Item
) : Dialog(context) {

    private lateinit var database: DatabaseReference

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_item_detalhes)
        setCancelable(true)

        // Views
        val imagemProduto: ImageView = findViewById(R.id.dialogProductImage)
        val tituloProduto: TextView = findViewById(R.id.dialogProductTitle)
        val descricaoProduto: TextView = findViewById(R.id.dialogProductDescription)
        val precoProduto: TextView = findViewById(R.id.dialogProductPrice)

        val imagemUsuario: ImageView = findViewById(R.id.dialogUserImage)
        val nomeUsuario: TextView = findViewById(R.id.dialogUserName)
        val emailUsuario: TextView = findViewById(R.id.dialogUserEmail)
        val escolaUsuario: TextView = findViewById(R.id.dialogUserSchool)

        val recyclerOutrosProdutos: RecyclerView = findViewById(R.id.dialogOtherProductsRecycler)
        recyclerOutrosProdutos.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // Imagem do produto
        item.imagemBase64?.let {
            val bytes = Base64.decode(it, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imagemProduto.setImageBitmap(bitmap)
        }

        tituloProduto.text = item.titulo ?: ""
        descricaoProduto.text = item.descricao ?: ""
        precoProduto.text = "R$ ${item.preco ?: "0,00"}"

        // Dados do usuário e outros produtos
        item.userId?.let { userId ->
            database = FirebaseDatabase.getInstance().getReference("usuarios")
            database.child(userId).get().addOnSuccessListener { snapshot ->
                val usuario = snapshot.getValue(Usuario::class.java)
                usuario?.let { user ->
                    nomeUsuario.text = user.nome ?: ""
                    emailUsuario.text = user.email ?: ""
                    escolaUsuario.text = user.escola ?: ""

                    user.fotoBase64?.let { foto ->
                        val bytes = Base64.decode(foto, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        imagemUsuario.setImageBitmap(bitmap)
                    }

                    // Outros produtos do usuário
                    database = FirebaseDatabase.getInstance().getReference("itens")
                    database.child(userId).get().addOnSuccessListener { itensSnapshot ->
                        val outrosItens = mutableListOf<Item>()
                        for (child in itensSnapshot.children) {
                            val i = child.getValue(Item::class.java)
                            if (i != null && i.itemId != item.itemId) {
                                outrosItens.add(i)
                            }
                        }
                        val adapter = OutrosProdutosAdapter(outrosItens)
                        recyclerOutrosProdutos.adapter = adapter
                    }
                }
            }
        }
    }
}
