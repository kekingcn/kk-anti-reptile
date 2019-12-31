package cn.keking.anti_reptile.config;

import cn.keking.anti_reptile.ValidateFormService;
import cn.keking.anti_reptile.filter.AntiReptileFilter;
import cn.keking.anti_reptile.rule.AntiReptileRule;
import cn.keking.anti_reptile.rule.IpRule;
import cn.keking.anti_reptile.rule.RuleActuator;
import cn.keking.anti_reptile.rule.UaRule;
import cn.keking.anti_reptile.util.VerifyImageUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RedissonAutoConfiguration 的 AutoConfigureOrder 为默认值(0)，此处在它后面加载
 * @author kl @kailing.pub
 * @since 2019/7/8
 */
@Configuration
@EnableConfigurationProperties(AntiReptileProperties.class)
@ConditionalOnProperty(prefix = "anti.reptile.manager", value = "enabled", havingValue = "true", matchIfMissing = false)
@Import(RedissonAutoConfig.class)
public class AntiReptileAutoConfig {

    @Bean
    public FilterRegistrationBean antiReptileFilter(){
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new AntiReptileFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    @ConditionalOnProperty(prefix = "anti.reptile.manager.ip-rule",value = "enabled", havingValue = "true", matchIfMissing = true)
    public IpRule ipRule(){
        return new IpRule();
    }

    @Bean
    @ConditionalOnProperty(prefix = "anti.reptile.manager.ua-rule",value = "enabled", havingValue = "true", matchIfMissing = true)
    public UaRule uaRule() {
        return new UaRule();
    }

    @Bean
    public VerifyImageUtil verifyImageUtil() {
        return new VerifyImageUtil();
    }

    @Bean
    public RuleActuator ruleActuator(final List<AntiReptileRule> rules){
        final List<AntiReptileRule> antiReptileRules = rules.stream()
                .sorted(Comparator.comparingInt(AntiReptileRule::getOrder)).collect(Collectors.toList());
        return new RuleActuator(antiReptileRules);
    }

    @Bean
    public ValidateFormService validateFormService(){
        return new ValidateFormService();
    }

}
