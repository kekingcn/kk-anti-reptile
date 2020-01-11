package cn.keking.anti_reptile.filter;

import cn.keking.anti_reptile.ValidateFormService;
import cn.keking.anti_reptile.config.AntiReptileProperties;
import cn.keking.anti_reptile.constant.AntiReptileConsts;
import cn.keking.anti_reptile.module.VerifyImageDTO;
import cn.keking.anti_reptile.rule.RuleActuator;
import cn.keking.anti_reptile.util.CrosUtil;
import cn.keking.anti_reptile.util.VerifyImageUtil;
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

/**
 * @author kl @kailing.pub
 * @since 2019/7/8
 */
public class AntiReptileFilter implements Filter {

    private String antiReptileForm;

    private RuleActuator actuator;

    private List<String> includeUrls = new ArrayList<>();

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
        this.globalFilterMode =  ctx.getBean(AntiReptileProperties.class).isGlobalFilterMode();
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
     * @param requestUrl
     * @return
     */
    public boolean isFilter(String requestUrl){
        if(this.globalFilterMode){
            return true;
        }else {
            return includeUrls.contains(requestUrl);
        }
    }

    @Override
    public void destroy() {

    }


}
