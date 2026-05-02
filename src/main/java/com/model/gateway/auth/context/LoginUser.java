package com.model.gateway.auth.context;

import com.model.gateway.auth.domain.SysUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户上下文载体，仅保留鉴权下游需要的字段，刻意不携带密码。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginUser {

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 用户昵称。
     */
    private String nickname;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 用户角色。
     */
    private String role;

    /**
     * 用户状态。
     */
    private Integer status;

    /**
     * 由系统用户实体构造登录上下文用户，丢弃密码字段。
     *
     * @param user 系统用户实体
     * @return 登录上下文用户
     */
    public static LoginUser from(SysUser user) {
        if (user == null) {
            return null;
        }
        return LoginUser.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
