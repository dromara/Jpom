package io.jpom.model.dto;

import io.jpom.model.data.UserModel;

/**
 * @author bwcx_jzy
 * @date 2020/11/2
 */
public class UserLoginDto {

    private String token;

    private String longTermToken;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getLongTermToken() {
        return longTermToken;
    }

    public void setLongTermToken(String longTermToken) {
        this.longTermToken = longTermToken;
    }

    public UserLoginDto() {

    }

    public UserLoginDto(UserModel userModel, String token) {
        this.setLongTermToken(userModel.getUserMd5Key());
        this.setToken(token);
    }
}
