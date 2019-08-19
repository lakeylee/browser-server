package com.platon.browser.engine;

import com.platon.browser.client.PlatonClient;
import com.platon.browser.dao.mapper.NodeMapper;
import com.platon.browser.dao.mapper.StakingMapper;
import com.platon.browser.dto.CustomBlock;
import com.platon.browser.dto.CustomProposal;
import com.platon.browser.dto.CustomTransaction;
import com.platon.browser.engine.cache.NodeCache;
import com.platon.browser.engine.config.BlockChainConfig;
import com.platon.browser.engine.result.BlockChainResult;
import com.platon.browser.service.DbService;
import com.platon.browser.utils.HexTool;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.platon.BaseResponse;
import org.web3j.platon.bean.Node;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

/**
 * @Auther: Chendongming
 * @Date: 2019/8/10 16:11
 * @Description:
 */
@Component
@Data
public class BlockChain {
    private static Logger logger = LoggerFactory.getLogger(BlockChain.class);
    @Autowired
    private BlockChainConfig chainConfig;
    @Autowired
    private StakingExecute stakingExecute;
    @Autowired
    private ProposalExecute proposalExecute;
    @Autowired
    private AddressExecute addressExecute;
    @Autowired
    private NodeMapper nodeMapper;
    @Autowired
    private StakingMapper stakingMapper;
    @Autowired
    private DbService dbService;
    @Autowired
    private PlatonClient client;

    // 业务数据暂存容器
    public static final BlockChainResult STAGE_BIZ_DATA = new BlockChainResult();
    // 当前结算周期轮数
    private long curSettingEpoch;
    // 当前共识周期轮数
    private long curConsensusEpoch;
    // 增发周期轮数
    private long addIssueEpoch;
    // 当前块
    private CustomBlock curBlock;

    // 全量数据(质押相关)，需要根据业务变化，保持与数据库一致
    public static final NodeCache NODE_CACHE = new NodeCache();

    // 全量数据(提案相关)，需要根据业务变化，保持与数据库一致
    public static final Map<String, CustomProposal> PROPOSALS = new HashMap<>();

    /***
     * 以下字段业务使用说明：
     * 在当前共识周期发生选举的时候，需要对上一共识周期的验证节点计算出块率，如果发现出块率低的节点，就要看此节点是否在curValidator中，如果在则
     * 剔除
     */
    // 上轮结算周期验证人
    private Map <String, org.web3j.platon.bean.Node> preVerifier = new HashMap <>();
    // 当前结算周期验证人
    private Map <String, org.web3j.platon.bean.Node> curVerifier = new HashMap <>();
    // 上轮共识周期验证人
    private Map <String, org.web3j.platon.bean.Node> preValidator = new HashMap <>();
    // 当前共识周期验证人
    private Map <String, org.web3j.platon.bean.Node> curValidator = new HashMap <>();

    @PostConstruct
    private void init () {

    }

    /**
     * 分析区块内的业务信息
     * @param block
     */
    public void execute ( CustomBlock block ) {
        curBlock = block;
        // 推算并更新共识周期和结算周期
        updateEpoch();
        // 更新共识周期验证人和结算周期验证人列表
        updateVerifierAndValidator();
        // 分析交易信息, 通知质押引擎补充质押相关信息，通知提案引擎补充提案相关信息
        analyzeTransaction();
        // 通知各引擎周期临界点事件
        periodChangeNotify();
        // 更新node表中的节点出块数信息
        stakingExecute.updateNodeStatBlockQty(curBlock.getNodeId());
    }

