package com.platon.browser.collection.service;

import com.platon.browser.AgentTestBase;
import com.platon.browser.common.service.epoch.EpochRetryService;
import com.platon.browser.common.service.epoch.EpochService;
import com.platon.browser.config.BlockChainConfig;
import com.platon.browser.exception.BlockNumberException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class EpochServiceTest extends AgentTestBase {

    @Mock private BlockChainConfig chainConfig;
    @Mock EpochRetryService epochRetryService;
    @Spy private EpochService target;

    @Before
    public void setup() {
        ReflectionTestUtils.setField(target, "chainConfig", chainConfig);
        ReflectionTestUtils.setField(target, "epochRetryService", epochRetryService);
    }

    /**
     * 测试更新
     */
    @Test
    public void getEpochMessage() throws BlockNumberException {
        when(chainConfig.getConsensusPeriodBlockCount()).thenReturn(BigInteger.valueOf(10));
        when(chainConfig.getSettlePeriodBlockCount()).thenReturn(BigInteger.valueOf(50));
        when(chainConfig.getAddIssuePeriodBlockCount()).thenReturn(BigInteger.valueOf(250));
        when(chainConfig.getBlockRewardRate()).thenReturn(BigDecimal.valueOf(0.6));
        when(chainConfig.getStakeRewardRate()).thenReturn(BigDecimal.valueOf(0.4));
        when(chainConfig.getSettlePeriodCountPerIssue()).thenReturn(BigInteger.valueOf(5));
        // 测试区块-1
        target.getEpochMessage(-100L);
        assertEquals(0,target.getConsensusEpochRound().intValue());
        assertEquals(0,target.getSettleEpochRound().intValue());
        assertEquals(0,target.getIssueEpochRound().intValue());
        // 测试区块0
        target.getEpochMessage(0L);
        assertEquals(0,target.getConsensusEpochRound().intValue());
        assertEquals(0,target.getSettleEpochRound().intValue());
        assertEquals(0,target.getIssueEpochRound().intValue());
        // 测试区块1
        target.getEpochMessage(1L);
        assertEquals(1,target.getConsensusEpochRound().intValue());
        assertEquals(1,target.getSettleEpochRound().intValue());
        assertEquals(1,target.getIssueEpochRound().intValue());
        // 测试区块5
        target.getEpochMessage(5L);
        assertEquals(1,target.getConsensusEpochRound().intValue());
        assertEquals(1,target.getSettleEpochRound().intValue());
        assertEquals(1,target.getIssueEpochRound().intValue());
        // 测试区块10
        target.getEpochMessage(10L);
        assertEquals(1,target.getConsensusEpochRound().intValue());
        assertEquals(1,target.getSettleEpochRound().intValue());
        assertEquals(1,target.getIssueEpochRound().intValue());
        // 测试区块251
        target.getEpochMessage(251L);
        assertEquals(26,target.getConsensusEpochRound().intValue());
        assertEquals(6,target.getSettleEpochRound().intValue());
        assertEquals(2,target.getIssueEpochRound().intValue());
        // 增发周期切换
        target.getEpochMessage(501L);
        assertEquals(51,target.getConsensusEpochRound().intValue());
        assertEquals(11,target.getSettleEpochRound().intValue());
        assertEquals(3,target.getIssueEpochRound().intValue());

    }

}
