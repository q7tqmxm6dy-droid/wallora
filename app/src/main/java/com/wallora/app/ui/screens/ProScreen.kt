package com.wallora.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.R
import com.wallora.app.ads.AdsConfig
import com.wallora.app.billing.ProManager
import com.wallora.app.billing.ProMessage

/** The Wallora Pro paywall: what it unlocks, the price from Play, buy and restore. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state by ProManager.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val message = state.message?.let { messageText(it) }
    LaunchedEffect(state.message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            ProManager.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFFFC857), Color(0xFFFF7A59))),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.pro_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.pro_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(28.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (AdsConfig.ADS_ENABLED) Benefit(stringResource(R.string.pro_benefit_noads))
                Benefit(stringResource(R.string.pro_benefit_effects))
                Benefit(stringResource(R.string.pro_benefit_rotation))
                Benefit(stringResource(R.string.pro_benefit_support))
            }

            Spacer(Modifier.height(28.dp))
            if (state.isPro) {
                Text(
                    stringResource(R.string.pro_active),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            } else {
                val price = state.price
                Button(
                    onClick = { (context as? Activity)?.let(ProManager::purchase) },
                    enabled = price != null && !state.pending,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(
                        if (price != null) stringResource(R.string.pro_buy, price)
                        else stringResource(R.string.pro_buy_loading),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                TextButton(onClick = ProManager::restore, modifier = Modifier.padding(top = 4.dp)) {
                    Text(stringResource(R.string.pro_restore))
                }
            }
        }
    }
}

@Composable
private fun Benefit(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 14.dp))
    }
}

@Composable
private fun messageText(message: ProMessage): String = stringResource(
    when (message) {
        ProMessage.PURCHASED -> R.string.pro_msg_purchased
        ProMessage.RESTORED -> R.string.pro_msg_restored
        ProMessage.NOTHING_TO_RESTORE -> R.string.pro_msg_nothing
        ProMessage.PENDING -> R.string.pro_msg_pending
        ProMessage.UNAVAILABLE -> R.string.pro_msg_unavailable
        ProMessage.FAILED -> R.string.pro_msg_failed
    },
)
