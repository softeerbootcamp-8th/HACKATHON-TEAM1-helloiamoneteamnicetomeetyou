package com.helloiamoneteamnicetomeetyou.hackathon.admin.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
