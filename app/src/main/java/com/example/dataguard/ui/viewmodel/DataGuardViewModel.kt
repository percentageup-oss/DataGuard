package com.example.dataguard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dataguard.DataGuardApp
import com.example.dataguard.data.model.AppUsageItem
import com.example.dataguard.data.model.DailySnapshot
import com.example.dataguard.data.model.DataPlan
import com.example.dataguard.data.network.NetworkStatsHelper
import com.example.dataguard.data.repository.BillingCycleStats
import com.example.dataguard.service.SpeedMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ScreenTab {
    DASHBOARD,
    TOP_APPS,
    HISTORY,
    PLAN_SETUP
}

enum class AppFilter {
    MOBILE,
    WIFI,
    BOTH
}

enum class AppSort {
    TOTAL,
    FOREGROUND,
    BACKGROUND
}

class DataGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DataGuardApp
    private val repository = app.repository
    private val planPrefs = app.planPreferences

    val planFlow: StateFlow<DataPlan> = repository.planFlow

    val snapshots: StateFlow<List<DailySnapshot>> = repository.allSnapshots
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentTab = MutableStateFlow(ScreenTab.DASHBOARD)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    private val _isPermissionGranted = MutableStateFlow(repository.hasUsagePermission())
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val _billingStats = MutableStateFlow(BillingCycleStats())
    val billingStats: StateFlow<BillingCycleStats> = _billingStats.asStateFlow()

    private val _appList = MutableStateFlow<List<AppUsageItem>>(emptyList())
    val appList: StateFlow<List<AppUsageItem>> = _appList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Filters for Top Apps
    private val _appFilter = MutableStateFlow(AppFilter.MOBILE)
    val appFilter: StateFlow<AppFilter> = _appFilter.asStateFlow()

    private val _appSort = MutableStateFlow(AppSort.TOTAL)
    val appSort: StateFlow<AppSort> = _appSort.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        checkPermissionAndLoad()
    }

    fun selectTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun setAppFilter(filter: AppFilter) {
        _appFilter.value = filter
    }

    fun setAppSort(sort: AppSort) {
        _appSort.value = sort
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun checkPermissionAndLoad() {
        val granted = repository.hasUsagePermission()
        _isPermissionGranted.value = granted
        if (granted) {
            refreshAll()
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isLoading.value = true
            val plan = planPrefs.loadPlan()

            // 1. Record today's snapshot in Room
            repository.recordTodaySnapshot()

            // 2. Fetch billing cycle stats
            val stats = repository.fetchBillingCycleStats(plan)
            _billingStats.value = stats

            // 3. Fetch app breakdown for current billing cycle
            val apps = withContext(Dispatchers.IO) {
                NetworkStatsHelper.getAppUsageBreakdown(
                    getApplication(),
                    stats.cycleStartMillis,
                    System.currentTimeMillis()
                )
            }
            _appList.value = apps

            _isLoading.value = false
        }
    }

    fun savePlan(plan: DataPlan) {
        viewModelScope.launch {
            repository.savePlan(plan)
            refreshAll()
        }
    }

    fun toggleLiveSpeed(enabled: Boolean) {
        viewModelScope.launch {
            repository.setLiveSpeedEnabled(enabled)
            val context = getApplication<Application>()
            if (enabled) {
                SpeedMonitorService.start(context)
            } else {
                SpeedMonitorService.stop(context)
            }
        }
    }

    fun exportCsvReport(onReady: (File) -> Unit) {
        viewModelScope.launch {
            val list = snapshots.value
            val file = repository.exportUsageReportCsv(list)
            onReady(file)
        }
    }
}
