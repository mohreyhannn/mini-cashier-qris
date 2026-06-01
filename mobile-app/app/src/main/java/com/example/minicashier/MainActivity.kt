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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Logout
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.RestaurantMenu
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
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
    object Insight : Screen("insight")
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
fun LoadingProductSkeleton() {
    var startAnimation by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 0.35f else 0.9f,
        animationSpec = tween(
            durationMillis = 700
        ),
        label = "skeletonAlpha"
    )

    LaunchedEffect(Unit) {
        while (true) {
            startAnimation = !startAnimation
            kotlinx.coroutines.delay(700)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(22.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    )
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    )
            )
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
    var bestSellers by remember {
        mutableStateOf<List<BestSellerResponse>>(emptyList())
    }

    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var adminMessage by remember { mutableStateOf<String?>(null) }
    var editingProductId by remember { mutableStateOf<Int?>(null) }
    var activeShiftId by remember { mutableStateOf<Int?>(null) }
    var shiftMessage by remember { mutableStateOf<String?>(null) }
    var openingCash by remember { mutableStateOf("") }
    var showStartShiftDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            products = RetrofitClient.api.getProducts()
            categoriesData = RetrofitClient.api.getCategories()
            dashboard = RetrofitClient.api.getDashboard()
            bestSellers =
                RetrofitClient.api.getBestSellers()
            val activeShift = RetrofitClient.api.getActiveShift(user.id)

            if (activeShift.active && activeShift.shift != null) {
                activeShiftId = activeShift.shift.id
                shiftMessage = "Shift aktif"
            } else {
                activeShiftId = null
                shiftMessage = "Belum mulai shift"
            }
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
        if (activeShiftId == null) {
            checkoutMessage = "Silakan mulai shift terlebih dahulu"
            appPopup = AppPopup(
                title = "Shift Belum Aktif",
                message = "Mulai shift dulu sebelum melakukan checkout",
                icon = "⚠️"
            )
            return
        }

        scope.launch {
            try {
                val transactionItems = cartItems.map {
                    TransactionItemRequest(
                        product_id = it.product.id,
                        quantity = it.quantity
                    )
                }

                val response = RetrofitClient.api.createTransaction(
                    TransactionRequest(
                        user_id = user.id,
                        shift_id = activeShiftId!!,
                        items = transactionItems
                    )
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

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = if (activeShiftId != null)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = if (activeShiftId != null)
                        "Shift Aktif"
                    else
                        "Shift Belum Aktif",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = if (activeShiftId != null)
                        "Kasir: ${user.name}"
                    else
                        "Mulai shift sebelum transaksi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (activeShiftId == null) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            showStartShiftDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Mulai Shift")
                    }
                }
            }
        }

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

                Column {

                    repeat(3) {

                        LoadingProductSkeleton()
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

                            onEditProduct = {},

                            onRestockProduct = {},

                            onDeleteProduct = {}
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
    if (showStartShiftDialog) {
        AlertDialog(
            onDismissRequest = {
                showStartShiftDialog = false
            },
            title = {
                Text("Mulai Shift")
            },
            text = {
                Column {
                    Text("Masukkan kas awal shift")

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = openingCash,
                        onValueChange = {
                            openingCash = it
                        },
                        label = {
                            Text("Kas Awal")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showStartShiftDialog = false
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
                                val response = RetrofitClient.api.startShift(
                                    StartShiftRequest(
                                        user_id = user.id,
                                        opening_cash = openingCash.toIntOrNull() ?: 0
                                    )
                                )

                                activeShiftId = response.shift_id
                                shiftMessage = response.message
                                showStartShiftDialog = false
                                openingCash = ""

                                appPopup = AppPopup(
                                    title = "Shift Dimulai",
                                    message = "Kasir ${user.name} sudah mulai shift",
                                    icon = "✅"
                                )

                            } catch (e: Exception) {
                                shiftMessage = "Gagal mulai shift: ${e.message}"

                                appPopup = AppPopup(
                                    title = "Gagal Mulai Shift",
                                    message = e.message ?: "Terjadi kesalahan",
                                    icon = "❌"
                                )
                            }
                        }
                    }
                ) {
                    Text("Mulai")
                }
            }
        )
    }

    AppPopupDialog(
        popup = appPopup,
        onDismiss = {
            appPopup = null
        }
    )
}

