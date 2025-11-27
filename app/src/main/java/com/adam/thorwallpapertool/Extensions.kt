package com.adam.thorwallpapertool

import android.text.Editable
import android.text.TextWatcher

/**
 * EditText扩展函数，简化TextWatcher的使用
 */
inline fun android.widget.EditText.setOnTextChanged(
    crossinline onTextChanged: (text: CharSequence?, start: Int, before: Int, count: Int) -> Unit
) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChanged(s, start, before, count)
        }
        override fun afterTextChanged(s: Editable?) {}
    })
}