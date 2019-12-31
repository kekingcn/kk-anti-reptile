package cn.keking.anti_reptile;

import cn.keking.anti_reptile.module.VerifyImageDTO;
import cn.keking.anti_reptile.module.VerifyImageVO;
import cn.keking.anti_reptile.rule.RuleActuator;
import cn.keking.anti_reptile.util.VerifyImageUtil;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

/**
 * @author kl @kailing.pub
 * @since 2019/7/9
 */

public class ValidateFormService {

    @Autowired
    private RuleActuator actuator;

    @Autowired
    private VerifyImageUtil verifyImageUtil;

    public String validate(HttpServletRequest request) {
        String verifyId = request.getParameter("verifyId");
        String result = request.getParameter("result");
        String realRequestUri = request.getParameter("realRequestUri");
        String actruaResult = verifyImageUtil.getVerifyCodeFromRedis(verifyId);
        if (actruaResult != null && request != null && actruaResult.equals(result.toLowerCase())) {
            actuator.reset(request, realRequestUri);
            return "{\"result\":true}";
        }
        return "{\"result\":false}";
    }

    public String refresh(HttpServletRequest request) {
        String verifyId = request.getParameter("verifyId");
        verifyImageUtil.deleteVerifyCodeFromRedis(verifyId);
        VerifyImageDTO verifyImage = verifyImageUtil.generateVerifyImg();
        verifyImageUtil.saveVerifyCodeToRedis(verifyImage);
        VerifyImageVO verifyImageVO = new VerifyImageVO();
        BeanUtils.copyProperties(verifyImage, verifyImageVO);
        String result = "{\"verifyId\": \"" + verifyImageVO.getVerifyId() + "\",\"verifyImgStr\": \"" + verifyImageVO.getVerifyImgStr() + "\"}";
        return result;
    }
}
