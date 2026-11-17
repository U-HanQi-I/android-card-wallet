package com.clowly.cardwallet

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.UUID

internal const val MAX_NOTE_LENGTH = 2000
private const val MAX_IMAGE_SIZE = 50L * 1024 * 1024
private const val SCHEMA_VERSION = 1

internal data class SavedCard(val id: String, val note: String, val imageName: String)

internal class ImageTooLarge : IOException()
internal class InvalidImage : IOException()

internal class CardStore(
    private val directory: File,
    private val validateImage: (File) -> Unit = {},
) {
    private val index = File(directory, "cards.dat")

    @Synchronized
    fun list(): List<SavedCard> {
        if (!index.exists()) return emptyList()
        return DataInputStream(index.inputStream().buffered()).use { input ->
            if (input.readInt() != SCHEMA_VERSION) throw IOException("Unsupported wallet format")
            val count = input.readInt()
            if (count < 0 || count > index.length() / 6) throw IOException("Invalid wallet index")
            List(count) {
                val id = input.readUTF()
                val name = input.readUTF()
                val note = input.readUTF()
                SavedCard(id, note, name)
            }
        }
    }

    fun imageFile(card: SavedCard): File = File(directory, card.imageName)

    @Synchronized
    fun save(id: String?, note: String, image: InputStream?): SavedCard {
        require(note.length <= MAX_NOTE_LENGTH)
        val cards = list().toMutableList()
        val oldIndex = if (id == null) -1 else cards.indexOfFirst { it.id == id }
        if (id != null && oldIndex == -1) throw IOException("Card not found")
        val old = cards.getOrNull(oldIndex)
        if (old == null && image == null) throw InvalidImage()
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create wallet directory")

        val newImage = image?.let(::copyImage)
        val card = SavedCard(old?.id ?: UUID.randomUUID().toString(), note, newImage?.name ?: checkNotNull(old).imageName)
        if (oldIndex == -1) cards.add(0, card) else cards[oldIndex] = card
        try {
            writeIndex(cards)
        } catch (error: Exception) {
            newImage?.delete()
            throw error
        }
        if (newImage != null && old != null) imageFile(old).delete()
        return card
    }

    @Synchronized
    fun delete(id: String) {
        val cards = list()
        val removed = cards.find { it.id == id } ?: return
        writeIndex(cards.filterNot { it.id == id })
        imageFile(removed).delete()
    }

    private fun copyImage(source: InputStream): File {
        val temporary = File.createTempFile("image-", ".tmp", directory)
        try {
            FileOutputStream(temporary).use { output ->
                var size = 0L
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    size += count
                    if (size > MAX_IMAGE_SIZE) throw ImageTooLarge()
                    output.write(buffer, 0, count)
                }
                if (size == 0L) throw InvalidImage()
                output.fd.sync()
            }
            validateImage(temporary)
            val permanent = File(directory, "${UUID.randomUUID()}.image")
            Files.move(temporary.toPath(), permanent.toPath(), ATOMIC_MOVE)
            return permanent
        } finally {
            temporary.delete()
        }
    }

    private fun writeIndex(cards: List<SavedCard>) {
        val temporary = File.createTempFile("index-", ".tmp", directory)
        try {
            FileOutputStream(temporary).use { file ->
                val output = DataOutputStream(file.buffered())
                output.writeInt(SCHEMA_VERSION)
                output.writeInt(cards.size)
                for (card in cards) {
                    output.writeUTF(card.id)
                    output.writeUTF(card.imageName)
                    output.writeUTF(card.note)
                }
                output.flush()
                file.fd.sync()
            }
            Files.move(temporary.toPath(), index.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            temporary.delete()
        }
    }
}
