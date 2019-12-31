package cn.keking.anti_reptile.util;

import javax.servlet.http.HttpServletResponse;

/**
 * @author chenjh
 * @since 2019/7/17 17:22
 */
public class CrosUtil {

    private static final String ALLOWED_HEADERS = "x-requested-with, authorization, Content-Type, Authorization, credential, X-XSRF-TOKEN,token,username,client";

    private static final String ALLOWED_METHODS = "*";

    private static final String ALLOWED_ORIGIN = "*";

    private static final String ALLOWED_EXPOSE = "*";

    private static final String MAX_AGE = "18000L";

    public static void setCrosHeader(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
        response.setHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
        response.setHeader("Access-Control-Max-Age", MAX_AGE);
        response.setHeader("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        response.setHeader("Access-Control-Expose-Headers", ALLOWED_EXPOSE);
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }

}
