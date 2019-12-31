package cn.keking.anti_reptile.rule;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author kl @kailing.pub
 * @since 2019/7/8
 */
public abstract class AbstractRule implements AntiReptileRule {


    @Override
    public boolean execute(HttpServletRequest request, HttpServletResponse response) {
        return doExecute(request,response);
    }

    protected abstract boolean doExecute(HttpServletRequest request, HttpServletResponse response);
}
