package com.platon.browser.util;


import com.platon.browser.dto.CustomTransaction;
import com.platon.browser.param.*;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * 交易参数解析器
 * User: dongqile
 * Date: 2019/1/9
 * Time: 11:46
 */
public class TxParamResolver {

    @Data
    public static class Result {
        private Object param;
        private CustomTransaction.TxTypeEnum txTypeEnum;
        @SuppressWarnings("unchecked")
		public <T> T convert(Class<T> clazz){
            return (T)param;
        }
    }

    public static Result analysis ( String input ) {
        Result result = new Result();
        result.txTypeEnum = CustomTransaction.TxTypeEnum.OTHERS;
        try {
            if (StringUtils.isNotEmpty(input) && !input.equals("0x")) {
                RlpList rlpList = RlpDecoder.decode(Hex.decode(input.replace("0x", "")));
                List <RlpType> rlpTypes = rlpList.getValues();
                RlpList rlpList1 = (RlpList) rlpTypes.get(0);

                RlpString rlpString = (RlpString) rlpList1.getValues().get(0);
                RlpList rlpList2 = RlpDecoder.decode(rlpString.getBytes());
                RlpString rl = (RlpString) rlpList2.getValues().get(0);
                BigInteger txCode = new BigInteger(1, rl.getBytes());

                CustomTransaction.TxTypeEnum typeEnum = CustomTransaction.TxTypeEnum.getEnum(txCode.toString());
                result.txTypeEnum = typeEnum;

                switch (typeEnum) {
                    case CREATE_VALIDATOR: // 1000
                        // 发起质押
                        //typ  表示使用账户自由金额还是账户的锁仓金额做质押 0: 自由金额； 1: 锁仓金额
                        BigInteger stakingTyp =  Resolver.bigIntegerResolver((RlpString) rlpList1.getValues().get(1));
                        //用于接受出块奖励和质押奖励的收益账户benefitAddress
                        String addr = Resolver.stringResolver((RlpString) rlpList1.getValues().get(2));
                        //被质押的节点的NodeId
                        String stakNodeId = Resolver.stringResolver((RlpString) rlpList1.getValues().get(3));
                        //外部Id externalId
                        String extrnaIdHex = Resolver.stringResolver((RlpString) rlpList1.getValues().get(4));
                        String extrnaId = new String(Numeric.hexStringToByteArray(extrnaIdHex));

                        //被质押节点的名称 nodeName
                        String hexStakNodeName = Resolver.stringResolver((RlpString) rlpList1.getValues().get(5));
                        String stakNodeName = new String(Numeric.hexStringToByteArray(hexStakNodeName));
                        //节点的第三方主页 website
                        String hexWebSiteAdd = Resolver.stringResolver((RlpString) rlpList1.getValues().get(6));
                        String webSiteAdd = new String(Numeric.hexStringToByteArray(hexWebSiteAdd));

                        //节点的描述 details
                        String hexDeteils = Resolver.stringResolver((RlpString) rlpList1.getValues().get(7));
                        String deteils = new String(Numeric.hexStringToByteArray(hexDeteils));

                        //质押的von amount programVersion
                        BigInteger stakingAmount =  Resolver.bigIntegerResolver((RlpString) rlpList1.getValues().get(8));

                        //程序的真实版本，治理rpc获取
                        BigInteger versions =  Resolver.bigIntegerResolver((RlpString) rlpList1.getValues().get(9));

                        CreateValidatorParam createValidatorParam = new CreateValidatorParam();
                        createValidatorParam.init(stakingTyp.intValue(), addr, stakNodeId, extrnaId.equals("0x")?"":extrnaId, stakNodeName,
                                webSiteAdd, deteils, stakingAmount.toString(), String.valueOf(versions));
                        result.param = createValidatorParam;
                        break;
                    case EDIT_VALIDATOR: // 1001
                        // 修改质押信息
                        //用于接受出块奖励和质押奖励的收益账户
                        String editAddr = Resolver.stringResolver((RlpString) rlpList1.getValues().get(1));
                        //被质押的节点的NodeId
                        String editNodeId = Resolver.stringResolver((RlpString) rlpList1.getValues().get(2));
                        //外部Id
                        String hexeditExtrId = Resolver.stringResolver((RlpString) rlpList1.getValues().get(3));
                        String extrId = new String(Numeric.hexStringToByteArray(hexeditExtrId));
                        //被质押节点的名称
                        String hexEditNodeName = Resolver.stringResolver((RlpString) rlpList1.getValues().get(4));
                        String editNodeName = new String(Numeric.hexStringToByteArray(hexEditNodeName));

                        //节点的第三方主页
                        String hexEditWebSiteAdd = Resolver.stringResolver((RlpString) rlpList1.getValues().get(5));
                        String editWebSiteAdd = new String(Numeric.hexStringToByteArray(hexEditWebSiteAdd));

                        //节点的描述
                        String hexEditDetail = Resolver.stringResolver((RlpString) rlpList1.getValues().get(6));
                        String editDetail = new String(Numeric.hexStringToByteArray(hexEditDetail));


                        EditValidatorParam editValidatorParam = new EditValidatorParam();
                        editValidatorParam.init(editAddr,editNodeId,extrId.equals("0x")?"":extrId,editNodeName,editWebSiteAdd,editDetail);
                        result.param = editValidatorParam;
                        break;
                    case INCREASE_STAKING: // 1002
                        // 增持质押

                        //被质押的节点的NodeId
                        String increaseNodeId = Resolver.stringResolver((RlpString) rlpList1.getValues().get(1));
                        //typ  表示使用账户自由金额还是账户的锁仓金额做质押 0: 自由金额； 1: 锁仓金额
                        BigInteger increaseTyp =  Resolver.bigIntegerResolver((RlpString) rlpList1.getValues().get(2));
                        //质押的von
                        BigInteger increaseAmount =  Resolver.bigIntegerResolver((RlpString) rlpList1.getValues().get(3));

                        IncreaseStakingParam increaseStakingParam = new IncreaseStakingParam();
                        increaseStakingParam.init(increaseNodeId,increaseTyp.intValue(),increaseAmount.toString(),"");
                        result.param = increaseStakingParam;
                        break;
                    case EXIT_VALIDATOR: // 1003
                        // 撤销质押

                        //被质押的节点的NodeId
                        String exitNodeId = Resolver.stringResolver((RlpString) rlpList1.getValues().get(1));
                        ExitValidatorParam exitValidatorParam = new ExitValidatorParam();
                        exitValidatorParam.init(exitNodeId,"","");
                        result.param = exitValidatorParam;
                        break;
                    case DELEGATE: // 1004
                        // 发起委托

                        //typ  表示使用账户自由金额还是账户的锁仓金额做质押 0: 自由金额； 1: 锁仓金额
                        BigInteger type =  Resolver.bigIntegerResolver((RlpString) rlpList1.getValues().get(1));
                        //被质押的节点的NodeId
                        String delegateNodeId = Resolver.stringResolver((RlpString) rlpList1.getValues().get(2));
                        //委托的金额
                        BigInteger delegateAmount =  Resolver.bigIntegerResolver((RlpString) rlpList1.getValues().get(3));

                        DelegateParam delegateParam = new DelegateParam();
                        delegateParam.init(type.intValue(),delegateNodeId,delegateAmount.toString(),"","");
                        result.param = delegateParam;
                        break;
                    case UN_DELEGATE: // 1005
                        // 减持/撤销委托

                        //代表着某个node的某次质押的唯一标示
                        String stakingBlockNum = Resolver.stringResolver((RlpString) rlpList1.getValues().get(1));
                        //被质押的节点的NodeId
                        String unDelegateNodeId = Resolver.stringResolver((RlpString) rlpList1.getValues().get(2));
                        //减持委托的金额(按照最小单位算，1LAT = 10**18 von)
                        BigInteger unDelegateAmount =  Resolver.bigIntegerResolver((RlpString) rlpList1.getValues().get(3));

                        UnDelegateParam unDelegateParam = new UnDelegateParam();
                        unDelegateParam.init(new BigInteger(stakingBlockNum.replace("0x",""),16).longValue(),unDelegateNodeId,unDelegateAmount.toString());
                        result.param = unDelegateParam;
                        break;

                    case CREATE_PROPOSAL_TEXT: // 2000
                        // 提交文本提案

                        //提交提案的验证人
                        String proposalVerifier = Resolver.stringResolver((RlpString) rlpList1.getValues().get(1));
                        //pIDID
                        String proposalPIDID = Resolver.stringResolver((RlpString) rlpList1.getValues().get(2));
                        String PIDID =  new String(Numeric.hexStringToByteArray(proposalPIDID));
                        CreateProposalTextParam createProposalTextParam = new CreateProposalTextParam();
                        createProposalTextParam.init(proposalVerifier,PIDID);
                        result.param=createProposalTextParam;
                        break;
                    case CREATE_PROPOSAL_UPGRADE: // 2001
                        // 提交升级提案

                        //提交提案的验证人
                        String upgradeVerifier = Resolver.stringResolver((RlpString) rlpList1.getValues().get(1));
                        //pIDID
                        String upgradelpIDID = Resolver.stringResolver((RlpString) rlpList1.getValues().get(2));
                        String upgradelpidid =  new String(Numeric.hexStringToByteArray(upgradelpIDID));
                        //升级版本
                        BigInteger newVersion =  Resolver.bigIntegerResolver((RlpString) rlpList1.getValues().get(3));
                        //投票截止区块高度
                        BigInteger endBlockRound =  Resolver.bigIntegerResolver((RlpString) rlpList1.getValues().get(4));
                        //结束轮转换结束区块高度

                        CreateProposalUpgradeParam createProposalUpgradeParam = new CreateProposalUpgradeParam();
                        createProposalUpgradeParam.init(upgradeVerifier,upgradelpidid,new BigDecimal(endBlockRound),newVersion.intValue());
                        result.param = createProposalUpgradeParam;
                        break;
                    case CANCEL_PROPOSAL: // 2005
                        // 提交取消提案

                        //提交提案的验证人
                        String cancelVerifier = Resolver.stringResolver((RlpString) rlpList1.getValues().get(1));
                        //本提案的pIDID
                        String cancelpIDID = Resolver.stringResolver((RlpString) rlpList1.getValues().get(2));
                        String cancelpidid =  new String(Numeric.hexStringToByteArray(cancelpIDID));
                        //投票截止区块高度
                        BigInteger cancelEndBlockRound =  Resolver.bigIntegerResolver((RlpString) rlpList1.getValues().get(3));
                        //被取消的pIDID
                        String canceledProposalID =  Resolver.stringResolver((RlpString) rlpList1.getValues().get(4));


                        CancelProposalParam cancelProposalParam = new CancelProposalParam();
                        cancelProposalParam.init(cancelVerifier,cancelpidid,new BigDecimal(cancelEndBlockRound),canceledProposalID);
                        result.param = cancelProposalParam;
                        break;
                    case VOTING_PROPOSAL: // 2003
                        // 给提案投票

                        //投票验证人
                        String voteVerifier = Resolver.stringResolver((RlpString) rlpList1.getValues().get(1));
                        //提案ID
                        String proposalID = Resolver.stringResolver((RlpString) rlpList1.getValues().get(2));
                        //投票选项
                        BigInteger option =  Resolver.bigIntegerResolver((RlpString) rlpList1.getValues().get(3));
                        //节点代码版本，有rpc的getProgramVersion接口获取
                        BigInteger programVersions =  Resolver.bigIntegerResolver((RlpString) rlpList1.getValues().get(4));
                        //代码版本签名，有rpc的getProgramVersion接口获取
                        String versionSign = Resolver.stringResolver((RlpString) rlpList1.getValues().get(5));

                        VotingProposalParam votingProposalParam = new VotingProposalParam();
                        votingProposalParam.init(voteVerifier,proposalID,option.toString(),programVersions.toString(),versionSign);
                        result.param = votingProposalParam;
                        break;
                    case DECLARE_VERSION: // 2004
                        // 版本声明

                        //声明的节点，只能是验证人/候选人
                        String activeNode = Resolver.stringResolver((RlpString) rlpList1.getValues().get(1));
                        //声明的版本，有rpc的getProgramVersion接口获取
                        BigInteger version =  Resolver.bigIntegerResolver((RlpString) rlpList1.getValues().get(2));
                        //声明的版本签名，有rpc的getProgramVersion接口获取
                        String versionSigns = Resolver.stringResolver((RlpString) rlpList1.getValues().get(3));
                        DeclareVersionParam declareVersionParam = new DeclareVersionParam();
                        declareVersionParam.init(activeNode,version.intValue(),versionSigns);
                        result.param = declareVersionParam;
                        break;
                    case REPORT_VALIDATOR: // 3000
                        // 举报双签
                        //type
                        BigInteger dupType = Resolver.bigIntegerResolver((RlpString) rlpList1.getValues().get(1));
                        //data
                        String data = Resolver.stringResolver((RlpString) rlpList1.getValues().get(2));
                        String evdences = new String(Numeric.hexStringToByteArray(data));

                        ReportValidatorParam reportValidatorParam = new ReportValidatorParam();
                        reportValidatorParam.init(dupType,evdences);
                        result.param = reportValidatorParam;
                        break;
                    case CREATE_RESTRICTING: // 4000
                        //创建锁仓计划

                        //锁仓释放到账账户
                        String account = Resolver.stringResolver((RlpString) rlpList1.getValues().get(1));

                        // RestrictingPlan 类型的列表（数组）
                        List<PlanParam> planDtoList = Resolver.objectResolver((RlpString) rlpList1.getValues().get(2));

                        CreateRestrictingParam createRestrictingParam = new CreateRestrictingParam();
                        createRestrictingParam.setPlan(planDtoList);
                        createRestrictingParam.setAccount(account);
                        result.param = createRestrictingParam;
                        break;
				default:
					break;
                }
            }
        } catch (Exception e) {
            result.txTypeEnum = CustomTransaction.TxTypeEnum.OTHERS;

            return result;
        }
        return result;
    }

