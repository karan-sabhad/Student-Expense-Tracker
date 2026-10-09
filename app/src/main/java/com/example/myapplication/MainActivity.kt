
package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Purple = Color(0xFF6750A4)
private val LightPurple = Color(0xFFEADDFF)
private val PageBackground = Color(0xFFF8F5FF)
private val DarkText = Color(0xFF241A35)
private val Green = Color(0xFF21865B)

data class Expense(
    val id: Long,
    val title: String,
    val amount: Double,
    val category: String,
    val timestamp: Long
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.rgb(248, 245, 255)
        window.navigationBarColor = android.graphics.Color.rgb(248, 245, 255)
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Purple,
                    secondary = LightPurple,
                    background = PageBackground,
                    surface = Color.White
                )
            ) {
                ExpenseTracker()
            }
        }
    }
}

private fun money(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(
        Locale("en", "IN")
    )
    formatter.maximumFractionDigits = 2
    return formatter.format(amount)
}

private fun dateText(timestamp: Long): String {
    return SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    ).format(Date(timestamp))
}

private fun loadExpenses(
    prefs: android.content.SharedPreferences
): List<Expense> {
    return try {
        val array = JSONArray(prefs.getString("expenses", "[]"))
        val result = mutableListOf<Expense>()

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)

            result.add(
                Expense(
                    id = item.getLong("id"),
                    title = item.getString("title"),
                    amount = item.getDouble("amount"),
                    category = item.getString("category"),
                    timestamp = item.getLong("timestamp")
                )
            )
        }

        result.sortedByDescending { it.timestamp }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveExpenses(
    prefs: android.content.SharedPreferences,
    expenses: List<Expense>
) {
    val array = JSONArray()

    expenses.forEach { expense ->
        array.put(
            JSONObject().apply {
                put("id", expense.id)
                put("title", expense.title)
                put("amount", expense.amount)
                put("category", expense.category)
                put("timestamp", expense.timestamp)
            }
        )
    }

    prefs.edit().putString("expenses", array.toString()).apply()
}

