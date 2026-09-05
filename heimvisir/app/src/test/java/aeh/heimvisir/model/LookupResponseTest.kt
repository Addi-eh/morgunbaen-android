package aeh.heimvisir.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Þáttun á svörum skrárinnar.
 *
 * ÖLL gögn hér eru tilbúin. Engin raunveruleg dýr, engin raunveruleg
 * merki og engin raunveruleg eigendagögn eiga heima í kóðasafni.
 *
 * Prófin herma eftir sömu stillingum og [aeh.heimvisir.net.DyraAudkenniApi]
 * notar, því það er hegðunin sem skiptir máli.
 */
class LookupResponseTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `fullt svar les alla reiti`() {
        val pet = json.decodeFromString<Pet>(
            """
            {
              "petId": 1,
              "name": "Dæmi",
              "gender": "Karldýr",
              "breed": "Blendingur",
              "species": "Köttur",
              "birthDate": "2020-01-15T00:00:00",
              "birthYear": 2020,
              "color": "Grár",
              "isCastrated": true,
              "isLost": false,
              "owners": [
                {
                  "personId": 2,
                  "name": "Nafn Nafnsson",
                  "personEmail": "netfang@example.is",
                  "personAddress": "Gata 1",
                  "postalCode": "101",
                  "location": "Reykjavík",
                  "phoneNumbers": ["555 0000", "555 0001"]
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(1, pet.petId)
        assertEquals("Dæmi", pet.name)
        assertEquals(true, pet.isCastrated)
        assertEquals(1, pet.owners?.size)
        assertEquals(listOf("555 0000", "555 0001"), pet.owners?.first()?.phones)
        assertEquals("101 Reykjavík", pet.owners?.first()?.place)
        assertTrue(pet.owners!!.first().hasContact)
    }

    @Test
    fun `dayOfDisappear ma vanta`() {
        // Reiturinn kemur aðeins þegar dýr er skráð týnt. Í vefútgáfunni
        // var hann skráður sem hluti af týpunni og ekkert gekk úr skugga
        // um að hann væri til.
        val pet = json.decodeFromString<Pet>("""{"petId": 1, "isLost": false}""")
        assertNull(pet.dayOfDisappear)
    }

    @Test
    fun `tynt dyr ber dagsetningu`() {
        val pet = json.decodeFromString<Pet>(
            """{"petId": 1, "isLost": true, "dayOfDisappear": "2026-03-04"}""",
        )
        assertTrue(pet.isLost)
        assertEquals("2026-03-04", pet.dayOfDisappear)
    }

    @Test
    fun `isLost sjalfgefid osatt tegar reitinn vantar`() {
        val pet = json.decodeFromString<Pet>("""{"petId": 1}""")
        assertEquals(false, pet.isLost)
    }

    @Test
    fun `owners ma vera tomt`() {
        val pet = json.decodeFromString<Pet>("""{"petId": 1, "owners": []}""")
        assertEquals(emptyList<Owner>(), pet.owners)
    }

    @Test
    fun `owners ma vera null`() {
        val pet = json.decodeFromString<Pet>("""{"petId": 1, "owners": null}""")
        assertNull(pet.owners)
    }

    @Test
    fun `margir eigendur haldast allir`() {
        // Vefútgáfan tók aðeins owners[0] og henti hinum þegjandi.
        val pet = json.decodeFromString<Pet>(
            """
            {
              "petId": 1,
              "owners": [
                {"name": "Fyrri eigandi"},
                {"name": "Seinni eigandi"}
              ]
            }
            """.trimIndent(),
        )
        assertEquals(2, pet.owners?.size)
        assertEquals("Seinni eigandi", pet.owners?.get(1)?.name)
    }

    @Test
    fun `othekktur reitur fellir ekki tattunina`() {
        // Þetta er ástæðan fyrir ignoreUnknownKeys: bæti skráin reit við
        // á appið að halda áfram að virka.
        val pet = json.decodeFromString<Pet>(
            """{"petId": 1, "nyrReiturSemViDekkjumEkki": {"eitthvad": [1, 2, 3]}}""",
        )
        assertEquals(1, pet.petId)
    }

    @Test
    fun `eigandi an upplysinga hefur engin samskipti`() {
        // Eigandi sem hefur ekki samþykkt birtingu kemur auður frá skránni.
        val owner = json.decodeFromString<Owner>("""{"personId": 2}""")
        assertEquals(false, owner.hasContact)
        assertNull(owner.place)
        assertEquals(emptyList<String>(), owner.phones)
    }

    @Test
    fun `aud simanumer eru fjarlaegd`() {
        val owner = json.decodeFromString<Owner>(
            """{"name": "Nafn", "phoneNumbers": ["555 0000", "", "   "]}""",
        )
        assertEquals(listOf("555 0000"), owner.phones)
    }
}
