package rest.api.genre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


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
import rest.api.Server;
import rest.api.PagedData;
import rest.api.genre.query.QDGenre;

public class GenreHandlerTest {

    private static Server api = new Server();
    private static String genresUrl;

    @BeforeAll
    public static void beforeAll() throws Throwable {
        api.start(0);
        genresUrl = String.format("http://localhost:%d/v1/genres", api.port());
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
        HttpResponse<JsonNode> response = getGenres().asJson();
        assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());
        assertNotNull(response.getBody().getObject().getString("msg"));
    }

    @Test
    void dataExistingCase() throws Exception {
        DGenreTest.insertRecords(1, 10);

        HttpResponse<JsonNode> res = getGenres().asJson();
        JSONObject result = res.getBody().getObject();
        JSONArray data = result.getJSONArray("data");
        JSONObject firstElement = data.getJSONObject(0);

        assertEquals(HttpStatus.OK_200, res.getStatus());
        assertEquals(10, data.length());
        assertEquals("name1", firstElement.get("name"));

        res = getGenres().queryString(PagedData.PAGE_NUMBER, 4).queryString(PagedData.RECORDS_PER_PAGE, 3).asJson();
        result = res.getBody().getObject();
        data = result.getJSONArray("data");
        firstElement = data.getJSONObject(0);

        assertEquals(HttpStatus.OK_200, res.getStatus());
        assertEquals(1, data.length());

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

        res = getGenres().queryString(PagedData.PAGE_NUMBER, 1)
                .queryString(PagedData.RECORDS_PER_PAGE, 50).asJson();
        assertEquals(HttpStatus.OK_200, res.getStatus());
        assertEquals(10, res.getBody().getObject().get(PagedData.RECORDS_PER_PAGE));
    }

    @Test
    void invalidPaginationParamsAreRejected() {
        assertEquals(HttpStatus.BAD_REQUEST_400,
                getGenres()
                        .queryString("pageNumber", "abc").asString().getStatus());
        assertEquals(HttpStatus.BAD_REQUEST_400,
                getGenres()
                        .queryString(PagedData.PAGE_NUMBER, "-1").queryString(PagedData.RECORDS_PER_PAGE, 3)
                        .asString().getStatus());
        assertEquals(HttpStatus.BAD_REQUEST_400,
                getGenres()
                        .queryString(PagedData.PAGE_NUMBER, 1).queryString(PagedData.RECORDS_PER_PAGE, "0")
                        .asString().getStatus());
        assertEquals(HttpStatus.BAD_REQUEST_400,
                getGenres()
                        .queryString(PagedData.PAGE_NUMBER, 1).asString().getStatus());
        assertEquals(HttpStatus.BAD_REQUEST_400,
                getGenres()
                        .queryString(PagedData.RECORDS_PER_PAGE, 10).asString().getStatus());
    }

    private static GetRequest getGenres() {
        return Unirest.get(genresUrl);
    }

}