    /**
     * 周期变更通知：
     *  通知各钩子方法处理周期临界点事件，以便更新与周期切换相关的信息
     */
    private void periodChangeNotify() {
        // 根据区块号是否整除周期来触发周期相关处理方法
        Long blockNumber = curBlock.getNumber();

        if (blockNumber % chainConfig.getElectionDistance() == 0) {
            logger.debug("开始验证人选举：Block Number({})", blockNumber);
            stakingExecute.onElectionDistance(curBlock,this);

            // 通知BlockChain实例内部做与开始验证人选举相关操作
            onElectionDistance();
        }

        if (blockNumber % chainConfig.getConsensusPeriod() == 0) {
            logger.debug("进入新共识周期：Block Number({})", blockNumber);
            stakingExecute.onNewConsEpoch(curBlock,this);

            // 通知BlockChain实例内部做与共识周期切换相关操作
            onNewConsEpoch();
        }

        if (blockNumber % chainConfig.getSettingPeriod() == 0) {
            logger.debug("进入新结算周期：Block Number({})", blockNumber);
            stakingExecute.onNewSettingEpoch(curBlock,this);

            // 通知BlockChain实例内部做与结算周期切换相关操作
            onNewSettingEpoch();
        }
    }


    /**
     * 根据区块号推算并更新共识周期和结算周期轮数
     */
    private void updateEpoch () {
        // 根据区块号是否与周期整除来触发周期相关处理方法
        // 计算共识周期轮数
        curConsensusEpoch = BigDecimal.valueOf(curBlock.getNumber()).divide(BigDecimal.valueOf(chainConfig.getConsensusPeriod()), 0, RoundingMode.CEILING).longValue();
        // 计算结算周期轮数
        curSettingEpoch = BigDecimal.valueOf(curBlock.getNumber()).divide(BigDecimal.valueOf(chainConfig.getSettingPeriod()), 0, RoundingMode.CEILING).longValue();
        //计算增发周期轮数
        addIssueEpoch = BigDecimal.valueOf(curBlock.getNumber()).divide(BigDecimal.valueOf(chainConfig.getAddIssuePeriod()), 0, RoundingMode.CEILING).longValue();
    }

    /**
     * 更新共识周期验证人和结算周期验证人映射缓存
     * // 假设当前链上最高区块号为750
     * 1         250        500        750
     * |----------|----------|----------|
     * A B C      A C D       B C D
     * 共识周期的临界块号分别是：1,250,500,750
     * 使用临界块号查到的验证人：1=>"A,B,C",250=>"A,B,C",500=>"A,C,D",750=>"B,C,D"
     * 如果当前区块号为753，由于未达到
     */
    private void updateVerifierAndValidator () {
        // 根据区块号是否整除周期来触发周期相关处理方法
        // 查询当前轮的候选人，至少需要在周期切换后出的第一个块号才可以查到，所以需要减一
        Long blockNumber = curBlock.getNumber(), prevBlockNumber = blockNumber - 1;
        if (prevBlockNumber % chainConfig.getConsensusPeriod() == 0) {
            logger.debug("共识周期切换块号:{}, 查新共识周期验证节点时的块号:{}", prevBlockNumber, blockNumber);
            // 直接查当前最新的共识周期验证人列表来初始化blockChain的curValidators属性
            try {
                // 1、当agent落后链上至少一个共识周期时，使用blockNumber是可以查询到验证人信息的
                // 2、当agent与链都在同一个共识周期时，使用blockNumber是查询不到验证人信息的
                // 所以，先使用blockNumber查询，结果为空证明agent已经追上链，则换为实时查询

                // 先根据区块号查
                BaseResponse <List <Node>> validators = client.getHistoryValidatorList(BigInteger.valueOf(blockNumber));
                if (!validators.isStatusOk() || validators.data == null || validators.data.size() == 0) {
                    logger.debug("通过区块号[{}]查询历史共识周期验证人列表为空:{}", blockNumber, validators.errMsg);
                    logger.debug("查询实时共识周期验证人列表...");
                    validators = client.getNodeContract().getValidatorList().send();
                }

                if (validators.isStatusOk()) {
                    // 把curValidator转存至preValidator
                    preValidator.clear();
                    preValidator.putAll(curValidator);
                    curValidator.clear();
                    // 设置新的当前共识周期验证人
                    validators.data.stream().filter(Objects::nonNull).forEach(node -> curValidator.put(HexTool.prefix(node.getNodeId()), node));
                }
            } catch (Exception e) {
                logger.error("查询最新共识周期验证人列表失败,原因：{}", e.getMessage());
            }
        }

        if (prevBlockNumber % chainConfig.getSettingPeriod() == 0) {
            logger.debug("结算周期切换块号:{}, 查新结算周期验证节点时的块号:{}", prevBlockNumber, blockNumber);
            // 直接查当前最新的结算周期验证人列表来初始化blockChain的curVerifiers属性
            try {
                // 1、当agent落后链上至少一个结算周期时，使用blockNumber是可以查询到验证人信息的
                // 2、当agent与链都在同一个结算周期时，使用blockNumber是查询不到验证人信息的
                // 所以，先使用blockNumber查询，结果为空证明agent已经追上链，则换为实时查询

                // 先根据区块号查
                BaseResponse <List <Node>> verifiers = client.getHistoryVerifierList(BigInteger.valueOf(blockNumber));
                if (!verifiers.isStatusOk() || verifiers.data == null || verifiers.data.size() == 0) {
                    logger.debug("通过区块号[{}]查询历史结算周期验证人列表为空:{}", blockNumber, verifiers.errMsg);
                    logger.debug("查询实时结算周期验证人列表...");
                    verifiers = client.getNodeContract().getVerifierList().send();
                }

                if (verifiers.isStatusOk()) {
                    // 把curVerifier转存至preVerifier
                    preVerifier.clear();
                    preVerifier.putAll(curVerifier);
                    curVerifier.clear();
                    // 设置新的当前结算周期验证人
                    verifiers.data.stream().filter(Objects::nonNull).forEach(node -> curVerifier.put(HexTool.prefix(node.getNodeId()), node));
                }
            } catch (Exception e) {
                logger.error("查询最新结算周期验证人列表失败,原因：{}", e.getMessage());
            }
        }
    }

