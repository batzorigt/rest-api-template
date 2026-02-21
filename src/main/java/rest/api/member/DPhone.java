package rest.api.member;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import rest.api.Domain;

@Entity
@Getter
@Setter
@Table(name = "phone_numbers")
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
