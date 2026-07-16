package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.example.util.BiometricHelper
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IndexedFile
import com.example.ui.viewmodel.FileManagerViewModel

@Composable
fun VaultScreen(
    viewModel: FileManagerViewModel,
    modifier: Modifier = Modifier
) {
    val isPinSet by viewModel.isPinSet.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val vaultError by viewModel.vaultError.collectAsState()
    val vaultFiles by viewModel.vaultFiles.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isPinSet) {
            // Setup PIN Flow
            VaultSetupPinView(onSetup = { viewModel.setupVaultPin(it) })
        } else if (!isVaultUnlocked) {
            // Keypad Unlock PIN Flow
            VaultUnlockKeypadView(
                errorText = vaultError,
                onUnlock = { viewModel.verifyVaultPin(it) },
                onReset = { viewModel.resetVaultPin() }
            )
        } else {
            // Unlocked Vault Files Browser
            VaultBrowserView(
                vaultFiles = vaultFiles,
                onLock = { viewModel.lockVault() },
                onRestore = { viewModel.toggleVaultStatus(it) },
                onDelete = { viewModel.deleteFile(it) }
            )
        }
    }
}

@Composable
fun VaultSetupPinView(
    onSetup: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Security,
            contentDescription = "Shield",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "पिन सेट करें (Secure Vault Setup)",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "अपनी पर्सनल फाइल्स को सुरक्षित रखने के लिए एक मजबूत 4-अंकीय (4-digit) पिन बनाएं।",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pin = it },
            label = { Text("4-अंकीय पिन दर्ज करें") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .testTag("vault_setup_pin_field"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = pinConfirm,
            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinConfirm = it },
            label = { Text("पिन की पुष्टि करें (Confirm)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .testTag("vault_setup_pin_confirm_field"),
            singleLine = true
        )

        if (localError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = localError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (pin.length != 4) {
                    localError = "पिन ठीक 4 अंकों का होना चाहिए।"
                } else if (pin != pinConfirm) {
                    localError = "दर्ज किए गए दोनों पिन आपस में मेल नहीं खाते!"
                } else {
                    localError = null
                    onSetup(pin)
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp)
                .testTag("vault_setup_btn"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("सुरक्षित पिन बनाएं")
        }
    }
}

@Composable
fun VaultUnlockKeypadView(
    errorText: String?,
    onUnlock: (String) -> Boolean,
    onReset: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    val context = LocalContext.current
    var biometricSupported by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val activity = context as? FragmentActivity
        if (activity != null) {
            biometricSupported = BiometricHelper.canAuthenticate(activity)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top status
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Vault Locked",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "वॉल्ट लॉक है (Vault is Locked)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "खोलने के लिए अपना पिन दर्ज करें",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Code entry dots indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            )
                    )
                }
            }

            if (errorText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("vault_unlock_error")
                )
            }
        }

        // Custom Numeric Keypad
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("Biometric", "0", "Back")
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { digit ->
                        if (digit == "Biometric" || digit == "Back") {
                            IconButton(
                                onClick = {
                                    if (digit == "Biometric") {
                                        val activity = context as? FragmentActivity
                                        if (activity != null) {
                                            BiometricHelper.showBiometricPrompt(
                                                activity,
                                                onSuccess = { onUnlock("BIOMETRIC") },
                                                onError = {}
                                            )
                                        }
                                        onReset()
                                    } else if (digit == "Back" && enteredPin.isNotEmpty()) {
                                        enteredPin = enteredPin.dropLast(1)
                                    }
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .testTag("keypad_btn_$digit")
                            ) {
                                if (digit == "Biometric") {
                                        val activity = context as? FragmentActivity
                                        if (activity != null) {
                                            BiometricHelper.showBiometricPrompt(
                                                activity,
                                                onSuccess = { onUnlock("BIOMETRIC") },
                                                onError = {}
                                            )
                                        }
                                    Icon(
                                        imageVector = Icons.Filled.Fingerprint,
                                        contentDescription = "Biometric Unlock",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable {
                                        if (enteredPin.length < 4) {
                                            enteredPin += digit
                                            if (enteredPin.length == 4) {
                                                val success = onUnlock(enteredPin)
                                                if (!success) {
                                                    enteredPin = "" // Clear entry on fail
                                                }
                                            }
                                        }
                                    }
                                    .testTag("keypad_btn_$digit"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultBrowserView(
    vaultFiles: List<IndexedFile>,
    onLock: () -> Unit,
    onRestore: (IndexedFile) -> Unit,
    onDelete: (IndexedFile) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Status header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LockOpen,
                        contentDescription = "Unlocked",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "सिक्योर वॉल्ट (Unlocked)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "सुरक्षित फाइलें: ${vaultFiles.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onLock,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("lock_vault_btn")
                ) {
                    Text("लॉक करें (Lock)")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (vaultFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Key,
                        contentDescription = "Key",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "वॉल्ट बिल्कुल खाली है।",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "ब्राउज़र टैब पर जाकर फाइलों को वॉल्ट में छुपाएं।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vaultFiles, key = { it.id }) { file ->
                    VaultItemRow(
                        file = file,
                        onRestore = { onRestore(file) },
                        onDelete = { onDelete(file) }
                    )
                }
            }
        }
    }
}

@Composable
fun VaultItemRow(
    file: IndexedFile,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .testTag("vault_item_${file.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Hidden File",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Encrypted • " + formatSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Restore Button (Move file back to non-vault)
                IconButton(
                    onClick = onRestore,
                    modifier = Modifier.testTag("vault_restore_btn_${file.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restore,
                        contentDescription = "Restore File",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Delete Button (Permanent removal)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("vault_delete_btn_${file.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete permanently",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
