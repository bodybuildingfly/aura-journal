package com.mabbology.aurajournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mabbology.aurajournal.ui.viewmodel.AuthViewModel

@Composable
fun AppDrawer(
    navController: NavController,
    authViewModel: AuthViewModel,
    closeDrawer: () -> Unit
) {
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
