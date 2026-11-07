package ma.etherealmber.atlascommerce.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushNotificationContentTest {

    @Test
    fun `notification payload takes precedence over data fields`() {
        assertEquals(
            "Server alert" to "Stock is low",
            resolvePushNotificationContent(
                notificationTitle = " Server alert ",
                notificationBody = "Stock is low",
                data = mapOf("title" to "Ignored", "body" to "Ignored"),
            ),
        )
    }

    @Test
    fun `common case-insensitive worker data keys are displayed`() {
        assertEquals(
            "Atlas Commerce" to "A new request needs review",
            resolvePushNotificationContent(
                notificationTitle = null,
                notificationBody = null,
                data = mapOf("MESSAGE" to " A new request needs review "),
            ),
        )
        assertEquals(
            "New stock alert" to "Stock is low",
            resolvePushNotificationContent(
                notificationTitle = null,
                notificationBody = null,
                data = mapOf("notificationTitle" to "New stock alert", "notification_body" to "Stock is low"),
            ),
        )
    }

    @Test
    fun `event-only data is still visibly reported and empty payload is ignored`() {
        assertEquals(
            "Atlas Commerce" to "You have a new Atlas Commerce notification.",
            resolvePushNotificationContent(
                notificationTitle = null,
                notificationBody = null,
                data = mapOf("event" to "stock_request_decided"),
            ),
        )
        assertNull(resolvePushNotificationContent(null, null, emptyMap()))
    }
}
