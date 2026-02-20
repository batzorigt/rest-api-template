package rest.api.member;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import rest.api.Domain;

@Data
@Entity
@Table(name = "members")
@EqualsAndHashCode(callSuper = false)
public class DMember extends Domain {

    @NotBlank
    @Size(min = 1, max = 10)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DPhone> phones;

    private Integer sex;

    public DMember() {

    }

    public DMember(String name) {
        setName(name);
    }

}
