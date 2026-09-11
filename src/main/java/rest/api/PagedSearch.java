package rest.api;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import io.ebean.PagedList;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ForbiddenResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PagedSearch {

    private PagedSearch() {
    }

    public static <T, R> PagedData<R> search(Integer pageNumber,
                                             Integer recordsPerPage,
                                             PagedDataFinder<T> pagedDataFinder,
                                             AllDataFinder<T> allDataFinder,
                                             Function<T, R> convertor) {
        FoundData<T> found = findData(pageNumber, recordsPerPage, pagedDataFinder, allDataFinder);
        List<T> data = found.data();

        if (CollectionUtils.isEmpty(data)) {
            return null;
        }

        List<R> result = data.parallelStream().filter(value -> value != null).map(convertor).collect(Collectors.toList());

        return new PagedData<R>(pageNumber, recordsPerPage, found.totalRowCount(), result);
    }

    public static <T> PagedData<T> search(Integer pageNumber,
                                          Integer recordsPerPage,
                                          PagedDataFinder<T> pagedDataFinder,
                                          AllDataFinder<T> allDataFinder) {
        return search(pageNumber, recordsPerPage, pagedDataFinder, allDataFinder, Function.identity());
    }

    private static <T> FoundData<T> findData(Integer pageNumber,
                                             Integer recordsPerPage,
                                             PagedDataFinder<T> pagedDataFinder,
                                             AllDataFinder<T> allDataFinder) {
        if ((pageNumber == null) != (recordsPerPage == null)) {
            throw new BadRequestResponse("pageNumber and recordsPerPage must be provided together!");
        }

        if (pageNumber != null) {
            PagedList<T> pagedData = pagedDataFinder.find();
            pagedData.loadCount();
            List<T> data = pagedData.getList();
            return new FoundData<>(data, pagedData.getTotalCount());
        }

        if (allDataFinder == null) {
            throw new ForbiddenResponse("Find all is not allowed!");
        }

        List<T> data = allDataFinder.find();
        log.warn("Unpaginated fetch returned {} rows - large tables can exhaust memory; clients should pass pageNumber & recordsPerPage", data.size());

        return new FoundData<>(data, data.size());
    }

    public static int offset(Integer pageNumber, Integer recordsPerPage) {
        long offset = ((long) pageNumber - 1L) * recordsPerPage;
        if (offset > Integer.MAX_VALUE) {
            throw new BadRequestResponse("Pagination offset is too large!");
        }
        return (int) offset;
    }

    @FunctionalInterface
    public interface PagedDataFinder<T> {
        PagedList<T> find();
    }

    @FunctionalInterface
    public interface AllDataFinder<T> {
        List<T> find();
    }

    private record FoundData<T>(List<T> data, int totalRowCount) {
    }

}
