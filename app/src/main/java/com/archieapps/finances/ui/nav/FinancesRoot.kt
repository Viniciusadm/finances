package com.archieapps.finances.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.archieapps.finances.LocalContainer
import kotlinx.coroutines.launch
import com.archieapps.finances.data.net.Session
import com.archieapps.finances.ui.components.IconBadge
import com.archieapps.finances.ui.screen.aientry.AiEntrySheet
import com.archieapps.finances.ui.screen.accounts.AccountDetailScreen
import com.archieapps.finances.ui.screen.accounts.AccountsScreen
import com.archieapps.finances.ui.screen.bills.BillsScreen
import com.archieapps.finances.ui.screen.cards.CardsScreen
import com.archieapps.finances.ui.screen.categories.CategoriesScreen
import com.archieapps.finances.ui.screen.composer.ComposerSheet
import com.archieapps.finances.ui.screen.connect.ConnectScreen
import com.archieapps.finances.ui.screen.lock.LockScreen
import com.archieapps.finances.ui.screen.entries.EntriesScreen
import com.archieapps.finances.ui.screen.installments.InstallmentsScreen
import com.archieapps.finances.ui.screen.composer.ItemsSheet
import com.archieapps.finances.ui.screen.overview.OverviewScreen
import com.archieapps.finances.ui.screen.people.PeopleScreen
import com.archieapps.finances.ui.screen.products.ProductsScreen
import com.archieapps.finances.ui.screen.recurrences.RecurrencesScreen
import com.archieapps.finances.ui.screen.reports.ReportsScreen
import com.archieapps.finances.ui.screen.settings.SettingsScreen
import com.archieapps.finances.ui.screen.statement.StatementScreen
import com.archieapps.finances.ui.screen.tags.TagsScreen
import com.archieapps.finances.ui.theme.AppIcons
import com.archieapps.finances.ui.theme.Ink
import com.archieapps.finances.ui.theme.OnPrimary
import com.archieapps.finances.ui.theme.Primary
import com.archieapps.finances.ui.theme.PrimaryBrush
import com.archieapps.finances.ui.theme.Surface
import com.archieapps.finances.ui.theme.SurfaceLift
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextStrong

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Overview("overview", "Início", AppIcons.Home),
    Entries("entries", "Extrato", AppIcons.List),
    Bills("bills", "Contas", AppIcons.Receipt),
    Cards("cards", "Cartões", AppIcons.CreditCard),
    Settings("settings", "Mais", AppIcons.Grid)
}

