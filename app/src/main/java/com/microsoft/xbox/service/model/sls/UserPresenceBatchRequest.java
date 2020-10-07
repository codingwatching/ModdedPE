package com.microsoft.xbox.service.model.sls;

import com.microsoft.xbox.toolkit.GsonUtil;

import java.util.ArrayList;

/**
 * 07.10.2020
 *
 * @author Тимашков Иван
 * @author https://github.com/TimScriptov
 */

public class UserPresenceBatchRequest {
    public String level = "all";
    public ArrayList<String> users;

    public UserPresenceBatchRequest(ArrayList<String> userIds) {
        users = userIds;
    }

    public static String getUserPresenceBatchRequestBody(UserPresenceBatchRequest userPresenceBatchRequest) {
        return GsonUtil.toJsonString(userPresenceBatchRequest);
    }
}
