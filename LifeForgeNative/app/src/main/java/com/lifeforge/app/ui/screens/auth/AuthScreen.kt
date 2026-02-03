package com.lifeforge.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeforge.app.ui.components.GradientButton
import com.lifeforge.app.ui.theme.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.app.R
import androidx.compose.ui.res.painterResource

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    initialIsLogin: Boolean = false,
    initialName: String = "",
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Navigate on success
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthSuccess()
        }
    }

    var isLogin by remember { mutableStateOf(initialIsLogin) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(initialName) }
    var showPassword by remember { mutableStateOf(false) }
    var isPolicyAccepted by remember { mutableStateOf(false) }
    
    // Floating Animation
    val infiniteTransition = rememberInfiniteTransition(label = "hero_float_auth")
    val dy by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dy"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Main Auth Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp) // Wider card (User Request)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E293B).copy(alpha = 0.95f), // Lighter Slate
                            Color(0xFF0F172A).copy(alpha = 0.98f)  // Dark Slate
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(White.copy(alpha = 0.2f), White.copy(alpha = 0.05f))
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp) // Less top padding needed here compared to onboarding as text is smaller? actually stick to 40dp for consistency if image overlaps
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                 // Header
                Text(
                    text = if (isLogin) "Welcome Back" else "Create Account",
                    style = MaterialTheme.typography.headlineMedium,
                    color = White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isLogin) "Enter details to access your forge." else "Start your productivity journey.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // --- Form Fields ---

                if (!isLogin && initialName.isBlank()) { // Only ask for name if not provided
                    AuthTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Full Name",
                        placeholder = "e.g. John Doe",
                        icon = Icons.Outlined.Person
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    placeholder = "you@example.com",
                    icon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                AuthTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    placeholder = "••••••••",
                    icon = Icons.Outlined.Lock,
                    isPassword = true,
                    showPassword = showPassword,
                    onTogglePassword = { showPassword = !showPassword }
                )
                
                if (isLogin) {
                    TextButton(
                        onClick = { /* Forgot password */ },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot Password?", color = TextSecondary, fontSize = 12.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Submit Button
                GradientButton(
                    text = if (isLogin) "Sign In" else "Sign Up",
                    onClick = {
                         if (isLogin) {
                            viewModel.signIn(email, password)
                        } else {
                            // Validation now handled in ViewModel
                            viewModel.signUp(name.trim(), email.trim(), password, isPolicyAccepted)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    isLoading = uiState.isLoading
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Switch Mode
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isLogin) "Don't have an account? " else "Already have an account? ",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isLogin) "Sign Up" else "Log In",
                        color = Accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { 
                            isLogin = !isLogin
                            viewModel.clearError() 
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isLogin) {
                     Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Checkbox(
                            checked = isPolicyAccepted,
                            onCheckedChange = { isPolicyAccepted = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Accent,
                                checkmarkColor = White,
                                uncheckedColor = TextSecondary
                            ),
                            modifier = Modifier.scale(0.8f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I read & agree to Privacy Policy",
                            color = Accent,
                            style = MaterialTheme.typography.bodySmall.copy(
                                 textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                 fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable { onPrivacyPolicyClick() }
                        )
                    }
                }
            }
        }
        
        // Floating Hero Image
        Box(
            modifier = Modifier
                .align(Alignment.Center) 
                .offset(x = 100.dp, y = (-250).dp + dy.dp) // Adjusted offset for AuthScreen which allows scrolling
        ) {
             androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.auth_hero),
                contentDescription = "Guardian",
                modifier = Modifier.size(180.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }

        // Error Dialog
        if (uiState.error != null) {
            com.lifeforge.app.ui.components.AuthErrorDialog(
                message = uiState.error!!,
                onDismiss = { viewModel.clearError() }
            )
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    val themeMode = LocalThemeMode.current
    val isDark = when(themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isDark) TextSecondary else TextSecondaryLight,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = TextSecondary.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextSecondary
                )
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { onTogglePassword?.invoke() }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = TextSecondary
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = White.copy(alpha = 0.08f), // Glass fill
                unfocusedContainerColor = White.copy(alpha = 0.05f), // Glass fill
                focusedBorderColor = Accent.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.Transparent, // No border for glass look usually, or very subtle
                focusedTextColor = White,
                unfocusedTextColor = White,
                cursorColor = Accent
            )
        )
    }
}
