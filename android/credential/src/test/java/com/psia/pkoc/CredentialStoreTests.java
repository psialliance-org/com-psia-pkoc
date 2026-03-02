package com.psia.pkoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class CredentialStoreTests
{
    @Before
    public void setUp()
    {
        CredentialStore.clear(RuntimeEnvironment.getApplication());
    }

    // MARK: - Save & Load

    @Test
    public void saveAndLoad_roundTrip()
    {
        // Given a list of credential hex IDs
        List<String> ids = Arrays.asList("aabbcc", "ddeeff", "112233");

        // When we save and then load them
        CredentialStore.saveSelectedCredentials(RuntimeEnvironment.getApplication(), ids);
        Set<String> loaded = CredentialStore.getSelectedCredentialIds(RuntimeEnvironment.getApplication());

        // Then all IDs should be present
        assertEquals(3, loaded.size());
        assertTrue(loaded.contains("aabbcc"));
        assertTrue(loaded.contains("ddeeff"));
        assertTrue(loaded.contains("112233"));
    }

    @Test
    public void saveAndLoad_singleItem()
    {
        // Given a single credential ID
        CredentialStore.saveSelectedCredentials(
                RuntimeEnvironment.getApplication(),
                Collections.singletonList("deadbeef"));

        // When we load
        Set<String> loaded = CredentialStore.getSelectedCredentialIds(RuntimeEnvironment.getApplication());

        // Then it should contain that single ID
        assertEquals(1, loaded.size());
        assertTrue(loaded.contains("deadbeef"));
    }

    @Test
    public void load_nothingSaved_returnsEmptySet()
    {
        // Given no prior save
        // When we load
        Set<String> loaded = CredentialStore.getSelectedCredentialIds(RuntimeEnvironment.getApplication());

        // Then the result should be empty
        assertTrue(loaded.isEmpty());
    }

    @Test
    public void save_emptyList_loadReturnsEmpty()
    {
        // Given an empty list saved
        CredentialStore.saveSelectedCredentials(
                RuntimeEnvironment.getApplication(),
                Collections.emptyList());

        // When we load
        Set<String> loaded = CredentialStore.getSelectedCredentialIds(RuntimeEnvironment.getApplication());

        // Then the result should be empty
        assertTrue(loaded.isEmpty());
    }

    // MARK: - Clear

    @Test
    public void clear_removesStoredData()
    {
        // Given previously saved credentials
        CredentialStore.saveSelectedCredentials(
                RuntimeEnvironment.getApplication(),
                Collections.singletonList("aabbcc"));

        // When we clear
        CredentialStore.clear(RuntimeEnvironment.getApplication());

        // Then load should return empty
        assertTrue(CredentialStore.getSelectedCredentialIds(RuntimeEnvironment.getApplication()).isEmpty());
    }

    // MARK: - Overwrite

    @Test
    public void save_overwritesPreviousData()
    {
        // Given previously saved credentials
        CredentialStore.saveSelectedCredentials(
                RuntimeEnvironment.getApplication(),
                Collections.singletonList("old-id"));

        // When we save new ones
        CredentialStore.saveSelectedCredentials(
                RuntimeEnvironment.getApplication(),
                Arrays.asList("new-id-1", "new-id-2"));

        // Then load should return only the new IDs
        Set<String> loaded = CredentialStore.getSelectedCredentialIds(RuntimeEnvironment.getApplication());
        assertEquals(2, loaded.size());
        assertTrue(loaded.contains("new-id-1"));
        assertTrue(loaded.contains("new-id-2"));
        assertFalse(loaded.contains("old-id"));
    }

    // MARK: - Deduplication

    @Test
    public void load_returnsDeduplicated()
    {
        // Given duplicate IDs saved
        CredentialStore.saveSelectedCredentials(
                RuntimeEnvironment.getApplication(),
                Arrays.asList("aabb", "aabb", "ccdd"));

        // When we load (returns a Set)
        Set<String> loaded = CredentialStore.getSelectedCredentialIds(RuntimeEnvironment.getApplication());

        // Then duplicates should be collapsed
        assertEquals(2, loaded.size());
        assertTrue(loaded.contains("aabb"));
        assertTrue(loaded.contains("ccdd"));
    }
}
