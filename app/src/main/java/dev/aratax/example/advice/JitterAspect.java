package dev.aratax.example.advice;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Aspect
@Order(-1)
@Component
public class JitterAspect {

    private final Random random = new Random();

    @Around("@annotation(dev.aratax.example.annotation.InjectJitter)")
    public Object addJitter(ProceedingJoinPoint pjp) throws Throwable {
        Thread.sleep(calculateJitter());
        try {
            return pjp.proceed();
        } finally {
            if (random.nextBoolean()) {
                Thread.sleep(calculateJitter());
            }
        }
    }

    private int calculateJitter() {
        return ThreadLocalRandom.current().nextInt(10, 51);
    }

}
