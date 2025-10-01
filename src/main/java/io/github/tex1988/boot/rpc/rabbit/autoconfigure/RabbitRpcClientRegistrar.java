package io.github.tex1988.boot.rpc.rabbit.autoconfigure;

import io.github.tex1988.boot.rpc.rabbit.annotation.EnableRabbitRpc;
import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpcInterface;
import io.github.tex1988.boot.rpc.rabbit.rabbit.RabbitRpcClientProxyFactory;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(EnableRabbitRpc.class)
public class RabbitRpcClientRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String CLIENT_BEAN_NAME_SUFFIX = "Client";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(EnableRabbitRpc.class.getName());
        if (attributes != null && (Boolean) attributes.get("enableClient")) {
            ClassPathScanningCandidateComponentProvider scanner = getScanner();
            Arrays.stream(((String[]) attributes.get("scanBasePackages"))).forEach(p -> {
                scanner.addIncludeFilter(new AnnotationTypeFilter(RabbitRpcInterface.class, true, true));
                Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(p);
                createClients(candidateComponents, registry);
            });
        }
    }

    @SneakyThrows
    private void createClients(Set<BeanDefinition> candidateComponents, BeanDefinitionRegistry registry) {
        for (BeanDefinition beanDefinition : candidateComponents) {
            Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
            GenericBeanDefinition proxyBeanDefinition = new GenericBeanDefinition();
            proxyBeanDefinition.setBeanClass(RabbitRpcClientProxyFactory.class);
            proxyBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(clazz);
            proxyBeanDefinition.setAutowireMode(3);
            String className = clazz.getSimpleName();
            String beanName = Character.toLowerCase(className.charAt(0)) + className.substring(1) + CLIENT_BEAN_NAME_SUFFIX;
            registry.registerBeanDefinition(beanName, proxyBeanDefinition);
        }
    }

    private ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(@NonNull AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isAbstract();
            }
        };
    }
}
