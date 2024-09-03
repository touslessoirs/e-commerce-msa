package com.project.memberservice.exception;

import feign.Response;
import feign.codec.ErrorDecoder;

public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        switch (response.status()) {
            case 400:   //NotFoundException
                break;
            case 404:
                if(methodKey.contains("getOrdersByMemberId")) {
                    return new CustomException(ErrorCode.ORDER_NOT_FOUND);
                }
                if(methodKey.contains("getProductDetail")) {
                    return new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
                }
                break;
            case 500:   //InternalServerErrorException
                break;
            default:
                return new Exception(response.reason());
        }

        return null;
    }
}
