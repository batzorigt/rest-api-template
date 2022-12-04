package rest.api.member;

import java.util.Date;

import lombok.Data;

@Data
public class Phone {

    private Integer id;

    private String phoneNo;
    private boolean isHomePhoneNo;

    private Date createdAt;
    private Date updatedAt;

}
