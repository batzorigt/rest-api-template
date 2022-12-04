package rest.api.genre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.ebean.DB;
import rest.api.genre.query.QDGenre;

public class DGenreTest {

    @BeforeEach
    public void before() {
        new QDGenre().delete();
    }

    @Test
    void save() throws Exception {
        DGenre genre = createGenre("key1", "name1", "imageKey1", "imagePath1", 1);
        genre.save();

        DGenre result = new QDGenre().id.eq(genre.getId()).findOne();
        assertFields(result, "key1", "name1", "imageKey1", "imagePath1", 1);
    }

    @Test
    void update() throws Exception {
        DGenre genre = createGenre("key2", "name2", "imageKey2", "imagePath2", 2);
        genre.save();
        Integer id = genre.getId();
        genre = DB.reference(DGenre.class, id);
        assertFields(genre, "key2", "name2", "imageKey2", "imagePath2", 2);

        Date createdAt = genre.getCreatedAt();
        Date updatedAt = genre.getUpdatedAt();

        fillFieldsExceptId(genre, "key3", "name3", "imageKey3", "imagePath3", 3);
        genre.update();

        DGenre result = new QDGenre().id.eq(id).findOne();
        assertFields(result, "key3", "name3", "imageKey3", "imagePath3", 3);

        assertEquals(createdAt, result.getCreatedAt());
        assertNotEquals(updatedAt, result.getUpdatedAt());
    }

    @Test
    void delete() throws Exception {
        DGenre genre = createGenre("key3", "name3", "imageKey3", "imagePath3", 3);
        genre.save();

        Integer id = genre.getId();
        genre = DB.reference(DGenre.class, id);
        assertFields(genre, "key3", "name3", "imageKey3", "imagePath3", 3);

        assertTrue(genre.delete());

        DGenre result = new QDGenre().id.eq(id).findOne();
        assertNull(result);
    }

    @Test
    void findList() throws Exception {
        insertRecords(4, 6);
        List<DGenre> genres = new QDGenre().findList();
        assertResults(genres, 4, 6);
    }

    private void assertResults(List<DGenre> genres, int beginIndex, int endIndex) {
        for (int i = beginIndex; i <= endIndex; i++) {
            assertFields(genres.get(i - beginIndex), "key" + i, "name" + i, "imageKey" + i, "imagePath" + i, i);
        }
    }

    public static void insertRecords(int beginIndex, int endIndex) {
        for (int i = beginIndex; i <= endIndex; i++) {
            createGenre("key" + i, "name" + i, "imageKey" + i, "imagePath" + i, i).save();
        }
    }

    public static DGenre createGenre(String key, String name, String imageKey, String imagePath, int orderNumber) {
        DGenre genre = new DGenre();
        fillFieldsExceptId(genre, key, name, imageKey, imagePath, orderNumber);
        return genre;
    }

    private static void fillFieldsExceptId(DGenre genre,
                                           String key,
                                           String name,
                                           String imageKey,
                                           String imagePath,
                                           int orderNumber) {
        genre.setKey(key);
        genre.setName(name);
        genre.setImageKey(imageKey);
        genre.setImagePath(imagePath);
        genre.setOrderNumber(orderNumber);
    }

    private void assertFields(DGenre result,
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
