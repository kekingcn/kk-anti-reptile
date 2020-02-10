package cn.keking.anti_reptile.rule;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author kl @kailing.pub
 * @since 2019/7/8
 */
public class RuleActuator {

    private  List<AntiReptileRule> ruleList;

    public RuleActuator(List<AntiReptileRule> rules) {
        ruleList = rules;
    }

    /**
     * 是否允许通过请求
     * @param request 请求
     * @param response 响应
     * @return 请求是否允许通过
     */
    public boolean isAllowed(HttpServletRequest request , HttpServletResponse response){
        for (AntiReptileRule rule: ruleList){
            if (rule.execute(request,response)){
                return false;
            }
        }
        return true;
    }

    public void reset(HttpServletRequest request, String realRequestUri){
       ruleList.forEach(rule -> rule.reset(request, realRequestUri));
    }
}
