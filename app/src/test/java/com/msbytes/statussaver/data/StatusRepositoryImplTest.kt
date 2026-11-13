package com.msbytes.statussaver.data

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.msbytes.statussaver.data.repository.StatusRepositoryImpl
import com.msbytes.statussaver.data.saf.SafStatusDataSource
import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.StatusAccessException
import com.msbytes.statussaver.domain.model.StatusItem
import com.msbytes.statussaver.domain.model.StatusSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class StatusRepositoryImplTest {

    private val dataSource = mockk<SafStatusDataSource>()

    private fun item(name: String, modified: Long) = StatusItem(
        uri = mockk<Uri>(),
        name = name,
        size = 100,
        lastModified = modified,
        type = MediaType.IMAGE,
        mimeType = "image/jpeg",
        source = StatusSource.WHATSAPP,
    )

    @Test
    fun `refresh returns items newest first and caches them`() = runTest {
        val repo = StatusRepositoryImpl(dataSource, StandardTestDispatcher(testScheduler))
        every { dataSource.list(StatusSource.WHATSAPP) } returns listOf(item("old", 1), item("new", 3), item("mid", 2))

        val result = repo.refresh(StatusSource.WHATSAPP)

        assertThat(result.getOrThrow().map { it.name }).containsExactly("new", "mid", "old").inOrder()
        assertThat(repo.statuses.value[StatusSource.WHATSAPP]?.map { it.name }).containsExactly("new", "mid", "old").inOrder()
    }

    @Test
    fun `revoked access fails and clears the cache`() = runTest {
        val repo = StatusRepositoryImpl(dataSource, StandardTestDispatcher(testScheduler))
        every { dataSource.list(StatusSource.WHATSAPP) } returns listOf(item("a", 1))
        repo.refresh(StatusSource.WHATSAPP)

        every { dataSource.list(StatusSource.WHATSAPP) } throws StatusAccessException.NoAccess()
        val result = repo.refresh(StatusSource.WHATSAPP)

        assertThat(result.exceptionOrNull()).isInstanceOf(StatusAccessException.NoAccess::class.java)
        assertThat(repo.statuses.value).doesNotContainKey(StatusSource.WHATSAPP)
    }

    @Test
    fun `sources are cached independently`() = runTest {
        val repo = StatusRepositoryImpl(dataSource, StandardTestDispatcher(testScheduler))
        every { dataSource.list(StatusSource.WHATSAPP) } returns listOf(item("wa", 1))
        every { dataSource.list(StatusSource.BUSINESS) } throws StatusAccessException.FolderMissing()

        repo.refresh(StatusSource.WHATSAPP)
        repo.refresh(StatusSource.BUSINESS)

        assertThat(repo.statuses.value.keys).containsExactly(StatusSource.WHATSAPP)
    }
}
