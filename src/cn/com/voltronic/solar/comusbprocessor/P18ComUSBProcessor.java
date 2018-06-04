package cn.com.voltronic.solar.comusbprocessor;

import cn.com.voltronic.solar.beanbag.BeanBag;
import cn.com.voltronic.solar.beanbag.P18BeanBag;
import cn.com.voltronic.solar.communicate.IComUSBHandler;
import cn.com.voltronic.solar.communicate.ICommunicateDevice;
import cn.com.voltronic.solar.control.P18ComUSBControlModule;
import cn.com.voltronic.solar.data.bean.Capability;
import cn.com.voltronic.solar.data.bean.ConfigData;
import cn.com.voltronic.solar.data.bean.DataBeforeFault;
import cn.com.voltronic.solar.data.bean.DefaultData;
import cn.com.voltronic.solar.data.bean.MachineInfo;
import cn.com.voltronic.solar.data.bean.ProtocolInfo;
import cn.com.voltronic.solar.data.bean.WorkInfo;
import cn.com.voltronic.solar.processor.AbstractProcessor;
import cn.com.voltronic.solar.protocol.IProtocol;
import cn.com.voltronic.solar.system.GlobalProcessors;
import cn.com.voltronic.solar.util.DateUtils;
import cn.com.voltronic.solar.util.VolUtil;
import javafx.beans.binding.BooleanBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.NameValuePair;
//import org.apache.commons.httpclient.NameValuePair;
//import org.apache.commons.httpclient.methods.PostMethod;
//import org.apache.commons.httpclient.params.HttpParams;
import org.apache.http.client.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
//import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.apache.tools.ant.taskdefs.Java;
//import org.apache.commons.httpclient.*;


