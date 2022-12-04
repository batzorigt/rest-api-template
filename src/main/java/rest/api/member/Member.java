package rest.api.member;

import java.util.Date;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@Data
@JsonInclude(value = Include.NON_NULL)
public class Member {

    private Integer id;
    private String name;
    private Integer sex;
    private List<Phone> phones;

    private Date createdAt;
    private Date updatedAt;

    @Mapper
    public interface Convertor {

        Convertor singleton = Mappers.getMapper(Convertor.class);

        Member member(DMember dMember);

    }

}
