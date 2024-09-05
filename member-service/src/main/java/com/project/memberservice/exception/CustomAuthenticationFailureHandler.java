//package com.project.memberservice.exception;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.web.authentication.AuthenticationFailureHandler;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//
//@Component
//public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
//
//    @Override
//    public void onAuthenticationFailure(HttpServletRequest request,
//                                        HttpServletResponse response,
//                                        AuthenticationException exception) throws IOException, ServletException {
//
//        response.setStatus(HttpStatus.UNAUTHORIZED.value());
//        response.setContentType("application/json;charset=UTF-8");
//
//        ErrorCode errorCode = ErrorCode.AUTHENTICATION_FAILED;
//
//        ErrorResponseEntity errorResponse = ErrorResponseEntity.builder()
//                .status(errorCode.getHttpStatus().value())
//                .name(errorCode.name())
//                .code(errorCode.getCode())
//                .message("입력하신 계정 정보가 올바르지 않습니다.")
//                .build();
//
//        ObjectMapper objectMapper = new ObjectMapper();
//        String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
//
//        response.getWriter().write(errorResponseJson);
//    }
//
//}
