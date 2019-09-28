package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

// 要将 service 注入到 controller 中，供其使用
@Service("iUserService")
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public ServerResponse<User> login(String username, String password) {
        // 检查用户是否存在
        int resultCount = userMapper.checkUsername(username);
        if ( resultCount == 0) {
            return ServerResponse.createByErrorMessage("用户名不存在");
        }

       //TODO 密码登录 MD5
        String md5Password = MD5Util.MD5EncodeUtf8(password);
        User user = userMapper.selectLogin(username,md5Password);
        if(user == null){
            return ServerResponse.createByErrorMessage("密码错误");
        }

        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登录成功",user);

    }

    // 注册
    public ServerResponse<String> register(User user){
//        校验数据 username,email


        ServerResponse validResPonse = this.checkValid(user.getUsername(),Const.USERNAME);
        if ( !validResPonse.isSuccess()){
            return  validResPonse;
        }

        validResPonse = this.checkValid(user.getEmail(),Const.EMAIL);
        if ( !validResPonse.isSuccess()){
            return  validResPonse;
        }


         user.setRole(Const.Role.ROLE_CUSTOMER);

        //  MD5 加密
         user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));

         int resultCount = userMapper.insert(user);
         if (resultCount == 0){
             return ServerResponse.createByErrorMessage("注册失败");
         }
         return ServerResponse.createBySuccessMessage("注册成功");
    }


    public ServerResponse<String> checkValid(String str,String type){
        if ( StringUtils.isNotBlank(type)){
            // 开始校验
             if ( Const.USERNAME.equals(type)){
                 int resultCount= userMapper.checkUsername(str);

                 if ( resultCount > 0){
                     return ServerResponse.createByErrorMessage("用户已存在");
                 }
             }

             if (Const.EMAIL.equals(type)){
                int    resultCount = userMapper.checkEmail(str);
                 if ( resultCount > 0){
                     return ServerResponse.createByErrorMessage("邮箱已存在");
                 }
             }

        }else{
            return ServerResponse.createByErrorMessage("参数错误");
        }
        return ServerResponse.createBySuccessMessage("校验成功");
    }

    public ServerResponse selectQuetion(String username){
        ServerResponse validResponse = this.checkValid(username,Const.USERNAME);
        if(validResponse.isSuccess()){
            // 用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");

        }
        String question = userMapper.slectQuestionByUsername(username);
        if(StringUtils.isNotBlank(question)){
            return ServerResponse.createBySuccessMessage(question);
        }
        return ServerResponse.createByErrorMessage("找回密码的问题是空的");
    }

    public ServerResponse<String> checkAnswer(String username,String question,String answer){
        int resultCount = userMapper.checkAnswer(username,question,answer);
        if (resultCount > 0){
            String forgetToken = UUID.randomUUID().toString();
            // 将 token 信息存储在本地缓存中
            TokenCache.setKey(TokenCache.TOKEN_PREFIX+username,forgetToken);
            return ServerResponse.createBySuccessMessage(forgetToken);
        }
        return ServerResponse.createByErrorMessage("问题答案错误");
    }


    public ServerResponse<String>  forgetRestPassword(String username,String passwordNew,String forgetToken){
        if (StringUtils.isBlank(forgetToken)){
            return ServerResponse.createByErrorMessage("参数错误，token 需要传递");
        }

        ServerResponse validResponse = this.checkValid(username,Const.USERNAME);
        if(validResponse.isSuccess()){
            // 用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");

        }

        String token = TokenCache.getkey(TokenCache.TOKEN_PREFIX+username);
        if(StringUtils.isBlank(token)){
            return ServerResponse.createByErrorMessage("token 无效，或过期");
        }

        if (StringUtils.equals(forgetToken,token)){
            String md5Password = MD5Util.MD5EncodeUtf8(passwordNew);
             int rowCount = userMapper.updatePasswordByUserName(username,md5Password);

             if (rowCount >0){
                 return ServerResponse.createBySuccessMessage("修改密码成功");
             }
        }else{
            return ServerResponse.createByErrorMessage("token 错误，请重新获取重置密码的 token");
        }

        return ServerResponse.createByErrorMessage("修改密码失败");
    }


    public ServerResponse<String> resetPassword(String passwordOld,String passwordNew,User user){
//        要效验用户的旧密码
        int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld),user.getId());
        if (resultCount == 0){
            return ServerResponse.createByErrorMessage("旧密码错误");
        }
        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if (updateCount >0){
            return ServerResponse.createBySuccessMessage("密码更新成功");
        }
        return ServerResponse.createByErrorMessage("密码更新失败");
    }

    // 更新个人信息
    public ServerResponse<User> updateInformation(User user){
//        username 不能改
//        email 也要效验，是否已经存在，是否能相同
        int resultCount = userMapper.checktEmailByUserId(user.getEmail(),user.getId());
        if (resultCount > 0){
            return ServerResponse.createByErrorMessage("email 已经存在，请更换");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());

        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if(updateCount > 0){
            return ServerResponse.createBySuccess("更新个人信息成功",updateUser);
        }
        return ServerResponse.createByErrorMessage("更新个人信息失败");
    }

    public ServerResponse<User> get_information(Integer userId){
        User user = userMapper.selectByPrimaryKey(userId);
        if(user == null){
            return ServerResponse.createByErrorMessage("找不到当前用户");
        }
        user.setPassword(org.apache.commons.lang3.StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);
    }

    public ServerResponse checkAdminRole(User user){
        if (user!=null && user.getRole().intValue() == Const.Role.ROLE_ADMIN){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }

}
