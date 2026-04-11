package com.workouttracker.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.workouttracker.R
import com.workouttracker.ui.viewmodel.SyncState
import com.workouttracker.ui.viewmodel.SyncViewModel
import com.workouttracker.ui.viewmodel.formatSyncTime

@Composable
fun AccountScreen(syncViewModel: SyncViewModel) {
    val context = LocalContext.current
    val user by syncViewModel.user.collectAsState()
    val syncStatus by syncViewModel.syncStatus.collectAsState()
    var showRestoreDialog by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf<String?>(null) }

    // 0 = Google, 1 = Email
    var selectedSignInTab by remember { mutableIntStateOf(1) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isCreatingAccount by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        if (user != null) syncViewModel.fetchLastSyncTime()
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken == null) {
                    signInError = "Google did not return an ID token. Check your Firebase Web Client ID."
                    return@rememberLauncherForActivityResult
                }
                syncViewModel.handleGoogleSignInResult(
                    idToken  = idToken,
                    onSuccess = { signInError = null },
                    onError   = { signInError = it }
                )
            } catch (e: ApiException) {
                signInError = "Google Sign-In failed (code ${e.statusCode}). " +
                    "Make sure your debug SHA-1 is added to Firebase console."
            } catch (e: Exception) {
                signInError = "Unexpected error: ${e.localizedMessage}"
            }
        } else {
            // Only show error for non-cancel result codes (0 = user cancelled, which is fine)
            if (result.resultCode != 0) {
                signInError = "Sign-in returned unexpected result code: ${result.resultCode}"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(stringResource(R.string.cloud_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.optional_backup),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (user == null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape  = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudOff, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(stringResource(R.string.no_account_needed),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.no_account_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(20.dp))

                        // Sign-in method tabs
                        TabRow(
                            selectedTabIndex = selectedSignInTab,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        ) {
                            Tab(
                                selected = selectedSignInTab == 0,
                                onClick = { selectedSignInTab = 0; signInError = null },
                                text = { Text("Google", fontWeight = FontWeight.SemiBold) }
                            )
                            Tab(
                                selected = selectedSignInTab == 1,
                                onClick = { selectedSignInTab = 1; signInError = null },
                                text = { Text("Email", fontWeight = FontWeight.SemiBold) }
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Error card
                        if (signInError != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Default.ErrorOutline, null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp).padding(top = 2.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(signInError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }

                        // Google sign-in
                        if (selectedSignInTab == 0) {
                            Button(
                                onClick = {
                                    signInError = null
                                    try {
                                        val client = syncViewModel.getGoogleSignInClient(context)
                                        signInLauncher.launch(client.signInIntent)
                                    } catch (e: Exception) {
                                        signInError = "Launch error: ${e.message}"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                            ) {
                                Icon(Icons.Default.AccountCircle, null,
                                    modifier = Modifier.size(20.dp), tint = Color.White)
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource(R.string.sign_in_google),
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "If Google sign-in fails, use the Email tab instead",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Email/password sign-in
                        if (selectedSignInTab == 1) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                leadingIcon = { Icon(Icons.Default.Email, null) }
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = if (passwordVisible) VisualTransformation.None
                                                       else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                leadingIcon = { Icon(Icons.Default.Lock, null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.VisibilityOff
                                            else Icons.Default.Visibility, null
                                        )
                                    }
                                }
                            )
                            Spacer(Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    signInError = null
                                    if (email.isBlank() || password.isBlank()) {
                                        signInError = "Please enter your email and password"
                                        return@Button
                                    }
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                        signInError = "Please enter a valid email address"
                                        return@Button
                                    }
                                    if (isCreatingAccount && password.length < 6) {
                                        signInError = "Password must be at least 6 characters"
                                        return@Button
                                    }
                                    isLoading = true
                                    if (isCreatingAccount) {
                                        syncViewModel.createAccountWithEmail(email, password,
                                            onSuccess = { isLoading = false; signInError = null },
                                            onError   = { isLoading = false; signInError = it })
                                    } else {
                                        syncViewModel.signInWithEmail(email, password,
                                            onSuccess = { isLoading = false; signInError = null },
                                            onError   = { isLoading = false; signInError = it })
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text(
                                        if (isCreatingAccount) "Create Account" else "Sign In",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = {
                                isCreatingAccount = !isCreatingAccount
                                signInError = null
                            }) {
                                Text(
                                    if (isCreatingAccount) "Already have an account? Sign in"
                                    else "No account yet? Create one",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (!isCreatingAccount) {
                                TextButton(onClick = { showForgotPasswordDialog = true }) {
                                    Text(
                                        "Forgot password?",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { SecurityInfoCard() }
        } else {
            val currentUser = user!!
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape  = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                currentUser.displayName?.firstOrNull()?.uppercase()
                                    ?: currentUser.email?.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentUser.displayName ?: currentUser.email ?: "User",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface)
                            if (currentUser.displayName != null) {
                                Text(currentUser.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(stringResource(R.string.signed_in),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                val lastSyncStr = syncStatus.lastSyncTime?.let { formatSyncTime(it) }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (syncStatus.state) {
                            SyncState.SUCCESS -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            SyncState.ERROR   -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            SyncState.SYNCING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            SyncState.IDLE    -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (syncStatus.state) {
                            SyncState.SYNCING ->
                                CircularProgressIndicator(modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            SyncState.SUCCESS ->
                                Icon(Icons.Default.CheckCircle, null,
                                    tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            SyncState.ERROR ->
                                Icon(Icons.Default.ErrorOutline, null,
                                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            SyncState.IDLE ->
                                Icon(Icons.Default.Cloud, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                when (syncStatus.state) {
                                    SyncState.SYNCING -> syncStatus.message
                                    SyncState.SUCCESS -> syncStatus.message
                                    SyncState.ERROR   -> syncStatus.message
                                    SyncState.IDLE    ->
                                        if (lastSyncStr != null) stringResource(R.string.last_backup, lastSyncStr)
                                        else stringResource(R.string.never_backed_up)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick   = { syncViewModel.uploadToCloud() },
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(14.dp),
                        enabled   = syncStatus.state != SyncState.SYNCING
                    ) {
                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.backup_now), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }

                    OutlinedButton(
                        onClick   = { showRestoreDialog = true },
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(14.dp),
                        enabled   = syncStatus.state != SyncState.SYNCING
                    ) {
                        Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.restore_from_cloud), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item { SecurityInfoCard() }

            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick  = { syncViewModel.signOut() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Logout, null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.sign_out),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            prefillEmail = email,
            onDismiss = { showForgotPasswordDialog = false },
            onSend = { resetEmail, onSuccess, onError ->
                syncViewModel.sendPasswordResetEmail(resetEmail, onSuccess, onError)
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            icon  = { Icon(Icons.Default.CloudDownload, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.restore_confirm_title), fontWeight = FontWeight.Bold) },
            text  = { Text(stringResource(R.string.restore_confirm_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDialog = false
                        syncViewModel.restoreFromCloud(onComplete = {})
                    },
                    shape = RoundedCornerShape(10.dp)
                ) { Text(stringResource(R.string.restore_button)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreDialog = false },
                    shape = RoundedCornerShape(10.dp)) { Text(stringResource(R.string.cancel)) }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun ForgotPasswordDialog(
    prefillEmail: String,
    onDismiss: () -> Unit,
    onSend: (String, () -> Unit, (String) -> Unit) -> Unit
) {
    var resetEmail by remember { mutableStateOf(prefillEmail) }
    var isSending by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) } // true=success

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        icon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Enter your email address and we'll send you a link to reset your password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it; result = null },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    enabled = !isSending
                )
                result?.let { (success, msg) ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (success)
                                Color(0xFF4CAF50).copy(alpha = 0.12f)
                            else
                                MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                null,
                                tint = if (success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp).padding(top = 1.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (resetEmail.isBlank()) return@Button
                    isSending = true
                    result = null
                    onSend(
                        resetEmail,
                        {
                            isSending = false
                            result = true to "Reset link sent! Check your inbox."
                        },
                        { err ->
                            isSending = false
                            result = false to err
                        }
                    )
                },
                shape = RoundedCornerShape(10.dp),
                enabled = !isSending && resetEmail.isNotBlank()
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send Reset Link")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                enabled = !isSending
            ) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun SecurityInfoCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.data_private_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
            }
            SecurityPoint(Icons.Default.Lock, stringResource(R.string.security_point_1))
            SecurityPoint(Icons.Default.Person, stringResource(R.string.security_point_2))
            SecurityPoint(Icons.Default.PhoneAndroid, stringResource(R.string.security_point_3))
            SecurityPoint(Icons.Default.DeleteForever, stringResource(R.string.security_point_4))
        }
    }
}

@Composable
fun SecurityPoint(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(15.dp).padding(top = 2.dp))
        Spacer(Modifier.width(8.dp))
        Text(text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp)
    }
}
