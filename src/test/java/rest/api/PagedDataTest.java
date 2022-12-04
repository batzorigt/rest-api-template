package rest.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class PagedDataTest {

    @Test
    void evenNumberOfRecords() {
        List<Integer> data = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        PagedData<Integer> pagedData = new PagedData<Integer>(1, 2, data.size(), data);

        assertEquals(1, pagedData.getPageNumber());
        assertEquals(2, pagedData.getRecordsPerPage());
        assertEquals(5, pagedData.getNumberOfPages());
        assertEquals(10, pagedData.getNumberOfRecords());
        // TODO check whether last page size is even for every actual service
        // assertEquals(2, pagedData.getData().size());
    }

    @Test
    void oddNumberOfRecords() {
        List<Integer> data = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        PagedData<Integer> pagedData = new PagedData<Integer>(5, 2, data.size(), data);

        assertEquals(5, pagedData.getPageNumber());
        assertEquals(2, pagedData.getRecordsPerPage());
        assertEquals(5, pagedData.getNumberOfPages());
        assertEquals(9, pagedData.getNumberOfRecords());
        // TODO check whether last page size is odd for every actual service
        // assertEquals(1, pagedData.getData().size());
    }

    @Test
    void recordsPerPageGreaterThanNumberOfRecords() {
        List<Integer> data = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        PagedData<Integer> pagedData = new PagedData<Integer>(1, 20, data.size(), data);

        assertEquals(1, pagedData.getPageNumber());
        assertEquals(20, pagedData.getRecordsPerPage());
        assertEquals(1, pagedData.getNumberOfPages());
        assertEquals(10, pagedData.getNumberOfRecords());
    }

    @Test
    void findAllForNullParams() {
        List<Integer> data = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        PagedData<Integer> pagedData = new PagedData<Integer>(null, null, data.size(), data);
        assertValuesWhenParamsAreNull(pagedData);
    }

    private void assertValuesWhenParamsAreNull(PagedData<Integer> pagedData) {
        assertEquals(null, pagedData.getPageNumber());
        assertEquals(null, pagedData.getRecordsPerPage());
        assertEquals(null, pagedData.getNumberOfPages());
        assertEquals(null, pagedData.getNumberOfRecords());
        assertEquals(9, pagedData.getData().size());
    }

    @Test
    void findAllForZeroedParams() {
        List<Integer> data = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        PagedData<Integer> pagedData = new PagedData<Integer>(0, 0, data.size(), data);
        assertValuesWhenParamsAreNull(pagedData);
    }

}
