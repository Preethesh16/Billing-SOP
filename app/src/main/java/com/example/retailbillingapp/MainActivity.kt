package com.example.retailbillingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RetailBillingApp()
        }
    }
}

@Composable
fun RetailBillingApp() {
    var totalAmount by remember { mutableStateOf(0) }
    val itemList = listOf("Alternagel - ₹200", "Bepanthen - ₹560", "Item 3 - ₹300") // Example items
    val itemPrices = listOf(200, 560, 300)
    var selectedItem by remember { mutableStateOf(itemList[0]) }
    var quantity by remember { mutableStateOf("") }
    var billItems by remember { mutableStateOf<List<BillItem>>(emptyList()) } // List of items added to the bill
    var showInvoice by remember { mutableStateOf(false) } // State to control invoice visibility

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Item Selection
        Text(text = "Select Items", style = MaterialTheme.typography.headlineSmall)
        Spinner(
            items = itemList,
            selectedItem = selectedItem,
            onItemSelected = { selectedItem = it }
        )

        // Quantity Input
        Text(
            text = "Quantity",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Enter Quantity") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Add to Bill Button
        Button(
            onClick = {
                val qty = quantity.toIntOrNull() ?: 0
                if (qty > 0) {
                    val itemPrice = itemPrices[itemList.indexOf(selectedItem)]
                    val itemTotal = itemPrice * qty
                    totalAmount += itemTotal

                    // Add item to the bill list
                    val billItem = BillItem(
                        name = selectedItem.split(" - ")[0],
                        quantity = qty,
                        price = itemPrice,
                        total = itemTotal
                    )
                    billItems = billItems + billItem
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Add to Bill")
        }

        // Display Selected Items
        Text(
            text = "Selected Items",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            items(billItems) { item ->
                SelectedItemRow(
                    item = item,
                    onDelete = {
                        // Remove item from the bill
                        billItems = billItems.filter { it != item }
                        totalAmount -= item.total
                    },
                    onQuantityChange = { newQty ->
                        // Update item quantity
                        val updatedItem = item.copy(quantity = newQty, total = item.price * newQty)
                        billItems = billItems.map { if (it == item) updatedItem else it }
                        totalAmount = billItems.sumOf { it.total }
                    }
                )
            }
        }

        // Print Invoice Button
        Button(
            onClick = {
                // Show the invoice
                showInvoice = true
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Print Invoice")
        }

        // Display Invoice
        if (showInvoice) {
            InvoiceTemplate(billItems, totalAmount)
        }
    }
}

@Composable
fun SelectedItemRow(
    item: BillItem,
    onDelete: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "${item.name}, Qty: ${item.quantity}, Total: ₹${item.total}", modifier = Modifier.weight(1f))
        Button(onClick = { onQuantityChange(item.quantity - 1) }, enabled = item.quantity > 1) {
            Text("-")
        }
        Button(onClick = { onQuantityChange(item.quantity + 1) }) {
            Text("+")
        }
        Button(onClick = onDelete) {
            Text("Delete")
        }
    }
}

@Composable
fun InvoiceTemplate(billItems: List<BillItem>, totalAmount: Int) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault())
    val currentDate = dateFormat.format(Date())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(text = "RAGA PVT LTD", style = MaterialTheme.typography.headlineSmall)
        Text(text = "S USMAN ROAD, T. NAGAR,", style = MaterialTheme.typography.bodySmall)
        Text(text = "CHENNAI, TAMIL NADU.", style = MaterialTheme.typography.bodySmall)
        Text(text = "PHONE : 044 258636222", style = MaterialTheme.typography.bodySmall)
        Text(text = "GSTIN : 33AAAGP0685F1ZH", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))

        // Invoice Details
        Text(text = "Retail Invoice", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Date : $currentDate", style = MaterialTheme.typography.bodySmall)
        Text(text = "Bill No: SR2", style = MaterialTheme.typography.bodySmall)
        Text(text = "Payment Mode: Cash", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))

        // Item Table
        Text(text = "| Item        | Qty | Amt   |", style = MaterialTheme.typography.bodySmall)
        billItems.forEach { item ->
            Text(text = "| ${item.name} | ${item.quantity} | ₹${item.total} |", style = MaterialTheme.typography.bodySmall)
        }

        // Totals and Taxes
        Text(text = "Sub Total    | ${billItems.size} | ₹$totalAmount |", style = MaterialTheme.typography.bodySmall)
//        Text(text = "(-) Discount |     | ₹26.00 |", style = MaterialTheme.typography.bodySmall)
//        Text(text = "CGST @ 14.00% |     | ₹24.36 |", style = MaterialTheme.typography.bodySmall)
//        Text(text = "SGST @ 14.00% |     | ₹22.35 |", style = MaterialTheme.typography.bodySmall)
//        Text(text = "CGST @ 2.50% |     | ₹14.00 |", style = MaterialTheme.typography.bodySmall)
//        Text(text = "SGST @ 2.50% |     | ₹14.00 |", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))

        // Total Amount
        Text(text = "**TOTAL** |     | ₹$totalAmount |", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        // Footer
        Text(text = "Cash : ₹$totalAmount", style = MaterialTheme.typography.bodySmall)
        Text(text = "Cash tendered: ₹$totalAmount", style = MaterialTheme.typography.bodySmall)
        Text(text = "E & O.E", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun Spinner(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedItem)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

data class BillItem(
    val name: String,
    var quantity: Int,
    val price: Int,
    var total: Int
)