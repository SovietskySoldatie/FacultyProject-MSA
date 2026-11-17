package com.example.securevault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Screen 1: AppIntroductionScreen
 *
 * Requirements:
 * "The 1st screen category (AppIntroductionScreen) I want will have either 1 or 2 buttons.
 * If a login password (momentarily consider the app to be single-user, local) is not set,
 * only 1 button is available, leading to the SetPasswordScreen.
 * If a login password is already set, the 2 buttons available are one that leads to LoginScreen
 * and one that leads to ResetPasswordScreen."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIntroductionScreen(
    isPasswordSet: Boolean,
    onNavigateToSetPassword: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToResetPassword: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SecureVault") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to SecureVault",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isPasswordSet) {
                    "Your personal notes and encrypted vault are protected."
                } else {
                    "No master password is currently set. Please configure your password to get started."
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(36.dp))

            if (!isPasswordSet) {
                // Exactly 1 button when password is not set
                Button(
                    onClick = onNavigateToSetPassword,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Set Password")
                }
            } else {
                // Exactly 2 buttons when password is set
                Button(
                    onClick = onNavigateToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Log In")
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onNavigateToResetPassword,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Reset Password")
                }
            }
        }
    }
}
