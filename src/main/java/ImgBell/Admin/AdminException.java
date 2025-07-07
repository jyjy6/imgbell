package ImgBell.Admin;

public class AdminException extends RuntimeException {
    private final String errorCode;
    
    public AdminException(String message) {
        super(message);
        this.errorCode = "ADMIN_ERROR";
    }
    
    public AdminException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public AdminException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ADMIN_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

// 구체적인 예외 클래스들
class MemberNotFoundException extends AdminException {
    public MemberNotFoundException(String username) {
        super("회원을 찾을 수 없습니다: " + username, "MEMBER_NOT_FOUND");
    }
}

class UnauthorizedAccessException extends AdminException {
    public UnauthorizedAccessException() {
        super("관리자 권한이 필요합니다", "UNAUTHORIZED_ACCESS");
    }
}

class InvalidMemberDataException extends AdminException {
    public InvalidMemberDataException(String message) {
        super("유효하지 않은 회원 데이터: " + message, "INVALID_MEMBER_DATA");
    }
} 