@Composable
fun DashboardCard(
    dashboard: DashboardResponse
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),

        shape = RoundedCornerShape(30.dp),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = "Dashboard Analytics",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(24.dp),

                color = MaterialTheme.colorScheme.primary
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "Total Income",

                        style =
                            MaterialTheme.typography.bodyLarge,

                        color =
                            MaterialTheme.colorScheme.onPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text =
                            formatRupiah(
                                dashboard.total_income
                            ),

                        style =
                            MaterialTheme.typography.headlineMedium,

                        color =
                            MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Surface(
                    modifier = Modifier.weight(1f),

                    shape = RoundedCornerShape(22.dp),

                    color =
                        MaterialTheme.colorScheme.primaryContainer
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "🧾",

                            fontSize = 28.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                dashboard.total_transactions
                                    .toString(),

                            style =
                                MaterialTheme.typography.headlineSmall
                        )

                        Text(
                            text = "Transactions",

                            style =
                                MaterialTheme.typography.bodyMedium,

                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),

                    shape = RoundedCornerShape(22.dp),

                    color =
                        MaterialTheme.colorScheme.secondaryContainer
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "📦",

                            fontSize = 28.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                dashboard.total_items_sold
                                    .toString(),

                            style =
                                MaterialTheme.typography.headlineSmall
                        )

                        Text(
                            text = "Items Sold",

                            style =
                                MaterialTheme.typography.bodyMedium,

                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(20.dp),

                color =
                    MaterialTheme.colorScheme.surfaceVariant
            ) {

                Column(
                    modifier = Modifier.padding(14.dp)
                ) {

                    Text(
                        text = "Last Transaction",

                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text =
                            dashboard.last_transaction.invoice_code,

                        color =
                            MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text =
                            formatRupiah(
                                dashboard.last_transaction.total_price
                            )
                    )
                }
            }
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = if (editingProductId == null)
                    "Tambah Produk"
                else
                    "Edit Produk",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Lengkapi data menu dengan benar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = productName,
                onValueChange = onProductNameChange,
                label = { Text("Nama Produk") },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = productPrice,
                onValueChange = onProductPriceChange,
                label = { Text("Harga Produk") },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Kategori",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoriesData) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Batal Edit")
                }
            }

            adminMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
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
        if (user.role == "ADMIN") "insight" else "home"

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    if (user.role != "ADMIN") {
                        NavigationBarItem(
                            selected = currentRoute == "home",
                            onClick = {
                                navController.navigate("home")
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            label = { Text("Home") },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Home"
                                )
                            }
                        )
                    }

                    NavigationBarItem(
                        selected = currentRoute == "history",
                        onClick = {
                            navController.navigate("history")
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        label = { Text("History") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History"
                            )
                        }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "insight",
                        onClick = {
                            navController.navigate("insight")
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        label = { Text("Insight") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Insight"
                            )
                        }
                    )

                    if (user.role == "ADMIN") {
                        NavigationBarItem(
                            selected = currentRoute == "admin",
                            onClick = {
                                navController.navigate("admin")
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            label = { Text("Produk") },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.RestaurantMenu,
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
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        label = { Text("Logout") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Logout"
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->

        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            composable("home") {
                ProductScreen(user = user)
            }

            composable("history") {
                TransactionHistoryScreen(user = user)
            }

            composable("insight") {
                InsightScreen()
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
                                userId = response.user.id,
                                name = response.user.name,
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
    onRestockProduct: () -> Unit,
    onDeleteProduct: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    var showCard by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        label = "productCardScale"
    )

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
                .scale(scale)
                .clickable {
                    pressed = true
                    pressed = false
                }
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                AsyncImage(
                    model = getProductImageUrl(product.name),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatRupiah(product.price),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = when {
                            product.stock <= 0 -> "Sold Out"
                            product.stock <= 5 -> "Stock ${product.stock}"
                            else -> "Stock ${product.stock}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            product.stock <= 0 -> MaterialTheme.colorScheme.error
                            product.stock <= 5 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isAdmin) {
                    Button(
                        onClick = onAddToCart,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = product.stock > 0,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("+ Tambah")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEditProduct,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Edit")
                        }

                        Button(
                            onClick = onRestockProduct,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Stock")
                        }

                        OutlinedButton(
                            onClick = onDeleteProduct,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
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
fun ModernPeriodTabs(
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val selected = selectedTab == index

                Button(
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Transparent,
                        contentColor = if (selected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (selected) 4.dp else 0.dp
                    )
                ) {
                    Text(title)
                }
            }
        }
    }
}

@Composable
fun InsightScreen() {
    var dashboard by remember { mutableStateOf<DashboardResponse?>(null) }
    var bestSellers by remember { mutableStateOf<List<BestSellerResponse>>(emptyList()) }

    var dailyReports by remember { mutableStateOf<List<DailyReportResponse>>(emptyList()) }
    var monthlyReports by remember { mutableStateOf<List<MonthlyReportResponse>>(emptyList()) }
    var yearlyReports by remember { mutableStateOf<List<YearlyReportResponse>>(emptyList()) }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Harian", "Bulanan", "Tahunan")

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            dashboard = RetrofitClient.api.getDashboard()
            bestSellers = RetrofitClient.api.getBestSellers()
            dailyReports = RetrofitClient.api.getDailyReports()
            monthlyReports = RetrofitClient.api.getMonthlyReports()
            yearlyReports = RetrofitClient.api.getYearlyReports()
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val isTablet = maxWidth > 700.dp

        when {
            loading -> LoadingHistorySkeleton()
            error != null -> Text("Error: $error")
            isTablet -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Business Insight",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        dashboard?.let {
                            DashboardCard(dashboard = it)
                        }

                        BestSellerCard(bestSellers = bestSellers)
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Income Analytics",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ModernPeriodTabs(
                            selectedTab = selectedTab,
                            tabs = tabs,
                            onTabSelected = { selectedTab = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        AnalyticsBarChart(
                            title = currentChartTitle(),
                            data = currentChartData()
                        )
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Business Insight",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    dashboard?.let {
                        DashboardCard(dashboard = it)
                    }

                    BestSellerCard(bestSellers = bestSellers)

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Income Analytics",
                                style = MaterialTheme.typography.headlineSmall
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            ModernPeriodTabs(
                                selectedTab = selectedTab,
                                tabs = tabs,
                                onTabSelected = {
                                    selectedTab = it
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            AnalyticsBarChart(
                                title = currentChartTitle(),
                                data = currentChartData()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
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
    var restockProduct by remember {
        mutableStateOf<Product?>(null)
    }

    var restockAmount by remember {
        mutableStateOf("")
    }
    var dashboard by remember { mutableStateOf<DashboardResponse?>(null) }
    var bestSellers by remember {
        mutableStateOf<List<BestSellerResponse>>(emptyList())
    }

    val scope = rememberCoroutineScope()

    suspend fun reloadProducts() {
        products = RetrofitClient.api.getProducts()
        dashboard = RetrofitClient.api.getDashboard()
        bestSellers = RetrofitClient.api.getBestSellers()
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
            bestSellers = RetrofitClient.api.getBestSellers()
        } catch (e: Exception) {
            adminMessage = "Gagal load data: ${e.message}"
        }
    }
    //Tablet
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
                        .fillMaxHeight()
                ) {
                    AdminManagementContent(
                        products = products,
                        dashboard = dashboard,
                        bestSellers = bestSellers,
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
                        onRestockProduct = { product ->
                            restockProduct = product
                        },
                        onDeleteProduct = { product ->
                            deletingProduct = product
                        }
                    )
                }
            }
        } else {
            //Heandphone
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                AdminManagementContent(
                    products = products,
                    dashboard = dashboard,
                    bestSellers = bestSellers,
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
                    onRestockProduct = { product ->
                        restockProduct = product
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
                Text("Yakin ingin menghapus ${product.name}?")
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
                                adminMessage = "Gagal hapus: ${e.message}"
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

    restockProduct?.let { product ->
        AlertDialog(
            onDismissRequest = {
                restockProduct = null
                restockAmount = ""
            },
            title = {
                Text("Tambah Stock")
            },
            text = {
                Column {
                    Text("Stock sekarang: ${product.stock}")

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = restockAmount,
                        onValueChange = {
                            restockAmount = it
                        },
                        label = {
                            Text("Tambah stock")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        restockProduct = null
                        restockAmount = ""
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
                                RetrofitClient.api.restockProduct(
                                    product.id,
                                    RestockRequest(
                                        add_stock = restockAmount.toInt()
                                    )
                                )

                                reloadProducts()
                                adminMessage = "Stock berhasil ditambahkan"

                            } catch (e: Exception) {
                                adminMessage = "Gagal restock: ${e.message}"
                            }

                            restockProduct = null
                            restockAmount = ""
                        }
                    }
                ) {
                    Text("Tambah")
                }
            }
        )
    }
}

@Composable
fun ModernSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}


@Composable
fun BestSellerCard(
    bestSellers: List<BestSellerResponse>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Best Seller",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Produk paling banyak terjual",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (bestSellers.isEmpty()) {
                Text("Belum ada data penjualan")
            } else {
                bestSellers.forEachIndexed { index, item ->

                    val medal = when (index) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> "${index + 1}"
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = medal,
                                fontSize = 24.sp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = item.product_name,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = "Terjual ${item.total_sold}x",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "${item.total_sold}x",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AdminManagementContent(
    products: List<Product>,
    dashboard: DashboardResponse?,
    bestSellers: List<BestSellerResponse>,
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
    onEditProduct: (Product) -> Unit,
    onRestockProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
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
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "Daftar Produk",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        text = "Kelola menu yang tersedia",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        products.forEach { product ->
                            ProductCard(
                                product = product,
                                isAdmin = true,
                                onAddToCart = {},
                                onEditProduct = { onEditProduct(product) },
                                onRestockProduct = { onRestockProduct(product) },
                                onDeleteProduct = { onDeleteProduct(product) }
                            )
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = if (editingProductId == null)
                            "Tambah Menu"
                        else
                            "Edit Menu",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        text = "Atur nama, harga, dan kategori",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Produk Menu",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        Text(
                            text = "Tambah dan kelola produk",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
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
                            onEditProduct = { onEditProduct(product) },
                            onRestockProduct = { onRestockProduct(product) },
                            onDeleteProduct = { onDeleteProduct(product) }
                        )
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
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
fun EmptyState(
    icon: String,
    title: String,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 42.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LoadingHistorySkeleton() {

    Column {

        repeat(3) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),

                shape = RoundedCornerShape(24.dp),

                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surface
                )
            ) {

                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                MaterialTheme.colorScheme.primary
                                    .copy(alpha = 0.25f)
                            )
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(20.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    MaterialTheme.colorScheme.primary
                                        .copy(alpha = 0.25f)
                                )
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    MaterialTheme.colorScheme.primary
                                        .copy(alpha = 0.18f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodHeader(
    title: String,
    count: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 10.dp
            ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "$count transaksi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    var selectedTransaction by remember {
        mutableStateOf<TransactionData?>(null)
    }

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
                loading -> LoadingHistorySkeleton()

                error != null -> Text("Error: $error")

                filteredTransactions().isEmpty() -> EmptyState(
                    icon = "📦",
                    title = "Belum ada transaksi",
                    message = "Transaksi yang sudah dibayar akan muncul di sini"
                )

                else -> {
                    val groupedTransactions =
                        when (selectedTab) {
                            0 -> filteredTransactions()
                                .groupBy { it.created_at.take(10) }

                            1 -> filteredTransactions()
                                .groupBy { it.created_at.take(7) }

                            else -> filteredTransactions()
                                .groupBy { it.created_at.take(4) }
                        }

                    groupedTransactions.forEach { group ->

                        PeriodHeader(
                            title = when (selectedTab) {
                                0 -> formatTransactionDate(group.key)
                                1 -> formatMonthYear(group.key)
                                else -> "Tahun ${group.key}"
                            },
                            count = group.value.size
                        )

                        group.value.forEach { transaction ->
                            Box(
                                modifier = Modifier.clickable {
                                    selectedTransaction = transaction
                                }
                            ) {
                                TransactionDetailCard(transaction = transaction)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Riwayat Transaksi",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                ModernPeriodTabs(
                    selectedTab = selectedTab,
                    tabs = tabs,
                    onTabSelected = {
                        selectedTab = it
                        loadData(it)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

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

                TransactionListSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                Text(
                    text = "Riwayat Transaksi",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

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

                ModernPeriodTabs(
                    selectedTab = selectedTab,
                    tabs = tabs,
                    onTabSelected = {
                        selectedTab = it
                        loadData(it)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                TransactionListSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
    selectedTransaction?.let { transaction ->
        AlertDialog(
            onDismissRequest = {
                selectedTransaction = null
            },
            title = {
                Text("Detail Transaksi")
            },
            text = {
                Column {
                    Text("Invoice: ${transaction.invoice_code}")
                    Text("Tanggal: ${formatFullDate(transaction.created_at)}")
                    Text("Kasir: ${transaction.cashier_name}")
                    Text("Status: ${transaction.payment_status}")
                    Text("Metode: ${transaction.payment_method}")

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Item Pesanan",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    transaction.items.forEach { item ->
                        Column(
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(item.product_name)
                            Text(
                                "${item.quantity} x ${formatRupiah(item.price)} = ${formatRupiah(item.subtotal)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Divider()

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Total: ${formatRupiah(transaction.total_price)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedTransaction = null
                    }
                ) {
                    Text("Tutup")
                }
            }
        )
    }
}

fun formatFullDate(dateTime: String): String {
    return try {
        val cleanDate = dateTime.take(19)

        val inputFormat = java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale("id", "ID")
        )

        val outputFormat = java.text.SimpleDateFormat(
            "EEEE, dd MMMM yyyy HH:mm",
            Locale("id", "ID")
        )

        val date = inputFormat.parse(cleanDate)
        outputFormat.format(date!!)
    } catch (e: Exception) {
        dateTime
    }
}

@Composable
fun TransactionDetailCard(
    transaction: TransactionData
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),

        shape = RoundedCornerShape(26.dp),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    modifier = Modifier.size(58.dp),

                    shape = RoundedCornerShape(18.dp),

                    color =
                        MaterialTheme.colorScheme.primaryContainer
                ) {

                    Box(
                        modifier = Modifier.fillMaxSize(),

                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "🧾",
                            fontSize = 28.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Surface(
                        shape = RoundedCornerShape(14.dp),

                        color =
                            MaterialTheme.colorScheme.primary
                                .copy(alpha = 0.12f)
                    ) {

                        Text(
                            text = transaction.invoice_code,

                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 4.dp
                            ),

                            color =
                                MaterialTheme.colorScheme.primary,

                            style =
                                MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text =
                            formatTransactionDate(
                                transaction.created_at
                            ),

                        style =
                            MaterialTheme.typography.bodyMedium,

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Kasir: ${transaction.cashier_name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text =
                            transaction.payment_status,

                        style =
                            MaterialTheme.typography.bodySmall,

                        color =
                            MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    horizontalAlignment =
                        Alignment.End
                ) {

                    Text(
                        text =
                            formatRupiah(
                                transaction.total_price
                            ),

                        style =
                            MaterialTheme.typography.titleLarge,

                        color =
                            MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(
                        onClick = {
                            expanded = !expanded
                        }
                    ) {

                        Text(
                            if (expanded)
                                "Tutup"
                            else
                                "Detail"
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded
            ) {

                Column {

                    Spacer(modifier = Modifier.height(12.dp))

                    Divider()

                    Spacer(modifier = Modifier.height(12.dp))

                    transaction.items.forEach { item ->

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),

                            shape = RoundedCornerShape(18.dp),

                            color =
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {

                            Row(
                                modifier = Modifier.padding(14.dp),

                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = item.product_name,

                                        style =
                                            MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.height(4.dp)
                                    )

                                    Text(
                                        text =
                                            "${item.quantity} x ${
                                                formatRupiah(item.price)
                                            }",

                                        style =
                                            MaterialTheme.typography.bodyMedium,

                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                    )
                                }

                                Text(
                                    text =
                                        formatRupiah(item.subtotal),

                                    style =
                                        MaterialTheme.typography.titleMedium,

                                    color =
                                        MaterialTheme.colorScheme.primary
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

fun formatMonthYear(value: String): String {
    return if (value.length >= 7 && value.contains("-")) {
        val parts = value.split("-")
        "${monthName(parts[1])} ${parts[0]}"
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