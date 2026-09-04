package br.com.nog.exporter.config;

import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

@Configuration
@ConditionalOnProperty(name = "app.delivery.mode", havingValue = "SFTP")
public class SftpConfig {

    @Bean
    public SessionFactory<SftpClient.DirEntry> sftpSessionFactory(SftpProperties properties) {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost(properties.getHost());
        factory.setPort(properties.getPort());
        factory.setUser(properties.getUsername());
        factory.setPassword(properties.getPassword());
        factory.setAllowUnknownKeys(properties.isAllowUnknownKeys());
        return new CachingSessionFactory<>(factory);
    }

    @Bean
    public SftpRemoteFileTemplate sftpRemoteFileTemplate(
            SessionFactory<SftpClient.DirEntry> sessionFactory,
            SftpProperties properties) {

        SftpRemoteFileTemplate template = new SftpRemoteFileTemplate(sessionFactory);
        template.setRemoteDirectoryExpression(new LiteralExpression(properties.getRemoteDirectory()));
        template.setAutoCreateDirectory(true);
        template.setUseTemporaryFileName(true);
        return template;
    }
}
