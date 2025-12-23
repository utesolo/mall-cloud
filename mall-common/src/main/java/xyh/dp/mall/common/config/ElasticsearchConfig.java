package xyh.dp.mall.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.config.EnableElasticsearchAuditing;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * Elasticsearch 配置类
 * 处理 SSL 证书验证和其他 ES 配置
 *
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableElasticsearchAuditing
public class ElasticsearchConfig {

    /**
     * 静态代码块：初始化时禁用 SSL 证书验证
     * 在开发环境中使用，生产环境应使用正式的 SSL 证书
     */
    static {
        try {
            // 创建信任所有证书的 TrustManager
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        /**
                         * 检查客户端证书
                         *
                         * @param chain 证书链
                         * @param authType 认证类型
                         */
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            // 信任所有客户端证书
                        }

                        /**
                         * 检查服务器证书
                         *
                         * @param chain 证书链
                         * @param authType 认证类型
                         */
                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            // 信任所有服务器证书
                        }

                        /**
                         * 获取接受的发行者
                         *
                         * @return 空数组表示接受所有发行者
                         */
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            // 设置 SSL 上下文
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            // 设置主机名验证器，信任所有主机
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            log.warn("已禁用 SSL 证书验证，仅在开发环境中使用！生产环境应使用正式的 SSL 证书");
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error("初始化 SSL 配置失败", e);
        }
    }
}
