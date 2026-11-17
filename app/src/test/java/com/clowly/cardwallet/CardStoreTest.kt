package com.clowly.cardwallet

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.io.InputStream

class CardStoreTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun importsIndependentCopyAndReopensNewestFirst() {
        val source = folder.newFile().apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val dir = folder.newFolder()
        val store = CardStore(dir)
        val first = source.inputStream().use { store.save(null, "常用卡", it) }
        val second = store.save(null, "", byteArrayOf(9).inputStream())
        assertTrue(source.delete())
        val reopened = CardStore(dir)
        assertEquals(listOf(second, first), reopened.list())
        assertArrayEquals(byteArrayOf(1, 2, 3), reopened.imageFile(first).readBytes())
    }

    @Test fun editsInPlaceAndDeletesReplacedAndRemovedImages() {
        val store = CardStore(folder.newFolder())
        val first = store.save(null, "旧备注", byteArrayOf(1).inputStream())
        val second = store.save(null, "第二张", byteArrayOf(2).inputStream())
        val noted = store.save(first.id, "新备注\n第二行", null)
        assertEquals(listOf(second, noted), store.list())
        assertEquals(first.imageName, noted.imageName)
        val replaced = store.save(first.id, noted.note, byteArrayOf(3).inputStream())
        assertEquals(listOf(second, replaced), store.list())
        assertEquals(first.id, replaced.id)
        assertFalse(store.imageFile(first).exists())
        assertArrayEquals(byteArrayOf(3), store.imageFile(replaced).readBytes())
        store.delete(first.id)
        assertFalse(store.imageFile(replaced).exists())
        assertEquals(listOf(second), store.list())
    }

    @Test fun interruptedReplacementKeepsOldCardAndCleansPartialCopy() {
        val dir = folder.newFolder()
        val store = CardStore(dir)
        val saved = store.save(null, "保留", byteArrayOf(1).inputStream())
        val before = dir.list()!!.sorted()
        val interrupted = object : InputStream() {
            override fun read(): Int = throw IOException("source disappeared")
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                b[off] = 7
                if (!started) { started = true; return 1 }
                return read()
            }
            private var started = false
        }
        assertThrows(IOException::class.java) { store.save(saved.id, "不保存", interrupted) }
        assertEquals(listOf(saved), CardStore(dir).list())
        assertArrayEquals(byteArrayOf(1), store.imageFile(saved).readBytes())
        assertEquals(before, dir.list()!!.sorted())
    }
}
