package com.shakespend.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDatabase.get(this).expenseDao()
        setContent {
            MaterialTheme {
                var amount by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
                var note by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
                var category by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Food") }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Quick expense", style = MaterialTheme.typography.headlineMedium)
                        OutlinedTextField(amount, { amount = it }, label = { Text("Amount (₹)") })
                        Text("Category: $category")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Food", "Travel", "Shopping", "Other").forEach {
                                FilterChip(selected = category == it, onClick = { category = it }, label = { Text(it) })
                            }
                        }
                        OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") })
                        Button(onClick = {
                            val value = amount.toDoubleOrNull()
                            if (value != null && value > 0) {
                                lifecycleScope.launch {
                                    dao.insert(Expense(amountPaise = (value * 100).toLong(),
                                        category = category, note = note))
                                    finish()
                                }
                            }
                        }) { Text("Save expense") }
                        TextButton(onClick = { finish() }) { Text("Cancel") }
                    }
                }
            }
        }
    }
}
