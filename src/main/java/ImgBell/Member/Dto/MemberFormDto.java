package ImgBell.Member.Dto;

import ImgBell.Member.Member;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberFormDto {
    private String username;
    private String password;
    private String name;
    private String displayName;
    private String profileImage;
    private String email;
    private String phone;
    private String sex;
    private Integer age;
    private String country;
    private String mainAddress;
    private String subAddress;
    private Set<String> roleSet;
    private boolean privacyAccepted;
    private boolean termsAccepted;
    private boolean marketingAccepted;

    public Member convertToMember() {
        return Member.builder()
                .username(this.username)
                .password(this.password)
                .name(this.name)
                .displayName(this.displayName)
                .email(this.email)
                .phone(this.phone)
                .sex(this.sex)
                .age(this.age)
                .profileImage(this.profileImage)
                .country(this.country)
                .mainAddress(this.mainAddress)
                .subAddress(this.subAddress)
                .privacyAccepted(this.privacyAccepted)
                .termsAccepted(this.termsAccepted)
                .marketingAccepted(this.marketingAccepted)
                .build();
    }

    public void updateMember(Member member) {
        member.setName(this.name);
        member.setDisplayName(this.displayName);
        member.setEmail(this.email);
        member.setPhone(this.phone);
        member.setSex(this.sex);
        member.setAge(this.age);
        member.setProfileImage(this.profileImage);
        member.setCountry(this.country);
        member.setMainAddress(this.mainAddress);
        member.setSubAddress(this.subAddress);
        member.setMarketingAccepted(this.marketingAccepted);
        // password는 따로 처리하므로 여기선 제외
    }

    public void adminUpdateMember(Member member) {
        member.setName(this.name);
        member.setDisplayName(this.displayName);
        member.setEmail(this.email);
        member.setPhone(this.phone);
        member.setSex(this.sex);
        member.setAge(this.age);
        member.setRoles(this.roleSet);
        member.setProfileImage(this.profileImage);
        member.setCountry(this.country);
        member.setMainAddress(this.mainAddress);
        member.setSubAddress(this.subAddress);
        member.setMarketingAccepted(this.marketingAccepted);
        // password는 따로 처리하므로 여기선 제외
    }

}

