package com.project.memberservice.service;

import com.project.memberservice.entity.Member;
import com.project.memberservice.event.MailSendEvent;
import com.project.memberservice.exception.CustomException;
import com.project.memberservice.exception.ErrorCode;
import com.project.memberservice.repository.MemberRepository;
import com.project.memberservice.security.UserDetailsImpl;
import com.project.memberservice.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailSendService {

    @Value("${spring.mail.username}")  //____@email.com
    private String username;

    private static final String MAIL_SEND_TOPIC = "mail-send-topic";

    private final JavaMailSender mailSender;
    private final RedisUtil redisUtil;
    private final MemberRepository memberRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 인증번호 생성
     */
    public int makeRandomNumber() {
        //6자리 난수 생성
        Random r = new Random();
        return r.nextInt(888888) + 111111;
    }

    /**
     * 메일 전송 정보 생성
     *
     * @param userDetails
     * @return 인증번호
     */
    public void composeMail(UserDetailsImpl userDetails) {
        // 이미 인증된 사용자인지 조회
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (member.getIsVerified() == 1) {
            throw new CustomException(ErrorCode.ALREADY_VERIFIED);
        }

        String authNumber = String.valueOf(makeRandomNumber());  // 인증번호 생성 메서드 호출
        String from = username + "@gmail.com";
        String to = userDetails.getUsername();;
        String title = "[e-commerce] 인증번호 안내";
        String content = "인증 번호는 <strong>" + authNumber + "</strong>입니다.";

        MailSendEvent mailSendEvent = new MailSendEvent(
                from,       //발신자 메일주소
                to,         //수신자 메일주소
                title,      //메일 제목
                content,    //내용
                authNumber  //인증번호
        );

        //mailSend event send
        kafkaTemplate.send(MAIL_SEND_TOPIC, mailSendEvent);
    }

    /**
     * 메일 전송
     *
     * @param mailSendEvent
     */
    @KafkaListener(topics = MAIL_SEND_TOPIC)
    public void mailSend(MailSendEvent mailSendEvent) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
            helper.setFrom(mailSendEvent.from());
            helper.setTo(mailSendEvent.to());
            helper.setSubject(mailSendEvent.title());
            helper.setText(mailSendEvent.content(), true);   //html 설정
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        redisUtil.setDataExpire(mailSendEvent.to(), mailSendEvent.authNumber(), 60 * 5L);    //유효시간 5분
    }

    /**
     * 인증번호 검증
     * 입력한 인증번호에 해당하는 이메일(key)가 요청자와 일치하면 -> is_verified 컬럼의 값을 1(true)로 변경한다.
     *
     * @param userDetails 사용자 email
     * @param authNumber 사용자가 입력한 인증번호
     * @return
     */
    public ResponseEntity checkAuthNumber(UserDetailsImpl userDetails, String authNumber) {
        String email = userDetails.getUsername();
        String storedAuthNum = redisUtil.getData(email);   //해당 email에 발급된 인증번호

        if (storedAuthNum == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증번호입니다.");
        } else if (storedAuthNum.equals(authNumber)) {  //인증번호 일치
            //사용자 정보 조회
            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            member.setIsVerified(1);  //true
            memberRepository.save(member);

            return ResponseEntity.ok("인증이 완료되었습니다. memberId: " + member.getMemberId());

        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증번호입니다.");
        }
    }
}
