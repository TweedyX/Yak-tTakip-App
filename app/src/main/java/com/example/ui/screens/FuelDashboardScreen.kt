package com.example.ui.screens

import android.net.Uri
import android.provider.MediaStore
import android.graphics.ImageDecoder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.InteractiveBarChart
import com.example.ui.components.InteractiveLineChart
import com.example.ui.theme.*
import com.example.ui.viewmodel.FuelViewModel
import com.example.ui.viewmodel.ReportPeriod
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelDashboardScreen(
    viewModel: FuelViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Dialog state controllers
    var showVehicleEditDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    // Input fields state
    var fuelLitersInput by remember { mutableStateOf("") }
    var fuelCostInput by remember { mutableStateOf("") }
    var efficiencyInput by remember { mutableStateOf("") }

    // Vehicle profile edit inputs
    var vehicleNameInput by remember { mutableStateOf("") }
    var tankCapacityInput by remember { mutableStateOf("") }
    var selectedFuelType by remember { mutableStateOf("Benzin") }

    LaunchedEffect(showVehicleEditDialog) {
        if (showVehicleEditDialog) {
            vehicleNameInput = uiState.vehicleProfile.name
            tankCapacityInput = uiState.vehicleProfile.tankCapacity.toString()
            selectedFuelType = uiState.vehicleProfile.fuelType
        }
    }

    MyApplicationTheme(darkTheme = uiState.isDarkMode) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "YAKIT & TÜKETİM",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "MİNİMAL GÖSTERGE PANELİ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Tema Değiştir",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // SECTION 1: VEHICLE SUMMARY CARD (BENTO BOX)
                item {
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVehicleEditDialog = true }
                            .testTag("vehicle_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = "Araç",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ARAÇ PROFİLİ",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.vehicleProfile.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.SansSerif
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${uiState.vehicleProfile.fuelType}  •  Depo: ${uiState.vehicleProfile.tankCapacity}L",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Dynamic Circular Fill Indicator representing last fill ratio
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(75.dp)
                                    .padding(4.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { (uiState.fuelStats.tankFillPercentage / 100.0).toFloat() },
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 6.dp,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.0f%%", uiState.fuelStats.tankFillPercentage),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "DOLULUK",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION 1.5: AI RECEIPT SCANNER BENTO CARD
                item {
                    val contentResolver = context.contentResolver
                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        uri?.let {
                            try {
                                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, it)) { decoder, _, _ ->
                                        decoder.isMutableRequired = true
                                    }
                                } else {
                                    @Suppress("DEPRECATION")
                                    MediaStore.Images.Media.getBitmap(contentResolver, it)
                                }
                                viewModel.scanReceipt(bitmap)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Resim yüklenirken hata oluştu: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { launcher.launch("image/*") }
                            .testTag("receipt_scan_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Yapay Zeka",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "YAPAY ZEKA FİŞ TARAMA",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Fiyat ve tarihi otomatik doldurmak için tıklayıp fiş fotoğrafı seçin",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = "Git",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // SECTION 2: ADD FUEL PURCHASE & ADD MANUAL EFFICIENCY (TWO GRID BENTO CELLS)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // CARD A: QUICK FUEL ENTRY (LEFT CELL)
                        BentoCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(275.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalGasStation,
                                                contentDescription = "Yakıt Alımı",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "YAKIT ALIMI",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Liters Input (Borderless, Flat, Sharp ratio)
                                    TextField(
                                        value = fuelLitersInput,
                                        onValueChange = { fuelLitersInput = it },
                                        label = { Text("Litre (L)", fontSize = 11.sp) },
                                        placeholder = { Text("0.0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent
                                        ),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("liters_input"),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Cost Input (Borderless, Flat, Sharp ratio)
                                    TextField(
                                        value = fuelCostInput,
                                        onValueChange = { fuelCostInput = it },
                                        label = { Text("Tutar (TL)", fontSize = 11.sp) },
                                        placeholder = { Text("0.0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent
                                        ),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("cost_input"),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        val liters = fuelLitersInput.toDoubleOrNull()
                                        val cost = fuelCostInput.toDoubleOrNull()
                                        if (liters != null && cost != null && liters > 0 && cost > 0) {
                                            viewModel.addFuelLog(liters, cost)
                                            fuelLitersInput = ""
                                            fuelCostInput = ""
                                            Toast.makeText(context, "Yakıt alımı başarıyla eklendi!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Lütfen geçerli değerler girin!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .testTag("add_fuel_button")
                                ) {
                                    Text("Kaydet", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // CARD B: MANUAL EFFICIENCY ENTRY (RIGHT CELL)
                        BentoCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(275.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Speed,
                                                contentDescription = "Yakıt Verimliliği",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ARAÇ TÜKETİMİ",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Efficiency Reading Input
                                    TextField(
                                        value = efficiencyInput,
                                        onValueChange = { efficiencyInput = it },
                                        label = { Text("Tüketim (L/100km)", fontSize = 11.sp) },
                                        placeholder = { Text("6.4") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent
                                        ),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("efficiency_input"),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Display live evaluation or descriptive hint
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = if (uiState.efficiencyStats.latestL100km > 0) {
                                                "Son Tüketim: ${String.format(Locale.getDefault(), "%.1f", uiState.efficiencyStats.latestL100km)} L/100"
                                            } else {
                                                "Yol bilgisayarı verisini girin."
                                            },
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        val effVal = efficiencyInput.toDoubleOrNull()
                                        if (effVal != null && effVal > 0) {
                                            viewModel.addEfficiencyLog(effVal)
                                            efficiencyInput = ""
                                            Toast.makeText(context, "Yol bilgisayarı verimi eklendi!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Lütfen geçerli tüketim değeri girin!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .testTag("add_efficiency_button")
                                ) {
                                    Text("Ekle", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // SECTION 3: INTERACTIVE FUEL SPENDING REPORT CHART (DOUBLE WIDE BOX)
                item {
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header Row with Segmented Period Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "YAKIT HARCAMA RAPORU",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.2f TL", uiState.fuelStats.totalSpend),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                // Interactive Period Select Tabs
                                Row(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    ReportPeriod.values().forEach { period ->
                                        val isSelected = uiState.selectedPeriod == period
                                        val label = when (period) {
                                            ReportPeriod.WEEKLY -> "Haftalık"
                                            ReportPeriod.MONTHLY -> "Aylık"
                                            ReportPeriod.YEARLY -> "Yıllık"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                                )
                                                .clickable { viewModel.setPeriod(period) }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Interactive Bar Chart Custom Component
                            InteractiveBarChart(
                                data = uiState.spendingChartData,
                                accentColor = MaterialTheme.colorScheme.primary,
                                valueSuffix = " TL",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }
                }

                // SECTION 4: EFFICIENCY METRICS & COMPARISON (TYPOGRAPHY BENTO)
                item {
                    BentoCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = "Kıyaslama",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "YAKIT VERİMLİLİĞİ KIYASLAMASI",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Araç Gösterge Değeri",
                                        fontSize = 11.sp,
                                        color = TextGray
                                    )
                                    Text(
                                        text = if (uiState.efficiencyStats.latestL100km > 0) {
                                            String.format(Locale.getDefault(), "%.1f L/100km", uiState.efficiencyStats.latestL100km)
                                        } else {
                                            "Girildi"
                                        },
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Genel Tüketim Ort.",
                                        fontSize = 11.sp,
                                        color = TextGray
                                    )
                                    Text(
                                        text = if (uiState.efficiencyStats.avgL100km > 0) {
                                            String.format(Locale.getDefault(), "%.1f L/100km", uiState.efficiencyStats.avgL100km)
                                        } else {
                                            "Veri Yok"
                                        },
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Comparison details with progress/difference calculation (Borderless flat surface)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            MaterialTheme.colorScheme.background,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (uiState.efficiencyStats.diffVsPrevious <= 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = "Durum",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Yol Bilgisayarı Analizi",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = uiState.efficiencyStats.evaluationMessage,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION 5: INTERACTIVE EFFICIENCY HISTORY TREND (BENTO BOX VIOLET)
                item {
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShowChart,
                                            contentDescription = "Trend",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "TÜKETİM TRENDİ (L/100km)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (uiState.efficiencyStats.diffVsPrevious != 0.0) {
                                    Text(
                                        text = String.format(
                                            Locale.getDefault(),
                                            "%s%.1f L/100km",
                                            if (uiState.efficiencyStats.diffVsPrevious > 0) "+" else "",
                                            uiState.efficiencyStats.diffVsPrevious
                                        ),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            InteractiveLineChart(
                                data = uiState.efficiencyChartData,
                                accentColor = MaterialTheme.colorScheme.primary,
                                valueSuffix = " L/100km",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }
                }

                // SECTION 6: ANALYTICS GRID & VEHICLE PARAMETERS (CYAN ACCENT)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "DETAYLI YAKIT METRİKLERİ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricCell(
                                title = "Ort. Litre Fiyatı",
                                value = String.format(Locale.getDefault(), "%.2f TL", uiState.fuelStats.avgPricePerLiter),
                                subText = "Harcama / Litre",
                                icon = Icons.Default.AttachMoney,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCell(
                                title = "Toplam Alınan",
                                value = String.format(Locale.getDefault(), "%.1f L", uiState.fuelStats.totalLiters),
                                subText = "Tüm satın alımlar",
                                icon = Icons.Default.Opacity,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricCell(
                                title = "Dolum Başı Ort.",
                                value = String.format(Locale.getDefault(), "%.1f L", uiState.fuelStats.avgLitersPerFill),
                                subText = "Seans başına litre",
                                icon = Icons.Default.EvStation,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCell(
                                title = "Fiyat Değişimi",
                                value = uiState.fuelStats.priceTrend,
                                subText = "Son alım kıyaslaması",
                                icon = Icons.Default.TrendingUp,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // SECTION 7: RECENT TRANSACTIONS & MANAGE DIALOG TRIGGER
                item {
                    BentoCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Geçmiş",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SON İŞLEMLER",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "Yönet",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { showHistoryDialog = true }
                                        .padding(4.dp)
                                        .testTag("manage_history_button")
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (uiState.fuelLogs.isEmpty() && uiState.efficiencyLogs.isEmpty()) {
                                Text(
                                    text = "Henüz kaydedilmiş bir işlem yok.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextGray,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Merged last 3 actions
                                    val recentPurchases = uiState.fuelLogs.take(2).map { LogRowItem.Purchase(it) }
                                    val recentEff = uiState.efficiencyLogs.take(2).map { LogRowItem.Efficiency(it) }
                                    val combined = (recentPurchases + recentEff).sortedByDescending { it.timestamp }.take(3)

                                    combined.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                            CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = when (item) {
                                                            is LogRowItem.Purchase -> Icons.Default.LocalGasStation
                                                            is LogRowItem.Efficiency -> Icons.Default.Speed
                                                        },
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = when (item) {
                                                            is LogRowItem.Purchase -> "Yakıt Alımı"
                                                            is LogRowItem.Efficiency -> "Tüketim Girişi"
                                                        },
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp)),
                                                        fontSize = 10.sp,
                                                        color = TextGray
                                                    )
                                                }
                                            }

                                            Text(
                                                text = when (item) {
                                                    is LogRowItem.Purchase -> String.format(Locale.getDefault(), "%.1fL  •  %.2f TL", item.log.liters, item.log.totalCost)
                                                    is LogRowItem.Efficiency -> String.format(Locale.getDefault(), "%.1f L/100km", item.log.l100km)
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ================== DIALOGS ==================

        // AI RECEIPT SCANNING - LOADING DIALOG
        if (uiState.isScanningReceipt) {
            Dialog(onDismissRequest = { /* Don't dismiss during scanning */ }) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Yapay zeka fişi analiz ediyor...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lütfen bekleyin...",
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                    }
                }
            }
        }

        // AI RECEIPT SCANNING - CONFIRMATION & LITERS INPUT DIALOG
        val scanResult = uiState.scannedReceiptResult
        if (scanResult != null) {
            var extractedCost by remember(scanResult) { mutableStateOf(scanResult.price?.toString() ?: "") }
            var extractedDate by remember(scanResult) { mutableStateOf(scanResult.date ?: "") }
            var scanLitersInput by remember { mutableStateOf("") }

            Dialog(onDismissRequest = { viewModel.clearScannedReceipt() }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Yapay Zeka Onay",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Fiş Detaylarını Onayla",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Yapay zeka tarafından yakıt fişinden okunan veriler aşağıdadır. Litre bilgisini girip onaylayın.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tutar (TL) Editable
                        Text(
                            text = "Toplam Tutar (TL)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        TextField(
                            value = extractedCost,
                            onValueChange = { extractedCost = it },
                            placeholder = { Text("0.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(4.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("extracted_cost_input"),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tarih Editable
                        Text(
                            text = "Tarih (GG.AA.YYYY)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        TextField(
                            value = extractedDate,
                            onValueChange = { extractedDate = it },
                            placeholder = { Text("GG.AA.YYYY") },
                            singleLine = true,
                            shape = RoundedCornerShape(4.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("extracted_date_input"),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Litre (Required Input)
                        Text(
                            text = "Litre (L) *",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        TextField(
                            value = scanLitersInput,
                            onValueChange = { scanLitersInput = it },
                            placeholder = { Text("Alınan litre miktarını giriniz") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(4.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("scan_liters_input"),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.clearScannedReceipt() },
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("İptal", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    val liters = scanLitersInput.toDoubleOrNull()
                                    val cost = extractedCost.toDoubleOrNull()
                                    if (liters == null || liters <= 0) {
                                        Toast.makeText(context, "Lütfen geçerli bir litre girin!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (cost == null || cost <= 0) {
                                        Toast.makeText(context, "Lütfen geçerli bir tutar girin!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    // Local date parser helper
                                    val parsedTimestamp: Long = try {
                                        if (extractedDate.isNotBlank()) {
                                            val formats = listOf(
                                                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()),
                                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                            )
                                            var tempTime = System.currentTimeMillis()
                                            for (format in formats) {
                                                try {
                                                    val parsed = format.parse(extractedDate)
                                                    if (parsed != null) {
                                                        tempTime = parsed.time
                                                        break
                                                    }
                                                } catch (e: Exception) { }
                                            }
                                            tempTime
                                        } else {
                                            System.currentTimeMillis()
                                        }
                                    } catch (e: Exception) {
                                        System.currentTimeMillis()
                                    }

                                    viewModel.addFuelLog(liters, cost, parsedTimestamp)
                                    viewModel.clearScannedReceipt()
                                    Toast.makeText(context, "Yakıt alımı başarıyla kaydedildi!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Kaydet & Ekle", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 1. VEHICLE PROFILE EDIT DIALOG
        if (showVehicleEditDialog) {
            Dialog(onDismissRequest = { showVehicleEditDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("vehicle_edit_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Araç Profilini Düzenle",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        TextField(
                            value = vehicleNameInput,
                            onValueChange = { vehicleNameInput = it },
                            label = { Text("Araç Adı / Markası") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth().testTag("edit_vehicle_name")
                        )

                        TextField(
                            value = tankCapacityInput,
                            onValueChange = { tankCapacityInput = it },
                            label = { Text("Depo Hacmi (Litre)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth().testTag("edit_vehicle_capacity")
                        )

                        Text("Yakıt Türü", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGray)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val fuelTypes = listOf("Benzin", "Dizel", "LPG", "Elektrik")
                            fuelTypes.forEach { type ->
                                val isSelected = selectedFuelType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { selectedFuelType = type }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showVehicleEditDialog = false }) {
                                Text("Vazgeç", color = TextGray)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    val capacity = tankCapacityInput.toDoubleOrNull()
                                    if (capacity != null && capacity > 0) {
                                        viewModel.saveVehicleProfile(vehicleNameInput, capacity, selectedFuelType)
                                        showVehicleEditDialog = false
                                        Toast.makeText(context, "Araç profili güncellendi!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Lütfen geçerli bir depo hacmi girin!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("Kaydet", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. FULL DETAILED HISTORY MANAGEMENT DIALOG
        if (showHistoryDialog) {
            Dialog(onDismissRequest = { showHistoryDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .padding(8.dp)
                        .testTag("history_dialog")
                ) {
                    var selectedTab by remember { mutableStateOf(0) } // 0: Fuel logs, 1: Efficiency logs

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Title bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "İşlem Geçmişi Yönetimi",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            IconButton(onClick = { showHistoryDialog = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        // Tab selectors
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Yakıt Satın Alımları", fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Tüketim Girişleri", fontWeight = FontWeight.Bold) }
                            )
                        }

                        // Logs List
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            if (selectedTab == 0) {
                                if (uiState.fuelLogs.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Kayıtlı yakıt alımı bulunmuyor.", color = TextGray)
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(uiState.fuelLogs, key = { it.id }) { log ->
                                            HistoryLogRow(
                                                title = "${log.liters} L • ${log.totalCost} TL",
                                                subtitle = "Litre Fiyatı: ${String.format(Locale.getDefault(), "%.2f TL", log.pricePerLiter)}",
                                                timestamp = log.timestamp,
                                                accentColor = MaterialTheme.colorScheme.primary,
                                                onDelete = { viewModel.deleteFuelLog(log.id) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                if (uiState.efficiencyLogs.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Kayıtlı tüketim verisi bulunmuyor.", color = TextGray)
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(uiState.efficiencyLogs, key = { it.id }) { log ->
                                            HistoryLogRow(
                                                title = "${log.l100km} L/100km",
                                                subtitle = "Dashboard tüketim verisi",
                                                timestamp = log.timestamp,
                                                accentColor = MaterialTheme.colorScheme.primary,
                                                onDelete = { viewModel.deleteEfficiencyLog(log.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================== SUPPORTING COMPOSABLES ==================

sealed class LogRowItem {
    abstract val timestamp: Long

    data class Purchase(val log: com.example.data.model.FuelLog) : LogRowItem() {
        override val timestamp: Long = log.timestamp
    }

    data class Efficiency(val log: com.example.data.model.EfficiencyLog) : LogRowItem() {
        override val timestamp: Long = log.timestamp
    }
}

@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
    ) {
        content()
    }
}

@Composable
fun MetricCell(
    title: String,
    value: String,
    subText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                    color = TextGray
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subText,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun HistoryLogRow(
    title: String,
    subtitle: String,
    timestamp: Long,
    accentColor: Color,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextGray
                )
                Text(
                    text = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(timestamp)),
                    fontSize = 9.sp,
                    color = TextGray.copy(alpha = 0.7f)
                )
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag("delete_log_button")
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Sil",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )
        }
    }
}
