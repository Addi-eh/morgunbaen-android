package aeh.heimvisir.net

import aeh.heimvisir.model.LookupResult
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Netlagið gegn hermdum þjóni.
 *
 * Öll svör hér eru tilbúin — engin fyrirspurn fer út á netið og engin
 * raunveruleg gögn koma nálægt þessu.
 */
class DyraAudkenniApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: DyraAudkenniApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = DyraAudkenniApi(
            baseUrl = server.url("/api/MicroTags"),
            client = OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.SECONDS)
                .callTimeout(2, TimeUnit.SECONDS)
                .build(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `svar 200 skilar dyri`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"petId": 1, "name": "Dæmi", "species": "Köttur", "isLost": false}"""),
        )

        val result = api.lookup("352000000000000")

        assertTrue(result is LookupResult.Found)
        assertEquals("Dæmi", (result as LookupResult.Found).pet.name)
    }

    @Test
    fun `merkid fer i fyrirspurnina`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"petId": 1}"""))

        api.lookup("352000000000000")

        val request = server.takeRequest()
        assertEquals("352000000000000", request.requestUrl?.queryParameter("tagNumber"))
        assertEquals(DyraAudkenniApi.USER_AGENT, request.getHeader("User-Agent"))
    }

    @Test
    fun `svar 404 er NotFound en ekki villa`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("\"{\\\"Message\\\":\\\"NotFound\\\"}\""))

        assertEquals(LookupResult.NotFound, api.lookup("352000000000000"))
    }

    @Test
    fun `svar 500 ber kodann med ser`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("nei"))

        assertEquals(LookupResult.ServerError(500), api.lookup("352000000000000"))
    }

    @Test
    fun `tvikodad villusvar med koda 200 er MalformedResponse`() = runTest {
        // Skráin skilar stundum villu sem streng sem inniheldur JSON.
        // Vefútgáfan kastaði því beint í `as LookupPet` og notandinn fékk
        // auðan skjá með „Ónefnt dýr".
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("\"{\\\"Message\\\":\\\"Eitthvad for urskeidis\\\"}\""),
        )

        assertEquals(LookupResult.MalformedResponse, api.lookup("352000000000000"))
    }

    @Test
    fun `olaesilegt meginmal er MalformedResponse`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("<html>villa</html>"))

        assertEquals(LookupResult.MalformedResponse, api.lookup("352000000000000"))
    }

    @Test
    fun `svar an petId er MalformedResponse`() = runTest {
        // petId er eini reiturinn sem VERÐUR að vera til staðar. Vanti
        // hann er þetta ekki dýr, sama hvað annað stendur í svarinu.
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"name": "Dæmi"}"""))

        assertEquals(LookupResult.MalformedResponse, api.lookup("352000000000000"))
    }

    @Test
    fun `rofid samband er NetworkError`() = runTest {
        server.enqueue(MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AT_START })

        assertEquals(LookupResult.NetworkError, api.lookup("352000000000000"))
    }

    @Test
    fun `timamork sem renna ut eru NetworkError`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"petId": 1}""")
                .setBodyDelay(5, TimeUnit.SECONDS),
        )

        assertEquals(LookupResult.NetworkError, api.lookup("352000000000000"))
    }
}
