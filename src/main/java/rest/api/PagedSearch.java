package rest.api;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import io.ebean.PagedList;
import io.javalin.http.ForbiddenResponse;

public interface PagedSearch {

    static <T, R> PagedData<R> search(Integer pageNumber,
                                      Integer recordsPerPage,
                                      PagedDataFinder<T> pagedDataFinder,
                                      AllDataFinder<T> allDataFinder,
                                      Function<T, R> convertor) {
        Integer[] totalRowCount = {0};
        List<T> data = findData(pageNumber, recordsPerPage, pagedDataFinder, allDataFinder, totalRowCount);

        if (CollectionUtils.isEmpty(data)) {
            return null;
        }

        Predicate<? super T> isNotNull = value -> value != null;
        List<R> result = data.parallelStream().filter(isNotNull).map(convertor).collect(Collectors.toList());

        return new PagedData<R>(pageNumber, recordsPerPage, totalRowCount[0], result);
    }

    static <T> PagedData<T> search(Integer pageNumber,
                                   Integer recordsPerPage,
                                   PagedDataFinder<T> pagedDataFinder,
                                   AllDataFinder<T> allDataFinder) {
        Integer[] totalRowCount = {0};
        List<T> data = findData(pageNumber, recordsPerPage, pagedDataFinder, allDataFinder, totalRowCount);

        if (CollectionUtils.isEmpty(data)) {
            return null;
        }

        return new PagedData<T>(pageNumber, recordsPerPage, totalRowCount[0], data);
    }

    private static <T> List<T> findData(Integer pageNumber,
                                        Integer recordsPerPage,
                                        PagedDataFinder<T> pagedDataFinder,
                                        AllDataFinder<T> allDataFinder,
                                        Integer[] totalRowCount) {
        if (pageNumber != null && recordsPerPage != null) {
            PagedList<T> pagedData = pagedDataFinder.find();
            pagedData.loadCount();
            List<T> data = pagedData.getList();
            totalRowCount[0] = pagedData.getTotalCount();

            return data;
        }

        if (allDataFinder == null) {
            throw new ForbiddenResponse("Find all is not allowed!");
        }

        List<T> data = allDataFinder.find();
        totalRowCount[0] = data.size();

        return data;
    }

    static int offset(Integer pageNumber, Integer recordsPerPage) {
        return (pageNumber - 1) * recordsPerPage;
    }

    @FunctionalInterface
    public interface PagedDataFinder<T> {
        PagedList<T> find();
    }

    @FunctionalInterface
    public interface AllDataFinder<T> {
        List<T> find();
    }

}
