package com.msbytes.statussaver.data

import com.google.common.truth.Truth.assertThat
import com.msbytes.statussaver.data.saf.StatusFolderValidator
import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.StatusSource
import org.junit.Test

class StatusFolderValidatorTest {

    @Test
    fun `accepts the scoped WhatsApp status folder`() {
        val id = "primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
        assertThat(StatusFolderValidator.matches(id, StatusSource.WHATSAPP)).isTrue()
        assertThat(StatusFolderValidator.matches(id, StatusSource.BUSINESS)).isFalse()
    }

    @Test
    fun `accepts the WhatsApp Business folder and the legacy Android 10 path`() {
        assertThat(
            StatusFolderValidator.matches(
                "primary:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
                StatusSource.BUSINESS,
            ),
        ).isTrue()
        assertThat(StatusFolderValidator.matches("primary:WhatsApp/Media/.Statuses", StatusSource.WHATSAPP)).isTrue()
    }

    @Test
    fun `rejects wrong folders`() {
        assertThat(StatusFolderValidator.matches("primary:Download", StatusSource.WHATSAPP)).isFalse()
        assertThat(StatusFolderValidator.matches("primary:Android/media/com.whatsapp/WhatsApp/Media", StatusSource.WHATSAPP)).isFalse()
        assertThat(StatusFolderValidator.matches("no-colon", StatusSource.WHATSAPP)).isFalse()
    }

    @Test
    fun `detects supported media and ignores the rest`() {
        assertThat(StatusFolderValidator.mediaTypeOf("a.JPG")).isEqualTo(MediaType.IMAGE)
        assertThat(StatusFolderValidator.mediaTypeOf("a.webp")).isEqualTo(MediaType.IMAGE)
        assertThat(StatusFolderValidator.mediaTypeOf("a.gif")).isEqualTo(MediaType.IMAGE)
        assertThat(StatusFolderValidator.mediaTypeOf("b.mp4")).isEqualTo(MediaType.VIDEO)
        assertThat(StatusFolderValidator.mediaTypeOf("b.3gp")).isEqualTo(MediaType.VIDEO)
        assertThat(StatusFolderValidator.mediaTypeOf(".nomedia")).isNull()
        assertThat(StatusFolderValidator.mediaTypeOf("notes.txt")).isNull()
        assertThat(StatusFolderValidator.mediaTypeOf("noextension")).isNull()
    }

    @Test
    fun `maps mime types`() {
        assertThat(StatusFolderValidator.mimeTypeOf("x.jpeg")).isEqualTo("image/jpeg")
        assertThat(StatusFolderValidator.mimeTypeOf("x.3gp")).isEqualTo("video/3gpp")
    }
}
