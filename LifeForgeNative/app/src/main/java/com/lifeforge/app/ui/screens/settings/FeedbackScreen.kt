package com.lifeforge.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.app.ui.components.GlassCard
import com.lifeforge.app.ui.components.GradientButton
import com.lifeforge.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    viewModel: FeedbackViewModel = hiltViewModel()
) {
    var feedbackText by remember { mutableStateOf("") }
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val submitSuccess by viewModel.submitSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(submitSuccess) {
        if (submitSuccess) {
            // Show success and go back after delay or just stay
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SEND FEEDBACK",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = White,
                    navigationIconContentColor = White
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Primary, Color(0xFF020617))))
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your feedback helps Stoppy grow stronger. Tell us what's on your mind.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                if (submitSuccess) {
                    SuccessView(onBack)
                } else {
                    GlassCard(glowColor = Accent.copy(alpha = 0.2f)) {
                        Column {
                            TextField(
                                value = feedbackText,
                                onValueChange = { feedbackText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                placeholder = { Text("Type your message here...", color = TextSecondary.copy(alpha = 0.5f)) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = White,
                                    unfocusedTextColor = White,
                                    cursorColor = Accent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = Alert,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    GradientButton(
                        text = if (isSubmitting) "SENDING..." else "SEND FEEDBACK",
                        onClick = { viewModel.submitFeedback(feedbackText) },
                        icon = Icons.Default.Send,
                        enabled = feedbackText.isNotBlank() && !isSubmitting
                    )
                }
            }
        }
    }
}

@Composable
private fun SuccessView(onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        Text("✨", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "MESSAGE SENT!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = Success
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Thank you for your valuable feedback.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = onBack) {
            Text("GO BACK", color = Accent, fontWeight = FontWeight.Bold)
        }
    }
}
