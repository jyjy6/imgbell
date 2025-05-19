package ImgBell.Member.Dto;

import ImgBell.Member.Member;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberDto {
    private Long id;
    private String username;
    private String name;
    private String displayName;
    private String email;
    private String phone;
    private String sex;
    private Integer age;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String profileImage;
    private String country;
    private String mainAddress;
    private String subAddress;
    private LocalDateTime lastLogin;


    private boolean isPremium;
    private LocalDateTime premiumExpiryDate;

    private boolean marketingAccepted;
    private Set<String> roleSet;

    public MemberDto convertToDetailMemberDto(Member member) {
        return MemberDto.builder()
                .id(member.getId())
                .username(member.getUsername())
                .displayName(member.getDisplayName())
                .name(member.getName())
                .email(member.getEmail())
                .age(member.getAge())
                .sex(member.getSex())
                .phone(member.getPhone())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .profileImage(member.getProfileImage())
                .country(member.getCountry())
                .mainAddress(member.getMainAddress())
                .subAddress(member.getSubAddress())
                .isPremium(member.isPremium())
                .lastLogin(member.getLastLogin())
                .premiumExpiryDate(member.getPremiumExpiryDate())
                .marketingAccepted(member.isMarketingAccepted())
                .roleSet(member.getRoleSet())
                .build();
    }


}


