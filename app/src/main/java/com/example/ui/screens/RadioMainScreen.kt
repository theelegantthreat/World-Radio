package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.db.StationEntity
import com.example.player.PlaybackState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.viewmodel.RadioViewModel

// Aesthetic Immersive UI Color Palette
val ImmersiveDeepBg = Color(0xFF1C1B1F)
val ImmersiveCardBg = Color(0xFF2B2930)
val ImmersiveLavender = Color(0xFFD0BCFF)
val ImmersiveViolet = Color(0xFF381E72)
val ImmersiveDarkIndigo = Color(0xFF21005D)
val ImmersiveLightPurple = Color(0xFFE8DEF8)
val ImmersiveDarkPurpleText = Color(0xFF1D192B)
val ImmersiveMutedText = Color(0xFF938F99)
val ImmersiveSubheadingText = Color(0xFFCAC4D0)

val CosmicDeepNavy = ImmersiveDeepBg
val CosmicMidNavy = ImmersiveCardBg
val CosmicBrightBlue = ImmersiveViolet
val CyberTeal = ImmersiveLavender
val ActiveGreen = Color(0xFF00E676)
val SoftTextMuted = ImmersiveMutedText
val CardBackground = ImmersiveCardBg
val BorderCyan = ImmersiveLavender.copy(alpha = 0.25f)

