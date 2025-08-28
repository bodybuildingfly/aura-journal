package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mabbology.aurajournal.ui.viewmodel.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    partnersViewModel: PartnersViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val partnersState by partnersViewModel.state.collectAsState()
    val profileState by profileViewModel.profileState.collectAsState()

    val noteViewModel: NoteViewModel = hiltViewModel()

    val screens = listOf("Journal", "Assignments", "Notes")
    val icons = listOf(Icons.Default.Book, Icons.AutoMirrored.Filled.Assignment, Icons.Default.EditNote)

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
                    title = { Text(screens[pagerState.currentPage]) },
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
                    screens.forEachIndexed { index, title ->
                        NavigationBarItem(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = { Icon(icons[index], contentDescription = title) },
                            label = { Text(title) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(paddingValues)
            ) { page ->
                when (page) {
                    0 -> JournalListScreen(navController = navController)
                    1 -> AssignmentListScreen(navController = navController)
                    2 -> NoteListScreen(
                        navController = navController,
                        viewModel = noteViewModel
                    )
                }
            }
        }
    }
}
