/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.CountDownLatch;

/**
 * Mail: xiao@terminus.io <br>
 * Date: 2014-11-07 5:29 PM  <br>
 * Author: xiao
 */
@Slf4j
public class BootStrap {

    public static void main(String[] args) throws InterruptedException {
        final ClassPathXmlApplicationContext ac =
                new ClassPathXmlApplicationContext("spring/item-dubbo-consumer.xml","spring/item-dubbo-provider.xml");
        ac.start();
        log.info("Item Service started successfully");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                log.info("Shutdown hook was invoked. Shutting down Item Service.");
                ac.close();
            }
        });
        //prevent main thread from exit
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}
