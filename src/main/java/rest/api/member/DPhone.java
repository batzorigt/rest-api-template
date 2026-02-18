package rest.api.member;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;
import rest.api.Domain;

@Data
@Entity
@EqualsAndHashCode(callSuper = false)
public class DPhone extends Domain {

    private String phoneNo;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private DMember member;

    private boolean isHomePhoneNo;

    public DPhone() {

    }

    public DPhone(String phoneNo) {
        setPhoneNo(phoneNo);
    }
}
