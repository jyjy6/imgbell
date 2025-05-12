package ImgBell.Member;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberDto {
    private Long id;
    private String username;
    private String displayName;
    private String email;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String profileImage;
    private String country;
    private String mainAddress;
    private String subAddress;

    private boolean isPremium;
    private LocalDateTime premiumExpiryDate;

    private boolean marketingAccepted;
    private Set<String> roleSet;
}
