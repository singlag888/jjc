package help.pay.lingdian.test.wy;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import help.pay.lingdian.Config.MerConfig;
import help.pay.lingdian.Utils.SignUtil;
/**
 * 210111-网银B2C跳转银行预下单
 * 调用交易 210111
 * @author Administrator
 *
 */
public class WyB2CToBank {
		private static final String TxCode="210111";//
		private static final String ProductId="0611";//产品类型:0611-网银B2C跳转银行
	public static void main(String[] args) {
		  try{
			Map<String,String> map = new HashMap<String,String>();
			map.put("Version", "2.0");	//版本号
			map.put("SignMethod",MerConfig.SIGNMETHOD); //签名类型
			map.put("TxCode",TxCode);  		//交易编码			
			map.put("MerNo",MerConfig.MERNO);  //商户号-测试环境统一商户号
			//map.put("PayChannel","");  //指定渠道
			map.put("ProductId",ProductId);//
			map.put("TxSN",new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));//商户交易流水号(唯一)
			map.put("Amount","10");//金额:单位:分
			map.put("PdtName","面包");//商品名称
			map.put("Remark","测试");//备注
			//map.put("ReturnUrl","");//前台通知地址
			map.put("NotifyUrl",MerConfig.NOTIFYURL);//异步通知URL
			map.put("BankId","建设银行");//银行编号
			//map.put("DcType","0");//借贷类型:0：借记卡 1：贷记卡
			map.put("ReqTime",new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));//请求时间 格式:yyyyMMddHHmmss
	       // 设置签名
			MerConfig.setSignature(map);
	        //报文明文
	        String plain = SignUtil.getURLParam(map,MerConfig.URL_PARAM_CONNECT_FLAG, true, null);
	        System.out.println("请求原始报文:"+plain);
			//测试地址
	        String msg = MerConfig.sendMsg(MerConfig.REQURL, map);
	        if (null == msg) {
				System.out.println("报文发送失败或应答消息为空");
			} else {
				Map<String,String> resMap = SignUtil.parseResponse(msg,MerConfig.base64Keys,MerConfig.URL_PARAM_CONNECT_FLAG,MerConfig.CHARSET);
				System.out.println("URLDecoder处理后返回数据:"+SignUtil.getURLParam(resMap,MerConfig.URL_PARAM_CONNECT_FLAG, true, null));
				if (MerConfig.verifySign(resMap)){
					System.out.println("签名验证结果成功");
					if ("00000".equalsIgnoreCase(resMap.get("RspCod"))
						|| "1".equalsIgnoreCase(resMap.get("Status"))){
						//请求成功进行处理
						String PayUrl=resMap.get("PayUrl");
						System.out.println("支付请求Url:"+PayUrl);
//						HttpServletResponse resp = null;
//						Redirect.sendRedirect(SignUtil.parseUrlFromGetReqData(PayUrl)
//								, "_self", "UTF-8", SignUtil.parseReqMapFromGetReqData(PayUrl), resp);
					}
					else {
						//请求失败进行处理
					}
				}
				else {
					System.out.println("签名验证结果失败");
				}
			}
		  }
		  catch (Exception e){
			  e.printStackTrace();
		  }
	}
}
