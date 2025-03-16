package com.example.retailbillingapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val REQUEST_LOCATION_PERMISSIONS = 100

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // Register for activity result
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Bluetooth is enabled, proceed with device selection
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            pairedDevices?.forEach { device ->
                if (device.name.contains("Printer")) { // Replace "Printer" with your printer's name
                    connectToPrinter(device)
                }
            }
        } else {
            Toast.makeText(this, "Bluetooth is required for printing", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RetailBillingApp(this) // Pass MainActivity instance to RetailBillingApp
        }

        // Use BluetoothManager to get the Bluetooth adapter
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        checkPermissions()
        enableBluetooth()
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // All permissions already granted
            Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableBluetooth() {
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
        } else {
            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                // Use the Activity Result API to start the activity
                enableBluetoothLauncher.launch(enableBtIntent)
            }
        }
    }

    private fun connectToPrinter(device: BluetoothDevice) {
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SerialPortService ID
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            Toast.makeText(this, "Connected to printer", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Failed to connect to printer", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    fun printBill(billItems: List<BillItem>, totalAmount: Int, tokenNumber: Int) {
        if (outputStream == null) {
            Toast.makeText(this, "Printer not connected", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Initialize printer
            outputStream?.write(byteArrayOf(0x1B, 0x40)) // ESC @ (Initialize printer)

            // Add header
            outputStream?.write("RAGA PVT LTD\n".toByteArray(Charsets.US_ASCII))
            outputStream?.write("S USMAN ROAD, T. NAGAR,\n".toByteArray(Charsets.US_ASCII))
            outputStream?.write("CHENNAI, TAMIL NADU.\n".toByteArray(Charsets.US_ASCII))
            outputStream?.write("PHONE : 044 258636222\n".toByteArray(Charsets.US_ASCII))
            outputStream?.write("GSTIN : 33AAAGP0685F1ZH\n".toByteArray(Charsets.US_ASCII))
            outputStream?.write("\n".toByteArray(Charsets.US_ASCII))

            // Add invoice details
            outputStream?.write("Retail Invoice\n".toByteArray(Charsets.US_ASCII))
            outputStream?.write("Token Number: $tokenNumber\n".toByteArray(Charsets.US_ASCII))
            outputStream?.write("\n".toByteArray(Charsets.US_ASCII))

            // Add item table
            outputStream?.write("Item        | Qty | Amt\n".toByteArray(Charsets.US_ASCII))
            outputStream?.write("------------------------\n".toByteArray(Charsets.US_ASCII))
            billItems.forEach { item ->
                outputStream?.write("${item.name} | ${item.quantity} | ₹${item.total}\n".toByteArray(Charsets.US_ASCII))
            }
            outputStream?.write("\n".toByteArray(Charsets.US_ASCII))

            // Add total amount
            outputStream?.write("Total: ₹$totalAmount\n".toByteArray(Charsets.US_ASCII))
            outputStream?.write("\n\n\n".toByteArray(Charsets.US_ASCII)) // Add space at the bottom for cutting

            // Cut paper (if supported by the printer)
            outputStream?.write(byteArrayOf(0x1D, 0x56, 0x41, 0x10)) // GS V 41 (Cut paper)

            // Flush the output stream
            outputStream?.flush()

            Toast.makeText(this, "Bill printed successfully", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Failed to print bill", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // All permissions granted
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            // Some permissions denied
            Toast.makeText(this, "Permissions are required for Bluetooth scanning", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun RetailBillingApp(mainActivity: MainActivity) {
    var totalAmount by remember { mutableStateOf(0) }
    val itemList = listOf("Alternagel - ₹200", "Bepanthen - ₹560", "Item 3 - ₹300") // Example items
    val itemPrices = listOf(200, 560, 300)
    var selectedItem by remember { mutableStateOf(itemList[0]) }
    var quantity by remember { mutableStateOf("") }
    var billItems by remember { mutableStateOf<List<BillItem>>(emptyList()) }
    var showInvoice by remember { mutableStateOf(false) }
    var tokenNumber by remember { mutableStateOf(1) }
    var isEditingToken by remember { mutableStateOf(false) }
    var tempTokenNumber by remember { mutableStateOf(tokenNumber.toString()) }
    var showDevicePicker by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // Scroll state for the entire page
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState) // Make the entire page scrollable
            .clickable { focusManager.clearFocus() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Token Number Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Token Number: $tokenNumber",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    isEditingToken = true
                    tempTokenNumber = tokenNumber.toString()
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Edit Token")
            }
        }

        // Token Number Edit Field (Visible only when editing)
        if (isEditingToken) {
            OutlinedTextField(
                value = tempTokenNumber,
                onValueChange = { tempTokenNumber = it },
                label = { Text("Enter Token Number") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        isEditingToken = false
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val newToken = tempTokenNumber.toIntOrNull() ?: tokenNumber
                        tokenNumber = newToken
                        isEditingToken = false
                    }
                ) {
                    Text("Save")
                }
            }
        }

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
        val focusRequester = remember { FocusRequester() }
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Enter Quantity") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )

        // Buttons Row: Add to Bill, Print Invoice, Select Printer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
                    focusManager.clearFocus()
                },
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Text("Add to Bill")
            }

            // Print Invoice Button
            Button(
                onClick = {
                    showInvoice = true
                    focusManager.clearFocus()
                    mainActivity.printBill(billItems, totalAmount, tokenNumber)
                },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("Print Invoice")
            }

            // Select Printer Button
            Button(
                onClick = {
                    showDevicePicker = true
                },
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) {
                Text("Select Printer")
            }
        }

        // Display Selected Items (Scrollable)
        Text(
            text = "Selected Items",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(8.dp)
        ) {
            items(billItems) { item ->
                SelectedItemRow(
                    item = item,
                    onDelete = {
                        billItems = billItems.filter { it != item }
                        totalAmount -= item.total
                    },
                    onQuantityChange = { newQty ->
                        val updatedItem = item.copy(quantity = newQty, total = item.price * newQty)
                        billItems = billItems.map { if (it == item) updatedItem else it }
                        totalAmount = billItems.sumOf { it.total }
                    }
                )
            }
        }

        // Display Invoices (Scrollable)
        if (showInvoice) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SellerBillTemplate(billItems, tokenNumber)
                Spacer(modifier = Modifier.height(16.dp))
                InvoiceTemplate(billItems, totalAmount, tokenNumber)
                LaunchedEffect(showInvoice) {
                    tokenNumber++
                }
            }
        }
    }
}
@Composable
fun SellerBillTemplate(billItems: List<BillItem>, tokenNumber: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Seller Bill", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Token Number: $tokenNumber", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "| Item        | Qty |", style = MaterialTheme.typography.bodySmall)
        billItems.forEach { item ->
            Text(text = "| ${item.name} | ${item.quantity} |", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun InvoiceTemplate(billItems: List<BillItem>, totalAmount: Int, tokenNumber: Int) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault())
    val currentDate = dateFormat.format(Date())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "RAGA PVT LTD", style = MaterialTheme.typography.headlineSmall)
        Text(text = "S USMAN ROAD, T. NAGAR,", style = MaterialTheme.typography.bodySmall)
        Text(text = "CHENNAI, TAMIL NADU.", style = MaterialTheme.typography.bodySmall)
        Text(text = "PHONE : 044 258636222", style = MaterialTheme.typography.bodySmall)
        Text(text = "GSTIN : 33AAAGP0685F1ZH", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Retail Invoice", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Date : $currentDate", style = MaterialTheme.typography.bodySmall)
        Text(text = "Bill No: SR2", style = MaterialTheme.typography.bodySmall)
        Text(text = "Payment Mode: Cash", style = MaterialTheme.typography.bodySmall)
        Text(text = "Token Number: $tokenNumber", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "| Item        | Qty | Amt   |", style = MaterialTheme.typography.bodySmall)
        billItems.forEach { item ->
            Text(text = "| ${item.name} | ${item.quantity} | ₹${item.total} |", style = MaterialTheme.typography.bodySmall)
        }
        Text(text = "Sub Total    | ${billItems.size} | ₹$totalAmount |", style = MaterialTheme.typography.bodySmall)
        Text(text = "(-) Discount |     | ₹26.00 |", style = MaterialTheme.typography.bodySmall)
        Text(text = "CGST @ 14.00% |     | ₹24.36 |", style = MaterialTheme.typography.bodySmall)
        Text(text = "SGST @ 14.00% |     | ₹22.35 |", style = MaterialTheme.typography.bodySmall)
        Text(text = "CGST @ 2.50% |     | ₹14.00 |", style = MaterialTheme.typography.bodySmall)
        Text(text = "SGST @ 2.50% |     | ₹14.00 |", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "**TOTAL** |     | ₹$totalAmount |", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Cash : ₹$totalAmount", style = MaterialTheme.typography.bodySmall)
        Text(text = "Cash tendered: ₹$totalAmount", style = MaterialTheme.typography.bodySmall)
        Text(text = "E & O.E", style = MaterialTheme.typography.bodySmall)
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