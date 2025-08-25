package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mabbology.aurajournal.ui.viewmodel.AuthViewModel
import com.mabbology.aurajournal.ui.viewmodel.PartnersViewModel
import com.mabbology.aurajournal.ui.viewmodel.ProfileViewModel

@Composable
fun AppDrawer(
    navController: NavController,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    partnersViewModel: PartnersViewModel = hiltViewModel(),
    closeDrawer: () -> Unit
) {
    val profileState by profileViewModel.profileState.collectAsState()
    val partnersState by partnersViewModel.state.collectAsState()

    // Determine if the user is a submissive in any of their partnerships
    val isSubmissive = partnersState.partners.any { it.submissiveId == profileState.userId }

    ModalDrawerSheet {
        Spacer(Modifier.height(12.dp))
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = false,
            onClick = {
                navController.navigate("profile")
                closeDrawer()
            }
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Favorite, contentDescription = "Partners") },
            label = { Text("Partners") },
            selected = false,
            onClick = {
                navController.navigate("partners") // Updated route
                closeDrawer()
            }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        NavigationDrawerItem(
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout") },
            label = { Text("Logout") },
            selected = false,
            onClick = {
                authViewModel.logout()
                closeDrawer()
            }
        )
    }
}