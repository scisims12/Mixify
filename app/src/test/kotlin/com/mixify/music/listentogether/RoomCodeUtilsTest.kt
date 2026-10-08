package com.mixify.music.listentogether

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomCodeUtilsTest {

    @Test
    fun `5 character code is invalid and join room is disabled`() {
        val code = "ABC12"
        assertFalse(RoomCodeUtils.isValidRoomCode(code))
        assertFalse(RoomCodeUtils.isJoinRoomEnabled("TestUser", "", code))
        assertFalse(RoomCodeUtils.isJoinRoomEnabled("", "SavedUser", code))
    }

    @Test
    fun `6 character code is valid and join room is enabled`() {
        val code = "ABC123"
        assertTrue(RoomCodeUtils.isValidRoomCode(code))
        assertTrue(RoomCodeUtils.isJoinRoomEnabled("TestUser", "", code))
        assertTrue(RoomCodeUtils.isJoinRoomEnabled("", "SavedUser", code))
    }

    @Test
    fun `lowercase input is normalized to uppercase`() {
        val lowercaseCode = "abc123"
        assertEquals("ABC123", RoomCodeUtils.normalizeRoomCode(lowercaseCode))
        assertEquals("ABC123", RoomCodeUtils.sanitizeRoomCodeInput(lowercaseCode))
        assertTrue(RoomCodeUtils.isValidRoomCode(lowercaseCode))
        assertTrue(RoomCodeUtils.isJoinRoomEnabled("TestUser", "", lowercaseCode))
    }

    @Test
    fun `more than 6 characters cannot be entered`() {
        val longCode7 = "ABC1234"
        val longCodeLowercase = "abc123extra"
        assertEquals("ABC123", RoomCodeUtils.sanitizeRoomCodeInput(longCode7))
        assertEquals("ABC123", RoomCodeUtils.sanitizeRoomCodeInput(longCodeLowercase))
        assertEquals(RoomCodeUtils.ROOM_CODE_LENGTH, RoomCodeUtils.sanitizeRoomCodeInput(longCode7).length)
    }

    @Test
    fun `input with leading or trailing spaces is trimmed and normalized`() {
        val codeWithSpaces = "  abc123  "
        assertEquals("ABC123", RoomCodeUtils.normalizeRoomCode(codeWithSpaces))
        assertEquals("ABC123", RoomCodeUtils.sanitizeRoomCodeInput(codeWithSpaces))
        assertTrue(RoomCodeUtils.isValidRoomCode(codeWithSpaces))
    }

    @Test
    fun `missing username disables join room even with valid 6 character code`() {
        val validCode = "ABC123"
        assertFalse(RoomCodeUtils.isJoinRoomEnabled("   ", "", validCode))
    }
}
