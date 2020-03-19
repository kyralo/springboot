
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mage.service.MessageReceiveService;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.ObjectUtils;


import java.time.Duration;


@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.lettuce.pool.max-idle}")
    private int maxIdle;
    @Value("${spring.data.redis.lettuce.pool.min-idle}")
    private int minIdle;
    @Value("${spring.data.redis.lettuce.pool.max-active}")
    private int maxActive;
    @Value("${spring.data.redis.lettuce.pool.max-wait}")
    private Long maxWait;
    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int port;
    @Value("${spring.data.redis.password}")
    private String password;
    @Value("${spring.data.redis.timeout}")
    private Long timeout;

    /** 链接池通用配置 */
    public GenericObjectPoolConfig genericObjectPoolConfig(){
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(maxActive);
        config.setMinIdle(minIdle);
        config.setMaxIdle(maxIdle);
        config.setMaxWaitMillis(maxWait);
        return config;
    }

    public RedisStandaloneConfiguration redisStandaloneConfiguration(int database){
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(database);
        if (!ObjectUtils.isEmpty(password)) {
            RedisPassword redisPassword = RedisPassword.of(password);
            configuration.setPassword(redisPassword);
        }

        return configuration;
    }

    @Primary
    @Bean(name = "defaultFactory")
    public RedisConnectionFactory defaultConnectionFactory(){

        RedisStandaloneConfiguration configuration = redisStandaloneConfiguration(0);
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder();
        builder.poolConfig(genericObjectPoolConfig());
        builder.commandTimeout(Duration.ofSeconds(timeout));

        return new LettuceConnectionFactory(configuration, builder.build());
    }

    /** 消息使用 */
    @Bean(name = "messageRedisFactory")
    public RedisConnectionFactory messageConnectionFactory(){

        RedisStandaloneConfiguration configuration = redisStandaloneConfiguration(1);
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder();
        builder.poolConfig(genericObjectPoolConfig());
        builder.commandTimeout(Duration.ofSeconds(timeout));

        return new LettuceConnectionFactory(configuration, builder.build());
    }

    /** 缓存使用 */
    @Bean(name = "cacheRedisFactory")
    public RedisConnectionFactory cacheConnectionFactory(){

        RedisStandaloneConfiguration configuration = redisStandaloneConfiguration(2);
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder();
        builder.poolConfig(genericObjectPoolConfig());
        builder.commandTimeout(Duration.ofSeconds(timeout));

        return new LettuceConnectionFactory(configuration, builder.build());
    }


    /**
     * //如使用注解的话需要配置cacheManager
     * @param connectionFactory connectionFactory
     * @return cacheManager
     */
    @Bean
    CacheManager cacheManager(@Qualifier("cacheRedisFactory") RedisConnectionFactory connectionFactory) {
        //初始化一个RedisCacheWriter
        RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory);
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig();
        //设置默认超过期时间是1个小时
        defaultCacheConfig.entryTtl(Duration.ofHours(1));
        //初始化RedisCacheManager
        return new RedisCacheManager(redisCacheWriter, defaultCacheConfig);
    }

    /**
     * 默认
     */
    @Bean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(@Qualifier("defaultFactory") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        //使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(mapper);
        template.setValueSerializer(serializer);
        //使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 缓存
     */
    @Bean(name = "cacheRedisTemplate")
    public RedisTemplate<String, Object> cacheRedisTemplate(@Qualifier("cacheRedisFactory") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        //使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(mapper);
        template.setValueSerializer(serializer);
        //使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 消息
     */
    @Bean(name = "messageRedisTemplate")
    public RedisTemplate<String, Object> messageRedisTemplate(@Qualifier("messageRedisFactory") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        //使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(mapper);
        template.setValueSerializer(serializer);
        //使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    // redis消息配置

    @Bean
    RedisMessageListenerContainer container(@Qualifier("messageRedisFactory") RedisConnectionFactory connectionFactory,
                                            MessageListenerAdapter voteListenerAdapter,
                                            MessageListenerAdapter appraiseListenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        //订阅通道 container 可以添加多个 messageListener

        //voteListenerAdapter订阅一个叫vote(投票)的通道
        container.addMessageListener(voteListenerAdapter, new PatternTopic("vote"));
        //appraiseListenerAdapter订阅一个叫appraise(评价)的通道
        container.addMessageListener(appraiseListenerAdapter, new PatternTopic("appraise"));

        return container;
    }

    @Bean
    @Qualifier("voteListenerAdapter")
    MessageListenerAdapter voteListenerAdapter(MessageReceiveService receiver) {

        /*
            这个地方是给messageListenerAdapter传入一个消息接受的处理器，利用反射的方法调用“receiveMessage”

            receiver: 处理器
            receiveMessage: 执行处理器中定义得方法名
        */
        return new MessageListenerAdapter(receiver, "receiveVoteMessage");
    }

    @Bean
    @Qualifier("appraiseListenerAdapter")
    MessageListenerAdapter appraiseListenerAdapter(MessageReceiveService receiver) {

        /*
            这个地方是给messageListenerAdapter传入一个消息接受的处理器，利用反射的方法调用“receiveMessage”

            receiver: 处理器
            receiveMessage: 执行处理器中定义得方法名
        */
        return new MessageListenerAdapter(receiver, "receiveAppraiseMessage");
    }

}


