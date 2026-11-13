package com.msbytes.statussaver.data

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.SortOrder
import com.msbytes.statussaver.domain.model.StatusItem
import io.mockk.mockk
import org.junit.Test

class SortOrderTest {
    private fun item(name: String, size: Long, modified: Long) =
        StatusItem(mockk<Uri>(), name, size, modified, MediaType.IMAGE, "image/jpeg", null)

    private val items = listOf(item("a", 10, 3), item("b", 30, 1), item("c", 20, 2))

    @Test
    fun `sorts by date and size`() {
        assertThat(SortOrder.NEWEST.apply(items).map { it.name }).containsExactly("a", "c", "b").inOrder()
        assertThat(SortOrder.OLDEST.apply(items).map { it.name }).containsExactly("b", "c", "a").inOrder()
        assertThat(SortOrder.LARGEST.apply(items).map { it.name }).containsExactly("b", "c", "a").inOrder()
        assertThat(SortOrder.SMALLEST.apply(items).map { it.name }).containsExactly("a", "c", "b").inOrder()
    }
}
