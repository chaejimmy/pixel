package com.pacedream.notifications

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PushDeepLinkHandlerTest {

    @Test
    fun `parses request detail link`() {
        val link = PushDeepLink.parse(Uri.parse("pacedream://requests/abc123"))
        assertEquals(PushDeepLink.RequestDetail("abc123"), link)
    }

    @Test
    fun `parses request offers link`() {
        val link = PushDeepLink.parse(Uri.parse("pacedream://requests/abc123/offers"))
        assertEquals(PushDeepLink.RequestOffers("abc123"), link)
    }

    @Test
    fun `parses booking detail link`() {
        val link = PushDeepLink.parse(Uri.parse("pacedream://bookings/xyz"))
        assertEquals(PushDeepLink.BookingDetail("xyz"), link)
    }

    @Test
    fun `rejects non pacedream scheme`() {
        assertNull(PushDeepLink.parse(Uri.parse("https://pacedream.com/requests/abc")))
    }

    @Test
    fun `rejects unknown host`() {
        assertNull(PushDeepLink.parse(Uri.parse("pacedream://unknown/1")))
    }

    @Test
    fun `parses hyphenated request offers link`() {
        val link = PushDeepLink.parse(Uri.parse("pacedream://requests/abc-123/offers"))
        assertEquals(PushDeepLink.RequestOffers("abc-123"), link)
    }
}
