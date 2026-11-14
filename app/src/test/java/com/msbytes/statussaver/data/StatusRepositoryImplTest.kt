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

    private fun item(name: String, modified: Long, source: StatusSource = StatusSource.WHATSAPP) = StatusItem(
        uri = mockk<Uri>(),
        name = name,
        size = 100,
        lastModified = modified,
        type = MediaType.IMAGE,
        mimeType = "image/jpeg",
        source = source,
    )

    @Test
    fun `merges WhatsApp and Business statuses newest first`() = runTest {
        val repo = StatusRepositoryImpl(dataSource, StandardTestDispatcher(testScheduler))
        every { dataSource.list() } returns listOf(
            item("wa_old", 1),
            item("biz_new", 3, StatusSource.BUSINESS),
            item("wa_mid", 2),
        )

        val result = repo.refresh()

        assertThat(result.getOrThrow().map { it.name }).containsExactly("biz_new", "wa_mid", "wa_old").inOrder()
        assertThat(repo.statuses.value?.map { it.name }).containsExactly("biz_new", "wa_mid", "wa_old").inOrder()
    }

    @Test
    fun `revoked access fails and clears the cache`() = runTest {
        val repo = StatusRepositoryImpl(dataSource, StandardTestDispatcher(testScheduler))
        every { dataSource.list() } returns listOf(item("a", 1))
        repo.refresh()

        every { dataSource.list() } throws StatusAccessException.NoAccess()
        val result = repo.refresh()

        assertThat(result.exceptionOrNull()).isInstanceOf(StatusAccessException.NoAccess::class.java)
        assertThat(repo.statuses.value).isNull()
    }

    @Test
    fun `missing folder is reported`() = runTest {
        val repo = StatusRepositoryImpl(dataSource, StandardTestDispatcher(testScheduler))
        every { dataSource.list() } throws StatusAccessException.FolderMissing()

        assertThat(repo.refresh().exceptionOrNull()).isInstanceOf(StatusAccessException.FolderMissing::class.java)
    }
}
