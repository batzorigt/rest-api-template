package rest.api.genre;

import java.util.Date;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import lombok.Data;

@Data
public class Genre {

    @NotNull
    private Integer id;

    @NotNull
    private Date createdAt;

    @NotNull
    private Date updatedAt;

    @NotBlank
    @Size(min = 1, max = 10)
    private String name;

    private String key;

    private String imagePath;

    private String imageKey;

    private int orderNumber;

    @Mapper
    public interface Convertor {

        Convertor singleton = Mappers.getMapper(Convertor.class);

        Genre genre(DGenre dGenre);

    }
}
