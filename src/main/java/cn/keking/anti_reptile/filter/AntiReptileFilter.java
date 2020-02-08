package cn.keking.anti_reptile.filter;

import cn.keking.anti_reptile.ValidateFormService;
import cn.keking.anti_reptile.constant.AntiReptileConsts;
import cn.keking.anti_reptile.util.CrosUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author kl @kailing.pub
 * @since 2019/7/8
 */
public class AntiReptileFilter implements Filter {

    private ValidateFormService validateFormService;

    @Override
    public void init(FilterConfig filterConfig) {
        ServletContext context = filterConfig.getServletContext();
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
        assert ctx != null;
        this.validateFormService = ctx.getBean(ValidateFormService.class);
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
        }
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }


}
