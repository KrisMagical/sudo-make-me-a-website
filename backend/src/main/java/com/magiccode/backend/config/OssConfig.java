package com.magiccode.backend.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.Protocol;
import com.aliyun.oss.common.comm.SignVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OssConfig {
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.region:cn-guangzhou}")
    private String region;

    @Bean
    public OSS ossClient() {
        log.info("Initialize the OSS client, endpoint: {}, bucket: {}", endpoint, bucketName);
        DefaultCredentialProvider provider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);
        com.aliyun.oss.ClientBuilderConfiguration config = new com.aliyun.oss.ClientBuilderConfiguration();
        config.setProtocol(Protocol.HTTPS);
        config.setSupportCname(false);
        config.setSignatureVersion(SignVersion.V4);

        return OSSClientBuilder.create()
                .credentialsProvider(provider)
                .clientConfiguration(config)
                .region(region)
                .endpoint(endpoint)
                .build();
    }
}
