package ma.etherealmber.atlascommerce.data.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmployeeClientProfileContractTest {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @Test
    fun employeeDetailReadsSupportedHrAndIdentityFieldsAndPaymentHistory() {
        val employee = moshi.adapter(EmployeeDto::class.java).fromJson(
            """{
              "id": 19,
              "contract_type": "CDI",
              "weekly_hours": 44,
              "joined_date": "2024-02-01",
              "pay_amount": 8500,
              "rib": "MA6400000000000000000000000",
              "social_insurance": "CNSS-123",
              "health_insurance": "AMO-456",
              "tax_id": "IF-789",
              "national_id": "AB123456",
              "nationality": "Marocaine",
              "date_of_birth": "1995-06-12",
              "phone": "0600000000",
              "email": "employee@example.test",
              "payments": [{"id": 2, "amount": 8500, "paid_at": "2025-01-01", "period_label": "Jan 2025"}]
            }""".trimIndent(),
        )!!

        assertEquals("CDI", employee.contractType)
        assertEquals(44.0, employee.weeklyHours!!, 0.0)
        assertEquals("MA6400000000000000000000000", employee.rib)
        assertEquals("CNSS-123", employee.socialInsurance)
        assertEquals("AMO-456", employee.healthInsurance)
        assertEquals("IF-789", employee.taxId)
        assertEquals("AB123456", employee.nationalId)
        assertEquals("Marocaine", employee.nationality)
        assertEquals(8500.0, employee.payments.single().amount, 0.0)
    }

    @Test
    fun missingProfileFieldsRemainUnavailableRatherThanInvented() {
        val employee = moshi.adapter(EmployeeDto::class.java).fromJson("{\"id\": 7}")!!
        assertEquals("", employee.rib)
        assertEquals("", employee.nationalId)
        assertNull(employee.weeklyHours)
    }

    @Test
    fun clientDetailReadsCreditAndContactFields() {
        val client = moshi.adapter(ClientDto::class.java).fromJson(
            """{"id":4,"name":"Client","ice":"ICE-01","address":"Rabat","credit_limit":12000,"balance_due":3450}""",
        )!!
        assertEquals("ICE-01", client.ice)
        assertEquals("Rabat", client.address)
        assertEquals(12000.0, client.creditLimit!!, 0.0)
        assertEquals(3450.0, client.balanceDue, 0.0)
    }
}
