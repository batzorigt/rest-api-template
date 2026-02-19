package rest.api;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
@SuppressWarnings("serial")
public class PagedData<T> implements Serializable {

    public static String PAGE_NUMBER = "pageNumber";
    public static String RECORDS_PER_PAGE = "recordsPerPage";

    private List<T> data;

    private Integer pageNumber;
    private Integer numberOfPages;
    private Integer recordsPerPage;
    private Integer numberOfRecords;

    public PagedData(Integer pageNumber, Integer recordsPerPage, Integer numberOfRecords, List<T> data) {
        pageNumber = NumberHelpers.nullIfZero(pageNumber);
        recordsPerPage = NumberHelpers.nullIfZero(recordsPerPage);

        if (numberOfRecords != null && recordsPerPage != null) {
            this.pageNumber = pageNumber;
            this.recordsPerPage = recordsPerPage;
            this.numberOfRecords = numberOfRecords;
            this.numberOfPages = (int) Math.ceil((double) numberOfRecords / recordsPerPage);
        }

        this.data = data;
    }

}