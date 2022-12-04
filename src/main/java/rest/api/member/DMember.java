package rest.api.member;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;
import rest.api.Domain;

@Data
@Entity
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
