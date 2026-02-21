package rest.api;

import java.util.Date;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class Domain extends Model {

    @Id
    @NotNull
    @GeneratedValue
    private Integer id;

    @NotNull
    @WhenCreated
    private Date createdAt;

    @NotNull
    @WhenModified
    private Date updatedAt;

}
