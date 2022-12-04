package rest.api.member;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.ebean.DB;
import io.ebean.Database;
import rest.api.member.query.QDMember;

public class MemberServiceTest {

    @Test
    public void create() {
        MemberToAdd member = new MemberToAdd("Batzorigt", List.of("88381882"));
        Member result = MemberService.addMember(member);

        Assertions.assertNotNull(result.getId());
        Assertions.assertNotNull(result.getCreatedAt());
        Assertions.assertNotNull(result.getUpdatedAt());
        Assertions.assertEquals(member.getName(), result.getName());
        Assertions.assertEquals("88381882", result.getPhones().get(0).getPhoneNo());

        Database db = DB.getDefault();
        DMember foundMember = db.reference(DMember.class, result.getId());
        Assertions.assertEquals(result.getId(), foundMember.getId());
        Assertions.assertEquals(result.getCreatedAt(), foundMember.getCreatedAt());
        Assertions.assertEquals(result.getUpdatedAt(), foundMember.getUpdatedAt());
        Assertions.assertEquals(result.getName(), foundMember.getName());
        Assertions.assertEquals(member.getPhones().get(0), foundMember.getPhones().get(0).getPhoneNo());

        Assertions.assertTrue(db.find(DMember.class).where().eq("name", "Batzorigt").findCount() > 0);

        // using fetch to get only specified fields
        DMember foundByFetch = new QDMember().name.eq("Batzorigt").phones.phoneNo.eq("88381882").fetch("phones",
                "phone_no").findOne();
        Assertions.assertEquals(result.getId(), foundByFetch.getId());
        Assertions.assertEquals(result.getCreatedAt(), foundByFetch.getCreatedAt());
        Assertions.assertEquals(result.getUpdatedAt(), foundByFetch.getUpdatedAt());
        Assertions.assertEquals(result.getName(), foundByFetch.getName());
        Assertions.assertEquals(member.getPhones().get(0), foundByFetch.getPhones().get(0).getPhoneNo());

        QDMember fields = QDMember.alias(); // using alias to specify what fields to get
        DMember foundOne = new QDMember().select(fields.id, fields.name, fields.createdAt, fields.updatedAt).name.eq(
                "Batzorigt").phones.phoneNo.eq("88381882").findOne();
        Assertions.assertEquals(result.getId(), foundOne.getId());
        Assertions.assertEquals(result.getCreatedAt(), foundOne.getCreatedAt());
        Assertions.assertEquals(result.getUpdatedAt(), foundOne.getUpdatedAt());
        Assertions.assertEquals(result.getName(), foundOne.getName());
        Assertions.assertEquals(member.getPhones().get(0), foundOne.getPhones().get(0).getPhoneNo());
    }

}
