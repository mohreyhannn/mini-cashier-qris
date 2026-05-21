package com.example.minicashier

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.minicashier.ui.theme.MiniCashierTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.background


sealed class Screen(val route: String) {
    object Home : Screen("home")
    object History : Screen("history")
    object Admin : Screen("admin")
}

data class AppPopup(
    val title: String,
    val message: String,
    val icon: String = "✅"
)

@Composable
fun AppPopupDialog(
    popup: AppPopup?,
    onDismiss: () -> Unit
) {
    popup?.let {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = null,
            text = {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(
                        animationSpec = tween(350),
                        initialScale = 0.75f
                    ) + fadeIn(animationSpec = tween(350)),
                    exit = scaleOut(
                        animationSpec = tween(250),
                        targetScale = 0.75f
                    ) + fadeOut(animationSpec = tween(250))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = it.icon,
                            fontSize = 58.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = it.title,
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = it.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text("OK")
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

data class CartItem(
    val product: Product,
    var quantity: Int
)

class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            installSplashScreen()
            super.onCreate(savedInstanceState)
            setContent {
                MiniCashierTheme {
                    AppEntry()
                }
            }
        }
    }

@Composable
fun ProductScreen(
    user: UserData
) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var categoriesData by remember { mutableStateOf<List<Category>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var searchQuery by remember { mutableStateOf("") }

    val cartItems = remember { mutableStateListOf<CartItem>() }
    val scope = rememberCoroutineScope()

    var checkoutMessage by remember { mutableStateOf<String?>(null) }
    var currentTransactionId by remember { mutableStateOf<Int?>(null) }
    var currentInvoiceCode by remember { mutableStateOf<String?>(null) }
    var currentTotalPrice by remember { mutableStateOf<Int?>(null) }
    var paymentMessage by remember { mutableStateOf<String?>(null) }
    var receiptInvoiceCode by remember { mutableStateOf<String?>(null) }
    var receiptTotalPrice by remember { mutableStateOf<Int?>(null) }
    var appPopup by remember {
        mutableStateOf<AppPopup?>(null)
    }

    var dashboard by remember { mutableStateOf<DashboardResponse?>(null) }

    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var adminMessage by remember { mutableStateOf<String?>(null) }
    var editingProductId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        try {
            products = RetrofitClient.api.getProducts()
            categoriesData = RetrofitClient.api.getCategories()
            dashboard = RetrofitClient.api.getDashboard()
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    val totalItems = cartItems.sumOf { it.quantity }
    val totalPrice = cartItems.sumOf { it.product.price * it.quantity }

    val categories = listOf("Semua") + products.map { it.category }.distinct()

    val filteredProducts = products.filter { product ->
        val matchCategory =
            selectedCategory == "Semua" || product.category == selectedCategory

        val matchSearch =
            product.name.contains(searchQuery, ignoreCase = true)

        matchCategory && matchSearch
    }

    fun checkout() {
        scope.launch {
            try {
                val transactionItems = cartItems.map {
                    TransactionItemRequest(
                        product_id = it.product.id,
                        quantity = it.quantity
                    )
                }

                val response = RetrofitClient.api.createTransaction(
                    TransactionRequest(items = transactionItems)
                )

                checkoutMessage = response.message
                currentTransactionId = response.transaction_id
                currentInvoiceCode = response.invoice_code
                currentTotalPrice = response.total_price
                paymentMessage = null
                cartItems.clear()

            } catch (e: Exception) {
                checkoutMessage = "Checkout gagal: ${e.message}"
            }
        }
    }

    @Composable
    fun MenuSection() {
        Text(
            text = "Mini Cashier QRIS",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Kasir makanan cepat dan sederhana",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Cari menu...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Daftar Menu", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Memuat menu...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            error != null -> Text("Error: $error")
            products.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🍽️",
                            fontSize = 42.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Belum ada menu",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Tambahkan produk baru terlebih dahulu",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            filteredProducts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔍",
                            fontSize = 42.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Produk tidak ditemukan",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Coba kategori atau pencarian lain",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(220.dp),
                    modifier = Modifier.height(700.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(filteredProducts) { product ->

                        ProductCard(
                            product = product,
                            isAdmin = false,

                            onAddToCart = {

                                val existingItem = cartItems.find {
                                    it.product.id == product.id
                                }

                                if (existingItem != null) {

                                    val index = cartItems.indexOf(existingItem)

                                    cartItems[index] =
                                        existingItem.copy(
                                            quantity = existingItem.quantity + 1
                                        )

                                } else {

                                    cartItems.add(
                                        CartItem(
                                            product = product,
                                            quantity = 1
                                        )
                                    )
                                }
                            },

                            onEditProduct = {

                                productName = product.name
                                productPrice = product.price.toString()

                                val category = categoriesData.find {
                                    it.name == product.category
                                }

                                selectedCategoryId = category?.id
                                editingProductId = product.id

                                adminMessage =
                                    "Mode edit produk: ${product.name}"
                            },

                            onDeleteProduct = {

                                scope.launch {

                                    try {

                                        val response =
                                            RetrofitClient.api.deleteProduct(product.id)

                                        adminMessage = response.message

                                        products =
                                            RetrofitClient.api.getProducts()

                                        cartItems.removeAll {
                                            it.product.id == product.id
                                        }

                                    } catch (e: Exception) {

                                        adminMessage =
                                            "Gagal hapus produk: ${e.message}"
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun CartSection() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Keranjang",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (cartItems.isEmpty()) {

                    Text("Belum ada item")

                } else {

                    cartItems.forEach { item ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),

                            shape = RoundedCornerShape(16.dp)
                        ) {

                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {

                                Text(
                                    text = item.product.name,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = formatRupiah(item.product.price),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.SpaceBetween,

                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Row(
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {

                                        OutlinedButton(
                                            onClick = {

                                                if (item.quantity > 1) {

                                                    val index =
                                                        cartItems.indexOf(item)

                                                    cartItems[index] =
                                                        item.copy(
                                                            quantity =
                                                                item.quantity - 1
                                                        )

                                                } else {

                                                    cartItems.remove(item)
                                                }
                                            }
                                        ) {
                                            Text("-")
                                        }

                                        Spacer(
                                            modifier = Modifier.width(12.dp)
                                        )

                                        Text(
                                            text = item.quantity.toString(),
                                            style =
                                                MaterialTheme.typography.titleMedium
                                        )

                                        Spacer(
                                            modifier = Modifier.width(12.dp)
                                        )

                                        OutlinedButton(
                                            onClick = {

                                                val index =
                                                    cartItems.indexOf(item)

                                                cartItems[index] =
                                                    item.copy(
                                                        quantity =
                                                            item.quantity + 1
                                                    )
                                            }
                                        ) {
                                            Text("+")
                                        }
                                    }

                                    Text(
                                        text = formatRupiah(
                                            item.product.price * item.quantity
                                        ),

                                        style =
                                            MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    TextButton(
                                        onClick = {
                                            cartItems.remove(item)
                                        }
                                    ) {
                                        Text("Hapus")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Total item: $totalItems")
                Text(
                    "Total bayar: ${
                        formatRupiah(totalPrice)
                    }"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { checkout() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = cartItems.isNotEmpty()
                ) {
                    Text("Checkout")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        cartItems.clear()
                        checkoutMessage = null
                        currentTransactionId = null
                        currentInvoiceCode = null
                        currentTotalPrice = null
                        paymentMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = cartItems.isNotEmpty() || currentTransactionId != null
                ) {
                    Text("Batalkan Pesanan")
                }

                checkoutMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (currentTransactionId != null) {
                    PaymentCard(
                        invoiceCode = currentInvoiceCode,
                        totalPrice = currentTotalPrice,
                        onPayClick = {
                            scope.launch {
                                try {
                                    val response = RetrofitClient.api.payTransaction(
                                        currentTransactionId!!
                                    )

                                    paymentMessage = response.message
                                    appPopup = AppPopup(
                                        title = "Pembayaran Berhasil",
                                        message = "Struk pembayaran berhasil dibuat",
                                        icon = "💰"
                                    )
                                    receiptInvoiceCode = currentInvoiceCode
                                    receiptTotalPrice = currentTotalPrice
                                    currentTransactionId = null
                                    dashboard = RetrofitClient.api.getDashboard()

                                } catch (e: Exception) {
                                    paymentMessage =
                                        "Pembayaran gagal: ${e.message}"
                                    appPopup = AppPopup(
                                        title = "Pembayaran Gagal",
                                        message = "Silakan coba lagi",
                                        icon = "❌"
                                    )
                                }
                            }
                        }
                    )
                }

                paymentMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (receiptInvoiceCode != null) {
                    ReceiptCard(
                        invoiceCode = receiptInvoiceCode,
                        totalPrice = receiptTotalPrice
                    )
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val isTablet = maxWidth > 700.dp

        if (isTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .verticalScroll(rememberScrollState())
                ) {
                    MenuSection()
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    CartSection()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                MenuSection()

                Spacer(modifier = Modifier.height(16.dp))

                CartSection()

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    AppPopupDialog(
        popup = appPopup,
        onDismiss = {
            appPopup = null
        }
    )
}

@Composable
fun DashboardCard(dashboard: DashboardResponse) {

    val maxValue = maxOf(
        dashboard.total_income,
        dashboard.total_transactions,
        dashboard.total_items_sold,
        1
    )

    fun barWidth(value: Int): Float {
        return value.toFloat() / maxValue.toFloat()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = "Dashboard Kasir",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("💰 Total Income")
            Text(
                text = formatRupiah(dashboard.total_income),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            LinearProgressIndicator(
                progress = { barWidth(dashboard.total_income) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("🧾 Total Transactions")
            Text(
                text = dashboard.total_transactions.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            LinearProgressIndicator(
                progress = { barWidth(dashboard.total_transactions) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("📦 Total Items Sold")
            Text(
                text = dashboard.total_items_sold.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            LinearProgressIndicator(
                progress = { barWidth(dashboard.total_items_sold) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Divider()

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Last Transaction",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                "Invoice: ${dashboard.last_transaction.invoice_code}"
            )

            Text(
                "Total: ${
                    formatRupiah(
                        dashboard.last_transaction.total_price
                    )
                }"
            )
        }
    }
}

@Composable
fun AdminProductForm(
    productName: String,
    productPrice: String,
    categoriesData: List<Category>,
    selectedCategoryId: Int?,
    editingProductId: Int?,
    adminMessage: String?,
    onProductNameChange: (String) -> Unit,
    onProductPriceChange: (String) -> Unit,
    onCategorySelected: (Int) -> Unit,
    onSubmit: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp    )) {
            Text(
                text = if (editingProductId == null)
                    "Admin Tambah Produk"
                else
                    "Admin Edit Produk",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = productName,
                onValueChange = onProductNameChange,
                label = { Text("Nama Produk") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = productPrice,
                onValueChange = onProductPriceChange,
                label = { Text("Harga") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Pilih Kategori")

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categoriesData) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (editingProductId == null)
                        "Tambah Produk"
                    else
                        "Update Produk"
                )
            }

            if (editingProductId != null) {
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onCancelEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Batal Edit")
                }
            }

            adminMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun AppEntry() {

    val context = LocalContext.current
    val sessionManager = remember {
        SessionManager(context)
    }

    var loggedInUser by remember {
        mutableStateOf<UserData?>(null)
    }

    var isCheckingSession by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        loggedInUser = sessionManager.getUser()
        isCheckingSession = false
    }

    if (isCheckingSession) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Memuat session...")
        }

    } else if (loggedInUser == null) {

        LoginScreen(
            onLoginSuccess = { user ->
                loggedInUser = user
            }
        )

    } else {

        MainNavigation(
            user = loggedInUser!!,
            onLogout = {
                loggedInUser = null
            }
        )
    }
}

@Composable
fun MainNavigation(
    user: UserData,
    onLogout: () -> Unit
) {

    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = remember {
        SessionManager(context)
    }
    val scope = rememberCoroutineScope()

    val startRoute =
        if (user.role == "ADMIN") {
            "admin"
        } else {
            "home"
        }

    Scaffold(
        bottomBar = {
            NavigationBar {

                if (user.role != "ADMIN") {
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            navController.navigate("home")
                        },
                        label = {
                            Text("Home")
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home"
                            )
                        }
                    )
                }

                NavigationBarItem(
                    selected = false,
                    onClick = {
                        navController.navigate("history")
                    },
                    label = {
                        Text("History")
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History"
                        )
                    }
                )

                if (user.role == "ADMIN") {
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            navController.navigate("admin")
                        },
                        label = {
                            Text("Admin")
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin"
                            )
                        }
                    )
                }

                NavigationBarItem(
                    selected = false,
                    onClick = {
                        scope.launch {
                            sessionManager.logout()
                            onLogout()
                        }
                    },
                    label = {
                        Text("Logout")
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Logout"
                        )
                    }
                )
            }
        }
    ) { paddingValues ->

        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                ProductScreen(user = user)
            }

            composable("history") {
                TransactionHistoryScreen(user = user)
            }

            composable("admin") {
                AdminScreen()
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: (UserData) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var appPopup by remember {
        mutableStateOf<AppPopup?>(null)
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sessionManager = remember {
        SessionManager(context)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Mini Cashier Login",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val response = RetrofitClient.api.login(
                                LoginRequest(
                                    username = username,
                                    password = password
                                )
                            )

                            sessionManager.saveLogin(
                                username = response.user.username,
                                role = response.user.role
                            )

                            appPopup = AppPopup(
                                title = "Login Berhasil",
                                message = "Selamat datang ${response.user.username}",
                                icon = "✅"
                            )

                            kotlinx.coroutines.delay(800)
                            onLoginSuccess(response.user)

                        } catch (e: Exception) {
                            errorMessage = "Login gagal: ${e.message}"

                            appPopup = AppPopup(
                                title = "Login Gagal",
                                message = "Cek username/password",
                                icon = "❌"
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        AppPopupDialog(
            popup = appPopup,
            onDismiss = {
                appPopup = null
            }
        )
    }
}
fun getProductImageUrl(productName: String): String {
    val name = productName.lowercase()

    return when {
        name.contains("nasi goreng") ->
            "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=600"

        name.contains("indomie goreng") ->
            "https://images.unsplash.com/photo-1603033172872-c2525115c7b9?w=600"

        name.contains("indomie+telur") ||
                name.contains("indomie telur") ->
            "https://images.unsplash.com/photo-1626804475297-41608ea09aeb?w=600"

        name.contains("es teh") ->
            "https://images.unsplash.com/photo-1499638673689-79a0b5115d87?w=600"

        name.contains("teh hangat") ->
            "https://images.unsplash.com/photo-1515823064-d6e0c04616a7?w=600"

        name.contains("roti bakar") ->
            "https://images.unsplash.com/photo-1484723091739-30a097e8f929?w=600"

        else ->
            "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600"
    }
}

@Composable
fun ProductCard(
    product: Product,
    isAdmin: Boolean,
    onAddToCart: () -> Unit,
    onEditProduct: () -> Unit,
    onDeleteProduct: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            AsyncImage(
                model = getProductImageUrl(product.name),
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(product.name, style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatRupiah(product.price),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAddToCart,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Tambah")
                    }

                    if (isAdmin) {
                        Button(onClick = onEditProduct) {
                            Text("Edit")
                        }

                        Button(onClick = onDeleteProduct) {
                            Text("Hapus")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionHistoryScreen(
    user: UserData
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        HistoryScreen(user = user)
    }
}

@Composable
fun AdminScreen() {

    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var categoriesData by remember { mutableStateOf<List<Category>>(emptyList()) }

    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var adminMessage by remember { mutableStateOf<String?>(null) }
    var editingProductId by remember { mutableStateOf<Int?>(null) }
    var deletingProduct by remember {
        mutableStateOf<Product?>(null)
    }
    var dashboard by remember { mutableStateOf<DashboardResponse?>(null) }

    val scope = rememberCoroutineScope()

    suspend fun reloadProducts() {
        products = RetrofitClient.api.getProducts()
        dashboard = RetrofitClient.api.getDashboard()
    }

    fun saveProduct() {
        scope.launch {
            try {
                if (
                    productName.isBlank() ||
                    productPrice.isBlank() ||
                    selectedCategoryId == null
                ) {
                    adminMessage = "Semua field wajib diisi"
                    return@launch
                }

                if (editingProductId == null) {
                    val response = RetrofitClient.api.createProduct(
                        CreateProductRequest(
                            category_id = selectedCategoryId!!,
                            name = productName,
                            price = productPrice.toInt()
                        )
                    )

                    adminMessage = response.message
                } else {
                    val response = RetrofitClient.api.updateProduct(
                        editingProductId!!,
                        CreateProductRequest(
                            category_id = selectedCategoryId!!,
                            name = productName,
                            price = productPrice.toInt()
                        )
                    )

                    adminMessage = response.message
                }

                productName = ""
                productPrice = ""
                selectedCategoryId = null
                editingProductId = null

                reloadProducts()

            } catch (e: Exception) {
                adminMessage = "Gagal simpan produk: ${e.message}"
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            products = RetrofitClient.api.getProducts()
            categoriesData = RetrofitClient.api.getCategories()
            dashboard = RetrofitClient.api.getDashboard()
        } catch (e: Exception) {
            adminMessage = "Gagal load data: ${e.message}"
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val isTablet = maxWidth > 700.dp

        if (isTablet) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.4f)
                        .verticalScroll(rememberScrollState())
                ) {
                    AdminManagementContent(
                        products = products,
                        categoriesData = categoriesData,
                        productName = productName,
                        productPrice = productPrice,
                        selectedCategoryId = selectedCategoryId,
                        editingProductId = editingProductId,
                        adminMessage = adminMessage,
                        onProductNameChange = { productName = it },
                        onProductPriceChange = { productPrice = it },
                        onCategorySelected = { selectedCategoryId = it },
                        onSubmit = { saveProduct() },
                        onEditProduct = { product ->
                            productName = product.name
                            productPrice = product.price.toString()

                            val category = categoriesData.find {
                                it.name == product.category
                            }

                            selectedCategoryId = category?.id
                            editingProductId = product.id
                            adminMessage = "Mode edit: ${product.name}"
                        },
                        onDeleteProduct = { product ->
                            deletingProduct = product
                        }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                AdminManagementContent(
                    products = products,
                    categoriesData = categoriesData,
                    productName = productName,
                    productPrice = productPrice,
                    selectedCategoryId = selectedCategoryId,
                    editingProductId = editingProductId,
                    adminMessage = adminMessage,
                    onProductNameChange = { productName = it },
                    onProductPriceChange = { productPrice = it },
                    onCategorySelected = { selectedCategoryId = it },
                    onSubmit = { saveProduct() },
                    onEditProduct = { product ->
                        productName = product.name
                        productPrice = product.price.toString()

                        val category = categoriesData.find {
                            it.name == product.category
                        }

                        selectedCategoryId = category?.id
                        editingProductId = product.id
                        adminMessage = "Mode edit: ${product.name}"
                    },
                    onDeleteProduct = { product ->
                        deletingProduct = product
                    }
                )
            }
        }
    }

    deletingProduct?.let { product ->

        AlertDialog(
            onDismissRequest = {
                deletingProduct = null
            },

            title = {
                Text("Hapus Produk")
            },

            text = {
                Text(
                    "Yakin ingin menghapus ${product.name}?"
                )
            },

            dismissButton = {
                OutlinedButton(
                    onClick = {
                        deletingProduct = null
                    }
                ) {
                    Text("Batal")
                }
            },

            confirmButton = {
                Button(
                    onClick = {

                        scope.launch {

                            try {

                                val response =
                                    RetrofitClient.api.deleteProduct(product.id)

                                adminMessage = response.message

                                reloadProducts()

                            } catch (e: Exception) {

                                adminMessage =
                                    "Gagal hapus: ${e.message}"
                            }

                            deletingProduct = null
                        }
                    }
                ) {
                    Text("Hapus")
                }
            }
        )
    }
}

@Composable
fun AdminManagementContent(
    products: List<Product>,
    categoriesData: List<Category>,
    productName: String,
    productPrice: String,
    selectedCategoryId: Int?,
    editingProductId: Int?,
    adminMessage: String?,
    onProductNameChange: (String) -> Unit,
    onProductPriceChange: (String) -> Unit,
    onCategorySelected: (Int) -> Unit,
    onSubmit: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit
) {
    Text(
        text = "Admin Produk",
        style = MaterialTheme.typography.headlineMedium
    )

    Spacer(modifier = Modifier.height(16.dp))

    AdminProductForm(
        productName = productName,
        productPrice = productPrice,
        categoriesData = categoriesData,
        selectedCategoryId = selectedCategoryId,
        editingProductId = editingProductId,
        adminMessage = adminMessage,
        onProductNameChange = onProductNameChange,
        onProductPriceChange = onProductPriceChange,
        onCategorySelected = onCategorySelected,
        onSubmit = onSubmit,
        onCancelEdit = {
            onProductNameChange("")
            onProductPriceChange("")
        }
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Daftar Produk",
        style = MaterialTheme.typography.headlineSmall
    )

    Spacer(modifier = Modifier.height(12.dp))

    products.forEach { product ->
        ProductCard(
            product = product,
            isAdmin = true,
            onAddToCart = {},
            onEditProduct = {
                onEditProduct(product)
            },
            onDeleteProduct = {
                onDeleteProduct(product)
            }
        )
    }
}

@Composable
fun ReceiptCard(
    invoiceCode: String?,
    totalPrice: Int?
) {
    val context = LocalContext.current
    var pdfMessage by remember { mutableStateOf<String?>(null) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Struk Pembayaran",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Mini Cashier QRIS")
            Text("Invoice: ${invoiceCode ?: "-"}")
            Text("Status: Berhasil Dibayar")

            Spacer(modifier = Modifier.height(12.dp))

            Divider()

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Total: ${formatRupiah(totalPrice ?: 0)}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    try {
                        saveReceiptPdf(
                            invoiceCode = invoiceCode ?: "-",
                            totalPrice = totalPrice ?: 0
                        )

                        pdfMessage = "PDF berhasil disimpan di Downloads"

                    } catch (e: Exception) {
                        pdfMessage = "Gagal simpan PDF: ${e.message}"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Struk PDF")
            }
            pdfMessage?.let {
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

fun saveReceiptPdf(
    invoiceCode: String,
    totalPrice: Int
) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(300, 500, 1).create()
    val page = pdfDocument.startPage(pageInfo)

    val canvas = page.canvas
    val paint = Paint()

    paint.textSize = 18f
    paint.isFakeBoldText = true
    canvas.drawText("Mini Cashier QRIS", 40f, 60f, paint)

    paint.textSize = 14f
    paint.isFakeBoldText = false
    canvas.drawText("Struk Pembayaran", 40f, 95f, paint)
    canvas.drawText("Invoice: $invoiceCode", 40f, 130f, paint)
    canvas.drawText("Status: Berhasil Dibayar", 40f, 165f, paint)

    paint.isFakeBoldText = true
    canvas.drawText("Total: ${formatRupiah(totalPrice)}", 40f, 215f, paint)

    paint.isFakeBoldText = false
    paint.textSize = 12f
    canvas.drawText("Terima kasih sudah berbelanja", 40f, 280f, paint)

    pdfDocument.finishPage(page)

    val downloadsDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    val file = File(
        downloadsDir,
        "struk_$invoiceCode.pdf"
    )

    pdfDocument.writeTo(FileOutputStream(file))
    pdfDocument.close()
}

@Composable
fun PaymentCard(
    invoiceCode: String?,
    totalPrice: Int?,
    onPayClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(12.dp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pembayaran QRIS",
                style = MaterialTheme.typography.headlineSmall
            )

            Text("Invoice: ${invoiceCode ?: "-"}")
            Text("Total: ${formatRupiah(totalPrice ?: 0)}")

            Spacer(modifier = Modifier.height(12.dp))

            val qrText = "QRIS-${invoiceCode ?: ""}-${totalPrice ?: 0}"

            val qrBitmap: Bitmap = remember(key1 = qrText) {
                generateQrCode(qrText)
            }

            Text(text = "Scan QR untuk bayar", fontSize = 16.sp)

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(220.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onPayClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Saya Sudah Bayar")
            }
        }
    }
}

@Composable
fun AnalyticsBarChart(
    title: String,
    data: List<Pair<String, Int>>
) {
    var showChart by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        showChart = true
    }

    AnimatedVisibility(
        visible = showChart,
        enter = fadeIn() + slideInVertically(
            initialOffsetY = { it / 3 }
        )
    ) {
        val safeData = data.filter { it.second > 0 }
        val maxValue = safeData.maxOfOrNull { it.second } ?: 1
        val totalIncome = safeData.sumOf { it.second }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(26.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Ringkasan income periode ini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("Total", style = MaterialTheme.typography.bodySmall)
                            Text(
                                formatRupiah(totalIncome),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (safeData.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Belum ada data transaksi")
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        safeData.forEach { item ->
                            val percentage = item.second.toFloat() / maxValue.toFloat()
                            val barHeight = (percentage * 105).dp.coerceAtLeast(42.dp)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = formatRupiah(item.second),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Box(
                                    modifier = Modifier
                                        .width(72.dp)
                                        .height(barHeight)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = formatChartDate(item.first),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    user: UserData
) {
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Harian", "Bulanan", "Tahunan")

    var dailyReports by remember { mutableStateOf<List<DailyReportResponse>>(emptyList()) }
    var monthlyReports by remember { mutableStateOf<List<MonthlyReportResponse>>(emptyList()) }
    var yearlyReports by remember { mutableStateOf<List<YearlyReportResponse>>(emptyList()) }

    var transactions by remember { mutableStateOf<List<TransactionData>>(emptyList()) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var historySearchQuery by remember { mutableStateOf("") }

    fun loadData(tabIndex: Int) {
        scope.launch {
            loading = true
            error = null

            try {
                transactions = RetrofitClient.api.getTransactions()

                when (tabIndex) {
                    0 -> dailyReports = RetrofitClient.api.getDailyReports()
                    1 -> monthlyReports = RetrofitClient.api.getMonthlyReports()
                    2 -> yearlyReports = RetrofitClient.api.getYearlyReports()
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun currentChartData(): List<Pair<String, Int>> {
        return when (selectedTab) {
            0 -> dailyReports.map { it.date to it.total_income }
            1 -> monthlyReports.map { it.month to it.total_income }
            else -> yearlyReports.map { it.year.toString() to it.total_income }
        }
    }

    fun currentChartTitle(): String {
        return when (selectedTab) {
            0 -> "Grafik Income Harian"
            1 -> "Grafik Income Bulanan"
            else -> "Grafik Income Tahunan"
        }
    }

    fun filteredTransactions(): List<TransactionData> {
        return transactions
            .filter { it.payment_status == "PAID" }
            .filter { transaction ->
                when (selectedTab) {
                    0 -> dailyReports.any { report ->
                        transaction.created_at.startsWith(report.date)
                    }

                    1 -> monthlyReports.any { report ->
                        transaction.created_at.startsWith(report.month)
                    }

                    else -> yearlyReports.any { report ->
                        transaction.created_at.startsWith(report.year.toString())
                    }
                }
            }
            .filter { transaction ->
                val query = historySearchQuery.trim()

                if (query.isBlank()) {
                    true
                } else {
                    transaction.invoice_code.contains(query, ignoreCase = true) ||
                            transaction.created_at.contains(query, ignoreCase = true) ||
                            transaction.items.any { item ->
                                item.product_name.contains(query, ignoreCase = true)
                            }
                }
            }
    }

    @Composable
    fun TransactionListSection(
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier.verticalScroll(rememberScrollState())
        ) {
            when {
                loading -> Text("Memuat riwayat...")
                error != null -> Text("Error: $error")
                filteredTransactions().isEmpty() -> Text("Belum ada transaksi")
                else -> {
                    filteredTransactions().forEach { transaction ->
                        TransactionDetailCard(transaction = transaction)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData(0)
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val isWide = maxWidth > 700.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Riwayat Transaksi",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TransactionListSection(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Analytics Center",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = {
                                    selectedTab = index
                                    loadData(index)
                                },
                                text = { Text(title) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (user.role == "ADMIN" && !loading && error == null) {
                        AnalyticsBarChart(
                            title = currentChartTitle(),
                            data = currentChartData()
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = if (user.role == "ADMIN")
                        "Analytics Center"
                    else
                        "Riwayat Transaksi",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                loadData(index)
                            },
                            text = { Text(title) }
                        )
                    }
                }

                if (user.role == "ADMIN") {
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!loading && error == null) {
                        AnalyticsBarChart(
                            title = currentChartTitle(),
                            data = currentChartData()
                        )
                    }
                }

                OutlinedTextField(
                    value = historySearchQuery,
                    onValueChange = { historySearchQuery = it },
                    label = { Text("Cari invoice / produk...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Riwayat Transaksi",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                TransactionListSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
fun TransactionDetailCard(
    transaction: TransactionData
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Tanggal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = transaction.invoice_code,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatTransactionDate(transaction.created_at),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = transaction.payment_status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = formatRupiah(transaction.total_price),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = {
                    expanded = !expanded
                }
            ) {
                Text(
                    if (expanded) "Sembunyikan detail"
                    else "Lihat detail"
                )
            }

            if (expanded) {
                Divider()

                Spacer(modifier = Modifier.height(10.dp))

                transaction.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = item.product_name,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Text(
                                text = "${item.quantity} x ${formatRupiah(item.price)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = formatRupiah(item.subtotal),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportCard(
    title: String,
    transactions: Int,
    income: Int
) {
    var showCard by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        showCard = true
    }

    AnimatedVisibility(
        visible = showCard,
        enter = fadeIn() + slideInVertically(
            initialOffsetY = { it / 4 }
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Tanggal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatReportTitle(title),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "$transactions Transaksi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = formatRupiah(income),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun formatChartDate(value: String): String {
    return if (value.length >= 10 && value.contains("-")) {
        val parts = value.split("-")
        "${parts[2]} ${monthNameShort(parts[1])}"
    } else {
        value
    }
}

fun formatReportTitle(value: String): String {
    val cleanValue = value
        .replace("Tanggal: ", "")
        .replace("Bulan: ", "")
        .replace("Tahun: ", "")

    return if (cleanValue.length >= 10 && cleanValue.contains("-")) {
        val parts = cleanValue.split("-")
        "${parts[2]} ${monthName(parts[1])} ${parts[0]}"
    } else {
        cleanValue
    }
}

fun formatTransactionDate(value: String): String {
    val dateOnly = value.take(10)

    return if (dateOnly.length >= 10 && dateOnly.contains("-")) {
        val parts = dateOnly.split("-")
        "${parts[2]} ${monthName(parts[1])} ${parts[0]}"
    } else {
        value
    }
}

fun monthName(month: String): String {
    return when (month) {
        "01" -> "Januari"
        "02" -> "Februari"
        "03" -> "Maret"
        "04" -> "April"
        "05" -> "Mei"
        "06" -> "Juni"
        "07" -> "Juli"
        "08" -> "Agustus"
        "09" -> "September"
        "10" -> "Oktober"
        "11" -> "November"
        "12" -> "Desember"
        else -> month
    }
}

fun monthNameShort(month: String): String {
    return when (month) {
        "01" -> "Jan"
        "02" -> "Feb"
        "03" -> "Mar"
        "04" -> "Apr"
        "05" -> "Mei"
        "06" -> "Jun"
        "07" -> "Jul"
        "08" -> "Agu"
        "09" -> "Sep"
        "10" -> "Okt"
        "11" -> "Nov"
        "12" -> "Des"
        else -> month
    }
}

fun formatRupiah(amount: Int): String {

    val localeID = Locale("in", "ID")

    val format =
        NumberFormat.getNumberInstance(localeID)

    return "Rp ${format.format(amount)}"
}

fun generateQrCode(text: String): Bitmap {
    val size = 512

    val bits = QRCodeWriter().encode(
        text,
        BarcodeFormat.QR_CODE,
        size,
        size
    )

    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(
                x,
                y,
                if (bits[x, y]) android.graphics.Color.BLACK
                else android.graphics.Color.WHITE
            )
        }
    }

    return bitmap
}