package rest.api.member;

import java.util.stream.Collectors;

import rest.api.member.Member.Convertor;
import rest.api.member.query.QDMember;

public interface MemberService {

    static Member addMember(MemberToAdd input) {
        DMember member = new DMember(input.getName());
        member.setPhones(input.getPhones().stream().map(DPhone::new).collect(Collectors.toList()));
        member.save();

        return Convertor.singleton.member(member);
    }

    static Member getMember(Integer id) {
        return Convertor.singleton.member(new QDMember().id.equalTo(id).findOne());
    }

}
