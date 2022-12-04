package rest.api.member;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MemberToAdd {

    @NotBlank
    @Size(min = 1, max = 10)
    private String name;

    private List<String> phones = new ArrayList<>();

    public MemberToAdd() {
        super();
    }

}
