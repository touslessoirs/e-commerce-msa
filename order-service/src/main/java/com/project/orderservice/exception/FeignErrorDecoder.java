package com.project.orderservice.exception;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        switch (response.status()) {
            case 400:
                //NotFoundException
                break;
            case 404:
                if(methodKey.contains("getProductDetail")) {
                    return new ResponseStatusException(HttpStatusCode.valueOf(response.status()),
                            "해당 상품의 정보가 없습니다.");
                }
                // 404 Not Found 처리
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다.");
            case 500:
                //InternalServerErrorException
                break;
            default:
                return new Exception(response.reason());
        }

        return null;
    }
}
