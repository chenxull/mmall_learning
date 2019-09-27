package com.mmall.controller.portal;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/user/") //所有 /user/开头的路由都导入到这里
public class UserController {

    @Autowired //这里属性的名字和 Service 中的注解是相同的，可以注入进来
    private IUserService iUserService;
    /**
     * 用户登录
     * @param username
     * @param password
     * @param session
     * @return
     */
    @RequestMapping(value = "login.do",method = RequestMethod.POST) // 只能是 post 请求
    @ResponseBody
    public ServerResponse<User> login(String username, String password, HttpSession session){
        // service -> mybaits --> dao
        ServerResponse<User> response = iUserService.login(username,password);
        if (response.isSuccess()){
        // 设置 session
            session.setAttribute(Const.CURRENT_USER,response.getData());
        }
        return response;
    }

    /**
     *  用户登出
     * @param session
     * @return
     */
    @RequestMapping(value = "logout.do",method = RequestMethod.GET) // 只能是 get 请求
    @ResponseBody
    public ServerResponse<String> logout(HttpSession session){
//        删除 session 即可
        session.removeAttribute(Const.CURRENT_USER);
        return ServerResponse.createBySuccess();
    }

    /**
     * 用户注册
     * @param user
     * @return
     */
    @RequestMapping(value = "register.do",method = RequestMethod.GET) // 只能是 get 请求
    @ResponseBody
    public ServerResponse<String> register(User user){
        return iUserService.register(user);
    }

    /**
     * 接口效验
     * @param str
     * @param type
     * @return
     */
    @RequestMapping(value = "check_valid.do",method = RequestMethod.GET) // 只能是 get 请求
    @ResponseBody
    public ServerResponse<String> checkValid(String str,String type){
        return iUserService.checkValid(str,type);
    }

    /**
     * 获取用户信息
     * @param session
     * @return
     */
    @RequestMapping(value = "get_user_info.do",method = RequestMethod.GET) // 只能是 get 请求
    @ResponseBody
    public ServerResponse<User> getUserInfo(HttpSession session){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user != null){
            return ServerResponse.createBySuccess(user);
        }

        return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
    }

    /**
     * 获取密码问题
     * @param username
     * @return
     */
    @RequestMapping(value = "forget_get_question.do",method = RequestMethod.GET) // 只能是 get 请求
    @ResponseBody
    public ServerResponse<String> forgetGetQuestion(String username){
        return iUserService.selectQuetion(username);
    }

    /**
     * 检查密码问题的答案
     * @param username
     * @param quetion
     * @param answer
     * @return
     */
    @RequestMapping(value = "forget_check_answer.do",method = RequestMethod.GET) // 只能是 get 请求
    @ResponseBody
    public ServerResponse<String> forgetCheckAnswer(String username,String quetion,String answer){
            return iUserService.checkAnswer(username,quetion,answer);
    }


    /**
     *  忘记密码时，重置密码
     *  用户获取到的 token 与缓存中的 token 做对比
     * @param username
     * @param passwordNew
     * @param forgetToken
     * @return
     */
    @RequestMapping(value = "forget_reset_password.do",method = RequestMethod.GET) // 只能是 get 请求
    @ResponseBody
    public ServerResponse<String> forgetRestPassword(String username,String passwordNew,String forgetToken){
        return iUserService.forgetRestPassword(username,passwordNew,forgetToken);
    }
}