public class P18ComUSBProcessor
  extends AbstractComUSBProcessor
{
  static Stack<List> stkFailedSent =new Stack<List>();
  private static boolean bFirstCallEver=true;  
  private static Logger logger = Logger.getLogger(P18ComUSBProcessor.class);
  public static Lock lock = new ReentrantLock();
  public int _preparalleltype = 0;
  public boolean bfirsttime = true;
  private Object query_day = new Integer(1);
  private Object query_month = new Integer(1);
  private Object query_year = new Integer(1);
  private Object query_tatal = new Integer(1);
  private static final String ACTION_QUERY_MACHINE_INFO = "qryMachineInfo";
  private static final String ACTION_QUERY_CONFIG_DATA = "qryConfigData";
  private static final String GAP = ",";
  private static final double UNIT_10 = 10.0D;
  private Map<String, String> batteryType = new HashMap();
  private Map<String, String> inputVoltageType = new HashMap();
  private Map<String, String> outputSourceType = new HashMap();
  private Map<String, String> chargerSourceType = new HashMap();
  private Map<String, String> machineType = new HashMap();
  private Map<String, String> topologyType = new HashMap();
  private Map<String, String> solarPriorityType = new HashMap();
  private Map<String, String> workModeType = new HashMap();
  private Map<String, String> outputModelType = new HashMap();
  private Map<String, String> enable = new HashMap();
  private Map<String, String> regulationType = new HashMap();
  
  public P18ComUSBProcessor(ICommunicateDevice handler, IProtocol protocol)
  {
    super(handler, protocol);
    init();
    System.out.println("Using protocol: " +protocol.getClass().getName());
  }
  
  private void init()
  {
    this.batteryType.put("0", "AGM");
    this.batteryType.put("1", "Flooded");
    this.batteryType.put("2", "User");
    
    this.inputVoltageType.put("0", "Appliance");
    this.inputVoltageType.put("1", "UPS");
    
    this.outputSourceType.put("0", "Solar-Utility-Battery");
    this.outputSourceType.put("1", "Solar-Battery-Utility");
    
    this.chargerSourceType.put("0", "Solar first");
    this.chargerSourceType.put("1", "Solar and Utility");
    this.chargerSourceType.put("2", "Solar only");
    
    this.machineType.put("0", "Off-grid");
    this.machineType.put("1", "Hybrid");
    
    this.topologyType.put("0", "Transformerless");
    this.topologyType.put("1", "Transformer");
    
    this.solarPriorityType.put("0", "Battery-Load-Utility");
    this.solarPriorityType.put("1", "Load-Battery-Utility");
    
    this.workModeType.put("00", "Power On Mode");
    this.workModeType.put("01", "Standby Mode");
    this.workModeType.put("02", "Bypass Mode");
    this.workModeType.put("03", "Battery Mode");
    this.workModeType.put("04", "Fault Mode");
    this.workModeType.put("05", "Hybrid Mode");
    this.workModeType.put("06", "Charge Mode");
    
    this.outputModelType.put("0", "Single");
    this.outputModelType.put("1", "Parallel output");
    this.outputModelType.put("2", "Phase R of 3 phase output");
    this.outputModelType.put("3", "Phase S of 3 phase output");
    this.outputModelType.put("4", "Phase T of 3 phase output");
    this.outputModelType.put("5", "Phase 1 of 2 phase output");
    this.outputModelType.put("6", "Phase 2 of 2 phase output");
    
    this.enable.put("0", "Disable");
    this.enable.put("1", "Enable");
    
    this.regulationType.put("00", "India");
    this.regulationType.put("01", "Germany");
    this.regulationType.put("02", "South America");
  }
  
  public String getDeviceMode()
  {
    return null;
  }
  
  protected void initBeanBag()
  {
    this._beanbag = new P18BeanBag();
  }
  
  protected void initControlModule()
  {
    this._control = new P18ComUSBControlModule(getHandler(), 
      (ConfigData)this._beanbag.getBean("configdata"), 
      (Capability)this._beanbag.getBean("capability"));
  }
  
  public void initProtocol()
  {
    ProtocolInfo info = (ProtocolInfo)getBeanBag().getBean("protocolinfo");
    info.setProdid(this._protocol.getProtocolID());
    info.setBaseInfo(this._protocol.getBaseInfo());
    info.setProductInfo(this._protocol.getProductInfo());
    info.setRatingInfo(this._protocol.getRatingInfo());
    info.setMoreInfo(this._protocol.getMoreInfo());
    info.setMpptTrackNumber(this._protocol.getMpptTrackNumber());
    try
    {
      info.setSerialno(this._protocol.getSerialNo());
    }
    catch (Exception e)
    {
      logger.error("initProtocol Func -->" + e.getMessage());
    }
  }
  
  public boolean pollQuery()
  {
    WorkInfo workInfo = (WorkInfo)getBeanBag().getBean("workinfo");
    IComUSBHandler handler = (IComUSBHandler)getHandler();
    if (handler == null) {
      return false;
    }
    workInfo.setProdid(this._protocol.getProtocolID());
    workInfo.setSerialno(this._protocol.getSerialNo());
    if (this.currenttime != null) {
      workInfo.setCurrentTime(this.currenttime.getTime());
    }
    if (this.bfirsttime)
    {
      this._preparalleltype = this._paralleltype;
      this.bfirsttime = false;
    }
    else if (this._preparalleltype != this._paralleltype)
    {
      close();
      return false;
    }
    if (this._paralleltype != 0) {
      try
      {
        lock.lock();
        return pollQueryParallel();
      }
      catch (Exception e)
      {
        return false;
      }
      finally
      {
        lock.unlock();
      }
    }
    try
    {
      pGS(workInfo, handler);
      pMOD(workInfo, handler);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    updateExternalEntity(workInfo);
    return true;
  }
  
  protected void pGS(WorkInfo workInfo, IComUSBHandler handler)
  {
    String pGS = handler.excuteCommand("GS", true);
    if (isEmpty(pGS)) {
      return;
    }
    String[] gs = pGS.split(",");
    double gridVoltage = VolUtil.parseDouble(gs[0]) / 10.0D;
    double gridFrequency = VolUtil.parseDouble(gs[1]) / 10.0D;
    double acOutputVoltage = VolUtil.parseDouble(gs[2]) / 10.0D;
    double acOutputFrequency = VolUtil.parseDouble(gs[3]) / 10.0D;
    int acOutputApperentPower = VolUtil.parseInt(gs[4]);
    int acOutputActivePower = VolUtil.parseInt(gs[5]);
    int outputLoadPercent = VolUtil.parseInt(gs[6]);
    double batteryVoltage = VolUtil.parseDouble(gs[7]) / 10.0D;
    double batteryVoltageFromSCC1 = VolUtil.parseDouble(gs[8]) / 10.0D;
    double batteryVoltageFromSCC2 = VolUtil.parseDouble(gs[9]) / 10.0D;
    double disChargingCurrent = VolUtil.parseDouble(gs[10]);
    double chargingCurrent = VolUtil.parseDouble(gs[11]);
    int batteryCapacity = VolUtil.parseInt(gs[12]);
    int heatSinkTemperature = VolUtil.parseInt(gs[13]);
    int mpptChargerTemperature1 = VolUtil.parseInt(gs[14]);
    int mpptChargerTemperature2 = VolUtil.parseInt(gs[15]);
    
    int pvInputPower1 = VolUtil.parseInt(gs[16]);
    int pvInputPower2 = VolUtil.parseInt(gs[17]);
    double pvInputVoltage1 = VolUtil.parseDouble(gs[18]) / 10.0D;
    double pvInputVoltage2 = VolUtil.parseDouble(gs[19]) / 10.0D;
    
    String settingValueState = gs[20];
    String pv1WorkStatus = gs[21];
    String pv2WorkStatus = gs[22];
    String loadConnection = gs[23];
    String batteryStatus = gs[24];
    String invDirection = gs[25];
    String lineDirection = gs[26];
    String localParallelID = gs[27];
    
    setParallKey(VolUtil.parseInt(localParallelID));
    
    workInfo.setGridVoltageR(gridVoltage);
    workInfo.setGridFrequency(gridFrequency);
    workInfo.setAcOutputVoltageR(acOutputVoltage);
    workInfo.setAcOutputFrequency(acOutputFrequency);
    workInfo.setAcOutputApperentPowerR(acOutputApperentPower);
    workInfo.setAcOutputActivePowerR(acOutputActivePower);
    workInfo.setOutputLoadPercent(outputLoadPercent);
    
    workInfo.setBatteryVoltage(batteryVoltage);
    workInfo.setBatteryVoltageFromSCC1(batteryVoltageFromSCC1);
    workInfo.setBatteryVoltageFromSCC2(batteryVoltageFromSCC2);
    workInfo.setDisChargingCurrent(disChargingCurrent);
    workInfo.setChargingCurrent(chargingCurrent);
    workInfo.setBatteryCapacity(batteryCapacity);
    
    workInfo.setHeatSinkTemperature(heatSinkTemperature);
    workInfo.setMpptChargerTemperature1(mpptChargerTemperature1);
    workInfo.setMpptChargerTemperature2(mpptChargerTemperature2);
    
    workInfo.setMaxTemperature(workInfo.getHeatSinkTemperature());
    
    workInfo.setPvInputPower1(pvInputPower1);
    workInfo.setPvInputPower2(pvInputPower2);
    workInfo.setPvInputVoltage1(pvInputVoltage1);
    workInfo.setPvInputVoltage2(pvInputVoltage2);
    
    workInfo.setPv1WorkStatus(pv1WorkStatus);
    workInfo.setPv2WorkStatus(pv2WorkStatus);
    if ((workInfo.getPv1WorkStatus().equals("2")) || (workInfo.getPv2WorkStatus().equals("2"))) {
      workInfo.setPvLoss(false);
    } else {
      workInfo.setPvLoss(true);
    }
    if (loadConnection.equals("0")) {
      workInfo.setHasLoad(false);
    } else {
      workInfo.setHasLoad(true);
    }
    workInfo.setBatteryStatus(batteryStatus);
    workInfo.setInvDirection(invDirection);
    workInfo.setLineDirection(lineDirection);
  }
  
  protected void pMOD(WorkInfo workInfo, IComUSBHandler handler)
  {
    String pMOD = handler.excuteCommand("MOD", true);
    if (isEmpty(pMOD)) {
      return;
    }
    String workMode = (String)this.workModeType.get(pMOD);
    workInfo.setWorkMode(workMode);
  }
  
  protected void pGMN(MachineInfo machineInfo, IComUSBHandler handler)
  {
    String pGMN = handler.excuteCommand("GMN", true);
    if (isEmpty(pGMN)) {
      return;
    }
    String machineModel = (String)this.machineType.get(pGMN);
    machineInfo.setMachineModel(machineModel);
  }
  
  public boolean pollQueryParallel()
  {
    boolean result = true;
    boolean bParentLoss = true;
    int parall_i = 0;
    ArrayList<String> curList = new ArrayList();
    ArrayList<String> delList = new ArrayList();
    IComUSBHandler handler = (IComUSBHandler)getHandler();
    if (handler == null) {
      return false;
    }
    try
    {
      String[] prin;      
      for (parall_i = 0; parall_i < this._parallelnum; parall_i++)
      {
        String qPRIn = handler.excuteCommand("PRI" + parall_i, true);
        if (!isEmpty(qPRIn))
        {
          prin = qPRIn.split(",");
          int existent = VolUtil.parseInt(prin[0]);
          if (existent != 0)
          {
            int validLength = VolUtil.parseInt(prin[1]);
            String serial = prin[2];
            serial = serial.substring(0, validLength);
            curList.add(serial);
            if (this.subMap.containsKey(serial))
            {
              ParallSubProcessor processor = (ParallSubProcessor)this.subMap.get(serial);
              processor.setParallKey(parall_i);
              pPGSn(parall_i, processor, handler);
              
              String oldKey = processor.processorKey();
              if (!processor.reGenProcesorKey().equalsIgnoreCase(oldKey))
              {
                GlobalProcessors.removeProcessor(oldKey);
                GlobalProcessors.addProcessor(processor.processorKey(), processor);
              }
            }
            else if (serial.equalsIgnoreCase(getSerialNo()))
            {
              setParallKey(parall_i);
              pPGSn(parall_i, this, handler);
              String oldKey = processorKey();
              if (!reGenProcesorKey().equalsIgnoreCase(oldKey))
              {
            	  try {GlobalProcessors.removeProcessor(oldKey); } catch (Exception e1) {}
                try {GlobalProcessors.addProcessor(processorKey(), this); } catch (Exception e2) {}
              }
              bParentLoss = false;
            }
            else
            {
              ParallSubProcessor processor = new ParallSubProcessor(this, new P18BeanBag());
              processor.setDeviceName(getDeviceName());
              processor.setSerialNo(serial);
              processor.setParallKey(parall_i);
              pPGSn(parall_i, processor, handler);
              this.subMap.put(serial, processor);
              processor.saveDevice();
              GlobalProcessors.addProcessor(processor.processorKey(), processor);
            }
          }
        }
      }
      for (Map.Entry<String, ParallSubProcessor> entry : this.subMap.entrySet()) {
        if (curList.indexOf(entry.getKey()) < 0)
        {
          ((ParallSubProcessor)entry.getValue()).close();
          delList.add((String)entry.getKey());
        }
      }
      for (String key : delList) {
        this.subMap.remove(key);
      }
      if (bParentLoss) {
        close();
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      result = false;
    }
    
    return result;
  }
  
  public boolean pollQueryStatus()
  {
    WorkInfo workInfo = (WorkInfo)getBeanBag().getBean("workinfo");
    IComUSBHandler handler = (IComUSBHandler)getHandler();
    if (handler == null) {
      return false;
    }
    workInfo.setProdid(this._protocol.getProtocolID());
    workInfo.setSerialno(this._protocol.getSerialNo());
    
    pFWS(workInfo, handler);
    
    workInfo.setNoBattery(false);
    
    return true;
  }
  
  private void pFWS(WorkInfo workInfo, IComUSBHandler handler)
  {
    String pFWS = handler.excuteCommand("FWS", true);
    if (isEmpty(pFWS)) {
      return;
    }
    String[] ws = pFWS.split(",");
    
    warnsHandler(ws[1], "2001", workInfo);
    warnsHandler(ws[2], "2002", workInfo);
    warnsHandler(ws[3], "2003", workInfo);
    warnsHandler(ws[4], "2004", workInfo);
    
    warnsHandler(ws[5], "2005", workInfo);
    warnsHandler(ws[6], "2006", workInfo);
    warnsHandler(ws[7], "2007", workInfo);
    warnsHandler(ws[8], "2008", workInfo);
    
    warnsHandler(ws[9], "2009", workInfo);
    warnsHandler(ws[10], "2010", workInfo);
    warnsHandler(ws[11], "2011", workInfo);
    warnsHandler(ws[12], "2012", workInfo);
    
    warnsHandler(ws[13], "2013", workInfo);
    warnsHandler(ws[14], "2014", workInfo);
    warnsHandler(ws[15], "2015", workInfo);
    warnsHandler(ws[16], "2016", workInfo);
    
    workInfo.setLineLoss(ws[1].equals("1"));
    workInfo.setOverLoad(ws[8].equals("1"));
    setFaults(workInfo, ws[0]);
  }
  
  private void setFaults(WorkInfo workInfo, String faultCode)
  {
    if (VolUtil.parseInt(faultCode) > 0)
    {
      workInfo.setFault(true);
      DataBeforeFault data = new DataBeforeFault();
      data.setProdid(workInfo.getProdid());
      data.setSerialno(workInfo.getSerialno());
      data.setTrandate(workInfo.getCurrentTime());
      data.setWorkMode(workInfo.getWorkMode());
      data.setGridVoltage(workInfo.getGridCurrentR());
      data.setGridFrequency(workInfo.getGridFrequency());
      data.setAcoutputvoltager(workInfo.getAcOutputVoltageR());
      data.setAcoutputfrequency(workInfo.getAcOutputFrequency());
      data.setAcoutputapperentpowerr(workInfo.getAcOutputActivePowerR());
      data.setAcoutputactivepowerr(workInfo.getAcOutputActivePowerR());
      data.setBatteryVoltage(workInfo.getPBatteryVoltage());
      data.setBatteryDischargeCurrent(workInfo.getDisChargingCurrent());
      data.setBatteryChargingCurrent(workInfo.getChargingCurrent());
      data.setBatteryCapacity(workInfo.getBatteryCapacity());
      data.setPvinputpower1(workInfo.getPvInputPower1());
      data.setPvinputpower2(workInfo.getPvInputPower1());
      data.setPvinputvoltage1(workInfo.getPvInputVoltage1());
      data.setPvinputvoltage2(workInfo.getPvInputVoltage2());
      faultsHandler(faultCode, workInfo, data);
    }
  }
  
  private void pPGSn(int parallelKey, AbstractProcessor processor, IComUSBHandler handler)
  {
    String pPGSn = handler.excuteCommand("PGS" + parallelKey, true);
    if (isEmpty(pPGSn)) {
      return;
    }
    String[] pgs = pPGSn.split(",");
    String workMode = (String)this.workModeType.get("0" + pgs[1]);
    String faultCode = pgs[2];
    double gridVoltage = VolUtil.parseDouble(pgs[3]) / 10.0D;
    double gridFrequency = VolUtil.parseDouble(pgs[4]) / 10.0D;
    double acoutputvoltage = VolUtil.parseDouble(pgs[5]) / 10.0D;
    double acoutputfrequency = VolUtil.parseDouble(pgs[6]) / 10.0D;
    int acoutputapperentpower = VolUtil.parseInt(pgs[7]);
    int acoutputactivepower = VolUtil.parseInt(pgs[8]);
    int totalAcoutputapperentpower = VolUtil.parseInt(pgs[9]);
    int totalAcoutputactivepower = VolUtil.parseInt(pgs[10]);
    int outputLoadPercent = VolUtil.parseInt(pgs[11]);
    int totalOutputLoadPercent = VolUtil.parseInt(pgs[12]);
    double batteryVoltage = VolUtil.parseDouble(pgs[13]) / 10.0D;
    double batteryDischargeCurrent = VolUtil.parseDouble(pgs[14]);
    double batteryChargingCurrent = VolUtil.parseDouble(pgs[15]);
    double totalBatteryChargingCurrent = VolUtil.parseDouble(pgs[16]);
    int batteryCapacity = VolUtil.parseInt(pgs[17]);
    int pvinputpower1 = VolUtil.parseInt(pgs[18]);
    int pvinputpower2 = VolUtil.parseInt(pgs[19]);
    double pvinputvoltage1 = VolUtil.parseDouble(pgs[20]) / 10.0D;
    double pvinputvoltage2 = VolUtil.parseDouble(pgs[21]) / 10.0D;
    String pv1WorkStatus = pgs[22];
    String pv2WorkStatus = pgs[23];
    String loadConnection = pgs[24];
    String batteryStatus = pgs[25];
    String invDirection = pgs[26];
    String lineDirection = pgs[27];
    int pallTemperature = 0;
    if (pgs.length - 27 - 1 > 0) {
      pallTemperature = VolUtil.parseInt(pgs[28]);
    }
    Calendar calendar = Calendar.getInstance();
    
    WorkInfo workInfo = (WorkInfo)processor.getBeanBag().getBean("workinfo");
    workInfo.setProdid(this._protocol.getProtocolID());
    workInfo.setSerialno(processor.getSerialNo());
    workInfo.setCurrentTime(calendar.getTime());
    
    workInfo.setWorkMode(workMode);
    workInfo.setGridVoltageR(gridVoltage);
    workInfo.setGridFrequency(gridFrequency);
    workInfo.setAcOutputVoltageR(acoutputvoltage);
    workInfo.setAcOutputFrequency(acoutputfrequency);
    workInfo.setAcOutputApperentPowerR(acoutputapperentpower);
    workInfo.setAcOutputActivePowerR(acoutputactivepower);
    workInfo.setTotalACOutputApparentPower(totalAcoutputapperentpower);
    workInfo.setTotalACOutputActivePower(totalAcoutputactivepower);
    workInfo.setOutputLoadPercent(outputLoadPercent);
    workInfo.setTotalOutputLoadPercent(totalOutputLoadPercent);
    
    workInfo.setBatteryVoltage(batteryVoltage);
    workInfo.setDisChargingCurrent(batteryDischargeCurrent);
    workInfo.setChargingCurrent(batteryChargingCurrent);
    workInfo.setTotalBatteryChargingCurrent(totalBatteryChargingCurrent);
    workInfo.setBatteryCapacity(batteryCapacity);
    
    workInfo.setPvInputPower1(pvinputpower1);
    workInfo.setPvInputPower2(pvinputpower2);
    workInfo.setPvInputVoltage1(pvinputvoltage1);
    workInfo.setPvInputVoltage2(pvinputvoltage2);
    
    workInfo.setPv1WorkStatus(pv1WorkStatus);
    workInfo.setPv2WorkStatus(pv2WorkStatus);
    if (pgs.length - 27 - 1 > 0)
    {
      workInfo.setPallTemperature(pallTemperature);
      workInfo.setMaxTemperature(workInfo.getPallTemperature());
    }
    if ((workInfo.getPv1WorkStatus().equals("2")) || (workInfo.getPv2WorkStatus().equals("2"))) {
      workInfo.setPvLoss(false);
    } else {
      workInfo.setPvLoss(true);
    }
    if (loadConnection.equals("0")) {
      workInfo.setHasLoad(false);
    } else {
      workInfo.setHasLoad(true);
    }
    WorkInfo hsWorkInfo = (WorkInfo)getBeanBag().getBean("workinfo");
    workInfo.setLineLoss(hsWorkInfo.isLineLoss());
    if (workInfo.isLineLoss()) {
      warnsHandler("1", "2001", workInfo);
    } else {
      warnsHandler("0", "2001", workInfo);
    }
    workInfo.setNoBattery(false);
    
    workInfo.setBatteryStatus(batteryStatus);
    workInfo.setInvDirection(invDirection);
    workInfo.setLineDirection(lineDirection);
    
    String qPRIn = handler.excuteCommand("PRI" + parallelKey, true);
    if (isEmpty(qPRIn)) {
      return;
    }
    String[] prin = qPRIn.split(",");
    
    String chargerSourcePriority = (String)this.chargerSourceType.get(prin[3]);
    double maxChargingCurrent = VolUtil.parseDouble(prin[4]);
    double maxAcChargingCurrent = VolUtil.parseDouble(prin[5]);
    String outputModel = prin[6];
    
    processor.setOutputmode(VolUtil.parseInt(outputModel));
    ConfigData configdata = (ConfigData)processor.getBeanBag().getBean("configdata");
    if ((processor instanceof ParallSubProcessor))
    {
      configdata.setSubOutputMode(processor.getSerialNo(), (String)this.outputModelType.get(prin[6]));
      configdata.setChargerSourcePriority(processor.getSerialNo(), chargerSourcePriority);
      configdata.setMaxChargingCurrent(processor.getSerialNo(), maxChargingCurrent);
      configdata.setMaxAcChargingCurrent(processor.getSerialNo(), maxAcChargingCurrent);
    }
    else
    {
      configdata.setOutputModel((String)this.outputModelType.get(prin[6]));
      configdata.setChargerSourcePriority(chargerSourcePriority);
      configdata.setMaxChargingCurrent(maxChargingCurrent);
      configdata.setMaxAcChargingCurrent(maxAcChargingCurrent);
    }
    setFaults(workInfo, faultCode);
    updateExternalEntity(workInfo);
  }
  
  public boolean queryCapability()
  {
    Capability capability = (Capability)getBeanBag().getBean("capability");
    IComUSBHandler handler = (IComUSBHandler)getHandler();
    if (handler == null) {
      return false;
    }
    try
    {
      pFLAG(capability, handler);
    }
    catch (Exception e)
    {
      logger.error("queryCapability Func -->" + e.getMessage());
      return false;
    }
    
    return true;
  }
  
  private void pFLAG(Capability capability, IComUSBHandler handler)
  {
    String pFLAG = handler.excuteCommand("FLAG", true);
    if (isEmpty(pFLAG)) {
      return;
    }
    String[] flag = pFLAG.split(",");
    capability.setCapableA(flag[0].equals("0"));
    capability.setCapableB(flag[1].equals("1"));
    capability.setCapableC(flag[2].equals("1"));
    capability.setCapableD(flag[3].equals("1"));
    capability.setCapableE(flag[4].equals("1"));
    capability.setCapableF(flag[5].equals("1"));
    capability.setCapableG(flag[6].equals("1"));
    capability.setCapableH(flag[7].equals("1"));
  }
  
  public boolean queryConfigData()
  {
    IComUSBHandler handler = (IComUSBHandler)getHandler();
    if (handler == null) {
      return false;
    }
    try
    {
      pPIRI(handler, "qryConfigData");
      pMCHGCR(handler);
      pMUCHGCR(handler);
      pACCT(handler);
      pACLT(handler);
    }
    catch (Exception e)
    {
      logger.error("queryConfigData Func -->" + e.getMessage());
      return false;
    }
    return true;
  }
  
  private void pMCHGCR(IComUSBHandler handler)
  {
    ConfigData configdata = (ConfigData)getBeanBag().getBean("configdata");
    String pMCHGCR = handler.excuteCommand("MCHGCR", true);
    if (isEmpty(pMCHGCR)) {
      return;
    }
    String[] mchgcr = pMCHGCR.split(",");
    for (int i = 0; i < mchgcr.length; i++) {
      mchgcr[i] = String.valueOf( VolUtil.parseDouble(mchgcr[i]));
    }
    configdata.setMaxChargingCurrentComBox(mchgcr);
  }
  
  private void pMUCHGCR(IComUSBHandler handler)
  {
    ConfigData configdata = (ConfigData)getBeanBag().getBean("configdata");
    String pMUCHGCR = handler.excuteCommand("MUCHGCR", true);
    if (isEmpty(pMUCHGCR)) {
      return;
    }
    String[] muchgcr = pMUCHGCR.split(",");
    for (int i = 0; i < muchgcr.length; i++) {
      muchgcr[i] = String.valueOf(VolUtil.parseDouble(muchgcr[i]));
    }
    configdata.setMaxAcChargingCurrentCombox(muchgcr);
  }
  
  private void pACCT(IComUSBHandler handler)
  {
    ConfigData configdata = (ConfigData)getBeanBag().getBean("configdata");
    String pACCT = handler.excuteCommand("ACCT", true);
    if (isEmpty(pACCT)) {
      return;
    }
    String[] acct = pACCT.split(",");
    configdata.setAcChargeStarttime(acct[0]);
    configdata.setAcChargeEndtime(acct[1]);
  }
  
  private void pACLT(IComUSBHandler handler)
  {
    ConfigData configdata = (ConfigData)getBeanBag().getBean("configdata");
    String pACLT = handler.excuteCommand("ACLT", true);
    if (isEmpty(pACLT)) {
      return;
    }
    String[] aclt = pACLT.split(",");
    configdata.setAcoutputStarttime(aclt[0]);
    configdata.setAcoutputEndtime(aclt[1]);
  }
  
  public boolean queryDefaultData()
  {
    DefaultData defaultData = (DefaultData)getBeanBag().getBean("defaultdata");
    IComUSBHandler handler = (IComUSBHandler)getHandler();
    if (handler == null) {
      return false;
    }
    try
    {
      pDI(defaultData, handler);
    }
    catch (Exception e)
    {
      logger.error("queryDefaultData Func -->" + e.getMessage());
      return false;
    }
    return true;
  }
  
  private void pDI(DefaultData defaultData, IComUSBHandler handler)
  {
    String pDI = handler.excuteCommand("DI", true);
    if (isEmpty(pDI)) {
      return;
    }
    String[] di = pDI.split(",");
    double acOutputVoltage = VolUtil.parseDouble(di[0]) / 10.0D;
    double acOutputFrequency = VolUtil.parseDouble(di[1]) / 10.0D;
    String acInputVoltageRange = (String)this.inputVoltageType.get(di[2]);
    double batteryUnderVoltage = VolUtil.parseDouble(di[3]) / 10.0D;
    double batteryFloatVoltage = VolUtil.parseDouble(di[4]) / 10.0D;
    double batteryBulkVoltage = VolUtil.parseDouble(di[5]) / 10.0D;
    double batteryRechargeVoltage = VolUtil.parseDouble(di[6]) / 10.0D;
    double batteryRedischargeVoltage = VolUtil.parseDouble(di[7]) / 10.0D;
    double maxChargingCurrent = VolUtil.parseDouble(di[8]);
    double maxACChargingCurrent = VolUtil.parseDouble(di[9]);
    String batType = (String)this.batteryType.get(di[10]);
    String outputSourcePriority = (String)this.outputSourceType.get(di[11]);
    String chargerSourcePriority = (String)this.chargerSourceType.get(di[12]);
    String solarPowerPriority = (String)this.solarPriorityType.get(di[13]);
    String mchType = (String)this.machineType.get(di[14]);
    String outputModel = (String)this.outputModelType.get(di[15]);
    
    defaultData.setAcOutputVoltage(acOutputVoltage);
    defaultData.setAcOutputFrequency(acOutputFrequency);
    defaultData.setAcInputVoltageRange(acInputVoltageRange);
    defaultData.setBatteryUnderVoltage(batteryUnderVoltage);
    defaultData.setBatteryFloatVoltage(batteryFloatVoltage);
    defaultData.setBatteryBulkVoltage(batteryBulkVoltage);
    defaultData.setBatteryRechargeVoltage(batteryRechargeVoltage);
    defaultData.setBatteryRedischargeVoltage(batteryRedischargeVoltage);
    defaultData.setMaxChargingCurrent(maxChargingCurrent);
    defaultData.setMaxACChargingCurrent(maxACChargingCurrent);
    defaultData.setBatteryType(batType);
    defaultData.setOutputSourcePriority(outputSourcePriority);
    defaultData.setChargerSourcePriority(chargerSourcePriority);
    defaultData.setSolarPowerPriority(solarPowerPriority);
    defaultData.setMachineType(mchType);
    defaultData.setOutputModel(outputModel);
    
    defaultData.setCapableA((String)this.enable.get(di[16]));
    defaultData.setCapableD((String)this.enable.get(di[17]));
    defaultData.setCapableE((String)this.enable.get(di[18]));
    defaultData.setCapableF((String)this.enable.get(di[19]));
    defaultData.setCapableG((String)this.enable.get(di[20]));
    defaultData.setCapableH((String)this.enable.get(di[21]));
    defaultData.setCapableB((String)this.enable.get(di[22]));
    defaultData.setCapableC((String)this.enable.get(di[23]));
  }
  
  public boolean queryMachineInfo()
  {
    IComUSBHandler handler = (IComUSBHandler)getHandler();
    if (handler == null) {
      return false;
    }
    pVFW(handler);
    pPIRI(handler, "qryMachineInfo");
    
    return true;
  }
  
  private void pVFW(IComUSBHandler handler)
  {
    MachineInfo machineInfo = (MachineInfo)getBeanBag().getBean("machineinfo");
    String pVFW = handler.excuteCommand("VFW", true);
    if (isEmpty(pVFW)) {
      return;
    }
    String[] version = pVFW.split(",");
    machineInfo.setMainFirmwareVersion(version[0]);
    machineInfo.setSlaveFirmwareVersion1(version[1]);
    machineInfo.setSlaveFirmwareVersion2(version[2]);
  }
  
  private void pPIRI(IComUSBHandler handler, String action)
  {
    String pPIRI = handler.excuteCommand("PIRI", true);
    if (isEmpty(pPIRI)) {
      return;
    }
    String[] piri = pPIRI.split(",");
    //System.out.print(pPIRI);
    double acInputRatingVoltage = VolUtil.parseDouble(piri[0]) / 10.0D;
    double acInputRatingCurrent = VolUtil.parseDouble(piri[1]) / 10.0D;
    double acOutputRatingVoltage = VolUtil.parseDouble(piri[2]) / 10.0D;
    double acOutputRatingFrequency = VolUtil.parseDouble(piri[3]) / 10.0D;
    double acOutputRatingCurrent = VolUtil.parseDouble(piri[4]) / 10.0D;
    int acOutputRatingApparentPower = VolUtil.parseInt(piri[5]);
    int acOutputRatingActivePower = VolUtil.parseInt(piri[6]);
    double batteryRatingVoltage = VolUtil.parseDouble(piri[7]) / 10.0D;
    
    double batteryRechargeVoltage = VolUtil.parseDouble(piri[8]) / 10.0D;
    double batteryRedischargeVoltage = VolUtil.parseDouble(piri[9]) / 10.0D;
    double batteryUnderVoltage = VolUtil.parseDouble(piri[10]) / 10.0D;
    double batteryBulkVoltage = VolUtil.parseDouble(piri[11]) / 10.0D;
    double batteryFloatVoltage = VolUtil.parseDouble(piri[12]) / 10.0D;
    String batType = (String)this.batteryType.get(piri[13]);
    double maxACChargingCurrent = VolUtil.parseDouble(piri[14]);
    double maxChargingCurrent = VolUtil.parseDouble(piri[15]);
    String inputVoltageRange = (String)this.inputVoltageType.get(piri[16]);
    String outputSourcePriority = (String)this.outputSourceType.get(piri[17]);
    String chargerSourcePriority = (String)this.chargerSourceType.get(piri[18]);
    int maxParallelNum = VolUtil.parseInt(piri[19]);
    String mchType = (String)this.machineType.get(piri[20]);
    String topology = (String)this.topologyType.get(piri[21]);
    String outputModelSetting = (String)this.outputModelType.get(piri[22]);
    String solarPowerPriority = (String)this.solarPriorityType.get(piri[23]);
    int mpptTrackNumber = VolUtil.parseInt(piri[24]);
    
    String regulationsState = "India";
    try
    {
      regulationsState = (String)this.regulationType.get(piri[25]);
    }
    catch (Exception localException) {}
    int outputmode = VolUtil.parseInt(piri[22]);
    this._protocol.setOutputMode(outputmode);
    
    this._outputmode = outputmode;
    if (outputmode != 0)
    {
      this._paralleltype = 1;
      this._parallelnum = VolUtil.parseInt(piri[19]);
    }
    else
    {
      this._paralleltype = 0;
    }
    if (action.equals("qryMachineInfo"))
    {
      MachineInfo machineInfo = (MachineInfo)getBeanBag().getBean("machineinfo");
      machineInfo.setAcInputRatingVoltage(acInputRatingVoltage);
      machineInfo.setAcInputRatingCurrent(acInputRatingCurrent);
      machineInfo.setAcOutputRatingVoltage(acOutputRatingVoltage);
      machineInfo.setAcOutputRatingFrequency(acOutputRatingFrequency);
      machineInfo.setAcOutputRatingCurrent(acOutputRatingCurrent);
      machineInfo.setAcOutputRatingApparentPower(acOutputRatingApparentPower);
      machineInfo.setAcOutputRatingActivePower(acOutputRatingActivePower);
      machineInfo.setBatteryRatingVoltage(batteryRatingVoltage);
      machineInfo.setMachineType(mchType);
      machineInfo.setTopology(topology);
      
      machineInfo.setBatteryRechargeVoltage(batteryRechargeVoltage);
      machineInfo.setBatteryRedischargeVoltage(batteryRedischargeVoltage);
      
      machineInfo.setBatteryUnderVoltage(batteryUnderVoltage);
      machineInfo.setBatteryBulkVoltage(batteryBulkVoltage);
      machineInfo.setBatteryFloatVoltage(batteryFloatVoltage);
      
      machineInfo.setBatteryType(batType);
      machineInfo.setMaxACChargingCurrent(maxACChargingCurrent);
      machineInfo.setMaxChargingCurrent(maxChargingCurrent);
      machineInfo.setInputVoltageRange(inputVoltageRange);
      machineInfo.setOutputSourcePriority(outputSourcePriority);
      machineInfo.setChargerSourcePriority(chargerSourcePriority);
      machineInfo.setMaxParallelNum(maxParallelNum);
      machineInfo.setOutputModel(outputModelSetting);
      machineInfo.setSolarPowerPriority(solarPowerPriority);
      machineInfo.setMpptTrackNumber(mpptTrackNumber);
    }
    else if (action.equals("qryConfigData"))
    {
      ConfigData configdata = (ConfigData)getBeanBag().getBean("configdata");
      Capability capability = (Capability)getBeanBag().getBean("capability");
      configdata.setAcOutputRatingVoltage(acOutputRatingVoltage);
      configdata.setAcOutputRatingFrequency(acOutputRatingFrequency);
      configdata.setBatteryRatingVoltage(batteryRatingVoltage);
      
      configdata.setBatteryRechargeVoltage(batteryRechargeVoltage);
      configdata.setBatteryRedischargeVoltage(batteryRedischargeVoltage);
      
      configdata.setBatteryUnderVoltage(batteryUnderVoltage);
      configdata.setBatteryBulkVoltage(batteryBulkVoltage);
      configdata.setBatteryFloatVoltage(batteryFloatVoltage);
      if (configdata.getBatteryRatingVoltage() > 40.0D)
      {
        configdata.setMinBatteryUnderVoltage(40.0D);
        configdata.setMaxBatteryUnderVoltage(48.01D);
        configdata.setMinBatteryBulkVoltage(48.0D);
        configdata.setMaxBatteryBulkVoltage(58.41D);
        configdata.setMinBatteryFloatVoltage(48.0D);
        configdata.setMaxBatteryFloatVoltage(58.41D);
      }
      else if (configdata.getBatteryRatingVoltage() > 20.0D)
      {
        configdata.setMinBatteryUnderVoltage(20.0D);
        configdata.setMaxBatteryUnderVoltage(24.01D);
        configdata.setMinBatteryBulkVoltage(24.0D);
        configdata.setMaxBatteryBulkVoltage(29.21D);
        configdata.setMinBatteryFloatVoltage(24.0D);
        configdata.setMaxBatteryFloatVoltage(29.21D);
      }
      else
      {
        configdata.setMinBatteryUnderVoltage(10.2D);
        configdata.setMaxBatteryUnderVoltage(12.01D);
        configdata.setMinBatteryBulkVoltage(12.0D);
        configdata.setMaxBatteryBulkVoltage(14.61D);
        configdata.setMinBatteryFloatVoltage(12.0D);
        configdata.setMaxBatteryFloatVoltage(14.61D);
      }
      configdata.setBatteryType(batType);
      configdata.setMaxAcChargingCurrent(maxACChargingCurrent);
      configdata.setMaxChargingCurrent(maxChargingCurrent);
      configdata.setInputVoltageRange(inputVoltageRange);
      configdata.setOutputSourcePriority(outputSourcePriority);
      configdata.setChargerSourcePriority(chargerSourcePriority);
      configdata.setOutputModel(outputModelSetting);
      configdata.setSolarPowerPriority(solarPowerPriority);
      
      capability.setCapableI(piri[20].equals("1"));
      
      configdata.setRegulationsState(regulationsState);
    }
  }
  
  public Calendar queryCurrentTime()
  {
    Calendar cal = null;
    IComUSBHandler handler = (IComUSBHandler)getHandler();
    String pT = handler.excuteCommand("T", true);
    if (isEmpty(pT)) {
      return cal;
    }
    if (pT.trim().length() != 14) {
      return cal;
    }
    Date date = DateUtils.parseDate(pT, "yyyyMMddHHmmss");
    if (date != null)
    {
      cal = Calendar.getInstance();
      cal.setTime(date);
    }
    return cal;
  }
  
  public void querySelfTestResult() {}
  
  public boolean supportSelfTest()
  {
    return false;
  }
  
  public boolean queryDeviceModel()
  {
    return false;
  }
  
  public void queryEnergyBeginDate() {}
  
  public double queryEnergyDay(Calendar trandate)
    throws Exception
  {
    synchronized (this.query_day)
    {
      double energyDay = 0.0D;
      IComUSBHandler handler = (IComUSBHandler)getHandler();
      if (handler == null) {
        throw new Exception("queryEnergyDay handler is null...");
      }
      Calendar calendar = (Calendar)trandate.clone();
      String value = DateUtils.getFormatDate(calendar.getTime(), "yyyyMMdd");
      String qedStr = handler.excuteCommand("ED" + value, true);
      if ((qedStr != null) && (!"".equals(qedStr)) && (!qedStr.equals("(NAK"))) {
        energyDay = parseDoubleV(qedStr);
      } else {
        throw new Exception("query day energy error");
      }
      return energyDay;
    }
  }
  
  public double queryEnergyHour(Calendar trandate, int hour)
    throws Exception
  {
    return 0.0D;
  }
  
  public double queryEnergyMonth(int year, int month)
    throws Exception
  {
    synchronized (this.query_month)
    {
      double energyMonth = 0.0D;
      IComUSBHandler handler = (IComUSBHandler)getHandler();
      if (handler == null) {
        throw new Exception("queryEnergyMonth handler is null");
      }
      String monthStr = String.valueOf(month);
      String value = year + monthStr;
      String qemStr = handler.excuteCommand("EM" + value, true);
      if ((qemStr != null) && (!"".equals(qemStr)) && (!qemStr.equals("(NAK"))) {
        energyMonth = parseDoubleV(qemStr);
      } else {
        throw new Exception("query month energy error");
      }
      return energyMonth;
    }
  }
  
  public double queryEnergyYear(int year)
    throws Exception
  {
    synchronized (this.query_year)
    {
      double energyYear = 0.0D;
      IComUSBHandler handler = (IComUSBHandler)getHandler();
      if (handler == null) {
        throw new Exception("queryEnergyYear handler is null");
      }
      String value = String.valueOf(year);
      String qeyStr = handler.excuteCommand("EY" + value, true);
      if ((qeyStr != null) && (!"".equals(qeyStr)) && (!qeyStr.equals("(NAK"))) {
        energyYear = parseDoubleV(qeyStr);
      } else {
        throw new Exception("query year energy error");
      }
      return energyYear;
    }
  }
  
  public double queryEnergyTotal()
  {
    synchronized (this.query_tatal)
    {
      double energyTotal = 0.0D;
      IComUSBHandler handler = (IComUSBHandler)getHandler();
      if (handler == null) {
        return energyTotal;
      }
      String qetStr = handler.excuteCommand("ET", true);
      if (!isEmpty(qetStr)) {
        energyTotal = VolUtil.parseDouble(qetStr);
      } else {
        energyTotal = 0.0D;
      }
      return energyTotal;
    }
  }
  
  public float queryFWVersion()
  {
    return 0.0F;
  }
  
@SuppressWarnings("unchecked")
public void updateExternalEntity(  WorkInfo workInfo ) {
	  
	  try {
		  //System.out.println("In updateExternalEntity" );

		  		File f = new File("SolarPowerExtender.debug");
		  		boolean bHeader = false;
		  		
		  		if(f.exists() && !f.isDirectory()) { 
		  		
		  			PrintStream w1 = null;
		  			try {
		  				File f1 = new File("SolarPowerExtender.out");
		  				if(!f1.exists()) {
		  					bHeader = true ;
		  				}
		  				f1=null;
		  				w1 = new PrintStream(new FileOutputStream("SolarPowerExtender.out", true));
		  			} catch (FileNotFoundException e) {
		
		  				e.printStackTrace();
		  			} 
		  			StringBuffer s1= new StringBuffer();		  			
				  
					    Class<?> c = Class.forName("cn.com.voltronic.solar.data.bean.WorkInfo");
					    Object t = workInfo;

					    Method[] allMethods = c.getDeclaredMethods();
					    
					    if (bHeader) {
						    for (Method m : allMethods) {
						    	String mname = m.getName();
						    	if (mname.startsWith("get")) {
						    		try { 
						    			s1.append(mname + ",");						    									    			
						    		}
						    		catch (Exception e) {}
						    	}
						    	
						    }
					    }
					    s1.append("\n");
					    
					    for (Method m : allMethods) {
					    	String mname = m.getName();
					    	if (mname.startsWith("get")) {
					    		try { 					    			
					    			m.setAccessible(true);
					    			Object o = m.invoke(t);
					    			s1.append(o + ",");					    			
					    		}
					    		catch (Exception e) {}
					    	}
					    	
					    }				  				
				 	  
				      w1.print(s1.toString());
				      w1.close();				      
		  		}
	      
	      //First time let's load what we didn't send before
		  if (bFirstCallEver) {
			  bFirstCallEver=false;
			  loadCurrentFailedFromDisk();
		  }
	      		
		  List<NameValuePair> nvp = new ArrayList<NameValuePair>();
	     		  
		  nvp.add(new BasicNameValuePair("date", new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date()).toString()));
		  nvp.add(new BasicNameValuePair("pv_volt",  String.valueOf(workInfo.getPvInputVoltage1()) ));
	     
	      if ( workInfo.getPvInputPower1() > 0 ) {
	    	  nvp.add(new BasicNameValuePair("pv_current",  String.valueOf(workInfo.getPvInputPower1()/workInfo.getPvInputVoltage1() ) ));
	      } else { 
	    	  nvp.add(new BasicNameValuePair("pv_current",  String.valueOf(0)));
	      }
	      
	      nvp.add(new BasicNameValuePair("pv_watt",  String.valueOf(workInfo.getPvInputPower1()) ));
	      nvp.add(new BasicNameValuePair("pv_total_charging_power",  String.valueOf(0))) ; //Don't have it here 
	      nvp.add(new BasicNameValuePair("pv_is_charging",  String.valueOf(0 ) ));//Don't have it here
	      nvp.add(new BasicNameValuePair("batt_remain_time",  String.valueOf(workInfo.getBatteryRemainTime()) ));
	      nvp.add(new BasicNameValuePair("batt_total_capacity",  String.valueOf(workInfo.getBatteryTotalCapacity())) );
	      nvp.add(new BasicNameValuePair("batt_capacity",  String.valueOf(workInfo.getBatteryCapacity()) ));
	      nvp.add(new BasicNameValuePair("batt_status",  String.valueOf(workInfo.getBatteryStatus()) ));
	      nvp.add(new BasicNameValuePair("batt_scc1",  String.valueOf(0) ));//Don't have it here
	      nvp.add(new BasicNameValuePair("batt_scc2",  String.valueOf(0) ));//Don't have it here
	      nvp.add(new BasicNameValuePair("batt_scc3",  String.valueOf(0) ));//Don't have it here
	      nvp.add(new BasicNameValuePair("batt_n_volt",  String.valueOf(workInfo.getNBatteryVoltage()) ));
	      nvp.add(new BasicNameValuePair("batt_p_volt",  String.valueOf(workInfo.getBatteryVoltage() ) ));
	      nvp.add(new BasicNameValuePair("batt_diss",  String.valueOf(workInfo.getDisChargingCurrent()) ));
	      nvp.add(new BasicNameValuePair("pbus_volt",  String.valueOf(workInfo.getPBUSVoltage()) ));
	      nvp.add(new BasicNameValuePair("charge_src",  String.valueOf(0) ));//Don't have it here
	      nvp.add(new BasicNameValuePair("grid_current",  String.valueOf(workInfo.getGridCurrentR()) ));
	      nvp.add(new BasicNameValuePair("inver_direction",  String.valueOf(workInfo.getInvDirection()) ));
	      nvp.add(new BasicNameValuePair("output_loadprcnt",  String.valueOf(workInfo.getOutputLoadPercent()) ));
	      nvp.add(new BasicNameValuePair("work_mode",  String.valueOf(workInfo.getWorkMode()) ));
	      nvp.add(new BasicNameValuePair("whole_power",  String.valueOf(workInfo.getAcOutputActivePowerR()) ));
	      nvp.add(new BasicNameValuePair("charge_current",  String.valueOf(workInfo.getChargingCurrent()) ));
	      nvp.add(new BasicNameValuePair("ac_output_power",  String.valueOf(workInfo.getAcOutputActivePowerR())) ); 
	      nvp.add(new BasicNameValuePair("ac_output_apparent_power",  String.valueOf(workInfo.getAcOutputApperentPowerR()) ));
	     
	      //Grid stuff
	      nvp.add(new BasicNameValuePair("ac_grid_output_power",  String.valueOf(workInfo.getWholeGridOutputPower()) ));
	      nvp.add(new BasicNameValuePair("ac_grid_output_power_r",  String.valueOf(workInfo.getGridPowerR()) ));
	      nvp.add(new BasicNameValuePair("unit_serial",  String.valueOf(workInfo.getSerialno()) ));
	      
	      //getGridVoltageR <- grid voltage
	      
	      
	      //printWorkInfo(workInfo);
	      try {	    	  
	    	  stkFailedSent.push(nvp);	    	 
			  while (!stkFailedSent.empty()) {
				  nvp = stkFailedSent.pop();
				  if (!SendPost2WebSsite(nvp)) {
					  saveCurrentFailedToDisk( (Stack<NameValuePair[]> )stkFailedSent.clone());
					  break; 	    		
				  }
	    	  }
			  //if we sent everything let's delete the temp file
			  if (stkFailedSent.isEmpty()) {
				  try { 		
						Files.deleteIfExists(Paths.get("tempcollected.ser")); 
					} catch (Exception e1) {}							
			  }
	      } catch (Exception e) {	    	  
	    	  e.printStackTrace();	    	  
	      }
	      updateWatchDogFiles();
	      
	  } catch (Exception e) {
		  e.printStackTrace();
	  }
	      
  }

public static void updateWatchDogFiles() {
		
	try {
		OutputStreamWriter oos = new OutputStreamWriter(new FileOutputStream("updated-watch-dog.txt"));
		oos.write(new Date().toString());
		oos.write("\r\n");
		oos.write(getProcessId("UNKNOWN"));
		oos.close();

	} catch (IOException e) {
		
		e.printStackTrace();
	}

}

public static String getProcessId(final String fallback) {
    // Note: may fail in some JVM implementations
    // therefore fallback has to be provided

    // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
    final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
    final int index = jvmName.indexOf('@');

    if (index < 1) {
        // part before '@' empty (index = 0) / '@' not found (index = -1)
        return fallback;
    }

    try {
        return Long.toString(Long.parseLong(jvmName.substring(0, index)));
    } catch (NumberFormatException e) {
        // ignore
    }
    return fallback;
}

@SuppressWarnings("unchecked")
public static void loadCurrentFailedFromDisk() {	  	
	
	try {
		
		File f = new File("tempcollected.ser");
  		
  		if(f.exists() && !f.isDirectory()) { 
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream("tempcollected.ser"));		
			stkFailedSent=(Stack<List>)ois.readObject();
			ois.close();
			System.out.println("Found and loaded events that were not sent, overall number of events: " + stkFailedSent.size() );
  		}
		
	} catch (Exception e) {
		e.printStackTrace();
	}
	
}

public static void saveCurrentFailedToDisk( Stack<NameValuePair[]> stkTempFailedSend ) {	  	  
	
	try {
		
		try { 		
			Files.deleteIfExists(Paths.get("tempcollected.ser")); 
		} catch (Exception e1) {}		
		
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("tempcollected.ser"));
		oos.writeObject(stkTempFailedSend);
		oos.close();
		
	} catch (IOException e) {
		
		e.printStackTrace();
	}
	  
  }
  
  public static boolean SendPost2WebSsite(List<NameValuePair> nvp) {
	    int CONNECTION_TIMEOUT = 15;
	    boolean bSuccess = false;
	   	    

	    RequestConfig config = RequestConfig.custom().
	    		  setConnectTimeout(CONNECTION_TIMEOUT * 1000).
	    		  setConnectionRequestTimeout(CONNECTION_TIMEOUT * 1000).
	    		  setSocketTimeout(CONNECTION_TIMEOUT * 1000).build();
	    
	    
	   CloseableHttpClient client = HttpClientBuilder.create()
	    		  .setDefaultRequestConfig(config).build();
	    		 
	    	
	    BufferedReader br = null;

	    HttpPost  method = new HttpPost("YOUR WEBSITE HERE");	   
	    method.setHeader("http.useragent", "SolarPower");
	    
	    try {
			method.setEntity(new UrlEncodedFormEntity(nvp));
		} catch (UnsupportedEncodingException e1) {
		
			e1.printStackTrace();
		}
	    
	    try{
	      	   	    
	        CloseableHttpResponse response = client.execute(method);	      
	   
	        if (response.getStatusLine().getStatusCode() != 200) {
		        System.err.println("Error from bidtracker.info : " + response.getStatusLine().getStatusCode());
		    }
	        
	    	InputStream ips  = response.getEntity().getContent();
	        br = new BufferedReader(new InputStreamReader(ips,"UTF-8"));
	        String readLine;
	        
	        while(((readLine = br.readLine()) != null)) {
	        	System.err.println(readLine);        	    	        
	        }
	        
	        bSuccess=true;
	    } catch (Exception e) {
	      e.printStackTrace();
	    } finally {
	      method.releaseConnection();	     
	      if(br != null) try { br.close(); } catch (Exception fe) {}	    	      
	      return bSuccess;
	    }	 
	  }
}
