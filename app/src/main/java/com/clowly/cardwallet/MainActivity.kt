package com.clowly.cardwallet

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    private val wallet by viewModels<WalletViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Ink,
                onPrimary = Paper,
                background = Paper,
                onBackground = Ink,
                surface = CardWhite,
                onSurface = Ink,
                surfaceVariant = Line,
                onSurfaceVariant = Quiet,
            )) { WalletApp(wallet) }
        }
    }
}

@Composable
private fun WalletApp(wallet: WalletViewModel) {
    val add = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) wallet.beginNew(uri)
    }
    val replace = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) wallet.replaceImage(uri)
    }
    val selectImage = PickVisualMediaRequest(PickVisualMedia.ImageOnly)
    val snackbar = remember { SnackbarHostState() }
    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
    val selected = wallet.selected
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val unsavedChanges = wallet.editing && (
        wallet.pickedImage != null || wallet.note != selected?.note.orEmpty()
    )
    val goBack: () -> Unit = {
        if (!wallet.busy) {
            if (unsavedChanges) confirmDiscard = true else wallet.back()
        }
    }

    BackHandler(wallet.editing || selected != null, onBack = goBack)
    LaunchedEffect(wallet.error) {
        wallet.error?.let {
            snackbar.showSnackbar(it, withDismissAction = true)
            wallet.clearError()
        }
    }

    Scaffold(containerColor = Paper, snackbarHost = { SnackbarHost(snackbar) }) { insets ->
        val page = Modifier.fillMaxSize().padding(insets)
        when {
            wallet.loading || wallet.loadFailed -> LoadScreen(
                modifier = page, failed = wallet.loadFailed, retry = wallet::load,
            )
            wallet.editing -> EditScreen(
                modifier = page, existing = selected != null, model = wallet.editorImage,
                note = wallet.note, busy = wallet.busy, onNote = wallet::changeNote,
                onBack = goBack, onChoose = { replace.launch(selectImage) }, onSave = wallet::save,
            )
            selected != null -> DetailScreen(
                modifier = page, model = wallet.imageFile(selected), note = selected.note,
                onBack = goBack, onEdit = wallet::beginEdit,
                onDelete = { confirmDelete = true }, busy = wallet.busy,
            )
            else -> HomeScreen(
                modifier = page, cards = wallet.cards, listState = scrollState,
                imageFile = wallet::imageFile, onOpen = wallet::open,
                onAdd = { add.launch(selectImage) },
            )
        }
    }

    if (confirmDelete && selected != null) {
        WalletConfirmation(
            title = "删除这张卡片？",
            message = "卡面和备注将从卡包中移除。",
            action = "删除",
            onDismiss = { confirmDelete = false },
            onConfirm = { confirmDelete = false; wallet.delete() },
        )
    }
    if (confirmDiscard && wallet.editing) {
        WalletConfirmation(
            title = "放弃未保存的修改？",
            message = "这次修改的卡面和备注不会保存。",
            action = "放弃",
            onDismiss = { confirmDiscard = false },
            onConfirm = { confirmDiscard = false; wallet.back() },
        )
    }
}
