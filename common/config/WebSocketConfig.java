

import com.mage.factory.SessionWebSocketHandlerDecoratorFactory;
import com.sun.security.auth.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;



@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 以war包运行时注释掉该配置, 原因(会和tomcat冲突)
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter(){
        return new ServerEndpointExporter();
    }

    // websocket消息配置

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        //指服务端接收地址的前缀，意思就是说客户端给服务端发消息的地址的前缀
        registry.setApplicationDestinationPrefixes("/app");

        //表示客户端订阅地址的前缀信息，也就是客户端接收服务端消息的地址的前缀信息

        // 规范:
        // 1. 广播式: /topic/..
        // 2. 点对点式: /queue/..
        registry.enableSimpleBroker( "/topic/", "/vote/", "/appraise/");

        //设置用户 消息前缀
        registry.setUserDestinationPrefix("/user/");

        // 以 "/" 分隔
        registry.setPathMatcher(new AntPathMatcher("/"));

    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry stompEndpointRegistry) {
        //建立连接端点，注册一个STOMP的协议节点,并指定使用SockJS协议
        stompEndpointRegistry.addEndpoint("/app")
                .addInterceptors(new HandshakeInterceptor(){

                    /**
                     * 功能描述：http握手拦截器，最早执行
                     * 可以通过这个类的方法获取request和response 给后面使用
                     */
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {


                        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest)request;

                        HttpServletRequest httpServletRequest = servletRequest.getServletRequest();

                        String header = httpServletRequest.getHeader(HEADER_STRING);

                        if (header == null || !header.startsWith(TOKEN_PREFIX)) {

                            // 未通过
                            response.setStatusCode( HttpStatus.UNAUTHORIZED );
                            return false;

                        }

                        // todo 相关判断

                        response.setStatusCode(HttpStatus.OK);


                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
                        // 该方法不用 所以不实现
                    }
                })
                .setHandshakeHandler(new DefaultHandshakeHandler(){

                    /*
                     * http握手处理器
                     */

                    @Override
                    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {

                        //设置认证用户 userId

                        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest)request;

                        HttpServletRequest httpServletRequest = servletRequest.getServletRequest();

                        String userId = JwtUtil.claimGet(httpServletRequest).getAudience();

                        return new UserPrincipal((String) attributes.get(userId));

                    }
                })
                //跨域处理
                .setAllowedOrigins("*")
                .withSockJS();

    }


    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        /*
          添加一个工厂来装饰类用于处理WebSocket
          消息的处理程序。这对于某些高级用例可能很有用，
          例如*允许Spring Security在*相应的HTTP会话过期时强行关闭WebSocket会话。
         */
        registry.addDecoratorFactory(new SessionWebSocketHandlerDecoratorFactory())
                // 指定发送数据最长响应时间 10s
                .setSendTimeLimit(15 * 1000)
                // 指定发送数据流的最大值 (默认值: 512K)
                // 实际上WebSocket服务器会施加限制,Tomcat上为8K，Jetty上为64K
                // STOMP客户端会在16K边界处分割较大的STOMP消息，并将它们作为多个WebSocket消息发送，因此需要服务器进行缓冲和重新组装。
                .setSendBufferSizeLimit(512 * 1024);
    }


    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {

        //设置消息输入通道的线程池线程数
        registration.taskExecutor().corePoolSize(4)
                //最大线程数
                .maxPoolSize(8)
                //线程最大空闲时间
                .keepAliveSeconds(60);

    }


}
