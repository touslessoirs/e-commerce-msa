package com.project.memberservice.exception;

import feign.Response;
import feign.codec.ErrorDecoder;
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
                if(methodKey.contains("getOrdersByMemberId")) {
                    return new ResponseStatusException(HttpStatusCode.valueOf(response.status()),
                            "해당 회원의 주문 정보가 없습니다.");
                }
                break;
            case 500:
                //InternalServerErrorException
                break;
            default:
                return new Exception(response.reason());
        }

        return null;
    }
}
