package com.project.orderservice.exception;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        switch (response.status()) {
            case 400:
//                if (methodKey.contains("updateStock")) {
//                    return new CustomException(ErrorCode.STOCK_INSUFFICIENT);
//                } else if (methodKey.contains("isProductPurchasable") || methodKey.contains("updateStock")) {
//                    return new CustomException(ErrorCode.STOCK_INSUFFICIENT);
//                }
                if (methodKey.contains("processPurchase")) {
                    return new CustomException(ErrorCode.STOCK_INSUFFICIENT);
                }
                return new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.");
            case 403:
                if (methodKey.contains("processPurchase")) {
                    return new CustomException(ErrorCode.PURCHASE_TIME_INVALID);
                }
                return new ResponseStatusException(HttpStatus.FORBIDDEN, "접근이 거부되었습니다.");
            case 404:
                if (methodKey.contains("getProductDetail")) {
                    return new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
                }
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다.");
            case 500:
                return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
            default:
                return new Exception(response.reason());
        }
    }
}
