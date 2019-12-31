package cn.keking.anti_reptile.util;

import cn.keking.anti_reptile.module.VerifyImageDTO;
import com.wf.captcha.utils.CaptchaUtil;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.Color;

import java.io.ByteArrayOutputStream;

import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author chenjh
 * @since 2019/7/16 11:05
 */
public class VerifyImageUtil {

    private static final String VERIFY_CODE_KEY = "kk-antireptile_verifycdoe_";

    @Autowired
    private RedissonClient redissonClient;

    public VerifyImageDTO generateVerifyImg() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String result = CaptchaUtil.out(outputStream);
        String base64Image = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        String verifyId = UUID.randomUUID().toString();
        return new VerifyImageDTO(verifyId, null, base64Image, result);
    }

    public void saveVerifyCodeToRedis(VerifyImageDTO verifyImage) {
        RBucket<String> rBucket = redissonClient.getBucket(VERIFY_CODE_KEY + verifyImage.getVerifyId());
        rBucket.set(verifyImage.getResult(), 60, TimeUnit.SECONDS);
    }

    public void deleteVerifyCodeFromRedis(String verifyId) {
        RBucket<String> rBucket = redissonClient.getBucket(VERIFY_CODE_KEY + verifyId);
        rBucket.delete();
    }

    public String getVerifyCodeFromRedis(String verifyId) {
        String result = null;
        RBucket<String> rBucket = redissonClient.getBucket(VERIFY_CODE_KEY + verifyId);
        result = rBucket.get();
        return result;
    }
}
