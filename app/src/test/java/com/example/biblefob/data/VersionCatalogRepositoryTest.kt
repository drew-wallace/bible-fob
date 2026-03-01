package com.example.biblefob.data

import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCatalogRepositoryTest {

    @Test
    fun builtInEntries_containsDefaultVersion() {
        val hasDefault = VersionCatalogRepository.builtInEntries
            .any { it.id == VersionCatalogRepository.defaultVersionId }

        assertTrue(hasDefault)
    }
}