@Composable
fun RadioMainScreen(
    viewModel: RadioViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val countryQuery by viewModel.countryQuery.collectAsState()
    val tagQuery by viewModel.tagQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val popularStations by viewModel.popularStations.collectAsState()
    val favoriteStations by viewModel.favoriteStations.collectAsState()
    val recentStations by viewModel.recentStations.collectAsState()
    val trendingStations by viewModel.trendingStations.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val currentTrackTitle by viewModel.currentTrackTitle.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Explore, 1: Favorites & History
    var showSearchFilters by remember { mutableStateOf(false) }
    var isPlayerMinimized by remember { mutableStateOf(false) }
    var isPlayerClosed by remember { mutableStateOf(false) }

    LaunchedEffect(playbackState) {
        if (playbackState is PlaybackState.Playing || playbackState is PlaybackState.Loading) {
            isPlayerClosed = false
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonString = viewModel.exportBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                    }
                    android.widget.Toast.makeText(context, "Backup exported successfully!", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "Failed to export backup", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    }
                    if (jsonString != null) {
                        val success = viewModel.importBackupJson(jsonString)
                        if (success) {
                            android.widget.Toast.makeText(context, "Backup imported successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Invalid backup format", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Failed to read backup file", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "Error importing backup: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveDeepBg),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ImmersiveDeepBg)
                .padding(innerPadding)
        ) {
            val scrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()
            val bottomPaddingValue = if (isPlayerClosed) 16.dp else if (isPlayerMinimized) 80.dp else 180.dp

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 8.dp)
                    .padding(bottom = bottomPaddingValue)
            ) {
                // Main Content Column (Scrollable)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                        .padding(end = 8.dp)
                ) {
                    // Top Header Section
                    HeaderSection(
                        onExportClick = {
                            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            val currentDateString = dateFormat.format(java.util.Date())
                            exportLauncher.launch("backup-worldradio-$currentDateString.json")
                        },
                        onImportClick = {
                            importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                        },
                        onRecordingsClick = {
                            activeTab = 2
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom Segmented Capsule Tabs
                    TabSelector(
                        activeTab = activeTab,
                        onTabSelected = { activeTab = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when (activeTab) {
                        0 -> {
                            // DISCOVER / EXPLORE TAB
                            ExploreTab(
                                searchQuery = searchQuery,
                                countryQuery = countryQuery,
                                tagQuery = tagQuery,
                                isLoading = isLoading,
                                searchResults = searchResults,
                                popularStations = popularStations,
                                trendingStations = trendingStations,
                                showFilters = showSearchFilters,
                                onToggleFilters = { showSearchFilters = !showSearchFilters },
                                onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                                onCountryQueryChanged = { viewModel.setCountryQuery(it) },
                                onTagQueryChanged = { viewModel.setTagQuery(it) },
                                onTriggerSearch = {
                                    keyboardController?.hide()
                                    viewModel.search()
                                },
                                onQuickCountrySearch = {
                                    viewModel.quickSearchByCountry(it)
                                    showSearchFilters = false
                                },
                                onQuickTagSearch = {
                                    viewModel.quickSearchByTag(it)
                                    showSearchFilters = false
                                },
                                onClearSearch = { viewModel.clearSearch() },
                                onStationSelected = { viewModel.playStation(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                favoriteStations = favoriteStations
                            )
                        }
                        1 -> {
                            // MY TUNES TAB (Favorites & History)
                            MyTunesTab(
                                favoriteStations = favoriteStations,
                                recentStations = recentStations,
                                onStationSelected = { viewModel.playStation(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) }
                            )
                        }
                        2 -> {
                            // RECORDINGS TAB
                            RecordingsTab(viewModel = viewModel)
                        }
                    }
                }

                // Vertical scrollbar with up and down arrows on the right side
                Column(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Scroll Up Arrow Button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val target = (scrollState.value - 300).coerceAtLeast(0)
                                scrollState.animateScrollTo(target)
                            }
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .background(CyberTeal.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(1.dp, CyberTeal.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Scroll Up",
                            tint = CyberTeal,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Scroll Bar Track and Dynamic Thumb
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(6.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(3.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        val totalScrollable = scrollState.maxValue.toFloat()
                        if (totalScrollable > 0f) {
                            val scrollFraction = scrollState.value.toFloat() / totalScrollable
                            // Position the thumb dynamically
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.15f) // Thumb is 15% of track height
                                    .align(androidx.compose.ui.BiasAlignment(0f, scrollFraction * 2f - 1f))
                                    .background(CyberTeal, RoundedCornerShape(3.dp))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                            )
                        } else {
                            // Non-scrollable default state thumb
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.15f)
                                    .background(CyberTeal.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Scroll Down Arrow Button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val target = (scrollState.value + 300).coerceAtMost(scrollState.maxValue)
                                scrollState.animateScrollTo(target)
                            }
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .background(CyberTeal.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(1.dp, CyberTeal.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll Down",
                            tint = CyberTeal,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (!isPlayerClosed) {
                // Beautiful Floating Radio digital receiver widget at bottom
                PlaybackWidget(
                    playbackState = playbackState,
                    volume = volume,
                    currentTrackTitle = currentTrackTitle,
                    onVolumeChanged = { viewModel.setVolume(it) },
                    onTogglePlayPause = {
                        val current = viewModel.playerManager.currentStation
                        if (current != null) {
                            viewModel.playerManager.togglePlayPause()
                        } else {
                            val fallback = viewModel.getActiveStationsList().firstOrNull()
                            if (fallback != null) {
                                viewModel.playStation(fallback)
                            }
                        }
                    },
                    onStop = { viewModel.playerManager.stop() },
                    onSeekForward = { viewModel.seekNext() },
                    onSeekBackward = { viewModel.seekPrev() },
                    onTuneToStation = { viewModel.playStation(it) },
                    onToggleFavorite = { station ->
                        viewModel.toggleFavorite(station)
                    },
                    favoriteStations = favoriteStations,
                    allStations = viewModel.getActiveStationsList(),
                    popularStations = popularStations,
                    recentStations = recentStations,
                    isMinimized = isPlayerMinimized,
                    onMinimizeToggle = { isPlayerMinimized = !isPlayerMinimized },
                    onCloseClick = {
                        viewModel.playerManager.stop()
                        isPlayerClosed = true
                    },
                    viewModel = viewModel,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding()
                )
            }
        }
    }
}

@Composable
fun HeaderSection(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onRecordingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var menuExpanded by remember { mutableStateOf(false) }

        Box(modifier = Modifier.padding(end = 4.dp)) {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Main Menu",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(ImmersiveCardBg)
            ) {
                DropdownMenuItem(
                    text = { Text("Export JSON", color = Color.White) },
                    onClick = {
                        menuExpanded = false
                        onExportClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export JSON",
                            tint = CyberTeal
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Import JSON", color = Color.White) },
                    onClick = {
                        menuExpanded = false
                        onImportClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "Import JSON",
                            tint = CyberTeal
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Recordings", color = Color.White) },
                    onClick = {
                        menuExpanded = false
                        onRecordingsClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Recordings",
                            tint = CyberTeal
                        )
                    }
                )
            }
        }

        // App Launcher Icon or high-fidelity image
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ImmersiveLavender)
                .padding(2.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_radio_logo),
                contentDescription = "World Radio Icon",
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = "WORLD RADIO",
                fontSize = 22.sp,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.5.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(CyberTeal)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "High-Fidelity Tuner Online",
                    fontSize = 11.sp,
                    color = CyberTeal,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun TabSelector(
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50.dp))
            .background(ImmersiveCardBg)
            .padding(4.dp)
    ) {
        // Tab 0
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(50.dp))
                .let { modifier ->
                    if (activeTab == 0) {
                        modifier.background(ImmersiveLightPurple)
                    } else {
                        modifier.background(Color.Transparent)
                    }
                }
                .clickable { onTabSelected(0) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = if (activeTab == 0) ImmersiveDarkPurpleText else ImmersiveMutedText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Discover",
                    color = if (activeTab == 0) ImmersiveDarkPurpleText else ImmersiveMutedText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Tab 1
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(50.dp))
                .let { modifier ->
                    if (activeTab == 1) {
                        modifier.background(ImmersiveLightPurple)
                    } else {
                        modifier.background(Color.Transparent)
                    }
                }
                .clickable { onTabSelected(1) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = if (activeTab == 1) ImmersiveDarkPurpleText else ImmersiveMutedText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "My Radio",
                    color = if (activeTab == 1) ImmersiveDarkPurpleText else ImmersiveMutedText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ExploreTab(
    searchQuery: String,
    countryQuery: String,
    tagQuery: String,
    isLoading: Boolean,
    searchResults: List<StationEntity>,
    popularStations: List<StationEntity>,
    trendingStations: List<StationEntity>,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onCountryQueryChanged: (String) -> Unit,
    onTagQueryChanged: (String) -> Unit,
    onTriggerSearch: () -> Unit,
    onQuickCountrySearch: (String) -> Unit,
    onQuickTagSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onStationSelected: (StationEntity) -> Unit,
    onToggleFavorite: (StationEntity) -> Unit,
    favoriteStations: List<StationEntity>
) {
    val quickCountries = listOf(
        "Australia" to "🇦🇺",
        "Brazil" to "🇧🇷",
        "Canada" to "🇨🇦",
        "France" to "🇫🇷",
        "Germany" to "🇩🇪",
        "Japan" to "🇯🇵",
        "Mexico" to "🇲🇽",
        "Puerto Rico" to "🇵🇷",
        "South Korea" to "🇰🇷",
        "Spain" to "🇪🇸",
        "United Kingdom" to "🇬🇧",
        "United States" to "🇺🇸"
    )

    val quickTags = listOf(
        "Chillout", "Classical", "Dance", "Electronic", "Jazz", "News", "Pop", "Rock"
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Search & Filter Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderCyan, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                // Top row with search field and filter expand button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = { Text("Search station name...", color = SoftTextMuted, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SoftTextMuted) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CosmicDeepNavy,
                            unfocusedContainerColor = CosmicDeepNavy,
                            disabledContainerColor = CosmicDeepNavy,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = CyberTeal,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onTriggerSearch() }),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onToggleFilters,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (showFilters) CosmicBrightBlue else CosmicDeepNavy)
                            .border(1.dp, CyberTeal.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filters",
                            tint = if (showFilters) CyberTeal else Color.White
                        )
                    }
                }

                // Expandable Location and Tag Filters
                AnimatedVisibility(visible = showFilters) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            text = "Advanced Search / Discovered Filters",
                            fontSize = 12.sp,
                            color = CyberTeal,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextField(
                                value = countryQuery,
                                onValueChange = onCountryQueryChanged,
                                placeholder = { Text("Country...", color = SoftTextMuted, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, tint = SoftTextMuted, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = CosmicDeepNavy,
                                    unfocusedContainerColor = CosmicDeepNavy,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = CyberTeal,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { onTriggerSearch() }),
                                shape = RoundedCornerShape(10.dp)
                            )

                            TextField(
                                value = tagQuery,
                                onValueChange = onTagQueryChanged,
                                placeholder = { Text("Genre...", color = SoftTextMuted, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null, tint = SoftTextMuted, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = CosmicDeepNavy,
                                    unfocusedContainerColor = CosmicDeepNavy,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = CyberTeal,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { onTriggerSearch() }),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onClearSearch) {
                                Text("Clear", color = SoftTextMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onTriggerSearch,
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicBrightBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Apply Search", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Country Location Discover Bar
        Text(
            text = "Easy Geographical Discovery",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickCountries) { (country, flag) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            if (countryQuery.lowercase() == country.lowercase()) CosmicBrightBlue
                            else CosmicBrightBlue.copy(alpha = 0.3f)
                        )
                        .border(
                            1.dp,
                            if (countryQuery.lowercase() == country.lowercase()) CyberTeal else Color.Transparent,
                            RoundedCornerShape(30.dp)
                        )
                        .clickable { onQuickCountrySearch(country) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = flag, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = country,
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Tag Genre Discover Bar
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickTags) { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            if (tagQuery.lowercase() == tag.lowercase()) CosmicBrightBlue
                            else CosmicBrightBlue.copy(alpha = 0.2f)
                        )
                        .border(
                            1.dp,
                            if (tagQuery.lowercase() == tag.lowercase()) CyberTeal else Color.Transparent,
                            RoundedCornerShape(30.dp)
                        )
                        .clickable { onQuickTagSearch(tag) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "#$tag",
                        fontSize = 12.sp,
                        color = if (tagQuery.lowercase() == tag.lowercase()) CyberTeal else SoftTextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (searchResults.isEmpty() && trendingStations.isNotEmpty()) {
            TrendingNowSection(
                trendingStations = trendingStations,
                favoriteStations = favoriteStations,
                onStationSelected = onStationSelected,
                onToggleFavorite = onToggleFavorite
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Interactive Station Header
        val displayTitle = if (searchResults.isNotEmpty()) "Search Results (${searchResults.size})" else "Worldwide Hits (Hot Radio)"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayTitle,
                fontSize = 15.sp,
                color = SoftTextMuted,
                fontWeight = FontWeight.Medium
            )
            if (searchResults.isNotEmpty()) {
                Text(
                    text = "Clear Results",
                    fontSize = 13.sp,
                    color = CyberTeal,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onClearSearch() }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main List Content
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyberTeal)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Tuning into worldwide waves...", color = SoftTextMuted, fontSize = 13.sp)
                }
            }
        } else {
            val listToDisplay = if (searchResults.isNotEmpty()) searchResults else popularStations
            
            if (listToDisplay.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = SoftTextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No stations found. Try refining search query.", color = SoftTextMuted, fontSize = 14.sp)
                    }
                }
            } else {
                val favoriteIds = favoriteStations.map { it.stationuuid }.toSet()
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listToDisplay.forEach { station ->
                        val isFavorited = favoriteIds.contains(station.stationuuid)
                        StationItemRow(
                            station = station,
                            isFavorited = isFavorited,
                            onPlayClick = { onStationSelected(station) },
                            onFavoriteClick = { onToggleFavorite(station) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyTunesTab(
    favoriteStations: List<StationEntity>,
    recentStations: List<StationEntity>,
    onStationSelected: (StationEntity) -> Unit,
    onToggleFavorite: (StationEntity) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FAVORITES SECTION
        Text(
            text = "Pinned Stations (${favoriteStations.size})",
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (favoriteStations.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = SoftTextMuted, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No pinned stations yet. Pin radio stations from the Explore tab!",
                        color = SoftTextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }
        } else {
            favoriteStations.forEach { station ->
                StationItemRow(
                    station = station,
                    isFavorited = true,
                    onPlayClick = { onStationSelected(station) },
                    onFavoriteClick = { onToggleFavorite(station) }
                )
            }
        }

        // HISTORY RECENTS SECTION
        Text(
            text = "Recently Listened",
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
        )

        if (recentStations.isEmpty()) {
            Text(
                text = "Your listened history will appear here.",
                color = SoftTextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        } else {
            recentStations.forEach { station ->
                val isFavorited = favoriteStations.any { it.stationuuid == station.stationuuid }
                StationItemRow(
                    station = station,
                    isFavorited = isFavorited,
                    onPlayClick = { onStationSelected(station) },
                    onFavoriteClick = { onToggleFavorite(station) }
                )
            }
        }
    }
}

@Composable
fun StationItemRow(
    station: StationEntity,
    isFavorited: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    // Quality indicators: High quality is bitrate >= 96 kbps
    val isHighQuality = station.bitrate >= 128

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isHighQuality) CyberTeal.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPlayClick() }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Station favicon logo
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CosmicDeepNavy)
            ) {
                if (station.favicon.isNotBlank()) {
                    AsyncImage(
                        model = station.favicon,
                        contentDescription = station.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.ic_radio_logo),
                        placeholder = painterResource(id = R.drawable.ic_radio_logo)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_radio_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                // Small audio visual quality badge
                if (isHighQuality) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(CyberTeal, RoundedCornerShape(bottomEnd = 6.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("HQ", fontSize = 8.sp, color = CosmicDeepNavy, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = station.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = SoftTextMuted,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = if (station.state.isNotBlank()) "${station.state}, ${station.country}" else station.country,
                        color = SoftTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (station.bitrate > 0) {
                        Text(
                            text = "${station.bitrate} kbps ${station.codec}",
                            color = if (isHighQuality) CyberTeal else SoftTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (station.tags.isNotBlank()) {
                        val finalTags = station.tags.split(",").take(2).joinToString(" #")
                        Text(
                            text = "#$finalTags",
                            color = SoftTextMuted.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons (Play & Pins)
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Pin Favorite",
                    tint = if (isFavorited) Color.Red else SoftTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(CyberTeal.copy(alpha = 0.1f))
                    .border(1.dp, CyberTeal.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = CyberTeal,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PlaybackWidget(
    playbackState: PlaybackState,
    volume: Float,
    currentTrackTitle: String?,
    onVolumeChanged: (Float) -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onTuneToStation: (StationEntity) -> Unit,
    onToggleFavorite: (StationEntity) -> Unit,
    favoriteStations: List<StationEntity>,
    allStations: List<StationEntity>,
    popularStations: List<StationEntity>,
    recentStations: List<StationEntity>,
    isMinimized: Boolean,
    onMinimizeToggle: () -> Unit,
    onCloseClick: () -> Unit,
    viewModel: RadioViewModel,
    modifier: Modifier = Modifier
) {
    val activeStation = when (playbackState) {
        is PlaybackState.Loading -> playbackState.station
        is PlaybackState.Playing -> playbackState.station
        is PlaybackState.Paused -> playbackState.station
        is PlaybackState.Error -> playbackState.station
        else -> null
    } ?: popularStations.firstOrNull() ?: recentStations.firstOrNull() ?: favoriteStations.firstOrNull()

    if (activeStation == null) return

    val isFavorited = favoriteStations.any { it.stationuuid == activeStation.stationuuid }

    val isRecording by viewModel.recordingManager.isRecording.collectAsState()
    val recordingDuration by viewModel.recordingManager.recordingDuration.collectAsState()
    val context = LocalContext.current

    // Stream elapsed play time
    var activeSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(playbackState) {
        if (playbackState is PlaybackState.Playing) {
            while (true) {
                delay(1000)
                activeSeconds += 1
            }
        } else if (playbackState !is PlaybackState.Paused) {
            activeSeconds = 0
        }
    }

    val minutes = activeSeconds / 60
    val seconds = activeSeconds % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    if (isMinimized) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .shadow(16.dp, RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(ImmersiveViolet, ImmersiveDarkIndigo)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(1.dp, ImmersiveLavender.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlowingLogo(
                        isPlaying = playbackState is PlaybackState.Playing,
                        favicon = activeStation.favicon,
                        stationName = activeStation.name,
                        size = 36.dp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = activeStation.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!currentTrackTitle.isNullOrBlank()) {
                            Text(
                                text = "🎵 $currentTrackTitle",
                                color = CyberTeal,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (playbackState is PlaybackState.Playing) ActiveGreen 
                                            else if (playbackState is PlaybackState.Loading) CyberTeal 
                                            else SoftTextMuted
                                        )
                                )
                                Text(
                                    text = when (playbackState) {
                                        is PlaybackState.Playing -> "LIVE • PLAYING"
                                        is PlaybackState.Loading -> "BUFFERING..."
                                        is PlaybackState.Paused -> "PAUSED"
                                        else -> "TUNER READY"
                                    },
                                    color = if (playbackState is PlaybackState.Playing) ActiveGreen else CyberTeal.copy(alpha = 0.8f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (playbackState is PlaybackState.Playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play or Pause",
                            tint = CyberTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onToggleFavorite(activeStation) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Pin Favorite",
                            tint = if (isFavorited) Color.Red else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onMinimizeToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Maximize Panel",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onCloseClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Panel",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    } else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(ImmersiveViolet, ImmersiveDarkIndigo)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(1.dp, ImmersiveLavender.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                // Console Deck header with minimize & close controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyberTeal)
                        )
                        Text(
                            text = "CONSOLE DECK",
                            color = CyberTeal.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onMinimizeToggle,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Minimize Panel",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onCloseClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Panel",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spinning vinyl/glowing logo
                GlowingLogo(
                    isPlaying = playbackState is PlaybackState.Playing,
                    favicon = activeStation.favicon,
                    stationName = activeStation.name
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = activeStation.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!currentTrackTitle.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Track Title",
                                tint = CyberTeal,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = currentTrackTitle,
                                color = CyberTeal,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activeStation.state.isNotBlank()) "${activeStation.state}, ${activeStation.country}" else activeStation.country,
                            color = SoftTextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Live tuning state indicator with real-time stream clock
                    val stateText = when {
                        isRecording -> "REC • ${formatDurationSec(recordingDuration)} • ${activeStation.bitrate} KBPS"
                        playbackState is PlaybackState.Loading -> "TUNING WAVE / BUFFERING..."
                        playbackState is PlaybackState.Playing -> "LIVE • $timeString • ${activeStation.bitrate} KBPS"
                        playbackState is PlaybackState.Paused -> "STATION PAUSED"
                        playbackState is PlaybackState.Error -> "STREAM CONNECTION ERROR"
                        else -> "TUNER DECK READY"
                    }

                    val stateColor = when {
                        isRecording -> Color.Red
                        playbackState is PlaybackState.Loading -> CyberTeal
                        playbackState is PlaybackState.Playing -> ActiveGreen
                        playbackState is PlaybackState.Paused -> SoftTextMuted
                        playbackState is PlaybackState.Error -> Color.Red
                        else -> CyberTeal.copy(alpha = 0.6f)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Flashing LED
                        if (playbackState is PlaybackState.Playing || isRecording) {
                            val pulseTransition = rememberInfiniteTransition(label = "pulse")
                            val alpha by pulseTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(stateColor.copy(alpha = alpha))
                              )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(stateColor)
                            )
                        }

                        Text(
                            text = stateText,
                            color = stateColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopRecording(context)
                            } else {
                                if (playbackState is PlaybackState.Playing) {
                                    viewModel.startRecording(context)
                                } else {
                                    android.widget.Toast.makeText(context, "Play a live station first to record!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                            tint = if (isRecording) Color.Red else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { onToggleFavorite(activeStation) }
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Pin Favorite",
                            tint = if (isFavorited) Color.Red else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // The gorgeous retro tuner dial
            RetroTunerDial(
                activeStation = activeStation,
                onTuneToStation = onTuneToStation,
                allStations = allStations,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Discrete Control buttons row:
            // back arrow, stop, play, pause, seek forward
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Seek Backward Arrow Button
                IconButton(
                    onClick = onSeekBackward,
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, CyberTeal.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Seek Backward",
                        tint = CyberTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Stop Button
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, CyberTeal.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Stream",
                        tint = CyberTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Play Button
                IconButton(
                    onClick = {
                        if (playbackState !is PlaybackState.Playing) {
                            onTogglePlayPause()
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (playbackState is PlaybackState.Playing) CyberTeal.copy(alpha = 0.2f) else CyberTeal,
                            CircleShape
                        )
                        .border(1.5.dp, CyberTeal, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (playbackState is PlaybackState.Playing) CyberTeal else ImmersiveDarkPurpleText,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Pause Button
                IconButton(
                    onClick = {
                        if (playbackState is PlaybackState.Playing) {
                            onTogglePlayPause()
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (playbackState is PlaybackState.Paused) CyberTeal.copy(alpha = 0.2f) else Color.Transparent,
                            CircleShape
                        )
                        .border(1.5.dp, CyberTeal, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = CyberTeal,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Seek Forward Arrow Button
                IconButton(
                    onClick = onSeekForward,
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, CyberTeal.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Seek Forward",
                        tint = CyberTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Beautiful Immersive Volume Slider Section
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = if (volume == 0f) Icons.Default.VolumeOff else if (volume < 0.5f) Icons.Default.VolumeDown else Icons.Default.VolumeUp,
                    contentDescription = "Volume State",
                    tint = CyberTeal,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Slider(
                    value = volume,
                    onValueChange = onVolumeChanged,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberTeal,
                        activeTrackColor = CyberTeal,
                        inactiveTrackColor = CyberTeal.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${(volume * 100).toInt()}%",
                    color = CyberTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.End
                )
            }

            // Beautiful Digital Audio Spectrum wave animation directly during active playback!
            AnimatedVisibility(visible = playbackState is PlaybackState.Playing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = CyberTeal.copy(alpha = 0.15f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    AudioSpectrumVisualizer()
                }
            }

            // Quick display of connection errors
            if (playbackState is PlaybackState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = playbackState.message,
                    color = Color.Red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
}

@Composable
fun GlowingLogo(
    isPlaying: Boolean,
    favicon: String,
    stationName: String,
    size: androidx.compose.ui.unit.Dp = 44.dp
) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotation.stop()
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, CyberTeal, CircleShape)
            .shadow(if (isPlaying) 10.dp else 0.dp, CircleShape, spotColor = CyberTeal)
    ) {
        if (favicon.isNotBlank()) {
            AsyncImage(
                model = favicon,
                contentDescription = stationName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.ic_radio_logo),
                placeholder = painterResource(id = R.drawable.ic_radio_logo)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_radio_logo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun AudioSpectrumVisualizer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        val barsCount = 35
        val infiniteTransition = rememberInfiniteTransition(label = "spectrum")

        for (i in 0 until barsCount) {
            // Give each audio bar a slightly staggered animation rhythm
            val duration = (400..1000).random()
            val startDelay = (0..300).random()
            val heightPercent by infiniteTransition.animateFloat(
                initialValue = 0.1f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, delayMillis = startDelay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 1.dp)
                    .fillMaxHeight(heightPercent)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(CyberTeal, CosmicBrightBlue)
                        ),
                        shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                    )
            )
        }
    }
}

fun getStationFrequencyAndBand(station: StationEntity): Pair<Float, String> {
    // 1. Try to find decimal numbers in the name (e.g. 101.1, 95.3) -> FM
    val fmRegex = """\b(8[7-9]\d?|9\d|10\d)\.(\d)\b""".toRegex()
    val fmMatch = fmRegex.find(station.name)
    if (fmMatch != null) {
        val freq = fmMatch.value.toFloatOrNull()
        if (freq != null && freq in 87.5f..108.0f) {
            return Pair(freq, "FM")
        }
    }

    // 2. Try to find whole numbers in the name (e.g. 540, 1010) -> AM
    val amRegex = """\b([5-9]\d{2}|1[0-6]\d{2})\b""".toRegex()
    val amMatch = amRegex.find(station.name)
    if (amMatch != null) {
        val freq = amMatch.value.toFloatOrNull()
        if (freq != null && freq in 530f..1700f) {
            return Pair(freq, "AM")
        }
    }

    // 3. Fallback: Deterministic hashing of the stationuuid
    val hash = station.stationuuid.hashCode().coerceAtLeast(0)
    val isFm = hash % 2 == 0
    return if (isFm) {
        val range = 108.0f - 87.5f
        val steps = (range / 0.1f).toInt()
        val step = hash % steps
        val freq = 87.5f + step * 0.1f
        Pair(freq, "FM")
    } else {
        val range = 1700f - 530f
        val steps = (range / 10f).toInt()
        val step = hash % steps
        val freq = 530f + step * 10f
        Pair(freq, "AM")
    }
}

@Composable
fun RetroTunerDial(
    activeStation: StationEntity?,
    onTuneToStation: (StationEntity) -> Unit,
    allStations: List<StationEntity>,
    modifier: Modifier = Modifier
) {
    val currentPair = activeStation?.let { getStationFrequencyAndBand(it) } ?: Pair(98.0f, "FM")
    val (stationFreq, stationBand) = currentPair

    val targetFraction = remember(stationFreq, stationBand) {
        if (stationBand == "FM") {
            (stationFreq - 87.5f) / (108.0f - 87.5f)
        } else {
            (stationFreq - 530f) / (1700f - 530f)
        }.coerceIn(0f, 1f)
    }

    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "needle_position"
    )

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(targetFraction) }

    val needleFraction = if (isDragging) dragFraction else animatedFraction

    val closestStation = remember(needleFraction, allStations) {
        if (allStations.isEmpty()) null
        else {
            allStations.minByOrNull { station ->
                val (freq, band) = getStationFrequencyAndBand(station)
                val frac = if (band == "FM") {
                    (freq - 87.5f) / (108.0f - 87.5f)
                } else {
                    (freq - 530f) / (1700f - 530f)
                }.coerceIn(0f, 1f)
                kotlin.math.abs(frac - needleFraction)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF070707))
            .border(1.5.dp, CyberTeal.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .pointerInput(allStations) {
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        dragFraction = fraction
                        tryAwaitRelease()
                        isDragging = false
                        closestStation?.let { onTuneToStation(it) }
                    }
                )
            }
            .pointerInput(allStations) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        closestStation?.let { onTuneToStation(it) }
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val containerWidth = size.width
                        if (containerWidth > 0) {
                            val deltaFrac = dragAmount.x / containerWidth
                            dragFraction = (dragFraction + deltaFrac).coerceIn(0f, 1f)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val centerY = h / 2f
            drawLine(
                color = CyberTeal.copy(alpha = 0.6f),
                start = androidx.compose.ui.geometry.Offset(0f, centerY),
                end = androidx.compose.ui.geometry.Offset(w, centerY),
                strokeWidth = 2f
            )

            val totalTicks = 50
            for (i in 0..totalTicks) {
                val x = (i.toFloat() / totalTicks) * w
                val isMajor = i % 5 == 0
                val tickHeight = if (isMajor) 14f else 7f
                val tickColor = if (isMajor) CyberTeal.copy(alpha = 0.8f) else CyberTeal.copy(alpha = 0.4f)
                
                drawLine(
                    color = tickColor,
                    start = androidx.compose.ui.geometry.Offset(x, centerY - tickHeight / 2f),
                    end = androidx.compose.ui.geometry.Offset(x, centerY + tickHeight / 2f),
                    strokeWidth = if (isMajor) 2f else 1f
                )
            }

            val needleX = needleFraction * w
            val needleColor = Color(0xFFFF5722)
            val needleGlowColor = Color(0xFFFF9800)

            drawLine(
                color = needleGlowColor.copy(alpha = 0.15f),
                start = androidx.compose.ui.geometry.Offset(needleX, 0f),
                end = androidx.compose.ui.geometry.Offset(needleX, h),
                strokeWidth = 14f
            )
            drawLine(
                color = needleColor.copy(alpha = 0.4f),
                start = androidx.compose.ui.geometry.Offset(needleX, 0f),
                end = androidx.compose.ui.geometry.Offset(needleX, h),
                strokeWidth = 6f
            )
            drawLine(
                color = Color.White,
                start = androidx.compose.ui.geometry.Offset(needleX, 0f),
                end = androidx.compose.ui.geometry.Offset(needleX, h),
                strokeWidth = 2f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AM",
                    color = CyberTeal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodySmall.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(CyberTeal.copy(alpha = 0.5f), blurRadius = 4f)
                    )
                )
                listOf("54", "60", "70", "80", "100", "120", "140", "160").forEach { label ->
                    Text(
                        text = label,
                        color = CyberTeal.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(CyberTeal.copy(alpha = 0.3f), blurRadius = 2f)
                        )
                    )
                }
                Text(
                    text = "x10kHz",
                    color = CyberTeal.copy(alpha = 0.6f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isDragging && closestStation != null) {
                    val closestPair = getStationFrequencyAndBand(closestStation)
                    Text(
                        text = "RELEASE TO TUNE: ${closestPair.first} ${closestPair.second} - ${closestStation.name.take(24)}...",
                        color = Color(0xFFFFB74D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (activeStation != null) {
                    val activePair = getStationFrequencyAndBand(activeStation)
                    Text(
                        text = "RECEIVING: ${activePair.first} ${activePair.second} (${activeStation.countrycode})",
                        color = CyberTeal.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FM",
                    color = CyberTeal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodySmall.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(CyberTeal.copy(alpha = 0.5f), blurRadius = 4f)
                    )
                )
                listOf("88", "90", "92", "94", "96", "98", "102", "106", "108").forEach { label ->
                    Text(
                        text = label,
                        color = CyberTeal.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(CyberTeal.copy(alpha = 0.3f), blurRadius = 2f)
                        )
                    )
                }
                Text(
                    text = "MHz",
                    color = CyberTeal.copy(alpha = 0.6f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun TrendingNowSection(
    trendingStations: List<StationEntity>,
    favoriteStations: List<StationEntity>,
    onStationSelected: (StationEntity) -> Unit,
    onToggleFavorite: (StationEntity) -> Unit
) {
    if (trendingStations.isEmpty()) return

    val favoriteIds = favoriteStations.map { it.stationuuid }.toSet()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Whatshot,
                contentDescription = null,
                tint = Color(0xFFFF5722), // Fire color
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Trending Now",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xFFFF5722).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFFFF5722).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Top 5",
                    fontSize = 10.sp,
                    color = Color(0xFFFF5722),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(trendingStations) { station ->
                val isFavorited = favoriteIds.contains(station.stationuuid)
                TrendingStationCard(
                    station = station,
                    isFavorited = isFavorited,
                    onPlayClick = { onStationSelected(station) },
                    onFavoriteClick = { onToggleFavorite(station) }
                )
            }
        }
    }
}

@Composable
fun TrendingStationCard(
    station: StationEntity,
    isFavorited: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(175.dp)
            .border(
                1.dp,
                CyberTeal.copy(alpha = 0.25f),
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onPlayClick() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Image or Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicDeepNavy),
                    contentAlignment = Alignment.Center
                ) {
                    if (station.favicon.isNotBlank()) {
                        AsyncImage(
                            model = station.favicon,
                            contentDescription = station.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.ic_radio_logo),
                            placeholder = painterResource(id = R.drawable.ic_radio_logo)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_radio_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Small play count overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(Color(0xFFFF5722), RoundedCornerShape(topStart = 8.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(8.dp)
                            )
                            Text(
                                text = "${station.playCount}",
                                fontSize = 8.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Station Name
                Text(
                    text = station.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Subtitle/Country
                Text(
                    text = station.country.ifBlank { "World" },
                    color = SoftTextMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Bottom row with Play count badge (text style) and Favorite button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge text
                    Text(
                        text = "${station.playCount} plays",
                        color = Color(0xFFFF5722),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Favorite Button
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorited) Color.Red else SoftTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingsTab(
    viewModel: com.example.ui.viewmodel.RadioViewModel
) {
    val context = LocalContext.current
    val recordings by viewModel.recordingsList.collectAsState()
    
    val isPlayingRecording by viewModel.recordingManager.isPlayingRecording.collectAsState()
    val playingFile by viewModel.recordingManager.playingFile.collectAsState()
    val playbackPosition by viewModel.recordingManager.recordingPlaybackPosition.collectAsState()
    val playbackDuration by viewModel.recordingManager.recordingPlaybackDuration.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshRecordings(context)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recorded Broadcasts (${recordings.size})",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Box(
                modifier = Modifier
                    .background(CyberTeal.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, CyberTeal.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Bit-perfect Audio",
                    fontSize = 10.sp,
                    color = CyberTeal,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (recordings.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberTeal.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = SoftTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No recordings found",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tune into any online radio station and press the red record button to save direct high-fidelity streams!",
                        color = SoftTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                recordings.forEach { item ->
                    val isCurrentPlaying = playingFile?.absolutePath == item.filePath
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isCurrentPlaying) CyberTeal.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(14.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentPlaying) CosmicDeepNavy else CardBackground
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCurrentPlaying) CyberTeal.copy(alpha = 0.15f) else CosmicDeepNavy),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentPlaying && isPlayingRecording) Icons.Default.VolumeUp else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isCurrentPlaying) CyberTeal else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = item.stationName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${item.dateFormatted} • ${formatSize(item.sizeBytes)}",
                                        color = SoftTextMuted,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (isCurrentPlaying) {
                                                viewModel.recordingManager.togglePlayPauseRecording()
                                            } else {
                                                viewModel.playRecording(item)
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentPlaying && isPlayingRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause Recording",
                                            tint = CyberTeal,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.deleteRecording(context, item)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Recording",
                                            tint = Color.Red.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (isCurrentPlaying && playbackDuration > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Slider(
                                        value = playbackPosition.toFloat(),
                                        onValueChange = { position ->
                                            viewModel.recordingManager.seekToRecording(position.toInt())
                                        },
                                        valueRange = 0f..playbackDuration.toFloat(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = CyberTeal,
                                            activeTrackColor = CyberTeal,
                                            inactiveTrackColor = CyberTeal.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(18.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = formatDurationMs(playbackPosition),
                                            color = CyberTeal,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = formatDurationMs(playbackDuration),
                                            color = SoftTextMuted,
                                            fontSize = 9.sp
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

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format(java.util.Locale.US, "%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}

fun formatDurationMs(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}

fun formatDurationSec(sec: Int): String {
    val minutes = sec / 60
    val seconds = sec % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}
