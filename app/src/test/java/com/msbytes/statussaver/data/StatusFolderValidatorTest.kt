package com.msbytes.statussaver.data

import com.google.common.truth.Truth.assertThat
import com.msbytes.statussaver.data.saf.StatusFolderValidator
import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.StatusSource
import org.junit.Test

class StatusFolderValidatorTest {

    private fun sources(treeId: String) = StatusFolderValidator.coveredFolders(treeId).map { it.source }.toSet()

    @Test
    fun `Android media grant covers WhatsApp and Business`() {
        assertThat(sources("primary:Android/media")).containsExactly(StatusSource.WHATSAPP, StatusSource.BUSINESS)
    }

    @Test
    fun `storage root grant on old Android covers every folder`() {
        assertThat(StatusFolderValidator.coveredFolders("primary:")).hasSize(StatusFolderValidator.allFolders.size)
    }

    @Test
    fun `single status folder grants are accepted`() {
        assertThat(sources("primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses")).containsExactly(StatusSource.WHATSAPP)
        assertThat(sources("primary:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"))
            .containsExactly(StatusSource.BUSINESS)
        assertThat(sources("primary:WhatsApp/Media/.Statuses")).containsExactly(StatusSource.WHATSAPP)
    }

    @Test
    fun `app folder grant only covers that app`() {
        assertThat(sources("primary:Android/media/com.whatsapp")).containsExactly(StatusSource.WHATSAPP)
    }

    @Test
    fun `unrelated folders are rejected`() {
        assertThat(StatusFolderValidator.coveredFolders("primary:Download")).isEmpty()
        assertThat(StatusFolderValidator.coveredFolders("primary:Android/media/com.other")).isEmpty()
        assertThat(StatusFolderValidator.coveredFolders("no-colon")).isEmpty()
    }

    @Test
    fun `detects supported media and ignores the rest`() {
        assertThat(StatusFolderValidator.mediaTypeOf("a.JPG")).isEqualTo(MediaType.IMAGE)
        assertThat(StatusFolderValidator.mediaTypeOf("a.webp")).isEqualTo(MediaType.IMAGE)
        assertThat(StatusFolderValidator.mediaTypeOf("b.mp4")).isEqualTo(MediaType.VIDEO)
        assertThat(StatusFolderValidator.mediaTypeOf("b.3gp")).isEqualTo(MediaType.VIDEO)
        assertThat(StatusFolderValidator.mediaTypeOf(".nomedia")).isNull()
        assertThat(StatusFolderValidator.mediaTypeOf("notes.txt")).isNull()
    }

    @Test
    fun `maps mime types`() {
        assertThat(StatusFolderValidator.mimeTypeOf("x.jpeg")).isEqualTo("image/jpeg")
        assertThat(StatusFolderValidator.mimeTypeOf("x.3gp")).isEqualTo("video/3gpp")
    }
}
