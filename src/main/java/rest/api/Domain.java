package rest.api;

import java.util.Date;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@MappedSuperclass
@EqualsAndHashCode(callSuper = false)
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
