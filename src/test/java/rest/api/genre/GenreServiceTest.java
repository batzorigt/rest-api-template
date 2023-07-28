package rest.api.genre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rest.api.PagedData;
import rest.api.genre.query.QDGenre;

public class GenreServiceTest {

    @BeforeEach
    public void before() {
        new QDGenre().delete();
    }

    @Test
    void notFoundCase() {
        assertNull(GenreService.getGenres(1, 10));
    }

    @Test
    void dataExistingCase() {
        DGenreTest.insertRecords(1, 10);

        PagedData<Genre> result = GenreService.getGenres(1, 5);
        assertData(result.getData(), 1, 5);
        assertEquals(1, result.getPageNumber());
        assertEquals(5, result.getData().size());
        assertEquals(5, result.getRecordsPerPage());
        assertEquals(2, result.getNumberOfPages());
        assertEquals(10, result.getNumberOfRecords());

        result = GenreService.getGenres(2, 5);
        assertData(result.getData(), 6, 10);
        assertEquals(2, result.getPageNumber());
        assertEquals(5, result.getData().size());
        assertEquals(5, result.getRecordsPerPage());
        assertEquals(2, result.getNumberOfPages());
        assertEquals(10, result.getNumberOfRecords());

        result = GenreService.getGenres(2, 4);
        assertData(result.getData(), 5, 8);
        assertEquals(2, result.getPageNumber());
        assertEquals(4, result.getData().size());
        assertEquals(4, result.getRecordsPerPage());
        assertEquals(3, result.getNumberOfPages());
        assertEquals(10, result.getNumberOfRecords());

        result = GenreService.getGenres(3, 4);
        assertData(result.getData(), 9, 10);
        assertEquals(3, result.getPageNumber());
        assertEquals(2, result.getData().size());
        assertEquals(4, result.getRecordsPerPage());
        assertEquals(3, result.getNumberOfPages());
        assertEquals(10, result.getNumberOfRecords());

    }

    private void assertData(List<Genre> genres, int beginIndex, int endIndex) {
        for (int i = beginIndex; i <= endIndex; i++) {
            assertFields(genres.get(i - beginIndex), "key" + i, "name" + i, "imageKey" + i, "imagePath" + i, i);
        }
    }

    private void assertFields(Genre result,
                              String key,
                              String name,
                              String imageKey,
                              String imagePath,
                              int orderNumber) {
        assertEquals(key, result.getKey());
        assertEquals(name, result.getName());

        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        assertEquals(imageKey, result.getImageKey());
        assertEquals(imagePath, result.getImagePath());
        assertEquals(orderNumber, result.getOrderNumber());
    }

}
