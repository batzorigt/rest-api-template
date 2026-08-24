package rest.api.genre;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GenreToAdd {

    @NotBlank
    @Size(min = 1, max = 10)
    private String name;

    private String key;

    private String imagePath;

    private String imageKey;

    private int orderNumber;
}
