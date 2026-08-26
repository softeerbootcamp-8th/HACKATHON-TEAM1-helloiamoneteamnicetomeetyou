package com.helloiamoneteamnicetomeetyou.hackathon.global.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helloiamoneteamnicetomeetyou.hackathon.global.config.SseConfig;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 구독한 연결에 다른 기능이 보낸 이벤트가 실제로 닿는지까지 확인한다.
 *
 * <p>연결 등록과 발행을 따로 검증하면 둘 다 통과하면서 화면에는 아무것도 안 가는 상태가 나올 수
 * 있어서, 구독부터 수신까지를 한 테스트로 태운다.
 */
@WebMvcTest(SseController.class)
@Import({SseConfig.class, SseConnectionManager.class, SseEventPublisher.class, SseEventDispatcher.class})
@TestPropertySource(properties = {"sse.emitter-timeout-ms=60000", "sse.heartbeat-interval-ms=60000"})
@DisplayName("부스 SSE 구독")
class SseControllerTest {

    private static final long WAIT_TIMEOUT_MS = 2000;

    private final MockMvc mockMvc;
    private final SseEventPublisher sseEventPublisher;

    @Autowired
    SseControllerTest(MockMvc mockMvc, SseEventPublisher sseEventPublisher) {
        this.mockMvc = mockMvc;
        this.sseEventPublisher = sseEventPublisher;
    }

    @Test
    @DisplayName("연결하면 이벤트 스트림으로 응답하고 CONNECTED 를 먼저 보낸다")
    void 구독하면_CONNECTED_를_먼저_받는다() throws Exception {
        MvcResult result = subscribe(1L, UUID.randomUUID());

        assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("event:CONNECTED");
    }

    @Test
    @DisplayName("앞단 프록시가 스트림을 버퍼에 모으지 않도록 X-Accel-Buffering 을 붙인다")
    void 버퍼링_차단_헤더를_붙인다() throws Exception {
        mockMvc.perform(get("/api/booths/1/subscribe").param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Accel-Buffering", "no"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    @DisplayName("userId 를 빠뜨리면 팀 공통 형식의 400 으로 알려준다")
    void userId_가_없으면_400() throws Exception {
        mockMvc.perform(get("/api/booths/1/subscribe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].field").value("userId"));
    }

    @Test
    @DisplayName("같은 부스에 발행한 이벤트가 구독 중인 연결로 나간다")
    void 부스로_발행하면_받는다() throws Exception {
        MvcResult result = subscribe(1L, UUID.randomUUID());

        sseEventPublisher.toBooth(1L, SseEventType.USER_JOINED, Map.of("username", "홍길동"));

        assertThat(waitForContent(result, "event:USER_JOINED")).contains("홍길동");
    }

    @Test
    @DisplayName("다른 부스에 발행한 이벤트는 받지 않는다")
    void 다른_부스_이벤트는_받지_않는다() throws Exception {
        MvcResult result = subscribe(1L, UUID.randomUUID());

        sseEventPublisher.toBooth(2L, SseEventType.USER_JOINED, Map.of("username", "홍길동"));

        assertThat(waitQuietly()).isTrue();
        assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .doesNotContain("USER_JOINED");
    }

    @Test
    @DisplayName("사용자 지정 이벤트는 그 사용자의 연결로만 나간다")
    void 사용자에게_발행하면_그_사용자만_받는다() throws Exception {
        UUID 받는사람 = UUID.randomUUID();
        UUID 안받는사람 = UUID.randomUUID();

        MvcResult 받는연결 = subscribe(1L, 받는사람);
        MvcResult 안받는연결 = subscribe(1L, 안받는사람);

        sseEventPublisher.toUser(받는사람, SseEventType.MATCH_SUGGESTED, Map.of("exchangeId", 7));

        assertThat(waitForContent(받는연결, "event:MATCH_SUGGESTED")).contains("7");
        assertThat(안받는연결.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .doesNotContain("MATCH_SUGGESTED");
    }

    private MvcResult subscribe(Long boothId, UUID userId) throws Exception {
        return mockMvc
                .perform(get("/api/booths/{boothId}/subscribe", boothId)
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * 전송이 SSE 전용 스레드에서 일어나기 때문에 발행 직후에는 아직 응답에 안 실려 있다.
     * 찾는 이벤트가 끝까지 실릴 때까지 기다렸다가 그때의 응답 전체를 돌려준다.
     *
     * <p><b>이름 줄만 보고 끝내면 안 된다.</b> {@code event:} 와 {@code data:} 가 따로 실려서,
     * 이름이 보이는 순간에는 아직 내용이 비어 있을 수 있다. 부르는 쪽이 전부 내용을 보기 때문에
     * 이벤트를 닫는 빈 줄까지 온 것을 확인하고 돌려준다.
     */
    private String waitForContent(MvcResult result, String expected) throws Exception {
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;

        while (System.currentTimeMillis() < deadline) {
            String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
            int at = body.indexOf(expected);
            if (at >= 0 && body.indexOf("\n\n", at) >= 0) {
                return body;
            }
            Thread.sleep(20);
        }

        throw new AssertionError("%s 가 %dms 안에 오지 않았다".formatted(expected, WAIT_TIMEOUT_MS));
    }

    /** "안 온다"를 확인하려면 올 만한 시간을 줘 본 뒤에 봐야 한다. */
    private boolean waitQuietly() throws InterruptedException {
        Thread.sleep(200);
        return true;
    }
}
