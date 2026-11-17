package com.clowly.cardwallet

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal class WalletViewModel(
    application: Application,
    private val saved: SavedStateHandle,
) : AndroidViewModel(application) {
    private val store = CardStore(File(application.noBackupFilesDir, "wallet")) { file ->
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) throw InvalidImage()
    }

    var cards by mutableStateOf(emptyList<SavedCard>())
        private set
    var selectedId by mutableStateOf(saved.get<String>("selected"))
        private set
    var editing by mutableStateOf(saved.get<Boolean>("editing") ?: false)
        private set
    var pickedImage by mutableStateOf(saved.get<String>("image"))
        private set
    var note by mutableStateOf(saved.get<String>("note") ?: "")
        private set
    var loading by mutableStateOf(true)
        private set
    var loadFailed by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val selected: SavedCard? get() = cards.find { it.id == selectedId }
    val editorImage: Any? get() = pickedImage?.let(Uri::parse) ?: selected?.let(::imageFile)

    init { load() }

    fun load() {
        loading = true
        loadFailed = false
        viewModelScope.launch {
            try {
                cards = withContext(Dispatchers.IO) { store.list() }
                if (selectedId != null && selected == null) {
                    setSelected(null)
                    closeEditor()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                loadFailed = true
            } finally {
                loading = false
            }
        }
    }

    fun imageFile(card: SavedCard): File = store.imageFile(card)

    fun open(card: SavedCard) {
        setSelected(card.id)
    }

    fun beginNew(uri: Uri) {
        // The system picker can return before the index has loaded after process recreation.
        if (busy) return
        setSelected(null)
        changeNote("")
        replaceImage(uri)
        setEditorOpen(true)
    }

    fun beginEdit() {
        val card = selected ?: return
        changeNote(card.note)
        clearPickedImage()
        setEditorOpen(true)
    }

    fun replaceImage(uri: Uri) {
        if (busy) return
        pickedImage = uri.toString()
        saved["image"] = pickedImage
    }

    fun changeNote(value: String) {
        if (value.length > MAX_NOTE_LENGTH) return
        note = value
        saved["note"] = value
    }

    fun back() {
        if (busy) return
        if (editing) closeEditor() else setSelected(null)
    }

    fun clearError() { error = null }

    fun save() {
        if (busy || !editing || editorImage == null) return
        val id = selectedId
        val message = note
        val uri = pickedImage?.let(Uri::parse)
        perform("无法保存卡片，请重新选择卡面后重试。") {
            val (card, updated) = withContext(Dispatchers.IO) {
                val card = if (uri != null) {
                    val stream = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?: throw InvalidImage()
                    stream.use { store.save(id, message, it) }
                } else {
                    store.save(id, message, null)
                }
                card to store.list()
            }
            cards = updated
            setSelected(card.id)
            closeEditor()
        }
    }

    fun delete() {
        val id = selectedId ?: return
        perform("删除失败，请重试。") {
            cards = withContext(Dispatchers.IO) {
                store.delete(id)
                store.list()
            }
            closeEditor()
            setSelected(null)
        }
    }

    private fun perform(fallback: String, operation: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                operation()
            } catch (e: CancellationException) {
                throw e
            } catch (_: ImageTooLarge) {
                error = "卡面图片超过 50 MB，请换一张。"
            } catch (_: InvalidImage) {
                error = "无法读取这张图片，请重新选择。"
            } catch (_: Exception) {
                error = fallback
            } finally {
                busy = false
            }
        }
    }

    private fun setSelected(id: String?) {
        selectedId = id
        saved["selected"] = id
    }

    private fun setEditorOpen(value: Boolean) {
        editing = value
        saved["editing"] = value
    }

    private fun clearPickedImage() {
        pickedImage = null
        saved.remove<String>("image")
    }

    private fun closeEditor() {
        setEditorOpen(false)
        clearPickedImage()
        changeNote("")
    }
}
