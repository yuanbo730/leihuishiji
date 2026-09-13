package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Random;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1、校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
            //2.如果手机格式不合适
            return Result.fail("手机号格式错误！");
        }
        //3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
        //4.保存验证码到session
        session.setAttribute("code",code);
        //5.发送短信验证码
        log.debug("短信验证码发送成功,验证码：{}",code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1、校验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            //2.如果手机格式不合适
            return Result.fail("手机号格式错误！");
        }
        //3.符合，校验验证码
        String cacheCode = (String) session.getAttribute("code");
        //4.如果验证码不存在
        if(cacheCode == null||!loginForm.getCode().equals(cacheCode)){
            return Result.fail("验证码错误！");
        }
        //5.查询用户是否存在
        User user = lambdaQuery().eq(User::getPhone, phone).one();
        if(user == null){
            createUserWithPhone(phone);
        }
        session.setAttribute("user",user);
        return Result.ok();

    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}