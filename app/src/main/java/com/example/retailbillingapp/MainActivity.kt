package com.example.retailbillingapp

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
//import com.google.zxing.BarcodeFormat
//import com.journeyapps.barcodescanner.BarcodeEncoder

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
    var totalAmount by remember { mutableIntStateOf(0) }
    val itemList = listOf("Item 1 - \$10", "Item 2 - \$20", "Item 3 - \$30")
    val itemPrices = listOf(10, 20, 30)
    var selectedItem by remember { mutableStateOf(itemList[0]) }
    var quantity by remember { mutableStateOf("") }
    var showQRCode by remember { mutableStateOf(false) }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var billItems by remember { mutableStateOf<List<String>>(emptyList()) } // List of items added to the bill

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
                    val billItem = "${selectedItem.split(" - ")[0]}, Quantity: $qty, Total: \$$itemTotal"
                    billItems = billItems + billItem
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Add to Bill")
        }

        // Bill Summary
        Text(
            text = "Bill Summary",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            items(billItems) { item ->
                Text(text = item, modifier = Modifier.padding(4.dp))
            }
        }
        Text(text = "Total Amount: \$$totalAmount", style = MaterialTheme.typography.headlineSmall)

        // Generate Invoice Button
        Button(
            onClick = {
                if (totalAmount > 0) {
                    val upiId = "preetheshcarvalho57-1@oksbi" // Replace with your UPI ID
                    val paymentDetails = "upi://pay?pa=$upiId&pn=Retailer&am=$totalAmount&cu=INR"
//                    val barcodeEncoder = BarcodeEncoder()
//                    qrCodeBitmap = barcodeEncoder.encodeBitmap(paymentDetails, BarcodeFormat.QR_CODE, 400, 400)
//                    showQRCode = true
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Generate Invoice")
        }

        // Invoice with QR Code
        if (showQRCode && qrCodeBitmap != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Display Bill Items
                Text(
                    text = "Invoice Details",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    items(billItems) { item ->
                        Text(text = item, modifier = Modifier.padding(4.dp))
                    }
                }
                Text(
                    text = "Total Amount: \$$totalAmount",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Display QR Code
                Text(
                    text = "Scan to Pay",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Image(
                    bitmap = qrCodeBitmap!!.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(200.dp)
                        .padding(top = 16.dp)
                )
            }
        }
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