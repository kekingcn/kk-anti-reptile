package cn.keking.anti_reptile.servlet;

import cn.keking.anti_reptile.ValidateFormService;
import cn.keking.anti_reptile.util.CrosUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author chenjh
 * @since 2020/2/10 09:20
 */
public class ValidateFormServlet extends HttpServlet {

    private ValidateFormService validateFormService;

    private AtomicBoolean initialized = new AtomicBoolean(false);

    private synchronized void init(ServletContext servletContext) {
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        assert ctx != null;
        this.validateFormService = ctx.getBean(ValidateFormService.class);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!initialized.get()) {
            init(request.getServletContext());
            initialized.set(true);
        }
        String result = validateFormService.validate(request);
        CrosUtil.setCrosHeader(response);
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(200);
        response.getWriter().write(result);
        response.getWriter().close();
        return;
    }
}