    public static void main ( String[] args ) {
        String date = "0xf905b883820bb801b905b0b905ad7b227072657061726541223a7b2265706f6368223a302c22766965774e756d626572223a302c22626c6f636b48617368223a22307830646336343036343735313835313165666161313432323431653063636561313264613738623437663730323666303432303065343036646165353837323530222c22626c6f636b4e756d626572223a3530302c22626c6f636b496e646578223a302c2276616c69646174654e6f6465223a7b22696e646578223a302c2261646472657373223a22307863666535316438356639393635663664303331653465336363653638386561623763393565393430222c226e6f64654964223a226266633964363537386261623465353130373535353735653437623764313337666366306164306263663130656434643032333634306466623431623139376239663064383031346534376563626534643531663135646235313430303963626461313039656263663062376166653036363030643664343233626237666266222c22626c735075624b6579223a22623437313337393764323936633966653137343964323265623539623033643936393461623839366237313434396230653664616632643165636233613964336436653963323538623337616362326430376661383262636235356365643134346662346230353664366364313932613530393835393631356230393031323864366535363836653834646634373935316531373831363235363237393037303534393735663736653432376461386433326433663330623961353365363066227d2c227369676e6174757265223a2230783336626665363961393834653161313936646331356230656664626664313534656266346335326665346337303835386234343936613566306334306438313063366162666137386161313435333739616234656435313438353231623430623030303030303030303030303030303030303030303030303030303030303030227d2c227072657061726542223a7b2265706f6368223a302c22766965774e756d626572223a302c22626c6f636b48617368223a22307839633531653736323064333831643061653230326435356332393265386364386465346465333731336633323866373135353131373032393133613434386661222c22626c6f636b4e756d626572223a3530302c22626c6f636b496e646578223a302c2276616c69646174654e6f6465223a7b22696e646578223a302c2261646472657373223a22307863666535316438356639393635663664303331653465336363653638386561623763393565393430222c226e6f64654964223a226266633964363537386261623465353130373535353735653437623764313337666366306164306263663130656434643032333634306466623431623139376239663064383031346534376563626534643531663135646235313430303963626461313039656263663062376166653036363030643664343233626237666266222c22626c735075624b6579223a22623437313337393764323936633966653137343964323265623539623033643936393461623839366237313434396230653664616632643165636233613964336436653963323538623337616362326430376661383262636235356365643134346662346230353664366364313932613530393835393631356230393031323864366535363836653834646634373935316531373831363235363237393037303534393735663736653432376461386433326433663330623961353365363066227d2c227369676e6174757265223a2230786465643936346562393235383235646335333837326361626265613739653632333735653636323337303839643830646263303961613964303830303664326162393461383737643035373232303637303966346564363433306366323439373030303030303030303030303030303030303030303030303030303030303030227d7d";
        TxParamResolver.analysis(date);
    }
}
