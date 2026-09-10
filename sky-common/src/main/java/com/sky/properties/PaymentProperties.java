package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付配置属性
 */
@Component
@ConfigurationProperties(prefix = "sky.payment")
@Data
public class PaymentProperties {

    /**
     * 支付模式：mock 模拟支付，wechat 微信支付
     */
    private String mode = "mock";

    public boolean isMock() {
        return "mock".equalsIgnoreCase(mode);
    }
}
