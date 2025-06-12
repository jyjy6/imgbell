package ImgBell.Config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;


public class MyWebSocketHandler extends TextWebSocketHandler {

    // 인스턴스 식별용 ID 추가
    private final String instanceId = java.util.UUID.randomUUID().toString().substring(0, 8);

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> usernameToSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("🆔 핸들러 인스턴스 ID: " + instanceId);
        System.out.println("🔗 WebSocket 연결됨: " + session.getId());
        System.out.println("📊 현재 총 연결 수: " + sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            System.out.println("📩 받은 메시지: " + payload);

            // JSON 파싱
            JsonNode jsonNode = objectMapper.readTree(payload);

            // 사용자 등록 요청 처리
            if (jsonNode.has("type") && "register".equals(jsonNode.get("type").asText())) {
                Long userId = jsonNode.get("userId").asLong();
                String username = jsonNode.has("username") ? jsonNode.get("username").asText() : null;

                System.out.println("🔍 사용자 등록 시도 - ID: " + userId + ", Username: " + username);

                // 사용자 세션 등록
                userSessions.put(userId, session);
                if (username != null && !username.trim().isEmpty()) {
                    usernameToSessions.put(username, session);
                    System.out.println("✅ Username 맵에 등록됨: " + username);
                }

                // 세션에 사용자 정보 저장
                session.getAttributes().put("userId", userId);
                if (username != null) {
                    session.getAttributes().put("username", username);
                }

                System.out.println("👤 사용자 등록 완료 - ID: " + userId + ", Username: " + username);
                System.out.println("📊 등록된 사용자 수 - ID 기준: " + userSessions.size() + ", Username 기준: " + usernameToSessions.size());

                // 디버깅용: 현재 등록된 모든 username 출력
                System.out.println("🗃️ 현재 등록된 사용자명들: " + usernameToSessions.keySet());

                // 등록 성공 응답
                session.sendMessage(new TextMessage("{\"type\":\"registered\",\"message\":\"사용자 등록 완료\"}"));
            }

        } catch (Exception e) {
            System.err.println("❌ 메시지 처리 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);

        // 사용자 세션에서도 제거
        Long userId = (Long) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");

        if (userId != null) {
            userSessions.remove(userId);
            System.out.println("👤 사용자 세션 제거됨 - ID: " + userId);
        }

        if (username != null) {
            usernameToSessions.remove(username);
            System.out.println("👤 사용자명 세션 제거됨 - Username: " + username);
        }

        System.out.println("🔌 WebSocket 연결 해제됨: " + session.getId());
        System.out.println("📊 현재 총 연결 수: " + sessions.size());
    }

    // 모든 연결된 사용자에게 메시지 전송
    public void sendToAll(String message) {
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 특정 사용자 ID에게 메시지 전송
    public void sendToUser(Long userId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                System.out.println("✅ 사용자 " + userId + "에게 알림 전송: " + message);
            } catch (IOException e) {
                System.err.println("❌ 사용자 " + userId + "에게 메시지 전송 실패: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ 사용자 " + userId + "의 세션을 찾을 수 없음 (오프라인 상태)");
        }
    }

    // 특정 사용자명에게 메시지 전송

    public void sendToUsername(String username, String message) {
        System.out.println("🆔 sendToUsername 호출 - 핸들러 인스턴스 ID: " + instanceId);
        System.out.println("🔍 sendToUsername 호출됨 - 대상: " + username);
        System.out.println("🗃️ 현재 등록된 사용자명들: " + usernameToSessions.keySet());

        WebSocketSession session = usernameToSessions.get(username);
        System.out.println("🔍 찾은 세션: " + session);

        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                System.out.println("✅ 사용자 " + username + "에게 알림 전송: " + message);
            } catch (IOException e) {
                System.err.println("❌ 사용자 " + username + "에게 메시지 전송 실패: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ 사용자 " + username + "의 세션을 찾을 수 없음 (오프라인 상태)");
            if (session != null && !session.isOpen()) {
                System.out.println("💀 세션은 존재하지만 닫혀있음");
            }
        }
    }

    // 현재 연결된 사용자 수 확인
    public int getConnectedUserCount() {
        return userSessions.size();
    }

    // 특정 사용자가 온라인인지 확인
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    public boolean isUsernameOnline(String username) {
        WebSocketSession session = usernameToSessions.get(username);
        return session != null && session.isOpen();
    }
}