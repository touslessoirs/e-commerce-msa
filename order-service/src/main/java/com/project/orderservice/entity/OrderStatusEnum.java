package com.project.orderservice.entity;

public enum OrderStatusEnum {
    PAYMENT_COMPLETED,  //결제완료
    SHIPPING,           //배송중
    DELIVERED,          //배송완료
    CANCELLED,          //주문취소
    RETURN_REQUESTED,   //반품신청
    RETURN_COMPLETED    //반품완료
}
