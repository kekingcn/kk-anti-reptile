package cn.keking.anti_reptile.interceptor;

import cn.keking.anti_reptile.annotation.AntiReptile;
import cn.keking.anti_reptile.config.AntiReptileProperties;
import cn.keking.anti_reptile.interceptor.path.impl.AntPathMatcher;
import cn.keking.anti_reptile.module.VerifyImageDTO;
import cn.keking.anti_reptile.rule.RuleActuator;
import cn.keking.anti_reptile.util.CrosUtil;
import cn.keking.anti_reptile.util.VerifyImageUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author chenjh
 * @since 2020/2/4 17:45
 */
public class AntiReptileInterceptor extends HandlerInterceptorAdapter {

    private String antiReptileForm;

    private RuleActuator actuator;

    private List<String> includeUrls;

    private List<String> excludedUrls;

    private boolean globalFilterMode;

    private VerifyImageUtil verifyImageUtil;

    private AtomicBoolean initialized = new AtomicBoolean(false);

    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public void init(ServletContext context) {
        ClassPathResource classPathResource = new ClassPathResource("verify/index.html");
        try {
            classPathResource.getInputStream();
            byte[] bytes = FileCopyUtils.copyToByteArray(classPathResource.getInputStream());
            this.antiReptileForm = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("反爬虫验证模板加载失败！");
            e.printStackTrace();
        }
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
        assert ctx != null;
        this.actuator = ctx.getBean(RuleActuator.class);
        this.verifyImageUtil = ctx.getBean(VerifyImageUtil.class);
        this.includeUrls = ctx.getBean(AntiReptileProperties.class).getIncludeUrls();
        this.excludedUrls = ctx.getBean(AntiReptileProperties.class).getExcludedUrls();
        this.globalFilterMode = ctx.getBean(AntiReptileProperties.class).isGlobalFilterMode();

        if (this.includeUrls == null) {
            this.includeUrls = new ArrayList<>();
            if(this.excludedUrls != null){
                includeUrls.add("/**");
            }
        }

        if (this.excludedUrls == null) {
            this.excludedUrls = new ArrayList<>();
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!initialized.get()) {
            init(request.getServletContext());
            initialized.set(true);
        }
        HandlerMethod handlerMethod;
        try {
            handlerMethod = (HandlerMethod) handler;
        } catch (ClassCastException e) {
            return true;
        }
        Method method = handlerMethod.getMethod();
        AntiReptile antiReptile = AnnotationUtils.findAnnotation(method, AntiReptile.class);
        boolean isAntiReptileAnnotation = antiReptile != null;
        String requestUrl = request.getRequestURI();
        if (isIntercept(requestUrl, isAntiReptileAnnotation)) {
            if (!isNotIntercept(requestUrl) && !actuator.isAllowed(request, response)) {
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

                return false;
            }
        }
        return true;
    }

    /**
     * 是否拦截
     *
     * @param requestUrl              请求uri
     * @param isAntiReptileAnnotation 是否有AntiReptile注解
     * @return 是否拦截
     */
    public boolean isIntercept(String requestUrl, Boolean isAntiReptileAnnotation) {
        if (this.globalFilterMode || isAntiReptileAnnotation || this.includeUrls.contains(requestUrl)) {
            return true;
        } else {
            for (String includeUrl : includeUrls) {
                if (antPathMatcher.match(includeUrl, requestUrl)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 是否不拦截
     *
     * @param requestUrl 请求uri
     * @return 是否不拦截  true不拦截  false拦截
     */
    public boolean isNotIntercept(String requestUrl) {
        //全局拦截
        if (this.globalFilterMode) {
            return false;
        }

        if (this.excludedUrls.contains(requestUrl)) {
            return true;
        } else {
            for (String excludedUrl : excludedUrls) {
                if (antPathMatcher.match(excludedUrl, requestUrl)) {
                    return true;
                }
            }
            return false;
        }
    }
}
