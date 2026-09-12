package com.sky.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务，用于向管理端推送订单消息
 */
@Component
@ServerEndpoint("/ws/{sid}")
@Slf4j
public class WebSocketServer {

    /**
     * 保存管理端客户端标识和WebSocket会话
     */
    private static final Map<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 客户端建立连接
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        Session previousSession = SESSION_MAP.put(sid, session);
        if (previousSession != null && previousSession != session && previousSession.isOpen()) {
            try {
                previousSession.close();
            } catch (Exception exception) {
                log.warn("关闭重复WebSocket连接失败，客户端：{}", sid, exception);
            }
        }
        log.info("WebSocket客户端建立连接：{}，当前连接数：{}", sid, SESSION_MAP.size());
    }

    /**
     * 接收客户端消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("收到WebSocket客户端消息，客户端：{}，内容：{}", sid, message);
    }

    /**
     * 客户端关闭连接
     */
    @OnClose
    public void onClose(Session session, @PathParam("sid") String sid) {
        SESSION_MAP.remove(sid, session);
        log.info("WebSocket客户端断开连接：{}，当前连接数：{}", sid, SESSION_MAP.size());
    }

    /**
     * WebSocket通信发生异常
     */
    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("sid") String sid) {
        SESSION_MAP.remove(sid, session);
        log.warn("WebSocket连接异常，客户端：{}", sid, throwable);
    }

    /**
     * 向所有已连接的管理端客户端发送消息
     *
     * @param message JSON格式的订单消息
     */
    public void sendToAllClient(String message) {
        SESSION_MAP.forEach((sid, session) -> {
            if (!session.isOpen()) {
                SESSION_MAP.remove(sid, session);
                return;
            }

            try {
                synchronized (session) {
                    session.getBasicRemote().sendText(message);
                }
            } catch (Exception exception) {
                SESSION_MAP.remove(sid, session);
                log.warn("向WebSocket客户端发送消息失败，客户端：{}", sid, exception);
            }
        });
    }
}
