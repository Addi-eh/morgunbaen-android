package aeh.heimvisir.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TagNumberTest {

    @Test
    fun `bil og bandstrik hverfa`() {
        // Fólk les merki af kvittun þar sem þau eru rituð með bilum.
        assertEquals("352000000000000", TagNumber.normalize("352 000 000 000 000"))
        assertEquals("352000000000000", TagNumber.normalize("352-000-000-000-000"))
        assertEquals("352000000000000", TagNumber.normalize(" 352.000.000_000000 "))
    }

    @Test
    fun `bokstafir verda hastafir`() {
        assertEquals("AB12CD", TagNumber.normalize("ab12cd"))
    }

    @Test
    fun `gilt merki skilar hreinsudu gildi`() {
        val check = TagNumber.check("352 000 000 000 000")
        assertEquals(TagNumber.Check.Valid("352000000000000"), check)
    }

    @Test
    fun `of stutt merki er hafnad`() {
        assertEquals(TagNumber.Check.TooShort, TagNumber.check("123"))
    }

    @Test
    fun `tomur strengur er of stuttur en ekki hrun`() {
        assertEquals(TagNumber.Check.TooShort, TagNumber.check(""))
        assertEquals(TagNumber.Check.TooShort, TagNumber.check("   "))
    }

    @Test
    fun `merki a morkunum eru gild`() {
        assertEquals(TagNumber.Check.Valid("1234"), TagNumber.check("1234"))
        val longest = "1".repeat(TagNumber.MAX_LENGTH)
        assertEquals(TagNumber.Check.Valid(longest), TagNumber.check(longest))
    }

    @Test
    fun `of langt merki er hafnad`() {
        val tooLong = "1".repeat(TagNumber.MAX_LENGTH + 1)
        assertEquals(TagNumber.Check.TooLong, TagNumber.check(tooLong))
    }

    @Test
    fun `ologlegir stafir eru hafnadir`() {
        assertEquals(TagNumber.Check.HasIllegalCharacters, TagNumber.check("3520/0000"))
        assertEquals(TagNumber.Check.HasIllegalCharacters, TagNumber.check("352<script>"))
    }
}