@Composable
fun ExpenseTracker() {
    val context = LocalContext.current

    val prefs = remember {
        context.getSharedPreferences(
            "student_expense_tracker",
            android.content.Context.MODE_PRIVATE
        )
    }

    val expenses = remember {
        mutableStateListOf<Expense>().apply {
            addAll(loadExpenses(prefs))
        }
    }

    var monthlyBudget by remember {
        mutableDoubleStateOf(
            prefs.getFloat("monthly_budget", 10000f).toDouble()
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    val totalExpenses = expenses.sumOf { it.amount }
    val remaining = monthlyBudget - totalExpenses
    val spentProgress = if (monthlyBudget > 0) {
        (totalExpenses / monthlyBudget).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    Scaffold(
        containerColor = PageBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Purple,
                contentColor = Color.White
            ) {
                Text("+", fontSize = 32.sp)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 20.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        "Student Expense Tracker",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        "Manage your daily spending",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Monthly Budget",
                                color = Purple,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )

                            TextButton(
                                onClick = { showBudgetDialog = true }
                            ) {
                                Text("Edit Budget")
                            }
                        }

                        Text(
                            money(monthlyBudget),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )

                        Spacer(Modifier.height(16.dp))

                        HorizontalDivider(color = LightPurple)

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Total Expenses",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    money(totalExpenses),
                                    fontWeight = FontWeight.Bold,
                                    color = Purple
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    "Remaining",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    money(remaining),
                                    fontWeight = FontWeight.Bold,
                                    color = if (remaining < 0) {
                                        Color.Red
                                    } else {
                                        Green
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        LinearProgressIndicator(
                            progress = { spentProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = if (totalExpenses > monthlyBudget) {
                                Color.Red
                            } else {
                                Purple
                            },
                            trackColor = LightPurple
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            "${(spentProgress * 100).toInt()}% of budget used",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Expenses",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )

                    Text(
                        "${expenses.size} records",
                        color = Purple,
                        fontSize = 13.sp
                    )
                }
            }

            if (expenses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("₹", fontSize = 42.sp, color = Purple)

                            Spacer(Modifier.height(8.dp))

                            Text(
                                "No expenses yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                "Tap + to add your first expense.",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(
                    items = expenses.toList(),
                    key = { it.id }
                ) { expense ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        LightPurple,
                                        RoundedCornerShape(15.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    categoryEmoji(expense.category),
                                    fontSize = 23.sp
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    expense.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = DarkText
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    "${expense.category} • ${dateText(expense.timestamp)}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    money(expense.amount),
                                    fontWeight = FontWeight.Bold,
                                    color = Purple,
                                    fontSize = 14.sp
                                )

                                TextButton(
                                    onClick = {
                                        expenseToDelete = expense
                                    },
                                    contentPadding = PaddingValues(
                                        horizontal = 4.dp,
                                        vertical = 0.dp
                                    )
                                ) {
                                    Text(
                                        "Delete",
                                        color = Color.Red,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))

                Text(
                    "Your money, your control.",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, amount, category ->
                val expense = Expense(
                    id = System.currentTimeMillis(),
                    title = title,
                    amount = amount,
                    category = category,
                    timestamp = System.currentTimeMillis()
                )

                expenses.add(0, expense)
                saveExpenses(prefs, expenses.toList())
                showAddDialog = false
            }
        )
    }

    if (showBudgetDialog) {
        BudgetDialog(
            currentBudget = monthlyBudget,
            onDismiss = { showBudgetDialog = false },
            onSave = { newBudget ->
                monthlyBudget = newBudget
                prefs.edit()
                    .putFloat("monthly_budget", newBudget.toFloat())
                    .apply()
                showBudgetDialog = false
            }
        )
    }

    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete expense?") },
            text = {
                Text("Delete \"${expense.title}\" from your expense history?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        expenses.removeAll { it.id == expense.id }
                        saveExpenses(prefs, expenses.toList())
                        expenseToDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { expenseToDelete = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun categoryEmoji(category: String): String {
    return when (category) {
        "Food" -> "🍔"
        "Travel" -> "🚌"
        "Education" -> "📚"
        "Shopping" -> "🛍️"
        "Bills" -> "🧾"
        else -> "💰"
    }
}

@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val categories = listOf(
        "Food",
        "Travel",
        "Education",
        "Shopping",
        "Bills",
        "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add Expense",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense title") },
                    placeholder = { Text("e.g. Lunch") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char ->
                            char.isDigit() || char == '.'
                        }
                        error = ""
                    },
                    label = { Text("Amount (₹)") },
                    placeholder = { Text("e.g. 150") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Box {
                    OutlinedButton(
                        onClick = {
                            categoryMenuExpanded = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Category: $category",
                            modifier = Modifier.weight(1f)
                        )
                        Text("▼")
                    }

                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = {
                            categoryMenuExpanded = false
                        }
                    ) {
                        categories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    category = item
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (error.isNotBlank()) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()

                    when {
                        title.isBlank() -> {
                            error = "Please enter an expense title."
                        }

                        amount == null || !amount.isFinite() || amount <= 0 -> {
                            error = "Enter a valid amount greater than zero."
                        }

                        else -> {
                            onSave(
                                title.trim(),
                                amount,
                                category
                            )
                        }
                    }
                }
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BudgetDialog(
    currentBudget: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var budgetText by remember {
        mutableStateOf(
            if (currentBudget % 1.0 == 0.0) {
                currentBudget.toLong().toString()
            } else {
                currentBudget.toString()
            }
        )
    }

    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Monthly Budget") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = {
                        budgetText = it.filter { char ->
                            char.isDigit() || char == '.'
                        }
                        error = ""
                    },
                    label = { Text("Budget amount (₹)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true
                )

                if (error.isNotBlank()) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val budget = budgetText.toDoubleOrNull()

                    if (budget == null ||
                        !budget.isFinite() ||
                        budget <= 0
                    ) {
                        error = "Enter a valid budget greater than zero."
                    } else {
                        onSave(budget)
                    }
                }
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}