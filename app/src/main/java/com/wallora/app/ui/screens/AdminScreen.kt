package com.wallora.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wallora.app.ui.WallpaperViewModel
import com.wallora.app.util.Analytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: WallpaperViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val signedIn by viewModel.isAdminSignedIn.collectAsStateWithLifecycle()

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (signedIn) {
                        TextButton(onClick = { viewModel.signOut() }) { Text("Sign out") }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (!signedIn) {
            SignInForm(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onSubmit = { email, password, onError ->
                    scope.launch {
                        val result = viewModel.signIn(email, password)
                        if (result.isFailure) {
                            onError(result.exceptionOrNull()?.message ?: "Sign in failed")
                        }
                    }
                },
            )
        } else {
            AdminContent(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                notify = ::notify,
            )
        }
    }
}

@Composable
private fun SignInForm(
    modifier: Modifier,
    onSubmit: (String, String, (String) -> Unit) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Sign in to manage wallpapers", style = MaterialTheme.typography.titleMedium)
        Text(
            "Use the admin account you created in the Firebase console.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; error = null },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = {
                loading = true
                onSubmit(email.trim(), password) { message ->
                    error = message
                    loading = false
                }
            },
            enabled = !loading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (loading) "Signing in…" else "Sign in")
        }
    }
}

@Composable
private fun AdminContent(
    viewModel: WallpaperViewModel,
    modifier: Modifier,
    notify: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val remote = remember(wallpapers) { wallpapers.filter { it.remote } }

    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    var newCategoryId by remember { mutableStateOf("") }
    var newCategoryTitle by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        pickedUri = uri
    }

    fun upload() {
        val uri = pickedUri ?: return
        uploading = true
        progress = 0f
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
            }
            if (bytes == null) {
                uploading = false
                notify("Couldn't read the selected image")
                return@launch
            }
            val result = viewModel.uploadWallpaper(title.trim(), category.trim(), bytes) { p ->
                progress = p
            }
            uploading = false
            if (result.isSuccess) {
                Analytics.log(context, Analytics.Events.ADMIN_UPLOAD, mapOf("category" to category.trim()))
                title = ""
                pickedUri = null
                progress = 0f
                notify("Uploaded to the cloud")
            } else {
                notify("Upload failed: ${result.exceptionOrNull()?.message ?: "unknown error"}")
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Signed in as ${viewModel.adminEmail() ?: "admin"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Add wallpaper", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category id") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (categories.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories, key = { it.id }) { c ->
                                FilterChip(
                                    selected = category == c.id,
                                    onClick = { category = c.id },
                                    label = { Text(c.id) },
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (pickedUri == null) "Choose image" else "Image selected ✓")
                    }
                    if (uploading) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(
                        onClick = { upload() },
                        enabled = !uploading && pickedUri != null && title.isNotBlank() && category.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (uploading) "Uploading…" else "Upload")
                    }
                }
            }
        }

        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Add category", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = newCategoryId,
                        onValueChange = { newCategoryId = it },
                        label = { Text("Id (e.g. nature)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newCategoryTitle,
                        onValueChange = { newCategoryTitle = it },
                        label = { Text("Display name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                val result = viewModel.addCategory(newCategoryId.trim(), newCategoryTitle.trim())
                                if (result.isSuccess) {
                                    newCategoryId = ""
                                    newCategoryTitle = ""
                                    notify("Category added")
                                } else {
                                    notify("Failed: ${result.exceptionOrNull()?.message ?: "unknown error"}")
                                }
                            }
                        },
                        enabled = newCategoryId.isNotBlank() && newCategoryTitle.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Add category")
                    }
                }
            }
        }

        item {
            Text(
                text = "Cloud wallpapers (${remote.size})",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        items(remote, key = { it.id }) { wallpaper ->
            Card {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = wallpaper.model,
                        contentDescription = wallpaper.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(wallpaper.title, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            wallpaper.categoryId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                val result = viewModel.deleteWallpaper(wallpaper.id)
                                notify(if (result.isSuccess) "Deleted" else "Delete failed")
                            }
                        },
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}
