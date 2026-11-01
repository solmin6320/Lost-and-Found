package com.example.lostandfound.service;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 채팅 본문의 개인정보를 저장 전에 가림
@Component
public class PersonalInfoMasker {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");

    // 휴대폰, 지역번호
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)0\\d{1,2}[-.\\s]?\\d{3,4}[-.\\s]?\\d{4}(?!\\d)");

    // 숫자와 -로 된 덩어리
    private static final Pattern NUMBER_BLOCK = Pattern.compile("(?<!\\d)\\d+(?:-\\d+)*(?!\\d)");

    private static final int ACCOUNT_MIN_DIGITS = 10;
    private static final int ACCOUNT_MAX_DIGITS = 16;


    // 이메일 -> 전화번호 -> 계좌번호 순서
    public Masked mask(String content) {
        String result = EMAIL.matcher(content).replaceAll("[이메일]");
        result = PHONE.matcher(result).replaceAll("[전화번호]");
        result = maskAccounts(result);

        return new Masked(result, !result.equals(content));
    }

    // 숫자가 10 ~ 16자리인 덩어리만 계좌번호로 봄
    private static String maskAccounts(String text) {
        Matcher matcher = NUMBER_BLOCK.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            long digits = matcher.group().chars().filter(Character::isDigit).count();
            boolean account = digits >= ACCOUNT_MIN_DIGITS && digits <= ACCOUNT_MAX_DIGITS;

            matcher.appendReplacement(result, account ? "[계좌번호]" : Matcher.quoteReplacement(matcher.group()));
        }

        matcher.appendTail(result);

        return result.toString();
    }

    // 가린 본문과 가린 곳이 있는지
    public record Masked(
            String content,
            boolean masked
    )
    {

    }


}

