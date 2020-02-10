package cn.keking.anti_reptile;

import cn.keking.anti_reptile.module.VerifyImageDTO;
import cn.keking.anti_reptile.module.VerifyImageVO;
import cn.keking.anti_reptile.rule.RuleActuator;
import cn.keking.anti_reptile.util.VerifyImageUtil;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author kl @kailing.pub
 * @since 2019/7/9
 */

public class ValidateFormService {

    @Autowired
    private RuleActuator actuator;

    @Autowired
    private VerifyImageUtil verifyImageUtil;

    public String validate(HttpServletRequest request) throws UnsupportedEncodingException {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setHeaderEncoding("UTF-8");
        List items = null;
        try {
            items = upload.parseRequest(request);
        } catch (FileUploadException e) {
            e.printStackTrace();
        }
        Map<String, String> params = new HashMap<String, String>();
        for(Object object : items){
            FileItem fileItem = (FileItem) object;
            if (fileItem.isFormField()) {
                params.put(fileItem.getFieldName(), fileItem.getString("UTF-8"));
            }
        }
        String verifyId = params.get("verifyId");
        String result =  params.get("result");
        String realRequestUri = params.get("realRequestUri");
        String actualResult = verifyImageUtil.getVerifyCodeFromRedis(verifyId);
        if (actualResult != null && request != null && actualResult.equals(result.toLowerCase())) {
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
