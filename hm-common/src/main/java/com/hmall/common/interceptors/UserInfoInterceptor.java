package com.hmall.common.interceptors;

import cn.hutool.core.util.StrUtil;
import com.hmall.common.utils.UserContext;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author 陈荣
 * @ClassName: UserInfoInterceptor
 * @Description: 登录用户拦截器
 * @date 2026-04-11  10:18
 * @Version: 1
 */
public class UserInfoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取用户信息
        String userInfo = request.getHeader("user-info");
        if (StrUtil.isBlank(userInfo)){
            return true;
        }
        Long userId = Long.valueOf(userInfo);
        // 2.存入ThreadLocal
        UserContext.setUser(userId);
        // 3.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserContext.removeUser();
    }
}
