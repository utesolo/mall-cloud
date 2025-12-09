package xyh.dp.mall.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI配置
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * 配置OpenAPI文档信息
     * 
     * @return OpenAPI配置
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("农业供销平台API文档")
                        .version("1.0.0")
                        .description("农业供销微信小程序后端API接口文档")
                        .contact(new Contact()
                                .name("Mall Cloud Team")
                                .email("contact@mall-cloud.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
