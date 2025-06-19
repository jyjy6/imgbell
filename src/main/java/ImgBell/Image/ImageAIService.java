package ImgBell.Image;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

@Service
@Slf4j
public class ImageAIService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${google.gemini.api.key}")
    private String geminiApiKey;

    public ImageAIService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

//    이미지 업로드 분석ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ
    /**
     * 이미지 URL로부터 자동 태그 생성 (Google Gemini API 사용)
     */
    public ImageAnalysisResult analyzeImage(String imageUrl) {
        try {
            log.info("Google Gemini API로 이미지 분석 시작: {}", imageUrl);

            // 이미지 다운로드 및 base64 인코딩
            String base64Image = downloadAndEncodeImage(imageUrl);
            if (base64Image == null) {
                return createErrorResult("이미지 다운로드 실패");
            }

            // 기본 분석 수행
            ImageAnalysisResult result = performDetailedAnalysis(base64Image);
            
            // TODO: 필요시 인물 식별 추가 분석
            // if (needsPersonIdentification(result)) {
            //     result = enhanceWithPersonIdentification(base64Image, result);
            // }
            
            return result;

        } catch (Exception e) {
            log.error("Gemini API 호출 실패: {}", e.getMessage(), e);
            return createErrorResult("AI 분석 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 업로드된 이미지 파일을 직접 분석 (Google Gemini API 사용)
     */
    public ImageAnalysisResult analyzeImageFile(MultipartFile file) {
        try {
            log.info("업로드된 파일로 이미지 분석 시작: {}", file.getOriginalFilename());

            // 파일을 base64로 인코딩
            String base64Image = encodeFileToBase64(file);
            if (base64Image == null) {
                return createErrorResult("파일 인코딩 실패");
            }

            // 분석 수행
            return performDetailedAnalysis(base64Image);

        } catch (Exception e) {
            log.error("파일 분석 실패: {}", e.getMessage(), e);
            return createErrorResult("파일 분석 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 상세 분석 수행
     */
    private ImageAnalysisResult performDetailedAnalysis(String base64Image) {
        try {
            log.info("Gemini API로 상세 분석 수행");

            // Gemini API 요청 구성
            GeminiRequest request = new GeminiRequest();
            
            // 프롬프트와 이미지 설정
            GeminiContent content = new GeminiContent();
            content.setParts(Arrays.asList(
                new GeminiPart("text", "이 이미지를 매우 상세하게 분석하여 다음 JSON 형태로만 반환하세요:\n\n" +
                    "{\n" +
                    "  \"tags\": [\"상세한 한국어 태그 7-10개\"],\n" +
                    "  \"grade\": \"등급\",\n" +
                    "  \"isAppropriate\": true/false,\n" +
                    "  \"qualityScore\": 점수\n" +
                    "}\n\n" +
                    "**상세 분석 기준:**\n" +
                    "- tags: 다음 요소들을 포함한 구체적인 한국어 키워드 (7-10개)\n" +
                    "  * 인물 정보: 성별, 연령대, 직업, 유명인이라면 이름이나 별명\n" +
                    "  * 신체적 특징: 체형, 헤어스타일, 의상, 액세서리\n" +
                    "  * 행동/포즈: 구체적인 동작, 표정, 자세\n" +
                    "  * 배경/환경: 장소, 분위기, 조명, 색상\n" +
                    "  * 스타일/장르: 사진 스타일, 촬영 기법\n" +
                    "  * 스포츠/직업: 종목, 리그, 팀, 타이틀, 성과\n" +
                    "- grade: 이미지 품질 평가\n" +
                    "  * S등급(0.9-1.0): 전문가 수준, 예술적 가치, 완벽한 구도/조명\n" +
                    "  * A등급(0.8-0.89): 높은 품질, 선명함, 좋은 구도\n" +
                    "  * B등급(0.7-0.79): 평균 이상, 일반적 품질\n" +
                    "  * C등급(0.6-0.69): 평균 수준, 약간의 흐림이나 구도 문제\n" +
                    "  * D등급(0.0-0.59): 낮은 품질, 흐림, 나쁜 구도\n" +
                    "- qualityScore: 위 기준에 맞는 0.0~1.0 점수\n" +
                    "- isAppropriate: 폭력/성인/혐오 내용 없으면 true, 있으면 false\n\n" +
                    "**예시:**\n" +
                    "권투선수 사진 → [\"우식\", \"올렉산드르우식\", \"권투선수\", \"십자가목걸이\", \"검은머리\", \"집중된표정\", \"운동복\", \"체육관\", \"헤비급챔피언\", \"우크라이나\"]\n" +
                    "축구선수 사진 → [\"메시\", \"리오넬메시\", \"축구선수\", \"아르헨티나\", \"유니폼\", \"드리블\", \"경기장\", \"월드컵우승\", \"발롱도르\", \"레전드\"]\n\n" +
                    "유명인, 가상인물이라면 이름을 포함하고 아닐 확률이 매우 높을시에만 직업이나 특징으로 대체하세요.\n" +
                    "JSON만 반환하고 다른 설명은 하지 마세요."),
                createImagePart(base64Image, "image/jpeg") // base64 이미지 데이터 사용
            ));

            
            request.setContents(List.of(content));

            // 일관성을 위한 설정 추가
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.4); // 창의성과 일관성의 균형
            generationConfig.put("candidateCount", 1);
            generationConfig.put("maxOutputTokens", 300); // 더 상세한 분석을 위해 증가
            request.setGenerationConfig(generationConfig);

            // API 호출
            log.info("Gemini API 요청 전송");
            
            Mono<String> responseMono = webClient.post()
                    .uri("/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), 
                        clientResponse -> {
                            log.error("4xx 클라이언트 오류: {}", clientResponse.statusCode());
                            return clientResponse.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Gemini API 클라이언트 오류: " + body));
                        })
                    .onStatus(status -> status.is5xxServerError(),
                        serverResponse -> {
                            log.error("5xx 서버 오류: {}", serverResponse.statusCode());
                            return serverResponse.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Gemini API 서버 오류: " + body));
                        })
                    .bodyToMono(String.class);

            String responseBody = responseMono.block();
            log.info("Gemini API 응답 성공");
            // 응답 파싱
            return parseGeminiResponse(responseBody);
        } catch (Exception e) {
            log.error("Gemini API 호출 실패: {}", e.getMessage(), e);
            return createErrorResult("AI 분석 중 오류 발생: " + e.getMessage());
        }
    }



//    캐릭터별 이미지 분석 ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ
    public ImageCharAnalysisResult charAnalyzeImage(String imageUrl) {
        try {
            log.info("Google Gemini API로 캐릭터가 이미지 분석 시작: {}", imageUrl);

            // 이미지 다운로드 및 base64 인코딩
            String base64Image = downloadAndEncodeImage(imageUrl);
            if (base64Image == null) {
                return createCharErrorResult("이미지 다운로드 실패");
            }

            // 기본 분석 수행
            ImageCharAnalysisResult result = characterDetailAnalyze(base64Image);

            // TODO: 필요시 인물 식별 추가 분석
            // if (needsPersonIdentification(result)) {
            //     result = enhanceWithPersonIdentification(base64Image, result);
            // }

            return result;

        } catch (Exception e) {
            log.error("Gemini API 호출 실패: {}", e.getMessage(), e);
            return createCharErrorResult("AI 분석 중 오류 발생: " + e.getMessage());
        }
    }
    /**
     * 업로드된 이미지 파일을 직접 분석 캐릭(Google Gemini API 사용)
     */
    public ImageCharAnalysisResult charAnalyzeImageFile(MultipartFile file) {
        try {
            log.info("업로드된 파일로 캐릭터가 이미지 분석 시작: {}", file.getOriginalFilename());

            // 파일을 base64로 인코딩
            String base64Image = encodeFileToBase64(file);
            if (base64Image == null) {
                return createCharErrorResult("파일 인코딩 실패");
            }

            // 분석 수행
            return characterDetailAnalyze(base64Image);

        } catch (Exception e) {
            log.error("파일 분석 실패: {}", e.getMessage(), e);
            return createCharErrorResult("파일 분석 중 오류 발생: " + e.getMessage());
        }
    }


    private ImageCharAnalysisResult characterDetailAnalyze(String base64Image) {
        try {
            log.info("Gemini API로 상세 분석 수행");

            // Gemini API 요청 구성
            GeminiRequest request = new GeminiRequest();

            // 프롬프트와 이미지 설정
            GeminiContent content = new GeminiContent();
            content.setParts(Arrays.asList(
                    new GeminiPart("text", "제공되는 이미지를보고 답변자가 마샬 D 티치(검은수염)이라고 생각하고 상세하게 분석하여 다음 JSON 형태로만 반환하세요:\n\n" +
                            "{\n" +
                            "  \"tags\": [\"상세한 한국어 태그 7-10개\"],\n" +
                            "  \"imageAnalyze\": 여기에 원피스의 \"마샬 D 티치(검은수염)\"가 대답하는이 작성해주세요 ,\n" +
                            "}\n\n" +
                            "**상세 분석 기준:**\n" +
                            "- tags: 다음 요소들을 포함한 구체적인 한국어 키워드 (7-10개)\n" +
                            "  * 인물 정보: 성별, 연령대, 직업, 유명인이라면 이름이나 별명\n" +
                            "  * 신체적 특징: 체형, 헤어스타일, 의상, 액세서리\n" +
                            "  * 행동/포즈: 구체적인 동작, 표정, 자세\n" +
                            "  * 배경/환경: 장소, 분위기, 조명, 색상\n" +
                            "  * 스타일/장르: 사진 스타일, 촬영 기법\n" +
                            "  * 스포츠/직업: 종목, 리그, 팀, 타이틀, 성과\n" +
                            "- imageAnalyze: 답변하는 사람이 만화 \"원피스\"의 \"마샬 D 티치(검은수염)\"라고 생각하고 제공된 이미지를 본 감상을 써주세요 만약 제공된 이미지에 원피스 등장인물이 있으면 그 관계성도 생각해도 되고, 그게 아니라면 현실 세계관에 적용해서 마샬 D 티치(검은수염)의 말투로 말해주면 됩니다. 예를들어 원피스의 \"흰수염\" 이미지 라면 원피스 세계 최강의사나이면서 골드로저의 라이벌이다, \"리오넬 메시 라면\" 제하하하하!! 상당히 축구를 잘한다더군!! 이런식으로\n\n, 유명인/가상인물이라면 반드시 이름을 언급해주세요" +
                            " \"검은수염 마샬 D. 티치\"의 말투는 투박하고 시끄럽고, 웃음이 많은 해적 스타일. \"제하하하하\"가 트레이드마크인 웃음소리. 항상 운명, 야망, 자유 같은 걸 강조. \n" +
                            "말투예시 : “제하하하! 인간의 꿈은 끝나지 않아!!” 녀석들이 말하는 '새 시대'란 건 엿 같은 얘기다. 해적이 꿈을 꾸는 시대가 끝난다고···?!! 응?!! 어이!!! 크하하하하하하하!!!\n" +
                            "\n" +
                            "사람의 꿈은!!! 끝나지 않아!!!! 그렇지?!!, 집어쳐. 정의나 악을 입에 올리는 건!! ···이 세상 어디를 뒤져봐도, 답은 없잖나. 너절하긴!!!\n"+
                            "그래, 헛수고라는 말은 않겠다. 이 세상에 불가능한 일이란 무엇 하나도 없으니 말이다. ──하늘섬은 있었지? 최고의 보물 '원피스'도 마찬가지!! 반드시 존재한다!!!!"+
                            "\n"+
                            "유명인, 가상인물이라면 이름을 포함하고 아닐 확률이 매우 높을시에만 직업이나 특징으로 대체하세요.\n" +
                            "JSON만 반환하고 다른 설명은 하지 마세요."),
                    createImagePart(base64Image, "image/jpeg") // base64 이미지 데이터 사용
            ));


            request.setContents(List.of(content));

            // 일관성을 위한 설정 추가
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.4); // 창의성과 일관성의 균형
            generationConfig.put("candidateCount", 1);
            generationConfig.put("maxOutputTokens", 500); // 더 상세한 분석을 위해 증가
            request.setGenerationConfig(generationConfig);

            // API 호출
            log.info("Gemini API 캐릭 요청 전송");

            Mono<String> responseMono = webClient.post()
                    .uri("/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                            clientResponse -> {
                                log.error("4xx 클라이언트 오류2: {}", clientResponse.statusCode());
                                return clientResponse.bodyToMono(String.class)
                                        .map(body -> new RuntimeException("Gemini API 클라이언트 오류: " + body));
                            })
                    .onStatus(status -> status.is5xxServerError(),
                            serverResponse -> {
                                log.error("5xx 서버 오류2: {}", serverResponse.statusCode());
                                return serverResponse.bodyToMono(String.class)
                                        .map(body -> new RuntimeException("Gemini API 서버 오류: " + body));
                            })
                    .bodyToMono(String.class);

            String responseBody = responseMono.block();
            log.info("Gemini API 캐릭 응답 성공");
            // 응답 파싱
            return parseGeminiCharResponse(responseBody);
        } catch (Exception e) {
            log.error("Gemini API 호출 실패: {}", e.getMessage(), e);
            return createCharErrorResult("AI 분석 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * Gemini 응답 파싱
     */
    private ImageAnalysisResult parseGeminiResponse(String responseBody) {
        try {
            log.info("Gemini 응답 파싱 시작");
            
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            
            if (candidates == null || candidates.isEmpty()) {
                return createErrorResult("Gemini AI 응답이 비어있습니다.");
            }

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String text = (String) parts.get(0).get("text");
            
            log.info("Gemini 응답 텍스트: {}", text);
            
            // JSON 부분만 추출 (```json과 ``` 제거)
            String jsonText = text.trim();
            if (jsonText.startsWith("```json")) {
                jsonText = jsonText.substring(7);
            }
            if (jsonText.startsWith("```")) {
                jsonText = jsonText.substring(3);
            }
            if (jsonText.endsWith("```")) {
                jsonText = jsonText.substring(0, jsonText.length() - 3);
            }
            jsonText = jsonText.trim();
            
            // JSON 파싱
            Map<String, Object> aiResult = objectMapper.readValue(jsonText, Map.class);
            
            return ImageAnalysisResult.builder()
                    .tags((List<String>) aiResult.getOrDefault("tags", new ArrayList<>()))
                    .grade((String) aiResult.getOrDefault("grade", "C"))
                    .isAppropriate((Boolean) aiResult.getOrDefault("isAppropriate", true))
                    .qualityScore(((Number) aiResult.getOrDefault("qualityScore", 0.7)).doubleValue())
                    .confidence(0.9)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Gemini 응답 파싱 실패: {}", e.getMessage());
            return createErrorResult("AI 응답 파싱 중 오류 발생: " + e.getMessage());
        }
    }

    private ImageCharAnalysisResult parseGeminiCharResponse(String responseBody) {
        try {
            log.info("Gemini 응답 파싱 시작");

            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");

            if (candidates == null || candidates.isEmpty()) {
                return createCharErrorResult("Gemini AI 응답이 비어있습니다.");
            }

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String text = (String) parts.get(0).get("text");

            log.info("Gemini 응답 텍스트: {}", text);

            // JSON 부분만 추출 (```json과 ``` 제거)
            String jsonText = text.trim();
            if (jsonText.startsWith("```json")) {
                jsonText = jsonText.substring(7);
            }
            if (jsonText.startsWith("```")) {
                jsonText = jsonText.substring(3);
            }
            if (jsonText.endsWith("```")) {
                jsonText = jsonText.substring(0, jsonText.length() - 3);
            }
            jsonText = jsonText.trim();

            // JSON 파싱
            Map<String, Object> aiResult = objectMapper.readValue(jsonText, Map.class);

            return ImageCharAnalysisResult.builder()
                    .tags((List<String>) aiResult.getOrDefault("tags", new ArrayList<>()))
                    .imageAnalyze((String) aiResult.getOrDefault("imageAnalyze", "캐릭터"))
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Gemini 캐릭 응답 파싱 실패: {}", e.getMessage());
            return createCharErrorResult("AI 응답 파싱 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 오류 결과 생성
     */
    private ImageAnalysisResult createErrorResult(String message) {
        return ImageAnalysisResult.builder()
                .tags(new ArrayList<>())
                .grade("C")
                .isAppropriate(true)
                .qualityScore(0.5)
                .confidence(0.0)
                .success(false)
                .errorMessage(message)
                .build();
    }

    private ImageCharAnalysisResult createCharErrorResult(String message) {
        return ImageCharAnalysisResult.builder()
                .tags(new ArrayList<>())
                .imageAnalyze("실패")
                .success(false)
                .errorMessage(message)
                .build();
    }

    /**
     * 이미지 URL에서 이미지를 다운로드하고 base64로 인코딩
     */
    private String downloadAndEncodeImage(String imageUrl) {
        try {
            log.info("이미지 다운로드 시작: {}", imageUrl);
            
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setConnectTimeout(10000); // 10초 타임아웃
            connection.setReadTimeout(30000); // 30초 읽기 타임아웃
            
            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                byte[] imageBytes = outputStream.toByteArray();
                String base64 = Base64.getEncoder().encodeToString(imageBytes);
                
                log.info("이미지 다운로드 및 인코딩 완료. 크기: {} bytes", imageBytes.length);
                return base64;
            }
            
        } catch (Exception e) {
            log.error("이미지 다운로드 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * MultipartFile을 base64로 인코딩
     */
    private String encodeFileToBase64(MultipartFile file) {
        try {
            log.info("파일 인코딩 시작: {}, 크기: {} bytes", file.getOriginalFilename(), file.getSize());
            
            byte[] fileBytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(fileBytes);
            
            log.info("파일 인코딩 완료. 크기: {} bytes", fileBytes.length);
            return base64;
            
        } catch (Exception e) {
            log.error("파일 인코딩 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * base64 이미지 데이터로 GeminiPart 생성
     */
    private GeminiPart createImagePart(String base64Data, String mimeType) {
        GeminiPart part = new GeminiPart();
        
        Map<String, String> inlineData = new HashMap<>();
        inlineData.put("mime_type", mimeType);
        inlineData.put("data", base64Data);
        
        part.setInlineData(inlineData);
        return part;
    }

    // === Google Gemini API 요청/응답 클래스들 ===

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiRequest {
        private List<GeminiContent> contents;
        private Map<String, Object> generationConfig;

        public void setContents(List<GeminiContent> contents) {
            this.contents = contents;
        }

        public Map<String, Object> getGenerationConfig() {
            return generationConfig;
        }

        public void setGenerationConfig(Map<String, Object> generationConfig) {
            this.generationConfig = generationConfig;
        }
    }

    @Data
    @NoArgsConstructor  
    @AllArgsConstructor
    public static class GeminiContent {
        private List<GeminiPart> parts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiPart {
        private String text;
        @JsonProperty("inline_data")
        private Map<String, String> inlineData;

        public GeminiPart(String type, String value) {
            if ("text".equals(type)) {
                this.text = value;
            } else if ("image_url".equals(type)) {
                // 더 이상 사용하지 않음 - base64 방식으로 변경
                log.warn("image_url 방식은 더 이상 지원하지 않습니다. base64 방식을 사용하세요.");
            }
        }

        public void setText(String text) {
            this.text = text;
        }

        public void setInlineData(Map<String, String> inlineData) {
            this.inlineData = inlineData;
        }
    }

    /**
     * AI 분석 결과 DTO
     */
    @Schema(description = "이미지 AI 분석 결과")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class ImageAnalysisResult {
        @Schema(description = "추출된 태그 목록", example = "[\"자연\", \"산\", \"하늘\"]")
        private List<String> tags;
        
        @Schema(description = "이미지 품질 등급", example = "A")
        private String grade;
        
        @Schema(description = "이미지 적절성 여부", example = "true")
        private boolean isAppropriate;
        
        @Schema(description = "품질 점수 (0.0~1.0)", example = "0.85")
        private double qualityScore;
        
        @Schema(description = "AI 신뢰도 (0.0~1.0)", example = "0.92")
        private double confidence;
        
        @Schema(description = "분석 성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "오류 메시지 (실패시)")
        private String errorMessage;
    }

    /**
     * AI 분석 결과 DTO
     */
    @Schema(description = "이미지 캐릭AI 분석 결과")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class ImageCharAnalysisResult {
        @Schema(description = "추출된 태그 목록", example = "[\"자연\", \"산\", \"하늘\"]")
        private List<String> tags;

        @Schema(description = "분석 성공 여부", example = "true")
        private boolean success;

        @Schema(description = "캐릭터의 이미지 분석", example = "제하하하하! 골드 로저는 해적왕이지")
        private String imageAnalyze;

        @Schema(description = "오류 메시지 (실패시)")
        private String errorMessage;
    }
} 