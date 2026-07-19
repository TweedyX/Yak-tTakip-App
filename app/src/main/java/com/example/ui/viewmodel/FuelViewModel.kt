package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.EfficiencyLog
import com.example.data.model.FuelLog
import com.example.data.model.VehicleProfile
import com.example.data.repository.FuelRepository
import com.example.data.api.GeminiReceiptService
import com.example.data.api.ReceiptScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

enum class ReportPeriod {
    WEEKLY, MONTHLY, YEARLY
}

data class ChartDataPoint(
    val label: String,
    val value: Double,
    val rawValue: Double = value
)

data class FuelStats(
    val totalLiters: Double = 0.0,
    val totalSpend: Double = 0.0,
    val avgPricePerLiter: Double = 0.0,
    val avgLitersPerFill: Double = 0.0,
    val avgCostPerFill: Double = 0.0,
    val tankFillPercentage: Double = 0.0,
    val lastFillLiters: Double = 0.0,
    val lastFillCost: Double = 0.0,
    val priceTrend: String = "N/A"
)

data class EfficiencyStats(
    val latestL100km: Double = 0.0,
    val previousL100km: Double = 0.0,
    val avgL100km: Double = 0.0,
    val diffVsPrevious: Double = 0.0, // negative means improvement
    val diffVsPreviousPercent: Double = 0.0,
    val diffVsAverage: Double = 0.0,
    val evaluationMessage: String = "Veri yok"
)

data class FuelUiState(
    val vehicleProfile: VehicleProfile = VehicleProfile(),
    val fuelLogs: List<FuelLog> = emptyList(),
    val efficiencyLogs: List<EfficiencyLog> = emptyList(),
    val fuelStats: FuelStats = FuelStats(),
    val efficiencyStats: EfficiencyStats = EfficiencyStats(),
    val spendingChartData: List<ChartDataPoint> = emptyList(),
    val efficiencyChartData: List<ChartDataPoint> = emptyList(),
    val selectedPeriod: ReportPeriod = ReportPeriod.MONTHLY,
    val isDarkMode: Boolean = false, // Theme toggle state (defaulting to light style)
    val isScanningReceipt: Boolean = false,
    val scannedReceiptResult: ReceiptScanResult? = null
)

class FuelViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FuelRepository
    private val receiptService = GeminiReceiptService()

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.MONTHLY)
    private val _isDarkMode = MutableStateFlow(false)
    private val _isScanningReceipt = MutableStateFlow(false)
    private val _scannedReceiptResult = MutableStateFlow<ReceiptScanResult?>(null)

    val uiState: StateFlow<FuelUiState>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FuelRepository(database.fuelDao())

        // Ensure default vehicle profile exists
        viewModelScope.launch {
            repository.initializeDefaultIfNeeded()
        }

        val baseFlow = combine(
            repository.vehicleProfile,
            repository.allFuelLogs,
            repository.allEfficiencyLogs,
            _selectedPeriod
        ) { profile, fuelLogs, efficiencyLogs, period ->
            profile to Triple(fuelLogs, efficiencyLogs, period)
        }

        uiState = combine(
            baseFlow,
            _isDarkMode,
            _isScanningReceipt,
            _scannedReceiptResult
        ) { (profile, logsAndPeriod), isDark, scanning, scanResult ->
            val (fuelLogs, efficiencyLogs, period) = logsAndPeriod
            val activeProfile = profile ?: VehicleProfile()
            val stats = calculateFuelStats(fuelLogs, activeProfile)
            val effStats = calculateEfficiencyStats(efficiencyLogs)
            val spendingChart = calculateSpendingChartData(fuelLogs, period)
            val efficiencyChart = calculateEfficiencyChartData(efficiencyLogs)

            FuelUiState(
                vehicleProfile = activeProfile,
                fuelLogs = fuelLogs,
                efficiencyLogs = efficiencyLogs,
                fuelStats = stats,
                efficiencyStats = effStats,
                spendingChartData = spendingChart,
                efficiencyChartData = efficiencyChart,
                selectedPeriod = period,
                isDarkMode = isDark,
                isScanningReceipt = scanning,
                scannedReceiptResult = scanResult
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FuelUiState()
        )
    }

    fun setPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun scanReceipt(bitmap: Bitmap) {
        viewModelScope.launch {
            _isScanningReceipt.value = true
            _scannedReceiptResult.value = null
            try {
                val result = receiptService.scanReceipt(bitmap)
                _scannedReceiptResult.value = result
            } catch (e: Exception) {
                Log.e("FuelViewModel", "Receipt scan failed", e)
                _scannedReceiptResult.value = null
            } finally {
                _isScanningReceipt.value = false
            }
        }
    }

    fun clearScannedReceipt() {
        _scannedReceiptResult.value = null
    }

    // Actions
    fun saveVehicleProfile(name: String, capacity: Double, fuelType: String) {
        viewModelScope.launch {
            repository.saveVehicleProfile(
                VehicleProfile(
                    id = 1,
                    name = name.ifBlank { "Aracım" },
                    tankCapacity = if (capacity > 0) capacity else 50.0,
                    fuelType = fuelType.ifBlank { "Benzin" }
                )
            )
        }
    }

    fun addFuelLog(liters: Double, totalCost: Double, timestamp: Long = System.currentTimeMillis()) {
        if (liters <= 0 || totalCost <= 0) return
        viewModelScope.launch {
            repository.insertFuelLog(
                FuelLog(
                    liters = liters,
                    totalCost = totalCost,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteFuelLog(id: Long) {
        viewModelScope.launch {
            repository.deleteFuelLog(id)
        }
    }

    fun addEfficiencyLog(l100km: Double) {
        if (l100km <= 0) return
        viewModelScope.launch {
            repository.insertEfficiencyLog(
                EfficiencyLog(
                    l100km = l100km,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteEfficiencyLog(id: Long) {
        viewModelScope.launch {
            repository.deleteEfficiencyLog(id)
        }
    }

    // Statistics Calculation Helpers
    private fun calculateFuelStats(logs: List<FuelLog>, profile: VehicleProfile): FuelStats {
        if (logs.isEmpty()) return FuelStats()

        val totalLiters = logs.sumOf { it.liters }
        val totalSpend = logs.sumOf { it.totalCost }
        val avgPricePerLiter = if (totalLiters > 0) totalSpend / totalLiters else 0.0
        val avgLitersPerFill = totalLiters / logs.size
        val avgCostPerFill = totalSpend / logs.size

        val lastLog = logs.first()
        val tankFillPercentage = ((lastLog.liters / profile.tankCapacity) * 100).coerceAtMost(100.0)

        // Calculate a simple price per liter trend
        val trend = if (logs.size >= 2) {
            val latestPrice = lastLog.pricePerLiter
            val previousPrice = logs[1].pricePerLiter
            val diff = latestPrice - previousPrice
            val diffPct = (diff / previousPrice) * 100
            if (diff > 0.05) {
                String.format(Locale.getDefault(), "▲ %s%% (Yükseldi)", String.format(Locale.getDefault(), "%.1f", diffPct))
            } else if (diff < -0.05) {
                String.format(Locale.getDefault(), "▼ %s%% (Düştü)", String.format(Locale.getDefault(), "%.1f", Math.abs(diffPct)))
            } else {
                "▬ Kararlı"
            }
        } else {
            "▬ Veri Az"
        }

        return FuelStats(
            totalLiters = totalLiters,
            totalSpend = totalSpend,
            avgPricePerLiter = avgPricePerLiter,
            avgLitersPerFill = avgLitersPerFill,
            avgCostPerFill = avgCostPerFill,
            tankFillPercentage = tankFillPercentage,
            lastFillLiters = lastLog.liters,
            lastFillCost = lastLog.totalCost,
            priceTrend = trend
        )
    }

    private fun calculateEfficiencyStats(logs: List<EfficiencyLog>): EfficiencyStats {
        if (logs.isEmpty()) return EfficiencyStats()

        val latest = logs.first().l100km
        val previous = if (logs.size >= 2) logs[1].l100km else 0.0
        val avg = logs.map { it.l100km }.average()

        val diffVsPrev = if (previous > 0) latest - previous else 0.0
        val diffVsPrevPercent = if (previous > 0) (diffVsPrev / previous) * 100 else 0.0
        val diffVsAvg = latest - avg

        val evaluation = when {
            previous <= 0 -> "İlk verim kaydedildi. Harika!"
            diffVsPrev < -0.2 -> String.format(Locale.getDefault(), "Tüketiminiz %s%% düştü. Verimlilik arttı!", String.format(Locale.getDefault(), "%.1f", Math.abs(diffVsPrevPercent)))
            diffVsPrev > 0.2 -> String.format(Locale.getDefault(), "Tüketiminiz %s%% arttı. Sürüş tarzınızı kontrol edin.", String.format(Locale.getDefault(), "%.1f", diffVsPrevPercent))
            else -> "Tüketiminiz kararlı seyrediyor."
        }

        return EfficiencyStats(
            latestL100km = latest,
            previousL100km = previous,
            avgL100km = avg,
            diffVsPrevious = diffVsPrev,
            diffVsPreviousPercent = diffVsPrevPercent,
            diffVsAverage = diffVsAvg,
            evaluationMessage = evaluation
        )
    }

    private fun calculateSpendingChartData(logs: List<FuelLog>, period: ReportPeriod): List<ChartDataPoint> {
        if (logs.isEmpty()) return emptyList()

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        return when (period) {
            ReportPeriod.WEEKLY -> {
                // Generate last 7 weeks
                val weekTotals = DoubleArray(7)
                val labels = Array(7) { "" }

                for (i in 0..6) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.WEEK_OF_YEAR, -i)
                    val targetWeek = cal.get(Calendar.WEEK_OF_YEAR)
                    val targetYear = cal.get(Calendar.YEAR)
                    labels[6 - i] = "${6 - i + 1}.Hafta"

                    val weekLogs = logs.filter { log ->
                        val logCal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                        logCal.get(Calendar.WEEK_OF_YEAR) == targetWeek && logCal.get(Calendar.YEAR) == targetYear
                    }
                    weekTotals[6 - i] = weekLogs.sumOf { it.totalCost }
                }

                labels.mapIndexed { idx, label ->
                    ChartDataPoint(label, weekTotals[idx])
                }
            }
            ReportPeriod.MONTHLY -> {
                // Generate last 6 months
                val turkishMonths = arrayOf("Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara")
                val monthTotals = DoubleArray(6)
                val labels = Array(6) { "" }

                for (i in 0..5) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.MONTH, -i)
                    val targetMonth = cal.get(Calendar.MONTH)
                    val targetYear = cal.get(Calendar.YEAR)
                    labels[5 - i] = turkishMonths[targetMonth]

                    val monthLogs = logs.filter { log ->
                        val logCal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                        logCal.get(Calendar.MONTH) == targetMonth && logCal.get(Calendar.YEAR) == targetYear
                    }
                    monthTotals[5 - i] = monthLogs.sumOf { it.totalCost }
                }

                labels.mapIndexed { idx, label ->
                    ChartDataPoint(label, monthTotals[idx])
                }
            }
            ReportPeriod.YEARLY -> {
                // Generate last 5 years
                val yearTotals = DoubleArray(5)
                val labels = Array(5) { "" }

                for (i in 0..4) {
                    val targetYear = currentYear - (4 - i)
                    labels[i] = targetYear.toString()

                    val yearLogs = logs.filter { log ->
                        val logCal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                        logCal.get(Calendar.YEAR) == targetYear
                    }
                    yearTotals[i] = yearLogs.sumOf { it.totalCost }
                }

                labels.mapIndexed { idx, label ->
                    ChartDataPoint(label, yearTotals[idx])
                }
            }
        }
    }

    private fun calculateEfficiencyChartData(logs: List<EfficiencyLog>): List<ChartDataPoint> {
        if (logs.isEmpty()) return emptyList()

        // Get the last 6 entries in chronological order to plot progress
        val recentLogs = logs.take(6).reversed()
        return recentLogs.mapIndexed { index, log ->
            val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = cal.get(Calendar.MONTH) + 1
            ChartDataPoint(
                label = "$day/$month",
                value = log.l100km
            )
        }
    }
}
