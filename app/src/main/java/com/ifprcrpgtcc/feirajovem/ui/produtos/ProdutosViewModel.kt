package com.ifprcrpgtcc.feirajovem.ui.produtos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProdutosViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Cadastro de "
    }
    val text: LiveData<String> = _text
}