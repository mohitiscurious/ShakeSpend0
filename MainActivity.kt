package com.shakespend.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val dao = AppDatabase.get(this).expenseDao()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFFB8F36B),
                background = Color(0xFF10131A),
                surface = Color(0xFF1A202B)
            )) {
                var expenses by remember { mutableStateOf(emptyList<Expense>()) }
                var showAdd by remember { mutableStateOf(false) }
                var listening by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { dao.observeAll().collect { expenses = it } }
                val total = expenses.filter {
                    val cal = Calendar.getInstance()
                    val start = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    it.createdAt >= start
                }.sumOf { it.amountPaise }

                Scaffold(
                    containerColor = Color(0xFF10131A),
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showAdd = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add expense")
                        }
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                        Text("ShakeSpend", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text("Shake. Record. Understand.", color = Color.LightGray)
                        Spacer(Modifier.height(22.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF253044)),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text("THIS MONTH", color = Color.LightGray)
                                Text("₹%.2f".format(total / 100.0),
                                    style = MaterialTheme.typography.headlineLarge, color = Color(0xFFB8F36B))
                                Text("${expenses.size} recorded expenses", color = Color.LightGray)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Shake detection", color = Color.White)
                                Text(if (listening) "Running in background" else "Currently off",
                                    color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = listening, onCheckedChange = {
                                listening = it
                                val intent = Intent(this@MainActivity, ShakeDetectionService::class.java)
                                if (it) ContextCompat.startForegroundService(this@MainActivity, intent)
                                else stopService(intent)
                            })
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Recent expenses", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        if (expenses.isEmpty()) {
                            Text("No expenses yet. Tap + or enable shake detection.",
                                color = Color.LightGray)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(expenses) { item ->
                                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A202B))) {
                                        Row(Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column {
                                                Text(item.category, color = Color.White)
                                                Text(item.note.ifBlank {
                                                    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                                        .format(Date(item.createdAt))
                                                }, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                                            }
                                            Text("₹%.2f".format(item.amountPaise / 100.0), color = Color(0xFFB8F36B))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (showAdd) {
                    ExpenseDialog(
                        onDismiss = { showAdd = false },
                        onSave = { amount, category, note ->
                            lifecycleScope.launch {
                                dao.insert(Expense(amountPaise = (amount * 100).toLong(),
                                    category = category, note = note))
                            }
                            showAdd = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseDialog(onDismiss: () -> Unit, onSave: (Double, String, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var note by remember { mutableStateOf("") }
    val categories = listOf("Food", "Travel", "Shopping", "Education", "Bills", "Other")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount (₹)") })
                Text("Category")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.take(3).forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.drop(3).forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                    }
                }
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = amount.toDoubleOrNull()
                if (value != null && value > 0) onSave(value, category, note)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
