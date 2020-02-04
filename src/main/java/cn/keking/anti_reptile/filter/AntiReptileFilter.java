package cn.keking.anti_reptile.filter;

import cn.keking.anti_reptile.ValidateFormService;
import cn.keking.anti_reptile.config.AntiReptileProperties;
import cn.keking.anti_reptile.constant.AntiReptileConsts;
import cn.keking.anti_reptile.module.VerifyImageDTO;
import cn.keking.anti_reptile.rule.RuleActuator;
import cn.keking.anti_reptile.util.CrosUtil;
import cn.keking.anti_reptile.util.VerifyImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author kl @kailing.pub
 * @since 2019/7/8
 */
public class AntiReptileFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AntiReptileFilter.class);

    private String antiReptileForm;

    private RuleActuator actuator;

    private List<String> includeUrls;

    private boolean globalFilterMode;

    private VerifyImageUtil verifyImageUtil;

    private ValidateFormService validateFormService;

    @Override
    public void init(FilterConfig filterConfig) {
        ClassPathResource classPathResource = new ClassPathResource("verify/index.html");
        try {
            classPathResource.getInputStream();
            byte[] bytes = FileCopyUtils.copyToByteArray(classPathResource.getInputStream());
            this.antiReptileForm = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("反爬虫验证模板加载失败！");
        }
        ServletContext context = filterConfig.getServletContext();
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
        assert ctx != null;
        this.actuator = ctx.getBean(RuleActuator.class);
        this.verifyImageUtil = ctx.getBean(VerifyImageUtil.class);
        this.validateFormService = ctx.getBean(ValidateFormService.class);
        this.includeUrls = ctx.getBean(AntiReptileProperties.class).getIncludeUrls();
        this.globalFilterMode = ctx.getBean(AntiReptileProperties.class).isGlobalFilterMode();
        if (this.includeUrls == null) {
            this.includeUrls = new ArrayList<>();
            if (!this.globalFilterMode) {
                LOGGER.warn("AntiReptileFilter提示：当前拦截模式为非全局拦截模式，并且未添加需要拦截的接口;可以通过在配置文件中配置anti.reptile.manager.globalFilterMode=true开启全局模式，或者通过配置anti.reptile.manager.include-urls添加需要拦截的接口，配置详情参考：https://github.com/kekingcn/kk-anti-reptile/blob/master/README.md");
            }
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String requestUrl = request.getRequestURI();
        if (requestUrl.startsWith(AntiReptileConsts.KK_ANTI_REPTILE_PREFIX)) {
            String result = "{}";
            if (requestUrl.startsWith(AntiReptileConsts.VALIDATE_REQUEST_URI)) {
                result = validateFormService.validate(request);
            } else if (requestUrl.startsWith(AntiReptileConsts.REFRESH_REQUEST_URI)) {
                result = validateFormService.refresh(request);
            }
            CrosUtil.setCrosHeader(response);
            response.setContentType("application/json;charset=utf-8");
            response.setStatus(200);
            response.getWriter().write(result);
            response.getWriter().close();
            return;
        } else if (isFilter(requestUrl) && !actuator.isAllowed(request, response)) {
            CrosUtil.setCrosHeader(response);
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(509);
            VerifyImageDTO verifyImage = verifyImageUtil.generateVerifyImg();
            verifyImageUtil.saveVerifyCodeToRedis(verifyImage);
            String str1 = this.antiReptileForm.replace("verifyId_value", verifyImage.getVerifyId());
            String str2 = str1.replaceAll("verifyImg_value", verifyImage.getVerifyImgStr());
            String str3 = str2.replaceAll("realRequestUri_value", requestUrl);
            response.getWriter().write(str3);
            response.getWriter().close();
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 是否拦截
     * @param requestUrl 请求uri
     * @return 是否拦截
     */
    public boolean isFilter(String requestUrl) {
        if (this.globalFilterMode || includeUrls.contains(requestUrl)) {
            return true;
        } else {
            for (String includeUrl : includeUrls) {
                if (Pattern.matches(includeUrl, requestUrl)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void destroy() {

    }


}
