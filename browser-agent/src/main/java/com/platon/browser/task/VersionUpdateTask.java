package com.platon.browser.task;

import com.platon.browser.common.utils.AppStatusUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 节点程序版本号更新任务
 */
@Component
@Slf4j
public class VersionUpdateTask {
	
    @Scheduled(cron = "0/30  * * * * ?")
    private void cron(){
    	// 只有程序正常运行才执行任务
		if(AppStatusUtil.isRunning()) start();

    }

    protected void start() {


    } 
   
}