

import org.elasticsearch.client.Client;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import javax.annotation.PostConstruct;


@Configuration
@AutoConfigureBefore(RedisConfig.class)
@EnableElasticsearchRepositories("com.mage.repository")
public class ElasticsearchConfig {

    @PostConstruct
    void init() {
        // 设置环境变量，解决Es的netty与redis服务本身不兼容问题 ( 原因是es和redis抢占设置cpu核心 )
        System.setProperty("es.set.netty.runtime.available.processors", "false");
    }

    @Bean(name = "elasticsearchTemplate")
    public ElasticsearchTemplate elasticsearchTemplate(Client client,
                                                       ElasticsearchConverter converter) {
        try {
            return new ElasticsearchTemplate(client, converter);
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Bean
    public ElasticsearchConverter elasticsearchConverter(
            SimpleElasticsearchMappingContext mappingContext) {
        return new MappingElasticsearchConverter(mappingContext);
    }

    @Bean
    public SimpleElasticsearchMappingContext mappingContext() {
        return new SimpleElasticsearchMappingContext();
    }

}
