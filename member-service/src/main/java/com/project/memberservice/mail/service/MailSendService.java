package com.project.memberservice.mail.service;

import com.project.memberservice.entity.Member;
import com.project.memberservice.mail.dto.MailCheckDto;
import com.project.memberservice.mail.redis.RedisUtil;
import com.project.memberservice.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Random;

@Slf4j
@Service
public class MailSendService {

    @Value("${spring.mail.username}")  //____@email.com
    private String username;
    private int authNumber;

    private JavaMailSender mailSender;
    private RedisUtil redisUtil;
    private MemberRepository memberRepository;

    public MailSendService(MemberRepository memberRepository, RedisUtil redisUtil, JavaMailSender mailSender) {
        this.memberRepository = memberRepository;
        this.redisUtil = redisUtil;
        this.mailSender = mailSender;
    }

    /**
     * 인증번호 생성
     */
    public void makeRandomNumber(){
        //6자리 난수 생성
        Random r = new Random();
        int randNum = r.nextInt(888888) + 111111;
        authNumber = randNum;
    }

    /**
     * 메일 전송 정보 생성
     *
     * @param email 수신자 이메일 주소
     * @return 인증번호
     */
    public String composeMail(String email) {
        makeRandomNumber();
        String from = username+"@gmail.com";
        String to = email;
        String title = "[e-commerce] 인증번호 안내";
        String content = "인증 번호는 " + authNumber + "입니다.";
        mailSend(from, to, title, content);
        return Integer.toString(authNumber);
    }

    /**
     * 메일 전송
     * 
     * @param from 발신자 메일주소
     * @param to 수신자 메일주소
     * @param title 메일 제목
     * @param content 메일 내용
     */
    public void mailSend(String from, String to, String title, String content) {
        MimeMessage message = mailSender.createMimeMessage();   //JavaMailSender 객체를 사용하여 MimeMessage 객체 생성
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message,true,"utf-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(title);
            helper.setText(content,true);   //html 설정 true
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        redisUtil.setDataExpire(Integer.toString(authNumber), to,60*5L);    //유효시간 5분
    }

    /**
     * 입력한 인증번호에 해당하는 이메일(key)가 요청자와 일치하면 -> is_verified 컬럼의 값을 1(true)로 변경한다.
     *
     * @param mailCheckDto
     * @return 인증번호 일치 여부
     */
    public ResponseEntity checkAuthNumber(MailCheckDto mailCheckDto) throws UsernameNotFoundException {
        String email = mailCheckDto.getEmail();
        String authNum = mailCheckDto.getAuthNum();

        String authEmail = redisUtil.getData(authNum);   //인증번호가 발급된 email

        if(authEmail==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증번호입니다.");
        } else if (authEmail.equals(email)){  //인증번호 일치
            //사용자 정보 조회
            Member member = memberRepository.findByEmail(email).orElse(null);

            if (member != null) {
                log.info("memberId : {}", member.getMemberId());
                member.setIsVerified(1);  //true
                memberRepository.save(member);
                return ResponseEntity.ok("인증이 완료되었습니다. memberId: " + member.getMemberId());
            } else {
                throw new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다.");
            }

        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증번호입니다.");
        }
    }
}
