package com.mabbology.aurajournal.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.mabbology.aurajournal.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    navController: NavController,
    authViewModel: AuthViewModel,
    currentScreenTitle: String,
    otherScreenTitle: String,
    onSwitchScreen: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            ScreenTitleDropdown(
                currentScreenTitle = currentScreenTitle,
                otherScreenTitle = otherScreenTitle,
                onOtherScreenSelected = onSwitchScreen
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        actions = {
            IconButton(onClick = { navController.navigate("partners") }) {
                Icon(
                    imageVector = Icons.Outlined.People,
                    contentDescription = "Partners"
                )
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options"
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Profile") },
                    onClick = {
                        showMenu = false
                        navController.navigate("profile")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Logout") },
                    onClick = {
                        showMenu = false
                        authViewModel.logout()
                    }
                )
            }
        }
    )
}
