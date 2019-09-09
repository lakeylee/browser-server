package com.platon.browser.config.bean;

import lombok.Data;

import java.util.List;

/**
 * @Auther: Chendongming
 * @Date: 2019/9/7 21:29
 * @Description:
 */
@Data
public class EconomicConfigParam {
    private String jsonrpc;
    private String method;
    private List<String> params;
    private int id;

    public EconomicConfigParam(String jsonrpc, String method, List<String> params, int id) {
        this.jsonrpc = jsonrpc;
        this.method = method;
        this.params = params;
        this.id = id;
    }
}
