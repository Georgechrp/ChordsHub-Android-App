package com.unipi.george.chordshub.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.unipi.george.chordshub.R
import com.unipi.george.chordshub.repository.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val fullname = AuthRepository.getFullName()
    val email = AuthRepository.getUserEmail()

    var isUploadingImage by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = fullname ?: "-") },
                navigationIcon = {
                    /*IconButton(onClick = { isUploadingImage = true }) { // **Ανοίγει το UploadUserImageScreen**
                        Icon(
                            painter = painterResource(id = R.drawable.edit_user_image),
                            contentDescription = "Edit Profile Image"
                        )
                    }*/
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            /*Text(
                text = "User Profile",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )*/
            /*Text(
                text = "Full Name:" + fullname ?: "-",
                style = MaterialTheme.typography.bodyMedium
            )*/
            Text(
                text = "Email:" + email?: "-",
                style = MaterialTheme.typography.bodyMedium
            )
            /*Button(onClick = {
                navController.popBackStack() // Επιστροφή στην προηγούμενη οθόνη
            }) {
                Text("Back")
            }*/
        }
    }

}
