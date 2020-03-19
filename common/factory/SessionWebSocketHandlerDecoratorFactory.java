


import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;



/**
 * \* Description: session建立时触发
 * \
 */

@Component
public class SessionWebSocketHandlerDecoratorFactory implements WebSocketHandlerDecoratorFactory {

//自定义RedisTemplate
    @Resource(name = "messageRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

//离线消息
    @Resource
    private OfflineMessageMapper offlineMessageMapper;

    @Resource
    private MessageReceiveService messageReceiveService;

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler){


            /**
             * WebSocket连接建立
             * @param session WebSocketSession
             * @throws Exception Exception
             */
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                super.afterConnectionEstablished(session);

                String token = Objects.requireNonNull(session.getHandshakeHeaders().get(HEADER_STRING)).get(0);
                String userId = Objects.requireNonNull(JwtUtil.parseToken(token)).getAudience();
                redisTemplate.opsForHash().put(WEBSOCKET_USER, userId, session);


                // 客户端上线 离线消息发送

                List<OfflineMessage> appraiseOfflineMessageList = offlineMessageMapper.listOfflineMessageByToIdAndType(userId, MessageConstant.MESSAGE_TYPE_APPRAISE);

                if (!appraiseOfflineMessageList.isEmpty()){

                    appraiseOfflineMessageList.forEach(appraiseOfflineMessage -> {

                        MessageVo<AppraiseDataVo> message = new MessageVo<>();
                        List<String> toIdList = new ArrayList<>();

                        toIdList.add(appraiseOfflineMessage.getToId());

                        message.setToIdList(toIdList);

                        BeanUtils.copyProperties(appraiseOfflineMessage, message);

                        messageReceiveService.receiveAppraiseMessage(message);


                    });

                }

                List<OfflineMessage> voteOfflineMessageList = offlineMessageMapper.listOfflineMessageByToIdAndType(userId, MessageConstant.MESSAGE_TYPE_VOTE);

                if (!voteOfflineMessageList.isEmpty()){

                    voteOfflineMessageList.forEach(voteOfflineMessage -> {

                        MessageVo<VoteDataVo> message = new MessageVo<>();
                        List<String> toIdList = new ArrayList<>();

                        toIdList.add(voteOfflineMessage.getToId());

                        message.setToIdList(toIdList);

                        BeanUtils.copyProperties(voteOfflineMessage, message);

                        messageReceiveService.receiveVoteMessage(message);

                    });


                }


                // 向当前会话发送消息
                // session.sendMessage(new TextMessage("message"));

                // 心跳检测
                // session.sendMessage(new PingMessage());
                // session.sendMessage(new PongMessage());

            }



            /**
             * WebSocket连接关闭
             * @param session WebSocketSession
             * @param closeStatus CloseStatus
             * @throws Exception Exception
             */
            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                super.afterConnectionClosed(session, closeStatus);
                String token = Objects.requireNonNull(session.getHandshakeHeaders().get(HEADER_STRING)).get(0);
                String userId = Objects.requireNonNull(JwtUtil.parseToken(token)).getAudience();
                redisTemplate.opsForHash().delete(WEBSOCKET_USER, userId);
            }

        };
    }
}
