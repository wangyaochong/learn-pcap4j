package org.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/test")
public class TestController {


    @Value("${server.port}")
    Integer serverPort;
    AtomicInteger index = new AtomicInteger(0);

    @RequestMapping("/test")
    public String test(String number) {
        System.out.println("test,serverPort=" + serverPort + ",number=" + number + ",index=" + index.get());
        return "test,serverPort=" + serverPort + ",number=" + number + ",index=" + index.getAndIncrement();
    }
}
