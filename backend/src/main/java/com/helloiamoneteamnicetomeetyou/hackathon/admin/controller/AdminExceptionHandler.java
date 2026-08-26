package com.helloiamoneteamnicetomeetyou.hackathon.admin.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 어드민 화면에서 난 예외를 화면 언어로 돌려준다.
 *
 * <p>{@code GlobalExceptionHandler} 는 {@code @RestControllerAdvice} 라 JSON 을 내려보내는데,
 * 브라우저로 폼을 넘긴 사람이 받는 것이 날 JSON 이면 방금 무엇이 안 됐는지 알 수 없고 뒤로
 * 가기로 돌아와야 한다. 부스에서 손이 급할 때 겪을 일이 아니다.
 *
 * <p>어드민 패키지로 범위를 좁히고 우선순위를 올려서 {@code /api} 응답 형식은 건드리지 않는다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(basePackages = "com.helloiamoneteamnicetomeetyou.hackathon.admin")
public class AdminExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public String handleApplicationException(
            ApplicationException e, HttpServletRequest request, RedirectAttributes redirectAttributes) {

        log.warn("어드민 화면 예외 - [{}] {} {}",
                request.getMethod(), request.getRequestURI(), e.getErrorType().getMessage());

        redirectAttributes.addFlashAttribute("toast", e.getErrorType().getMessage());
        redirectAttributes.addFlashAttribute("toastTone", "danger");

        return "redirect:" + refererOrHome(request);
    }

    /**
     * 아직 안 막아 둔 참조 제약에 걸렸을 때.
     *
     * <p>딸린 줄을 지우거나 미리 막아 두는 것이 먼저지만, 표가 늘어날 때마다 빠뜨리는 자리가
     * 생긴다. 그때 운영자가 받는 것이 스택 트레이스가 박힌 흰 화면이면 부스에서는 손을 놓게
     * 된다. 무엇에 걸렸는지 한 줄로 알려 주고 하던 자리로 돌려보낸다. 자세한 것은 서버 로그에
     * 남긴다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolation(
            DataIntegrityViolationException e, HttpServletRequest request, RedirectAttributes redirectAttributes) {

        log.error("어드민 화면 참조 제약 위반 - [{}] {}", request.getMethod(), request.getRequestURI(), e);

        redirectAttributes.addFlashAttribute("toast", "다른 데이터가 이걸 쓰고 있어 처리하지 못했습니다.");
        redirectAttributes.addFlashAttribute("toastTone", "danger");

        return "redirect:" + refererOrHome(request);
    }

    /**
     * 왔던 화면으로 되돌린다.
     *
     * <p>실패한 자리에 그대로 남아야 무엇을 고쳐야 하는지 보인다. {@code Referer} 가 없거나
     * 어드민 밖을 가리키면 대시보드로 보낸다. 남이 준 주소로 그대로 리다이렉트하면 바깥
     * 사이트로 튕겨 보낼 수 있어서, 어드민 경로인지 확인한다.
     */
    private String refererOrHome(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null) {
            return "/admin";
        }

        int pathStart = referer.indexOf("/admin");
        return pathStart >= 0 ? referer.substring(pathStart) : "/admin";
    }
}
