package rest.api.genre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Locale;
import java.util.Random;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kong.unirest.GetRequest;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import rest.api.API;
import rest.api.I18N;
import rest.api.PagedData;
import rest.api.genre.query.QDGenre;

public class GenreHandlerTest {

    private static API api = new API();
    private static final int PORT_NO = 1000 + new Random().nextInt(9000);
    private static final GetRequest GET_GENRES = Unirest.get(String.format("http://localhost:%d/v1/genres", PORT_NO));

    @BeforeAll
    public static void beforeAll() throws Throwable {
        api.start(PORT_NO);
    }

    @AfterAll
    public static void afterAll() {
        api.stop();
    }

    @BeforeEach
    public void before() {
        new QDGenre().delete();
    }

    @Test
    void notFoundCase() throws Exception {
        HttpResponse<String> response = GET_GENRES.asString();
        assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());
        assertEquals(String.format("{\"msg\": \"%s\"}", I18N.message("data.not.found", Locale.JAPAN)), response
                .getBody());
    }

    @Test
    void dataExistingCase() throws Exception {
        DGenreTest.insertRecords(1, 10);

        HttpResponse<JsonNode> res = GET_GENRES.asJson();
        JSONObject result = res.getBody().getObject();
        JSONArray data = result.getJSONArray("data");
        JSONObject firstElement = data.getJSONObject(0);

        assertEquals(HttpStatus.OK_200, res.getStatus());
        assertEquals(10, data.length());
        assertEquals("name1", firstElement.get("name"));

        res = GET_GENRES.queryString(PagedData.PAGE_NUMBER, 4).queryString(PagedData.RECORDS_PER_PAGE, 3).asJson();
        result = res.getBody().getObject();
        data = result.getJSONArray("data");
        firstElement = data.getJSONObject(0);

        assertEquals(HttpStatus.OK_200, res.getStatus());
        assertEquals(1, data.length());

        assertEquals(10, firstElement.get("id"));
        assertEquals("key10", firstElement.get("key"));
        assertEquals("name10", firstElement.getString("name"));

        assertNotNull(firstElement.get("createdAt"));
        assertNotNull(firstElement.getLong("updatedAt"));

        assertEquals("imageKey10", firstElement.get("imageKey"));
        assertEquals("imagePath10", firstElement.get("imagePath"));
        assertEquals(10, firstElement.get("orderNumber"));

        assertEquals(4, result.get(PagedData.PAGE_NUMBER));
        assertEquals(4, result.get("numberOfPages"));
        assertEquals(3, result.get(PagedData.RECORDS_PER_PAGE));
        assertEquals(10, result.get("numberOfRecords"));
    }

}
