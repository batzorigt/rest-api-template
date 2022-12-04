package rest.api.genre;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;
import rest.api.Domain;

@Data
@Entity
@Table(name = "genres")
@EqualsAndHashCode(callSuper = false)
public class DGenre extends Domain {

    @NotBlank
    @Size(min = 1, max = 10)
    private String name;

    private String key;

    private String imagePath;

    private String imageKey;

    private int orderNumber;

}
