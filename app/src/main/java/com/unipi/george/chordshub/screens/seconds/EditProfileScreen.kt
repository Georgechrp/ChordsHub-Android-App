package com.unipi.george.chordshub.screens.seconds

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.unipi.george.chordshub.R
import com.unipi.george.chordshub.repository.AuthRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.CoroutineScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(navController: NavController, userId: String, onDismiss: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // ✅ Αποθηκεύουμε την αρχική κατάσταση
    val initialUsername = remember { AuthRepository.fullNameState.value ?: "Unknown" }
    var newUsername by remember { mutableStateOf(initialUsername) }

    val initialImageUri = remember { mutableStateOf<Uri?>(null) }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            ProfileCard(
                selectedImage = selectedImage,
                newUsername = newUsername,
                onUsernameChange = { newUsername = it },
                onImageSelected = { uri -> selectedImage = uri },
                onSave = {
                    keyboardController?.hide()

                    val usernameChanged = newUsername != initialUsername
                    val imageChanged = selectedImage != initialImageUri.value

                    saveProfileChanges(
                        userId,
                        newUsername,
                        selectedImage,
                        snackbarHostState,
                        coroutineScope,
                        usernameChanged,
                        imageChanged
                    )
                },
                onCancel = {
                    keyboardController?.hide()
                    onDismiss()
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun ProfileCard(
    selectedImage: Uri?,
    newUsername: String,
    onUsernameChange: (String) -> Unit,
    onImageSelected: (Uri?) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val userId = AuthRepository.getUserId() ?: return
    Card(
        modifier = Modifier.width(320.dp).padding(24.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileImagePicker(selectedImage, onImageSelected, userId)

            UsernameInputField(newUsername, onUsernameChange)
            Spacer(modifier = Modifier.height(20.dp))
            SaveButton(onSave)
            CancelButton(onCancel)
        }
    }
}

@Composable
fun ProfileImagePicker(selectedImage: Uri?, onImageSelected: (Uri?) -> Unit, userId: String) {
    val context = LocalContext.current
    var imageUrl by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        coroutineScope.launch {
            imageUrl = getProfileImageUrl(userId)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImageSelected(uri)
    }

    Image(
        painter = if (selectedImage != null) {
            rememberAsyncImagePainter(selectedImage)
        } else if (imageUrl != null) {
            rememberAsyncImagePainter(imageUrl)
        } else {
            painterResource(id = R.drawable.edit_user_image)
        },
        contentDescription = "Profile Image",
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .clickable { launcher.launch("image/*") },
        contentScale = ContentScale.Crop
    )
}

suspend fun getProfileImageUrl(userId: String): String? {
    return try {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .await()

        snapshot.getString("profileImageUrl")
    } catch (e: Exception) {
        null
    }
}


suspend fun uploadImageToFirebase(imageUri: Uri, userId: String): String {
    val storageRef = FirebaseStorage.getInstance().reference
    val imageRef = storageRef.child("profile_pictures/$userId.jpg")

    return try {
        imageRef.putFile(imageUri).await() // ✅ Ανεβάζουμε την εικόνα
        imageRef.downloadUrl.await().toString() // ✅ Παίρνουμε το URL και το επιστρέφουμε
    } catch (e: Exception) {
        throw Exception("Failed to upload image: ${e.message}")
    }
}

fun saveProfileChanges(
    userId: String,
    newUsername: String,
    newImage: Uri?,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    usernameChanged: Boolean,
    imageChanged: Boolean
) {
    coroutineScope.launch {
        try {
            var imageUrl: String? = null

            // ✅ Αν υπάρχει νέα εικόνα, την ανεβάζουμε στο Firebase Storage
            if (imageChanged && newImage != null) {
                imageUrl = uploadImageToFirebase(newImage, userId)
            }

            // ✅ Ενημέρωση Firestore με το νέο username και το image URL
            val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)

            val updates = mutableMapOf<String, Any>()
            if (usernameChanged) updates["username"] = newUsername
            if (imageUrl != null) updates["profileImageUrl"] = imageUrl

            if (updates.isNotEmpty()) {
                userRef.update(updates).await()
            }

            // ✅ Εμφάνιση μηνύματος επιτυχίας
            val message = when {
                usernameChanged && imageChanged -> "Username and profile picture updated!"
                usernameChanged -> "Username updated successfully!"
                imageChanged -> "Profile picture updated successfully!"
                else -> "No changes were made."
            }

            snackbarHostState.showSnackbar(message)

        } catch (e: Exception) {
            snackbarHostState.showSnackbar("Error updating profile: ${e.message}")
        }
    }
}


@Composable
fun UsernameInputField(newUsername: String, onUsernameChange: (String) -> Unit) {
    OutlinedTextField(
        value = newUsername,
        onValueChange = onUsernameChange,
        label = { Text("Username") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SaveButton(onSave: () -> Unit) {
    Button(
        onClick = onSave,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Save Changes")
    }
}

@Composable
fun CancelButton(onCancel: () -> Unit) {
    TextButton(
        onClick = onCancel
    ) {
        Text("Cancel", color = Color.Gray)
    }
}

fun saveUsername(
    userId: String,
    newUsername: String,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    AuthRepository.saveUsername(userId, newUsername) { success, errorMessage ->
        coroutineScope.launch {
            if (success) {
                snackbarHostState.showSnackbar("Username updated successfully!")
            } else {
                snackbarHostState.showSnackbar(errorMessage ?: "Failed to update username.")
            }
        }
    }
}
