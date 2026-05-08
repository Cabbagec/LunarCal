package me.xcel.lunarcal.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.LocaleList
import android.view.HapticFeedbackConstants
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import me.xcel.lunarcal.R
import me.xcel.lunarcal.data.AppDestination
import me.xcel.lunarcal.data.AppLanguage
import me.xcel.lunarcal.data.CalendarBounds
import me.xcel.lunarcal.data.CalendarDisplayMode
import me.xcel.lunarcal.data.CalendarSyncManager
import me.xcel.lunarcal.data.ExternalDeletionDecision
import me.xcel.lunarcal.data.LunarDay
import me.xcel.lunarcal.data.LunarEvent
import me.xcel.lunarcal.data.LunarReminder
import me.xcel.lunarcal.data.LunarRecurrence
import me.xcel.lunarcal.data.MAX_REMINDERS_PER_EVENT
import me.xcel.lunarcal.data.ReminderOffsetUnit
import me.xcel.lunarcal.data.ThemePreference
import me.xcel.lunarcal.ui.theme.LunarCalTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun LunarCalApp(viewModel: LunarCalendarViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val baseContext = LocalContext.current
    val localizedContext = remember(baseContext, state.appLanguage) {
        baseContext.localizedContext(state.appLanguage)
    }
    val localizedConfiguration = remember(localizedContext) {
        Configuration(localizedContext.resources.configuration)
    }
    val activityResultRegistryOwner = checkNotNull(LocalActivityResultRegistryOwner.current) {
        "LunarCalApp must be hosted by an ActivityResultRegistryOwner"
    }
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (state.themePreference) {
        ThemePreference.SYSTEM -> systemDark
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    LunarCalTheme(darkTheme = darkTheme) {
        SystemBarsEffect(darkTheme = darkTheme)
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfiguration,
            LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
        ) {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            var revealKey by remember { mutableIntStateOf(0) }
            var pendingSyncEventId by remember { mutableStateOf<String?>(null) }
            var requestedInitialCalendarPermission by remember { mutableStateOf(false) }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { permissions ->
                val granted = permissions[Manifest.permission.READ_CALENDAR] == true &&
                    permissions[Manifest.permission.WRITE_CALENDAR] == true
                if (granted) {
                    viewModel.refreshPhoneCalendars()
                    pendingSyncEventId?.let(viewModel::syncEvent)
                    viewModel.scanVisibleWindow(force = true)
                }
                pendingSyncEventId = null
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.scanVisibleWindow(force = true)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            LaunchedEffect(state.visibleMonth, state.visibleYear, state.displayMode, state.isLoadingData) {
                if (!state.isLoadingData) {
                    viewModel.scanVisibleWindow()
                }
            }

            LaunchedEffect(state.isLoadingData, state.events.isEmpty()) {
                if (
                    !state.isLoadingData &&
                    state.events.isEmpty() &&
                    !requestedInitialCalendarPermission &&
                    !hasCalendarPermission(context)
                ) {
                    requestedInitialCalendarPermission = true
                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                }
            }

            BackHandler(enabled = state.destination != AppDestination.CALENDAR) {
                viewModel.setDestination(AppDestination.CALENDAR)
            }

            NativeDrawerScaffold(
                drawerWidth = 304.dp,
                localizedContext = localizedContext,
                localizedConfiguration = localizedConfiguration,
                drawerContent = { closeDrawer ->
                    AppDrawer(
                        darkTheme = darkTheme,
                        appLanguage = state.appLanguage,
                        selectedDestination = state.destination,
                        onToggleTheme = {
                            revealKey += 1
                            viewModel.setThemePreference(if (darkTheme) ThemePreference.LIGHT else ThemePreference.DARK)
                        },
                        onDestination = { destination ->
                            viewModel.setDestination(destination)
                            closeDrawer()
                        },
                    )
                },
                content = { openDrawer ->
                    val onSaveEvent: (LunarEvent) -> Unit = { event ->
                        val saved = viewModel.upsertEvent(event)
                        val hasCalendarPermission =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                        if (hasCalendarPermission) {
                            viewModel.syncEvent(saved.id)
                        } else {
                            pendingSyncEventId = saved.id
                            permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                        }
                    }

                    Box(Modifier.fillMaxSize()) {
                        DestinationHost(
                            state = state,
                            onOpenDrawer = openDrawer,
                            onVisibleMonth = viewModel::setVisibleMonth,
                            onVisibleYear = viewModel::setVisibleYear,
                            onToday = viewModel::goToday,
                            onJumpTo = viewModel::jumpTo,
                            onToggleMode = viewModel::toggleDisplayMode,
                            onSaveEvent = onSaveEvent,
                            onDeleteEvent = viewModel::deleteEvent,
                            onThemePreference = viewModel::setThemePreference,
                            onLanguage = viewModel::setAppLanguage,
                            onExternalCalendarSelected = viewModel::setExternalCalendarSelected,
                            onRefreshCalendars = {
                                val hasCalendarPermission =
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                                if (hasCalendarPermission) {
                                    viewModel.refreshPhoneCalendars()
                                    viewModel.scanVisibleWindow(force = true)
                                } else {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                                }
                            },
                        )
                        ThemeRevealOverlay(revealKey = revealKey, darkTheme = darkTheme)
                        state.pendingExternalDeletion?.let { decision ->
                            ExternalDeletionDecisionDialog(
                                decision = decision,
                                appLanguage = state.appLanguage,
                                onRegenerate = { viewModel.resolveExternalDeletion(decision, regenerateFuture = true) },
                                onPreserve = { viewModel.resolveExternalDeletion(decision, regenerateFuture = false) },
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun SystemBarsEffect(darkTheme: Boolean) {
    val view = LocalView.current
    val activity = LocalContext.current.findActivity()
    SideEffect {
        val window = activity?.window ?: return@SideEffect
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun hasCalendarPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun NativeDrawerScaffold(
    drawerWidth: androidx.compose.ui.unit.Dp,
    localizedContext: Context,
    localizedConfiguration: Configuration,
    drawerContent: @Composable (closeDrawer: () -> Unit) -> Unit,
    content: @Composable (openDrawer: () -> Unit) -> Unit,
) {
    val parentComposition = rememberCompositionContext()
    val latestDrawerContent by rememberUpdatedState(drawerContent)
    val latestContent by rememberUpdatedState(content)
    val latestLocalizedContext by rememberUpdatedState(localizedContext)
    val latestLocalizedConfiguration by rememberUpdatedState(localizedConfiguration)
    val drawerWidthPx = with(LocalDensity.current) { drawerWidth.roundToPx() }
    val drawerElevationPx = with(LocalDensity.current) { 8.dp.toPx() }
    var drawerLayout by remember { mutableStateOf<DrawerLayout?>(null) }
    var drawerVisible by remember { mutableStateOf(false) }
    val openDrawer: () -> Unit = { drawerLayout?.openDrawer(GravityCompat.START) }
    val closeDrawer: () -> Unit = { drawerLayout?.closeDrawer(GravityCompat.START) }

    BackHandler(enabled = drawerVisible) {
        closeDrawer()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            DrawerLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setScrimColor(android.graphics.Color.argb(82, 0, 0, 0))
                setDrawerElevation(drawerElevationPx)

                addView(
                    ComposeView(context).apply {
                        setParentCompositionContext(parentComposition)
                        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                        setContent {
                            CompositionLocalProvider(
                                LocalContext provides latestLocalizedContext,
                                LocalConfiguration provides latestLocalizedConfiguration,
                            ) {
                                latestContent(openDrawer)
                            }
                        }
                    },
                    DrawerLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )

                addView(
                    ComposeView(context).apply {
                        isClickable = true
                        isFocusable = true
                        setParentCompositionContext(parentComposition)
                        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                        setContent {
                            CompositionLocalProvider(
                                LocalContext provides latestLocalizedContext,
                                LocalConfiguration provides latestLocalizedConfiguration,
                            ) {
                                latestDrawerContent(closeDrawer)
                            }
                        }
                    },
                    DrawerLayout.LayoutParams(
                        drawerWidthPx,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ).apply {
                        gravity = GravityCompat.START
                    },
                )

                addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
                    override fun onDrawerSlide(drawerView: android.view.View, slideOffset: Float) {
                        val visible = slideOffset > 0.01f
                        if (drawerVisible != visible) drawerVisible = visible
                    }

                    override fun onDrawerOpened(drawerView: android.view.View) {
                        if (!drawerVisible) drawerVisible = true
                    }

                    override fun onDrawerClosed(drawerView: android.view.View) {
                        if (drawerVisible) drawerVisible = false
                    }
                })

                drawerLayout = this
            }
        },
        update = { layout ->
            if (drawerLayout !== layout) drawerLayout = layout
            layout.setDrawerElevation(drawerElevationPx)
            val drawerView = layout.getChildAt(1)
            val params = drawerView.layoutParams as DrawerLayout.LayoutParams
            if (params.width != drawerWidthPx) {
                params.width = drawerWidthPx
                drawerView.layoutParams = params
            }
        },
    )
}

@Composable
private fun DestinationHost(
    state: LunarUiState,
    onOpenDrawer: () -> Unit,
    onVisibleMonth: (YearMonth) -> Unit,
    onVisibleYear: (Int) -> Unit,
    onToday: () -> Unit,
    onJumpTo: (LocalDate) -> Unit,
    onToggleMode: () -> Unit,
    onSaveEvent: (LunarEvent) -> Unit,
    onDeleteEvent: (LunarEvent) -> Unit,
    onThemePreference: (ThemePreference) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onExternalCalendarSelected: (Long, Boolean) -> Unit,
    onRefreshCalendars: () -> Unit,
) {
    AnimatedContent(
        targetState = state.destination,
        transitionSpec = {
            val forward = targetState.stackDepth >= initialState.stackDepth
            val enter = slideInHorizontally(
                animationSpec = tween(240, easing = FastOutSlowInEasing),
                initialOffsetX = { width -> if (forward) width / 4 else -width / 4 },
            ) + fadeIn(animationSpec = tween(180))
            val exit = slideOutHorizontally(
                animationSpec = tween(240, easing = FastOutSlowInEasing),
                targetOffsetX = { width -> if (forward) -width / 6 else width / 6 },
            ) + fadeOut(animationSpec = tween(160))
            enter togetherWith exit using SizeTransform(clip = false)
        },
        label = "destinationTransition",
        modifier = Modifier.fillMaxSize(),
    ) { destination ->
        when (destination) {
            AppDestination.CALENDAR -> CalendarHome(
                state = state,
                onOpenDrawer = onOpenDrawer,
                onVisibleMonth = onVisibleMonth,
                onVisibleYear = onVisibleYear,
                onToday = onToday,
                onJumpTo = onJumpTo,
                onToggleMode = onToggleMode,
                onSaveEvent = onSaveEvent,
                onDeleteEvent = onDeleteEvent,
            )
            AppDestination.ALL_EVENTS -> AllEventsScreen(
                state = state,
                onOpenDrawer = onOpenDrawer,
                onSaveEvent = onSaveEvent,
                onDeleteEvent = onDeleteEvent,
            )
            AppDestination.SETTINGS -> SettingsScreen(
                state = state,
                onOpenDrawer = onOpenDrawer,
                onThemePreference = onThemePreference,
                onLanguage = onLanguage,
                onExternalCalendarSelected = onExternalCalendarSelected,
                onRefreshCalendars = onRefreshCalendars,
            )
        }
    }
}

private val AppDestination.stackDepth: Int
    get() = when (this) {
        AppDestination.CALENDAR -> 0
        AppDestination.ALL_EVENTS,
        AppDestination.SETTINGS -> 1
    }

@Composable
private fun AppDrawer(
    darkTheme: Boolean,
    appLanguage: AppLanguage,
    selectedDestination: AppDestination,
    onToggleTheme: () -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.fillMaxSize()) {
            FarmHeader(
                darkTheme = darkTheme,
                appLanguage = appLanguage,
                onToggleTheme = onToggleTheme,
            )
            HorizontalDivider()
            DrawerButton(
                label = uiString(appLanguage, R.string.all_events),
                selected = selectedDestination == AppDestination.ALL_EVENTS,
                icon = { Icon(Icons.Outlined.DateRange, contentDescription = null) },
                onClick = { onDestination(AppDestination.ALL_EVENTS) },
            )
            DrawerButton(
                label = uiString(appLanguage, R.string.settings),
                selected = selectedDestination == AppDestination.SETTINGS,
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                onClick = { onDestination(AppDestination.SETTINGS) },
            )
        }
    }
}

@Composable
private fun FarmHeader(
    darkTheme: Boolean,
    appLanguage: AppLanguage,
    onToggleTheme: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        Crossfade(
            targetState = darkTheme,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            label = "drawerBackground",
        ) { useDarkBackground ->
            Image(
                painter = painterResource(
                    if (useDarkBackground) {
                        R.drawable.drawer_background_dark
                    } else {
                        R.drawable.drawer_background_light
                    },
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        IconButton(
            onClick = onToggleTheme,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
        ) {
            ThemeModeGlyph(
                darkTheme = darkTheme,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = uiString(appLanguage, R.string.app_name),
            color = if (darkTheme) Color(0xFFEFF5FF) else Color(0xFF17231D),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
        )
    }
}

@Composable
private fun ThemeModeGlyph(
    darkTheme: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val cutoutColor = MaterialTheme.colorScheme.surface
    val morph by animateFloatAsState(
        targetValue = if (darkTheme) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "sunMoonMorph",
    )
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.28f
        val bodyRadius = radius * (1f + morph * 0.25f)
        drawCircle(tint, radius = bodyRadius, center = center)
        if (morph > 0.01f) {
            drawCircle(
                color = cutoutColor,
                radius = radius * (0.2f + morph * 0.85f),
                center = center + Offset(radius * (1.05f - morph * 0.5f), -radius * 0.35f),
            )
        }
        if (morph < 0.99f) {
            repeat(8) { index ->
                val angle = (Math.PI * 2.0 * index / 8.0).toFloat()
                val start = center + Offset(kotlin.math.cos(angle), kotlin.math.sin(angle)) * (radius * 1.45f)
                val end = center + Offset(kotlin.math.cos(angle), kotlin.math.sin(angle)) * (radius * 2.05f)
                drawLine(
                    color = tint,
                    start = start,
                    end = end,
                    strokeWidth = 2.dp.toPx(),
                    alpha = 1f - morph,
                )
            }
        }
    }
}

@Composable
private fun DrawerButton(
    label: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        modifier = Modifier
            .padding(end = 12.dp, top = 6.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarHome(
    state: LunarUiState,
    onOpenDrawer: () -> Unit,
    onVisibleMonth: (YearMonth) -> Unit,
    onVisibleYear: (Int) -> Unit,
    onToday: () -> Unit,
    onJumpTo: (LocalDate) -> Unit,
    onToggleMode: () -> Unit,
    onSaveEvent: (LunarEvent) -> Unit,
    onDeleteEvent: (LunarEvent) -> Unit,
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var editingEvent by remember { mutableStateOf<LunarEvent?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    val header = calendarHeader(state)
    val appLanguage = state.appLanguage

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(header.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = header.secondary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    TooltipIconButton(
                        tooltip = uiString(appLanguage, R.string.open_menu),
                        onClick = onOpenDrawer,
                    ) {
                        Icon(Icons.Outlined.Menu, contentDescription = uiString(appLanguage, R.string.open_menu))
                    }
                },
                actions = {
                    TooltipIconButton(
                        tooltip = uiString(appLanguage, R.string.search),
                        onClick = { showSearchDialog = true },
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = uiString(appLanguage, R.string.search))
                    }
                    TooltipIconButton(
                        tooltip = uiString(appLanguage, R.string.today),
                        onClick = onToday,
                    ) {
                        CalendarDateIcon(day = state.today.dayOfMonth)
                    }
                    TooltipIconButton(
                        tooltip = uiString(appLanguage, R.string.jump_to_day),
                        onClick = { showJumpDialog = true },
                    ) {
                        Icon(Icons.Outlined.DateRange, contentDescription = uiString(appLanguage, R.string.jump_to_day))
                    }
                    TooltipIconButton(
                        tooltip = if (state.displayMode == CalendarDisplayMode.MONTH) {
                            uiString(appLanguage, R.string.year_view_tooltip)
                        } else {
                            uiString(appLanguage, R.string.month_view_tooltip)
                        },
                        onClick = onToggleMode,
                    ) {
                        ViewModePaperIcon(
                            displayMode = state.displayMode,
                            language = state.appLanguage,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Box(Modifier.fillMaxWidth().height(2.dp)) {
                if (state.isSyncScanning) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxSize())
                }
            }
            when {
                state.isLoadingData -> LoadingState(text = uiString(appLanguage, R.string.data_loading))
                state.dataError != null -> LoadingState(text = state.dataError)
                state.displayMode == CalendarDisplayMode.MONTH -> MonthPager(
                    visibleMonth = state.visibleMonth,
                    appLanguage = state.appLanguage,
                    today = state.today,
                    lunarDays = state.lunarDays,
                    events = state.events,
                    onVisibleMonth = onVisibleMonth,
                    onDayClick = { selectedDate = it },
                )
                else -> YearPager(
                    visibleYear = state.visibleYear,
                    appLanguage = state.appLanguage,
                    today = state.today,
                    lunarDays = state.lunarDays,
                    events = state.events,
                    onVisibleYear = onVisibleYear,
                    onDayClick = { selectedDate = it },
                )
            }
        }
    }

    if (!showEditor) {
        selectedDate?.let { date ->
            DayEventsDialog(
                date = date,
                lunarDay = state.lunarDays[date],
                events = eventsOn(date, state),
                appLanguage = state.appLanguage,
                onDismiss = { selectedDate = null },
                onDelete = onDeleteEvent,
                onOpenEditor = {
                    editingEvent = it
                    showEditor = true
                },
            )
        }
    }

    selectedDate?.let { date ->
        if (showEditor) {
            EventEditorDialog(
                date = date,
                lunarDay = state.lunarDays[date],
                existing = editingEvent,
                appLanguage = state.appLanguage,
                onDismiss = {
                    editingEvent = null
                    showEditor = false
                },
                onSave = {
                    onSaveEvent(it)
                    editingEvent = null
                    showEditor = false
                    selectedDate = null
                },
            )
        }
    }

    if (showJumpDialog) {
        JumpToDayDialog(
            initialDate = state.visibleMonth.atDay(1),
            appLanguage = state.appLanguage,
            onDismiss = { showJumpDialog = false },
            onJump = {
                onJumpTo(it)
                showJumpDialog = false
            },
        )
    }
    if (showSearchDialog) {
        SearchDialog(
            events = state.events,
            appLanguage = state.appLanguage,
            onDismiss = { showSearchDialog = false },
            onJumpTo = {
                onJumpTo(it)
                showSearchDialog = false
            },
        )
    }
}

@Composable
private fun LoadingState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExternalDeletionDecisionDialog(
    decision: ExternalDeletionDecision,
    appLanguage: AppLanguage,
    onRegenerate: () -> Unit,
    onPreserve: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(uiString(appLanguage, R.string.external_deletion_title)) },
        text = {
            Text(
                externalDeletionMessage(appLanguage, decision),
            )
        },
        confirmButton = {
            Button(onClick = onRegenerate) {
                Text(uiString(appLanguage, R.string.regenerate_future))
            }
        },
        dismissButton = {
            TextButton(onClick = onPreserve) {
                Text(uiString(appLanguage, R.string.preserve_deletion))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipIconButton(
    tooltip: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val cleanTooltip = tooltip.trim()
    val tooltipState = rememberTooltipState()
    val view = LocalView.current
    LaunchedEffect(tooltipState.isVisible) {
        if (tooltipState.isVisible) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(cleanTooltip) } },
        state = tooltipState,
    ) {
        IconButton(onClick = onClick) {
            content()
        }
    }
}

@Composable
private fun CalendarDateIcon(day: Int) {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val outline = MaterialTheme.colorScheme.onSurfaceVariant
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val left = size.width * 0.14f
            val top = size.height * 0.14f
            val right = size.width * 0.86f
            val bottom = size.height * 0.86f
            drawRoundRect(
                color = outline,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.5.dp.toPx(), 4.5.dp.toPx()),
                style = Stroke(width = strokeWidth),
            )
        }
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 9.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun ViewModePaperIcon(
    displayMode: CalendarDisplayMode,
    language: AppLanguage,
) {
    val label = when (displayMode) {
        CalendarDisplayMode.MONTH -> if (language == AppLanguage.CHINESE) "月" else "Mon"
        CalendarDisplayMode.YEAR -> if (language == AppLanguage.CHINESE) "年" else "Year"
    }
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val outline = MaterialTheme.colorScheme.onSurfaceVariant
        val surface = MaterialTheme.colorScheme.surface
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val left = size.width * 0.12f
            val top = size.height * 0.1f
            val right = size.width * 0.88f
            val bottom = size.height * 0.9f
            val radius = 4.dp.toPx()
            val curl = 5.5.dp.toPx()
            val body = Path().apply {
                moveTo(left + radius, top)
                lineTo(right - curl, top)
                lineTo(right, top + curl)
                lineTo(right, bottom - radius)
                quadraticTo(right, bottom, right - radius, bottom)
                lineTo(left + radius, bottom)
                quadraticTo(left, bottom, left, bottom - radius)
                lineTo(left, top + radius)
                quadraticTo(left, top, left + radius, top)
                close()
            }
            drawPath(body, surface)
            drawPath(body, outline, style = Stroke(width = strokeWidth))
            val fold = Path().apply {
                moveTo(right - curl, top)
                quadraticTo(right - curl * 0.55f, top + curl * 0.55f, right, top + curl)
            }
            drawPath(fold, outline, style = Stroke(width = 1.4.dp.toPx()))
            val foldShadow = Path().apply {
                moveTo(right - curl * 0.45f, top + curl * 0.95f)
                lineTo(right, top + curl)
            }
            drawPath(foldShadow, outline.copy(alpha = 0.45f), style = Stroke(width = 1.dp.toPx()))
        }
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                (fadeIn(tween(120)) + scaleIn(tween(150), initialScale = 0.82f)) togetherWith
                    (fadeOut(tween(90)) + scaleOut(tween(120), targetScale = 1.18f)) using SizeTransform(clip = false)
            },
            label = "viewModeIconText",
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = when {
                        text.length >= 4 -> 5.8.sp
                        text.length > 1 -> 6.6.sp
                        else -> 11.sp
                    },
                    lineHeight = 11.sp,
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

private data class CalendarHeader(
    val primary: String,
    val secondary: String,
)

private fun calendarHeader(state: LunarUiState): CalendarHeader {
    return when (state.displayMode) {
        CalendarDisplayMode.MONTH -> CalendarHeader(
            primary = formatGregorianMonthHeader(state.visibleMonth, state.appLanguage),
            secondary = lunarMonthRangeHeader(state.visibleMonth, state.lunarDays),
        )
        CalendarDisplayMode.YEAR -> CalendarHeader(
            primary = state.visibleYear.toString(),
            secondary = lunarYearHeader(state.visibleYear, state.lunarDays),
        )
    }
}

private fun formatGregorianMonthHeader(month: YearMonth, language: AppLanguage): String {
    val monthName = if (language == AppLanguage.CHINESE) {
        GREGORIAN_MONTHS_ZH[month.monthValue - 1]
    } else {
        month.month.getDisplayName(TextStyle.SHORT, appLocale(language))
    }
    return "${month.year} $monthName"
}

private fun lunarMonthRangeHeader(month: YearMonth, lunarDays: Map<LocalDate, LunarDay>): String {
    val start = maxOf(month.atDay(1), CalendarBounds.startDate)
    val end = minOf(month.atEndOfMonth(), CalendarBounds.endDate)
    val days = generateSequence(start) { it.plusDays(1) }
        .takeWhile { !it.isAfter(end) }
        .mapNotNull { lunarDays[it] }
        .toList()
    val first = days.firstOrNull() ?: return ""
    val last = days.lastOrNull() ?: return first.yearLabel
    return when {
        first.lunarYear != last.lunarYear -> "${first.yearLabel}${first.monthName}-${last.yearLabel}${last.monthName}"
        first.monthName != last.monthName -> "${first.yearLabel}${first.monthName}-${last.monthName}"
        else -> "${first.yearLabel}${first.monthName}"
    }
}

private fun lunarYearHeader(year: Int, lunarDays: Map<LocalDate, LunarDay>): String {
    val chineseNewYear = lunarDays.values
        .asSequence()
        .filter { it.date.year == year && it.monthNumber == 1 && it.dayNumber == 1 && !it.isLeapMonth }
        .minByOrNull { it.date }
    return chineseNewYear?.yearLabel
        ?: lunarDays[LocalDate.of(CalendarBounds.clampYear(year), 7, 1)]?.yearLabel.orEmpty()
}

private val GREGORIAN_MONTHS_ZH = listOf(
    "一月", "二月", "三月", "四月", "五月", "六月",
    "七月", "八月", "九月", "十月", "十一月", "十二月",
)

@Composable
private fun MonthPager(
    visibleMonth: YearMonth,
    appLanguage: AppLanguage,
    today: LocalDate,
    lunarDays: Map<LocalDate, LunarDay>,
    events: List<LunarEvent>,
    onVisibleMonth: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val targetPage = CalendarBounds.monthToPage(visibleMonth)
    val pagerState = rememberPagerState(initialPage = targetPage, pageCount = { CalendarBounds.monthPageCount })
    LaunchedEffect(targetPage) {
        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(targetPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page -> onVisibleMonth(CalendarBounds.pageToMonth(page)) }
    }
    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        MonthCalendar(
            month = CalendarBounds.pageToMonth(page),
            appLanguage = appLanguage,
            today = today,
            lunarDays = lunarDays,
            events = events,
            onDayClick = onDayClick,
        )
    }
}

@Composable
private fun YearPager(
    visibleYear: Int,
    appLanguage: AppLanguage,
    today: LocalDate,
    lunarDays: Map<LocalDate, LunarDay>,
    events: List<LunarEvent>,
    onVisibleYear: (Int) -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val targetPage = CalendarBounds.yearToPage(visibleYear)
    val pagerState = rememberPagerState(initialPage = targetPage, pageCount = { CalendarBounds.yearPageCount })
    LaunchedEffect(targetPage) {
        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(targetPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page -> onVisibleYear(CalendarBounds.pageToYear(page)) }
    }
    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        YearCalendar(
            year = CalendarBounds.pageToYear(page),
            appLanguage = appLanguage,
            today = today,
            lunarDays = lunarDays,
            events = events,
            onDayClick = onDayClick,
        )
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    appLanguage: AppLanguage,
    today: LocalDate,
    lunarDays: Map<LocalDate, LunarDay>,
    events: List<LunarEvent>,
    onDayClick: (LocalDate) -> Unit,
) {
    val days = remember(month) { monthGridDays(month) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
    ) {
        WeekHeader(appLanguage = appLanguage)
        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.weight(1f)) {
                week.forEach { date ->
                    val lunarDay = lunarDays[date]
                    DayCell(
                        date = date,
                        inCurrentMonth = date.month == month.month,
                        enabled = CalendarBounds.isSupported(date),
                        isToday = date == today,
                        lunarDay = lunarDay,
                        events = eventsOn(date, lunarDay, events),
                        monthView = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onDayClick(date) },
                    )
                }
            }
        }
    }
}

@Composable
private fun YearCalendar(
    year: Int,
    appLanguage: AppLanguage,
    today: LocalDate,
    lunarDays: Map<LocalDate, LunarDay>,
    events: List<LunarEvent>,
    onDayClick: (LocalDate) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 112.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items((1..12).map { YearMonth.of(year, it) }) { month ->
            Column(Modifier.padding(horizontal = 2.dp, vertical = 2.dp)) {
                Text(
                    text = formatGregorianMonthName(month.monthValue, appLanguage),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                WeekHeader(compact = true, appLanguage = appLanguage)
                monthGridDays(month).chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            val lunarDay = lunarDays[date]
                            DayCell(
                                date = date,
                                inCurrentMonth = date.month == month.month,
                                enabled = CalendarBounds.isSupported(date),
                                isToday = date == today,
                                lunarDay = lunarDay,
                                events = eventsOn(date, lunarDay, events),
                                monthView = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                onClick = { onDayClick(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekHeader(
    compact: Boolean = false,
    appLanguage: AppLanguage,
) {
    val locale = appLocale(appLanguage)
    Row(Modifier.fillMaxWidth()) {
        DayOfWeek.values().forEach { day ->
            val label = day.getDisplayName(if (compact) TextStyle.NARROW else TextStyle.SHORT, locale)
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = if (compact) {
                    MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, lineHeight = 8.sp)
                } else {
                    MaterialTheme.typography.labelMedium
                },
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inCurrentMonth: Boolean,
    enabled: Boolean,
    isToday: Boolean,
    lunarDay: LunarDay?,
    events: List<LunarEvent>,
    monthView: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val alpha = if (enabled && inCurrentMonth) 1f else 0.32f
    if (!monthView) {
        Box(
            modifier = modifier
                .padding(0.5.dp)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (isToday) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(1.dp)
                        .background(colors.primaryContainer.copy(alpha = 0.52f), CircleShape),
                )
            }
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, lineHeight = 8.sp),
                color = colors.onSurface.copy(alpha = alpha),
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
            )
            if (events.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 1.dp),
                ) {
                    events.take(3).forEach {
                        Box(
                            modifier = Modifier
                                .size(2.dp)
                                .background(colors.tertiary, CircleShape),
                        )
                    }
                }
            }
        }
        return
    }

    val borderColor = if (isToday) colors.primary else DividerDefaults.color.copy(alpha = 0.4f)
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .padding(2.dp)
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(if (isToday) colors.primaryContainer.copy(alpha = 0.42f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface.copy(alpha = alpha),
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
        Text(
            text = lunarDay?.shortDayLabel ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant.copy(alpha = alpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(3.dp))
        events.take(2).forEach { event ->
            Surface(
                color = colors.tertiaryContainer,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .padding(top = 2.dp),
            ) {
                Text(
                    text = event.titleOn(date),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onTertiaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DayEventsDialog(
    date: LocalDate,
    lunarDay: LunarDay?,
    events: List<LunarEvent>,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onDelete: (LunarEvent) -> Unit,
    onOpenEditor: (LunarEvent?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onOpenEditor(null) }) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(uiString(appLanguage, R.string.add_event))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(uiString(appLanguage, R.string.cancel))
            }
        },
        title = { Text(formatFullDate(date, appLanguage)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = lunarDay?.fullLabel ?: uiString(appLanguage, R.string.data_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (events.isEmpty()) {
                    Text(uiString(appLanguage, R.string.events_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    events.forEach { event ->
                        ListItem(
                            headlineContent = { Text(event.titleOn(date), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text("${formatRecurrence(event.recurrence, appLanguage)} · ${event.lunarLabel}") },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { onOpenEditor(event) }) {
                                        Icon(Icons.Outlined.Edit, contentDescription = uiString(appLanguage, R.string.edit_event))
                                    }
                                    IconButton(onClick = { onDelete(event) }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = uiString(appLanguage, R.string.delete))
                                    }
                                }
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun EventEditorDialog(
    date: LocalDate,
    lunarDay: LunarDay?,
    existing: LunarEvent?,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (LunarEvent) -> Unit,
) {
    if (lunarDay == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = onDismiss) { Text(uiString(appLanguage, R.string.cancel)) } },
            title = { Text(uiString(appLanguage, R.string.add_event)) },
            text = { Text(uiString(appLanguage, R.string.data_unavailable)) },
        )
        return
    }
    var title by remember(existing?.id, date) { mutableStateOf(existing?.title ?: "") }
    var recurrence by remember(existing?.id, date) { mutableStateOf(existing?.recurrence ?: LunarRecurrence.YEARLY) }
    var reminders by remember(existing?.id, date) {
        mutableStateOf(existing?.reminders?.take(MAX_REMINDERS_PER_EVENT)?.ifEmpty { LunarReminder.defaultReminders() } ?: LunarReminder.defaultReminders())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) uiString(appLanguage, R.string.add_event) else uiString(appLanguage, R.string.edit_event)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text("${formatFullDate(date, appLanguage)} · ${lunarDay.fullLabel}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(uiString(appLanguage, R.string.event_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(uiString(appLanguage, R.string.recurrence), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecurrenceChip(
                        selected = recurrence == LunarRecurrence.YEARLY,
                        text = uiString(appLanguage, R.string.recurrence_yearly),
                        onClick = { recurrence = LunarRecurrence.YEARLY },
                    )
                    RecurrenceChip(
                        selected = recurrence == LunarRecurrence.MONTHLY,
                        text = uiString(appLanguage, R.string.recurrence_monthly),
                        onClick = { recurrence = LunarRecurrence.MONTHLY },
                    )
                    RecurrenceChip(
                        selected = recurrence == LunarRecurrence.ONCE,
                        text = uiString(appLanguage, R.string.recurrence_once),
                        onClick = { recurrence = LunarRecurrence.ONCE },
                    )
                }
                Text(uiString(appLanguage, R.string.reminders), style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    reminders.forEachIndexed { index, reminder ->
                        ReminderRow(
                            reminder = reminder,
                            appLanguage = appLanguage,
                            onReminder = { updated ->
                                reminders = reminders.toMutableList().also { it[index] = updated }
                            },
                            onRemove = {
                                reminders = reminders.toMutableList().also { it.removeAt(index) }
                            },
                        )
                    }
                    TextButton(
                        onClick = {
                            if (reminders.size < MAX_REMINDERS_PER_EVENT) {
                                reminders = reminders + LunarReminder(amount = 1, unit = ReminderOffsetUnit.DAYS, time = "09:00")
                            }
                        },
                        enabled = reminders.size < MAX_REMINDERS_PER_EVENT,
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (reminders.size < MAX_REMINDERS_PER_EVENT) {
                                uiString(appLanguage, R.string.add_reminder)
                            } else {
                                uiString(appLanguage, R.string.max_reminders)
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim().ifBlank { lunarDay.fullLabel }
                    onSave(
                        LunarEvent(
                            id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                            title = cleanTitle,
                            recurrence = recurrence,
                            lunarMonthNumber = lunarDay.monthNumber,
                            lunarMonthName = lunarDay.monthName,
                            lunarDayNumber = lunarDay.dayNumber,
                            lunarDayName = lunarDay.dayName,
                            isLeapMonth = lunarDay.isLeapMonth,
                            startDateIso = existing?.startDateIso ?: date.toString(),
                            reminders = reminders.take(MAX_REMINDERS_PER_EVENT).filter { it.amount >= 1 && isReminderTimeValid(it.time) },
                            occurrenceSyncs = existing?.occurrenceSyncs.orEmpty(),
                            calendarProviderIds = existing?.calendarProviderIds.orEmpty(),
                        ),
                    )
                },
                enabled = (title.isNotBlank() || existing == null) && reminders.all { it.amount >= 1 && isReminderTimeValid(it.time) },
            ) {
                Text(uiString(appLanguage, R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(uiString(appLanguage, R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderRow(
    reminder: LunarReminder,
    appLanguage: AppLanguage,
    onReminder: (LunarReminder) -> Unit,
    onRemove: () -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = reminder.amount.toString(),
                onValueChange = { value ->
                    val amount = value.filter(Char::isDigit).take(3).toIntOrNull()?.coerceIn(1, 365) ?: 1
                    onReminder(reminder.copy(amount = amount))
                },
                label = { Text(uiString(appLanguage, R.string.reminder_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.weight(1f),
            ) {
                Text("${uiString(appLanguage, R.string.reminder_time)} ${reminder.time}")
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = uiString(appLanguage, R.string.delete))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = reminder.unit == ReminderOffsetUnit.DAYS,
                onClick = { onReminder(reminder.copy(unit = ReminderOffsetUnit.DAYS)) },
                label = { Text(uiString(appLanguage, R.string.days_short)) },
            )
            FilterChip(
                selected = reminder.unit == ReminderOffsetUnit.WEEKS,
                onClick = { onReminder(reminder.copy(unit = ReminderOffsetUnit.WEEKS)) },
                label = { Text(uiString(appLanguage, R.string.weeks_short)) },
            )
        }
    }
    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialTime = reminder.time,
            appLanguage = appLanguage,
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                onReminder(reminder.copy(time = time))
                showTimePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialTime: String,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val parsed = parseReminderTime(initialTime) ?: LocalTime.of(9, 0)
    val state = rememberTimePickerState(
        initialHour = parsed.hour,
        initialMinute = parsed.minute,
        is24Hour = true,
    )
    val view = LocalView.current
    var lastHapticHour by remember { mutableIntStateOf(parsed.hour) }
    var lastHapticMinute by remember { mutableIntStateOf(parsed.minute) }
    LaunchedEffect(state.hour, state.minute) {
        if (state.hour != lastHapticHour || state.minute != lastHapticMinute) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            lastHapticHour = state.hour
            lastHapticMinute = state.minute
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiString(appLanguage, R.string.reminder_time)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            Button(onClick = { onConfirm(formatReminderTime(state.hour, state.minute)) }) {
                Text(uiString(appLanguage, R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(uiString(appLanguage, R.string.cancel))
            }
        },
    )
}

@Composable
private fun RecurrenceChip(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
    )
}

@Composable
private fun JumpToDayDialog(
    initialDate: LocalDate,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onJump: (LocalDate) -> Unit,
) {
    var yearText by remember { mutableStateOf(initialDate.year.toString()) }
    var monthText by remember { mutableStateOf(initialDate.monthValue.toString()) }
    var dayText by remember { mutableStateOf(initialDate.dayOfMonth.toString()) }
    val parsed = remember(yearText, monthText, dayText) {
        runCatching {
            LocalDate.of(yearText.toInt(), monthText.toInt(), dayText.toInt())
        }.getOrNull()?.takeIf(CalendarBounds::isSupported)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiString(appLanguage, R.string.jump_to_day)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatePartField(label = uiString(appLanguage, R.string.year), value = yearText, onValue = { yearText = it }, modifier = Modifier.weight(1.25f))
                    DatePartField(label = uiString(appLanguage, R.string.month), value = monthText, onValue = { monthText = it }, modifier = Modifier.weight(1f))
                    DatePartField(label = uiString(appLanguage, R.string.day), value = dayText, onValue = { dayText = it }, modifier = Modifier.weight(1f))
                }
                Text(
                    text = uiString(appLanguage, R.string.supported_date_range),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = { parsed?.let(onJump) }, enabled = parsed != null) {
                Text(uiString(appLanguage, R.string.go))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiString(appLanguage, R.string.cancel)) } },
    )
}

@Composable
private fun DatePartField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { next -> onValue(next.filter(Char::isDigit).take(4)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun SearchDialog(
    events: List<LunarEvent>,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onJumpTo: (LocalDate) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val matches = remember(query, events) {
        if (query.isBlank()) events else events.filter {
            it.title.contains(query, ignoreCase = true) || it.lunarLabel.contains(query)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiString(appLanguage, R.string.search_events)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(uiString(appLanguage, R.string.search)) },
                    singleLine = true,
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                            }
                        }
                    },
                )
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(matches) { event ->
                        ListItem(
                            headlineContent = { Text(event.title) },
                            supportingContent = { Text("${event.lunarLabel} · ${event.startDateIso}") },
                            modifier = Modifier.clickable { onJumpTo(LocalDate.parse(event.startDateIso)) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(uiString(appLanguage, R.string.cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllEventsScreen(
    state: LunarUiState,
    onOpenDrawer: () -> Unit,
    onSaveEvent: (LunarEvent) -> Unit,
    onDeleteEvent: (LunarEvent) -> Unit,
) {
    var editingEvent by remember { mutableStateOf<LunarEvent?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiString(state.appLanguage, R.string.all_events)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Outlined.Menu, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        if (state.events.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(uiString(state.appLanguage, R.string.events_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(state.events) { event ->
                    ListItem(
                        headlineContent = { Text(event.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text("${event.lunarLabel} · ${formatRecurrence(event.recurrence, state.appLanguage)}") },
                        modifier = Modifier.clickable { editingEvent = event },
                        trailingContent = {
                            IconButton(onClick = { onDeleteEvent(event) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = uiString(state.appLanguage, R.string.delete))
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
    editingEvent?.let { event ->
        val eventDate = runCatching { LocalDate.parse(event.startDateIso) }.getOrNull()
        if (eventDate != null) {
            EventEditorDialog(
                date = eventDate,
                lunarDay = state.lunarDays[eventDate],
                existing = event,
                appLanguage = state.appLanguage,
                onDismiss = { editingEvent = null },
                onSave = {
                    onSaveEvent(it)
                    editingEvent = null
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    state: LunarUiState,
    onOpenDrawer: () -> Unit,
    onThemePreference: (ThemePreference) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onExternalCalendarSelected: (Long, Boolean) -> Unit,
    onRefreshCalendars: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiString(state.appLanguage, R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Outlined.Menu, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(uiString(state.appLanguage, R.string.settings), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePreferenceButton(uiString(state.appLanguage, R.string.system_theme), state.themePreference == ThemePreference.SYSTEM) { onThemePreference(ThemePreference.SYSTEM) }
                ThemePreferenceButton(uiString(state.appLanguage, R.string.light_theme), state.themePreference == ThemePreference.LIGHT) { onThemePreference(ThemePreference.LIGHT) }
                ThemePreferenceButton(uiString(state.appLanguage, R.string.dark_theme), state.themePreference == ThemePreference.DARK) { onThemePreference(ThemePreference.DARK) }
            }
            Text(uiString(state.appLanguage, R.string.language), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePreferenceButton(uiString(state.appLanguage, R.string.english), state.appLanguage == AppLanguage.ENGLISH) { onLanguage(AppLanguage.ENGLISH) }
                ThemePreferenceButton(uiString(state.appLanguage, R.string.simplified_chinese), state.appLanguage == AppLanguage.CHINESE) { onLanguage(AppLanguage.CHINESE) }
            }
            DisabledSetting(title = uiString(state.appLanguage, R.string.timezone), value = "Asia/Shanghai GMT+8")
            DisabledSetting(title = uiString(state.appLanguage, R.string.recurring_range), value = uiString(state.appLanguage, R.string.recurring_range_value))
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    uiString(state.appLanguage, R.string.external_calendars),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                )
                TextButton(onClick = onRefreshCalendars) {
                    Text(uiString(state.appLanguage, R.string.search))
                }
            }
            if (state.phoneCalendars.isEmpty()) {
                AssistChip(
                    onClick = onRefreshCalendars,
                    label = { Text(uiString(state.appLanguage, R.string.none_selected)) },
                )
            } else {
                state.phoneCalendars.forEach { calendar ->
                    val selected = calendar.id in state.selectedExternalCalendarIds
                    ListItem(
                        headlineContent = { Text(calendar.name.ifBlank { calendar.accountName }) },
                        supportingContent = { Text(calendar.accountName) },
                        leadingContent = {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = { onExternalCalendarSelected(calendar.id, it) },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemePreferenceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick) { Text(text) }
    } else {
        FilledTonalButton(onClick = onClick) { Text(text) }
    }
}

@Composable
private fun DisabledSetting(title: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun ThemeRevealOverlay(revealKey: Int, darkTheme: Boolean) {
    if (revealKey == 0) return
    val progress = remember(revealKey) { Animatable(0f) }
    LaunchedEffect(revealKey) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 460, easing = FastOutSlowInEasing))
    }
    if (progress.value < 1f) {
        Canvas(Modifier.fillMaxSize()) {
            val maxRadius = kotlin.math.hypot(size.width, size.height)
            drawCircle(
                color = if (darkTheme) Color(0xFF111411) else Color(0xFFFAFCF8),
                radius = maxRadius * progress.value,
                center = Offset(266.dp.toPx(), 80.dp.toPx()),
                alpha = 0.22f,
            )
        }
    }
}

private fun monthGridDays(month: YearMonth): List<LocalDate> {
    val first = month.atDay(1)
    val last = month.atEndOfMonth()
    val start = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val end = last.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    return generateSequence(start) { it.plusDays(1) }
        .takeWhile { !it.isAfter(end) }
        .toList()
}

private fun eventsOn(date: LocalDate, state: LunarUiState): List<LunarEvent> {
    val lunarDay = state.lunarDays[date] ?: return emptyList()
    return eventsOn(date, lunarDay, state.events)
}

private fun eventsOn(date: LocalDate, lunarDay: LunarDay?, events: List<LunarEvent>): List<LunarEvent> {
    if (lunarDay == null) return emptyList()
    return events.filter { it.occursOn(lunarDay) && !it.isHiddenOn(date) }
}

private fun isReminderTimeValid(time: String): Boolean {
    return parseReminderTime(time) != null
}

private fun parseReminderTime(time: String): LocalTime? {
    val trimmed = time.trim()
    runCatching { LocalTime.parse(trimmed) }.getOrNull()?.let { return it }
    val parts = trimmed.split(":")
    if (parts.size < 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return if (hour in 0..23 && minute in 0..59) LocalTime.of(hour, minute) else null
}

private fun formatReminderTime(hour: Int, minute: Int): String {
    return String.format(Locale.ROOT, "%02d:%02d", hour, minute)
}

private fun formatRecurrence(recurrence: LunarRecurrence, language: AppLanguage): String {
    return when (recurrence) {
        LunarRecurrence.YEARLY -> uiString(language, R.string.recurrence_yearly)
        LunarRecurrence.MONTHLY -> uiString(language, R.string.recurrence_monthly)
        LunarRecurrence.ONCE -> uiString(language, R.string.recurrence_once)
    }
}

private fun externalDeletionMessage(language: AppLanguage, decision: ExternalDeletionDecision): String {
    return if (language == AppLanguage.CHINESE) {
        "${decision.eventTitle} 从 ${decision.fromDateIso} 起有 ${decision.deletedCount} 个手机日历事件缺失。要重新生成之后的事件，还是保留外部删除？"
    } else {
        "${decision.eventTitle} has ${decision.deletedCount} missing phone calendar occurrence(s) from ${decision.fromDateIso}. Regenerate future occurrences, or preserve the external deletion?"
    }
}

private fun formatGregorianMonthName(month: Int, language: AppLanguage): String {
    return if (language == AppLanguage.CHINESE) {
        GREGORIAN_MONTHS_ZH[month - 1]
    } else {
        java.time.Month.of(month).getDisplayName(TextStyle.SHORT, appLocale(language))
    }
}

private fun formatFullDate(date: LocalDate, language: AppLanguage): String {
    return if (language == AppLanguage.CHINESE) {
        "${date.year} ${date.monthValue} 月 ${date.dayOfMonth} 日"
    } else {
        date.format(DateTimeFormatter.ofPattern("yyyy MMM d", appLocale(language)))
    }
}

private fun appLocale(language: AppLanguage): Locale {
    return if (language == AppLanguage.CHINESE) Locale.SIMPLIFIED_CHINESE else Locale.ENGLISH
}

private fun uiString(language: AppLanguage, id: Int): String {
    val chinese = language == AppLanguage.CHINESE
    return when (id) {
        R.string.app_name -> if (chinese) "农历提醒" else "LunarCal"
        R.string.all_events -> if (chinese) "全部事件" else "All events"
        R.string.settings -> if (chinese) "设置" else "Settings"
        R.string.search -> if (chinese) "搜索" else "Search"
        R.string.open_menu -> if (chinese) "打开菜单" else "Open menu"
        R.string.today -> if (chinese) "今天" else "Today"
        R.string.jump_to_day -> if (chinese) "跳转日期" else "Jump to day"
        R.string.month_view -> if (chinese) "月" else "Month"
        R.string.year_view -> if (chinese) "年" else "Year"
        R.string.month_view_tooltip -> if (chinese) "月视图" else "Month view"
        R.string.year_view_tooltip -> if (chinese) "年视图" else "Year view"
        R.string.add_event -> if (chinese) "添加事件" else "Add event"
        R.string.edit_event -> if (chinese) "编辑事件" else "Edit event"
        R.string.event_title -> if (chinese) "事件标题" else "Event title"
        R.string.recurrence -> if (chinese) "重复" else "Recurrence"
        R.string.recurrence_yearly -> if (chinese) "每年" else "Yearly"
        R.string.recurrence_monthly -> if (chinese) "每月" else "Monthly"
        R.string.recurrence_once -> if (chinese) "一次" else "One time"
        R.string.reminders -> if (chinese) "提醒" else "Reminders"
        R.string.add_reminder -> if (chinese) "添加提醒" else "Add reminder"
        R.string.max_reminders -> if (chinese) "最多 5 个提醒" else "Maximum 5 reminders"
        R.string.reminder_amount -> if (chinese) "提前" else "Before"
        R.string.reminder_time -> if (chinese) "时间" else "Time"
        R.string.days_short -> if (chinese) "天" else "Days"
        R.string.weeks_short -> if (chinese) "周" else "Weeks"
        R.string.save -> if (chinese) "保存" else "Save"
        R.string.cancel -> if (chinese) "取消" else "Cancel"
        R.string.delete -> if (chinese) "删除" else "Delete"
        R.string.timezone -> if (chinese) "时区" else "Timezone"
        R.string.recurring_range -> if (chinese) "重复范围" else "Recurring range"
        R.string.recurring_range_value -> if (chinese) "10年" else "10 years"
        R.string.external_calendars -> if (chinese) "手机上的其他日历" else "Other phone calendars"
        R.string.none_selected -> if (chinese) "未选择" else "None selected"
        R.string.system_theme -> if (chinese) "跟随系统" else "System"
        R.string.light_theme -> if (chinese) "浅色" else "Light"
        R.string.dark_theme -> if (chinese) "深色" else "Dark"
        R.string.calendar_permission_needed -> if (chinese) "需要日历权限才能添加手机通知。" else "Calendar permission is needed to add phone notifications."
        R.string.data_loading -> if (chinese) "正在加载农历数据" else "Loading lunar calendar data"
        R.string.data_unavailable -> if (chinese) "这个日期没有可用的农历数据。" else "Lunar data is unavailable for this date."
        R.string.events_empty -> if (chinese) "还没有事件" else "No events yet"
        R.string.search_events -> if (chinese) "搜索事件" else "Search events"
        R.string.year -> if (chinese) "年" else "Year"
        R.string.month -> if (chinese) "月" else "Month"
        R.string.day -> if (chinese) "日" else "Day"
        R.string.go -> if (chinese) "跳转" else "Go"
        R.string.language -> if (chinese) "语言" else "Language"
        R.string.english -> "English"
        R.string.simplified_chinese -> if (chinese) "简体中文" else "Simplified Chinese"
        R.string.supported_date_range -> if (chinese) {
            "支持日期范围：1901年1月1日 - 2100年12月31日"
        } else {
            "Supported date range: Jan 1, 1901 - Dec 31, 2100"
        }
        R.string.external_deletion_title -> if (chinese) "日历事件已被删除" else "Calendar event deleted"
        R.string.regenerate_future -> if (chinese) "重新生成之后事件" else "Regenerate future"
        R.string.preserve_deletion -> if (chinese) "保留删除" else "Preserve deletion"
        else -> ""
    }
}

private fun Context.localizedContext(language: AppLanguage): Context {
    val configuration = Configuration(resources.configuration)
    configuration.setLocales(LocaleList(appLocale(language)))
    val activity = findActivity() ?: return createConfigurationContext(configuration)
    return ContextThemeWrapper(activity, 0).apply {
        applyOverrideConfiguration(configuration)
    }
}
