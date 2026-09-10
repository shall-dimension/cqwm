package com.sky.controller.notify;

import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.properties.WeChatProperties;
import com.sky.service.OrderService;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付回调接口，仅在真实微信支付模式下启用
 */
@RestController
@RequestMapping("/notify")
@ConditionalOnProperty(name = "sky.payment.mode", havingValue = "wechat")
@Slf4j
public class PayNotifyController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private WeChatProperties weChatProperties;

    /**
     * 支付成功回调
     */
    @PostMapping("/paySuccess")
    public void paySuccessNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String body = readData(request);
        log.info("收到微信支付成功回调");

        String plainText = decryptData(body);
        JSONObject jsonObject = JSON.parseObject(plainText);
        String orderNumber = jsonObject.getString("out_trade_no");
        log.info("支付成功，订单号：{}", orderNumber);

        orderService.paySuccess(orderNumber);
        responseToWeChat(response);
    }

    private String readData(HttpServletRequest request) throws Exception {
        BufferedReader reader = request.getReader();
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (result.length() > 0) {
                result.append('\n');
            }
            result.append(line);
        }
        return result.toString();
    }

    private String decryptData(String body) throws Exception {
        JSONObject resultObject = JSON.parseObject(body);
        JSONObject resource = resultObject.getJSONObject("resource");
        String ciphertext = resource.getString("ciphertext");
        String nonce = resource.getString("nonce");
        String associatedData = resource.getString("associated_data");

        AesUtil aesUtil = new AesUtil(weChatProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        return aesUtil.decryptToString(
                associatedData.getBytes(StandardCharsets.UTF_8),
                nonce.getBytes(StandardCharsets.UTF_8),
                ciphertext
        );
    }

    private void responseToWeChat(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        Map<String, Object> result = new HashMap<>();
        result.put("code", "SUCCESS");
        result.put("message", "SUCCESS");
        response.setHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
        response.getOutputStream().write(JSONUtils.toJSONString(result).getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();
    }
}
