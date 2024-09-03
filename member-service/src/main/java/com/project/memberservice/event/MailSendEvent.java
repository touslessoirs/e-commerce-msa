package com.project.memberservice.event;

public record MailSendEvent(
        String from,       //발신자 메일주소
        String to,         //수신자 메일주소
        String title,      //메일 제목
        String content,    //내용
        String authNumber  //인증번호
) {
}