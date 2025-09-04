package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mabbology.aurajournal.ui.viewmodel.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel = hiltViewModel(),
    partnersViewModel: PartnersViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val mainScreenState by mainViewModel.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { if (mainScreenState.selectedScope is Scope.Personal) 2 else 3 })
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var expanded by remember { mutableStateOf(false) }

    val partnersState by partnersViewModel.state.collectAsState()
    val profileState by profileViewModel.profileState.collectAsState()

    val noteViewModel: NoteViewModel = hiltViewModel()

    data class Screen(val title: String, val icon: ImageVector, val content: @Composable () -> Unit)

    val personalScreens = listOf(
        Screen("Journal", Icons.Default.Book, { JournalListScreen(navController = navController, scope = mainScreenState.selectedScope) }),
        Screen("Notes", Icons.Default.EditNote, { NoteListScreen(navController = navController, viewModel = noteViewModel, scope = mainScreenState.selectedScope) })
    )

    val partnerScreens = listOf(
        Screen("Journal", Icons.Default.Book, { JournalListScreen(navController = navController, scope = mainScreenState.selectedScope) }),
        Screen("Assignments", Icons.AutoMirrored.Filled.Assignment, { AssignmentListScreen(navController = navController, scope = mainScreenState.selectedScope) }),
        Screen("Notes", Icons.Default.EditNote, { NoteListScreen(navController = navController, viewModel = noteViewModel, scope = mainScreenState.selectedScope) })
    )

    val screens = if (mainScreenState.selectedScope is Scope.Personal) personalScreens else partnerScreens

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                navController = navController,
                authViewModel = authViewModel,
                closeDrawer = {
                    coroutineScope.launch {
                        drawerState.close()
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Box {
                            Row(
                                modifier = Modifier.clickable { expanded = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(mainScreenState.selectedScope.displayName)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                mainScreenState.scopes.forEach { scope ->
                                    DropdownMenuItem(
                                        text = { Text(scope.displayName) },
                                        onClick = {
                                            mainViewModel.selectScope(scope)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    actions = {
                        if (partnersState.partners.isNotEmpty()) {
                            IconButton(onClick = {
                                if (partnersState.partners.size == 1) {
                                    val partner = partnersState.partners.first()
                                    val partnerId = if (partner.dominantId == profileState.userId) partner.submissiveId else partner.dominantId
                                    navController.navigate("chat/${partner.id}/$partnerId")
                                } else {
                                    navController.navigate("partners")
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    screens.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(paddingValues)
            ) { page ->
                screens[page].content()
            }
        }
    }
}
