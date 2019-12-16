package com.platon.browser.controller;

import com.platon.browser.common.BrowserConst;
import com.platon.browser.enums.I18nEnum;
import com.platon.browser.enums.RetEnum;
import com.platon.browser.now.service.AddressService;
import com.platon.browser.req.address.QueryDetailRequest;
import com.platon.browser.req.address.QueryRPPlanDetailRequest;
import com.platon.browser.res.BaseResp;
import com.platon.browser.res.address.QueryDetailResp;
import com.platon.browser.res.address.QueryRPPlanDetailResp;
import com.platon.browser.util.I18nUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import javax.validation.Valid;

/**
 *          地址具体实现Controller 提供地址详情页面使用
 *  @file AppDocAddressController.java
 *  @description 
 *	@author zhangrj
 *  @data 2019年8月31日
 */
@RestController
public class AppDocAddressController implements AppDocAddress {

	@Autowired
	private AddressService addressService;
	
    @Autowired
    private I18nUtil i18n;

	@Override
	public WebAsyncTask<BaseResp<QueryDetailResp>> details(@Valid QueryDetailRequest req) {
		// 5s钟没返回，则认为超时  
        WebAsyncTask<BaseResp<QueryDetailResp>> webAsyncTask = new WebAsyncTask<>(BrowserConst.WEB_TIME_OUT, () -> {
            QueryDetailResp queryDetailResp = addressService.getDetails(req);
            return BaseResp.build(RetEnum.RET_SUCCESS.getCode(), i18n.i(I18nEnum.SUCCESS), queryDetailResp);
        });
        webAsyncTask.onTimeout(() -> {
            // 超时的时候，直接抛异常，让外层统一处理超时异常
            throw new TimeoutException("System busy!");
        });
        return webAsyncTask;  
	}

	@Override
	public WebAsyncTask<BaseResp<QueryRPPlanDetailResp>> rpplanDetail(@Valid QueryRPPlanDetailRequest req) {
		// 5s钟没返回，则认为超时  
        WebAsyncTask<BaseResp<QueryRPPlanDetailResp>> webAsyncTask = new WebAsyncTask<>(BrowserConst.WEB_TIME_OUT, new Callable<BaseResp<QueryRPPlanDetailResp>>() {  
            @Override  
            public BaseResp<QueryRPPlanDetailResp> call() throws Exception {  
            	QueryRPPlanDetailResp dueryRPPlanDetailResp = addressService.rpplanDetail(req);
        		return BaseResp.build(RetEnum.RET_SUCCESS.getCode(), i18n.i(I18nEnum.SUCCESS), dueryRPPlanDetailResp);
            }  
        });  
        webAsyncTask.onTimeout(() -> {
            // 超时的时候，直接抛异常，让外层统一处理超时异常
            throw new TimeoutException("System busy!");
        });
        return webAsyncTask;
	}

}