    /**
     * 根据交易信息新增或更新相关记录：
     */
    private void analyzeTransaction () {
        curBlock.getTransactionList().forEach(tx -> {
            // 链上执行失败的交易不予处理
            if (CustomTransaction.TxReceiptStatusEnum.FAILURE.code == tx.getTxReceiptStatus()) return;
            // 调用交易分析引擎分析交易，以补充相关数据
            switch (tx.getTypeEnum()) {
                case CREATE_VALIDATOR: // 创建验证人
                case EDIT_VALIDATOR: // 编辑验证人
                case INCREASE_STAKING: // 增持质押
                case EXIT_VALIDATOR: // 撤销质押
                case DELEGATE: // 发起委托
                case UN_DELEGATE: // 撤销委托
                case REPORT_VALIDATOR: // 举报多签验证人
                    stakingExecute.execute(tx, this);
                    break;
                case CREATE_PROPOSAL_TEXT: // 创建文本提案
                case CREATE_PROPOSAL_UPGRADE: // 创建升级提案
                case CREATE_PROPOSAL_PARAMETER: // 创建参数提案
                case VOTING_PROPOSAL: // 给提案投票
                case DUPLICATE_SIGN: // 双签举报
                    proposalExecute.execute(tx, this);
                    break;
                case CONTRACT_CREATION: // 合约发布(合约创建)
                    logger.debug("合约发布(合约创建): txHash({}),contract({})", tx.getHash(), tx.getTo());
                    break;
                case TRANSFER: // 转账
                    logger.debug("转账: txHash({}),from({}),to({})", tx.getHash(), tx.getFrom(), tx.getTo());
                    break;
                case OTHERS: // 其它
                case MPC:
            }
            // 地址相关
            //addressExecute.execute(tx);
        });
    }


    /**
     * 导出需要入库的业务数据
     *
     * @return
     */
    public BlockChainResult exportResult () {
        return STAGE_BIZ_DATA;
    }

    /**
     * 清除分析后的业务数据
     */
    public void commitResult () {
        STAGE_BIZ_DATA.clear();
    }

    /**
     * 进入新的结算周期
     */
    private void onNewSettingEpoch () {

    }

    /**
     * 进入新的共识周期变更
     */
    private void onNewConsEpoch () {

    }

    /**
     * 进行选择验证人时触发
     */
    private void onElectionDistance () {

    }
}
