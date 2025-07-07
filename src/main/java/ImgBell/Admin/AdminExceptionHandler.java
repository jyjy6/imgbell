package ImgBell.Admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = "ImgBell.Admin")
public class AdminExceptionHandler {

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMemberNotFound(MemberNotFoundException ex) {
        log.error("Member not found: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            ex.getErrorCode(), 
            ex.getMessage(), 
            HttpStatus.NOT_FOUND.value()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedAccess(UnauthorizedAccessException ex) {
        log.warn("Unauthorized access attempt: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            ex.getErrorCode(), 
            ex.getMessage(), 
            HttpStatus.FORBIDDEN.value()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(InvalidMemberDataException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidMemberData(InvalidMemberDataException ex) {
        log.error("Invalid member data: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            ex.getErrorCode(), 
            ex.getMessage(), 
            HttpStatus.BAD_REQUEST.value()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(AdminException.class)
    public ResponseEntity<Map<String, Object>> handleAdminException(AdminException ex) {
        log.error("Admin error: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            ex.getErrorCode(), 
            ex.getMessage(), 
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error in admin module: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = createErrorResponse(
            "INTERNAL_SERVER_ERROR", 
            "서버 내부 오류가 발생했습니다", 
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private Map<String, Object> createErrorResponse(String errorCode, String message, int statusCode) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("statusCode", statusCode);
        return errorResponse;
    }
} 