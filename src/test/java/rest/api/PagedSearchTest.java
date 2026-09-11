package rest.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.ebean.PagedList;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ForbiddenResponse;
import rest.api.PagedSearch.AllDataFinder;
import rest.api.PagedSearch.PagedDataFinder;

public class PagedSearchTest {

    @SuppressWarnings("unchecked")
    private PagedList<Integer> pagedList = Mockito.mock(PagedList.class);

    private List<Integer> data;
    private Function<Integer, Integer> convertor = value -> value;
    private AllDataFinder<Integer> allDataFinder = () -> data;
    private PagedDataFinder<Integer> pagedDataFinder = () -> pagedList;

    @Test
    void itemConvertor() {
        data = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        when(pagedList.getList()).thenReturn(List.of(1, 2));
        when(pagedList.getTotalCount()).thenReturn(10);

        PagedData<Integer> pagedData = PagedSearch.search(1, 2, pagedDataFinder, allDataFinder, convertor);

        assertEquals(1, pagedData.getPageNumber());
        assertEquals(2, pagedData.getRecordsPerPage());
        assertEquals(5, pagedData.getNumberOfPages());
        assertEquals(10, pagedData.getNumberOfRecords());
    }

    @Test
    void evenNumberOfRecords() {
        data = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        when(pagedList.getList()).thenReturn(List.of(1, 2));
        when(pagedList.getTotalCount()).thenReturn(10);

        PagedData<Integer> pagedData = PagedSearch.search(1, 2, pagedDataFinder, allDataFinder);

        assertEquals(1, pagedData.getPageNumber());
        assertEquals(2, pagedData.getRecordsPerPage());
        assertEquals(5, pagedData.getNumberOfPages());
        assertEquals(10, pagedData.getNumberOfRecords());
    }

    @Test
    void oddNumberOfRecords() {
        data = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        when(pagedList.getList()).thenReturn(List.of(9));
        when(pagedList.getTotalCount()).thenReturn(9);

        PagedData<Integer> pagedData = PagedSearch.search(5, 2, pagedDataFinder, allDataFinder, convertor);

        assertEquals(5, pagedData.getPageNumber());
        assertEquals(2, pagedData.getRecordsPerPage());
        assertEquals(5, pagedData.getNumberOfPages());
        assertEquals(9, pagedData.getNumberOfRecords());
    }

    @Test
    void findAllForNullParams() {
        data = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);

        PagedData<Integer> pagedData = PagedSearch.search(null, null, pagedDataFinder, allDataFinder);
        assertEquals(null, pagedData.getPageNumber());
        assertEquals(null, pagedData.getRecordsPerPage());
        assertEquals(null, pagedData.getNumberOfPages());
        assertEquals(null, pagedData.getNumberOfRecords());
        assertEquals(9, pagedData.getData().size());
        verify(pagedList, never()).getList();
    }

    @Test
    void rejectsIncompletePaginationPair() {
        assertThrows(BadRequestResponse.class,
                () -> PagedSearch.search(1, null, pagedDataFinder, allDataFinder));
        assertThrows(BadRequestResponse.class,
                () -> PagedSearch.search(null, 10, pagedDataFinder, allDataFinder));
    }

    @Test
    void rejectsUnpaginatedSearchWhenAllFinderIsDisabled() {
        assertThrows(ForbiddenResponse.class,
                () -> PagedSearch.search(null, null, pagedDataFinder, null));
    }

    @Test
    void rejectsOffsetOutsideIntegerRange() {
        assertThrows(BadRequestResponse.class, () -> PagedSearch.offset(Integer.MAX_VALUE, 10));
    }

}