@Composable
fun FinancesRoot() {
    val container = LocalContainer.current
    val session by container.sessionStore.session.collectAsStateWithLifecycle(
        initialValue = Session(token = "", baseUrl = "")
    )
    var ready by remember { mutableStateOf(false) }
    var unlocked by remember { mutableStateOf(container.codeGate.unlocked) }

    LaunchedEffect(session) {
        if (session.baseUrl.isNotBlank()) {
            container.sessionHolder.baseUrl = session.baseUrl
            container.sessionHolder.token = session.token
            ready = true
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        when {
            !ready -> Unit
            !session.authenticated -> ConnectScreen()
            !unlocked -> LockScreen(onUnlocked = { unlocked = true })
            else -> SignedIn()
        }
    }
}

@Composable
private fun SignedIn() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val tab = Tab.entries.firstOrNull { it.route == route }

    var composerFor by remember { mutableStateOf<ComposerRequest?>(null) }
    var itemsFor by remember { mutableStateOf<Int?>(null) }
    var aiEntryOpen by remember { mutableStateOf(false) }
    var composerMenuOpen by remember { mutableStateOf(false) }
    var dataVersion by remember { mutableIntStateOf(0) }

    val selectTab: (Tab) -> Unit = { selected ->
        if (selected.route != route) {
            navController.navigate(selected.route) {
                popUpTo(Tab.Overview.route) { inclusive = selected == Tab.Overview }
                launchSingleTop = true
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .then(if (tab == null) Modifier.navigationBarsPadding() else Modifier)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Tab.Overview.route,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) {
                    composable(Tab.Overview.route) {
                        key(dataVersion) {
                            OverviewScreen(
                                onOpenEntry = { id -> composerFor = ComposerRequest(id) },
                                onOpenStatement = { cardId -> navController.navigate("statement/$cardId") },
                                onOpenReports = { navController.navigate("reports") },
                                onOpenBills = { selectTab(Tab.Bills) },
                                onOpenAccount = { id -> navController.navigate("account/$id") },
                                refreshKey = dataVersion
                            )
                        }
                    }
                    composable(Tab.Entries.route) {
                        key(dataVersion) {
                            EntriesScreen(onEdit = { id -> composerFor = ComposerRequest(id) })
                        }
                    }
                    composable(Tab.Cards.route) {
                        CardsScreen(onOpenStatement = { cardId -> navController.navigate("statement/$cardId") })
                    }
                    composable(Tab.Settings.route) {
                        SettingsScreen(onOpen = { destination -> navController.navigate(destination) })
                    }
                    composable(
                        "accounts?edit={edit}",
                        arguments = listOf(navArgument("edit") {
                            type = NavType.IntType
                            defaultValue = -1
                        })
                    ) { entry ->
                        AccountsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenAccount = { id -> navController.navigate("account/$id") },
                            editAccountId = entry.arguments?.getInt("edit")?.takeIf { it > 0 }
                        )
                    }
                    composable("account/{accountId}") { entry ->
                        val accountId = entry.arguments?.getString("accountId")?.toIntOrNull() ?: 0

                        AccountDetailScreen(
                            accountId = accountId,
                            onBack = { navController.popBackStack() },
                            onEdit = { id -> navController.navigate("accounts?edit=$id") },
                            onOpenEntry = { id -> composerFor = ComposerRequest(id) },
                            refreshKey = dataVersion
                        )
                    }
                    composable(Tab.Bills.route) {
                        BillsScreen(
                            onBack = null,
                            onChanged = { dataVersion++ }
                        )
                    }
                    composable("categories") { CategoriesScreen(onBack = { navController.popBackStack() }) }
                    composable("tags") { TagsScreen(onBack = { navController.popBackStack() }) }
                    composable("people") {
                        key(dataVersion) {
                            PeopleScreen(
                                onBack = { navController.popBackStack() },
                                onOpenEntry = { id -> composerFor = ComposerRequest(id) }
                            )
                        }
                    }
                    composable("reports") { ReportsScreen(onBack = { navController.popBackStack() }) }
                    composable("products") { ProductsScreen(onBack = { navController.popBackStack() }) }
                    composable("installments") {
                        key(dataVersion) {
                            InstallmentsScreen(
                                onBack = { navController.popBackStack() },
                                onOpenEntry = { id -> composerFor = ComposerRequest(id) }
                            )
                        }
                    }
                    composable("recurrences") {
                        RecurrencesScreen(
                            onBack = { navController.popBackStack() },
                            onOpenBills = { selectTab(Tab.Bills) }
                        )
                    }
                    composable("statement/{cardId}") { entry ->
                        val cardId = entry.arguments?.getString("cardId")?.toIntOrNull() ?: 0

                        StatementScreen(
                            cardId = cardId,
                            onBack = { navController.popBackStack() },
                            onOpenEntry = { id -> composerFor = ComposerRequest(id) },
                            refreshKey = dataVersion
                        )
                    }
                }
            }

            if (tab != null) {
                TabBar(current = tab, onSelect = selectTab)
            }
        }

        if (tab == Tab.Overview || tab == Tab.Entries || tab == Tab.Bills) {
            AnimatedVisibility(visible = composerMenuOpen, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Ink.copy(alpha = 0.72f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { composerMenuOpen = false }
                )
            }

            ComposerLauncher(
                expanded = composerMenuOpen,
                onToggle = { composerMenuOpen = !composerMenuOpen },
                onManual = {
                    composerMenuOpen = false
                    composerFor = ComposerRequest(null)
                },
                onByText = {
                    composerMenuOpen = false
                    aiEntryOpen = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = 92.dp)
            )
        }
    }

    if (aiEntryOpen) {
        AiEntrySheet(
            onDismiss = { aiEntryOpen = false },
            onSaved = {
                aiEntryOpen = false
                dataVersion++
            }
        )
    }

    val request = composerFor

    if (request != null) {
        ComposerSheet(
            editingId = request.entryId,
            onDismiss = { composerFor = null },
            onSaved = {
                composerFor = null
                dataVersion++
            },
            onOpenItems = { entryId ->
                composerFor = null
                itemsFor = entryId
            }
        )
    }

    val itemsEntry = itemsFor

    if (itemsEntry != null) {
        ItemsSheet(
            entryId = itemsEntry,
            onDismiss = { itemsFor = null },
            onSaved = {
                itemsFor = null
                dataVersion++
            }
        )
    }
}

private data class ComposerRequest(val entryId: Int?)

@Composable
private fun ComposerLauncher(
    expanded: Boolean,
    onToggle: () -> Unit,
    onManual: () -> Unit,
    onByText: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 135f else 0f, label = "composer-plus")

    Column(modifier, horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LauncherOption(label = "Lançar por texto", hint = "Descreva e a IA preenche", icon = AppIcons.Sparkle, onClick = onByText)
                LauncherOption(label = "Lançamento manual", hint = "Valor, conta e categoria", icon = AppIcons.Edit, onClick = onManual)
                Box(Modifier.height(4.dp))
            }
        }

        Box(
            Modifier
                .shadow(16.dp, CircleShape, ambientColor = Primary, spotColor = Primary)
                .size(60.dp)
                .clip(CircleShape)
                .background(PrimaryBrush)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Add,
                contentDescription = "Novo lançamento",
                tint = OnPrimary,
                modifier = Modifier
                    .size(30.dp)
                    .rotate(rotation)
            )
        }
    }
}

@Composable
private fun LauncherOption(label: String, hint: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceLift)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, color = Primary, size = 36)
        Column(Modifier.padding(start = 12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = TextStrong)
            Text(text = hint, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
private fun TabBar(current: Tab, onSelect: (Tab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Surface)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Tab.entries.forEach { tab ->
            val active = tab == current
            val pill by animateColorAsState(if (active) Primary.copy(alpha = 0.18f) else Color.Transparent, label = "tab-pill")
            val tint by animateColorAsState(if (active) Primary else TextFaint, label = "tab-tint")

            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(tab) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(pill)
                        .padding(horizontal = 18.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                }
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (active) FontWeight.W700 else FontWeight.W600
                    ),
                    color = if (active) TextStrong else TextFaint,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
