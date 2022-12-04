package rest.api.genre;

import rest.api.PagedData;
import rest.api.PagedSearch;
import rest.api.PagedSearch.AllDataFinder;
import rest.api.PagedSearch.PagedDataFinder;
import rest.api.genre.Genre.Convertor;
import rest.api.genre.query.QDGenre;

public class GenreService {

    static PagedData<Genre> getGenres(Integer pageNumber, Integer pageSize) {
        PagedDataFinder<DGenre> pagedDataFinder = () -> {
            int offset = PagedSearch.offset(pageNumber, pageSize);
            return new QDGenre().orderBy().orderNumber.asc().setFirstRow(offset).setMaxRows(pageSize.intValue())
                    .findPagedList();
        };

        AllDataFinder<DGenre> allDataFinder = () -> new QDGenre().orderBy().orderNumber.asc().findList();

        return PagedSearch.search(pageNumber, pageSize, pagedDataFinder, allDataFinder, Convertor.singleton::genre);
    }
}
