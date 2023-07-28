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
        DGenre genre = insertRecord("key1", "name1", "imageKey1", "imagePath1", 1);
        DGenre result = new QDGenre().id.eq(genre.getId()).findOne();
        assertFields(result, "key1", "name1", "imageKey1", "imagePath1", 1);
    }

    @Test
    void update() throws Exception {
        DGenre updateTarget = insertRecord("key2", "name2", "imageKey2", "imagePath2", 2);
        Integer id = updateTarget.getId();

        DGenre beforeUpdate = DB.reference(DGenre.class, id);
        assertFields(beforeUpdate, "key2", "name2", "imageKey2", "imagePath2", 2);

        Date createdAtBeforeUpdate = beforeUpdate.getCreatedAt();
        Date updatedAtBeforeUpdate = beforeUpdate.getUpdatedAt();

        fillFieldsExceptId(beforeUpdate, "key3", "name3", "imageKey3", "imagePath3", 3);
        beforeUpdate.update();

        DGenre afterUpdate = new QDGenre().id.eq(id).findOne();
        assertFields(afterUpdate, "key3", "name3", "imageKey3", "imagePath3", 3);

        assertEquals(createdAtBeforeUpdate, afterUpdate.getCreatedAt());
        assertNotEquals(updatedAtBeforeUpdate, afterUpdate.getUpdatedAt());
    }

    @Test
    void delete() throws Exception {
        DGenre genre = insertRecord("key3", "name3", "imageKey3", "imagePath3", 3);
        Integer id = genre.getId();

        DGenre genreTobeDeleted = DB.reference(DGenre.class, id);
        assertFields(genreTobeDeleted, "key3", "name3", "imageKey3", "imagePath3", 3);
        assertTrue(genreTobeDeleted.delete());

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
            insertRecord("key" + i, "name" + i, "imageKey" + i, "imagePath" + i, i);
        }
    }

    public static DGenre insertRecord(String key, String name, String imageKey, String imagePath, int orderNumber) {
        DGenre genre = new DGenre();
        fillFieldsExceptId(genre, key, name, imageKey, imagePath, orderNumber);
        genre.save();
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
