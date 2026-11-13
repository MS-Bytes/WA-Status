package com.msbytes.statussaver.data

import com.google.common.truth.Truth.assertThat
import com.msbytes.statussaver.data.media.MediaStoreSaver
import org.junit.Test

class MediaStoreSaverTest {

    private val mb = 1024L * 1024

    @Test
    fun `keeps a 20 MB safety margin when checking free space`() {
        assertThat(MediaStoreSaver.hasEnoughSpace(availableBytes = 100 * mb, neededBytes = 10 * mb)).isTrue()
        assertThat(MediaStoreSaver.hasEnoughSpace(availableBytes = 25 * mb, neededBytes = 10 * mb)).isFalse()
        assertThat(MediaStoreSaver.hasEnoughSpace(availableBytes = 5 * mb, neededBytes = 10 * mb)).isFalse()
    }
}
