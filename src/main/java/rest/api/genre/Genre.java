package rest.api.genre;

import java.util.Date;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
