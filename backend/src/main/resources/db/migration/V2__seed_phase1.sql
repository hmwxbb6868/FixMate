-- FixMate AI 阶段 1 种子数据
-- 依据：冻结规则 18.3 + ADR-0001（格力→西门子）+ 基线 V1.0.2
-- 范围：品牌、型号（39 条）、服务项目、价格规则、高风险规则
-- 说明：staff_user 与演示工单属于阶段 2 管理端内容，此处不种；AI 相关表在阶段 3 追加。

-- ============ 品牌 ============
INSERT INTO appliance_brand (code, name, status, sort_order, created_at, updated_at) VALUES
  ('HAIER',   '海尔',   'ENABLED',  1, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  ('MIDEA',   '美的',   'ENABLED',  2, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  ('SIEMENS', '西门子', 'ENABLED',  3, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  ('GREE',    '格力',   'DISABLED', 4, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3));

-- ============ 型号（39 条）============
-- COVERED：海尔 KFR-35GW/JSD-AP、美的 MR-527WUF3PZE、西门子三款（共用一份官方说明书）
-- 其余 PENDING_SOURCE；格力全部 DISABLED
INSERT INTO appliance_model
  (brand_id, appliance_type, model_code, normalized_model_code, display_name,
   official_product_url, verified_at, source_status, coverage_status, status, created_at, updated_at)
SELECT b.id, v.appliance_type, v.model_code, v.norm, v.display,
       NULLIF(v.url, ''), UTC_TIMESTAMP(3), v.src, v.cov, v.st, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
FROM (
  -- 海尔 12
  SELECT 'HAIER' AS brand_code, 'AIR_CONDITIONER' AS appliance_type, 'KFR-35GW/JSD-AP' AS model_code,
         'KFR35GWJSDAP' AS norm, '海尔 空调 KFR-35GW/JSD-AP' AS display,
         'https://www.haier.com/air_conditioners/20261010_295369.shtml' AS url, 'VERIFIED' AS src, 'COVERED' AS cov, 'ENABLED' AS st
  UNION ALL SELECT 'HAIER','AIR_CONDITIONER','KFR-26GW/Z200-1','KFR26GWZ2001','海尔 空调 KFR-26GW/Z200-1','https://www.haier.com/air_conditioners/20260917_295029.shtml','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'HAIER','AIR_CONDITIONER','KFR-35GW/E60-AP','KFR35GWE60AP','海尔 空调 KFR-35GW/E60-AP','https://www.haier.com/air_conditioners/20260916_295001.shtml','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'HAIER','REFRIGERATOR','BCD-607WGHTD1CJDU1','BCD607WGHTD1CJDU1','海尔 冰箱 BCD-607WGHTD1CJDU1','https://www.haier.com/cooling/20261023_295877.shtml','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'HAIER','REFRIGERATOR','BCD-617WGHFDBGVWU1','BCD617WGHFDBGVWU1','海尔 冰箱 BCD-617WGHFDBGVWU1','https://www.haier.com/cooling/20261023_295880.shtml','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'HAIER','REFRIGERATOR','BCD-607WGHFD1CJDU1','BCD607WGHFD1CJDU1','海尔 冰箱 BCD-607WGHFD1CJDU1','https://www.haier.com/cooling/20261023_295869.shtml','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'HAIER','WASHING_MACHINE','XQS100-BZ6VE1','XQS100BZ6VE1','海尔 洗衣机 XQS100-BZ6VE1','https://www.haier.com/laundry/20261027_295931.shtml','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'HAIER','WASHING_MACHINE','XQG30-GH68DHU1','XQG30GH68DHU1','海尔 洗衣机 XQG30-GH68DHU1','https://www.haier.com/laundry/20261022_295808.shtml','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'HAIER','WASHING_MACHINE','XQG100-BLE7AD1WU1','XQG100BLE7AD1WU1','海尔 洗衣机 XQG100-BLE7AD1WU1','https://www.haier.com/laundry/20261012_295397.shtml','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'HAIER','AIR_SOURCE_HEAT_PUMP','KD150-ZE7U1','KD150ZE7U1','海尔 空气能 KD150-ZE7U1','https://www.haier.com/water_heaters/kqnrsq/20260708_292947.shtml','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'HAIER','AIR_SOURCE_HEAT_PUMP','KF300-IF3AU1','KF300IF3AU1','海尔 空气能 KF300-IF3AU1','https://www.haier.com/water_heaters/kqnrsq/20260708_292938.shtml','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'HAIER','AIR_SOURCE_HEAT_PUMP','KF75/200-QFE7AU1','KF75200QFE7AU1','海尔 空气能 KF75/200-QFE7AU1','https://www.haier.com/water_heaters/kqnrsq/20260707_292875.shtml','VERIFIED','PENDING_SOURCE','ENABLED'
  -- 美的 12
  UNION ALL SELECT 'MIDEA','AIR_CONDITIONER','KFR-35GW/KS2P','KFR35GWKS2P','美的 空调 KFR-35GW/KS2P','https://www.midea.cn/#/detail?skuId=124729','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'MIDEA','AIR_CONDITIONER','KFR-72LW/MXC3','KFR72LWMXC3','美的 空调 KFR-72LW/MXC3','https://www.midea.cn/#/detail?skuId=204909','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'MIDEA','AIR_CONDITIONER','KFR-72LW/MXC3P','KFR72LWMXC3P','美的 空调 KFR-72LW/MXC3P','https://www.midea.cn/#/detail?skuId=204908','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'MIDEA','REFRIGERATOR','MR-469WUFPZE','MR469WUFPZE','美的 冰箱 MR-469WUFPZE','https://www.midea.cn/#/detail?skuId=206450','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'MIDEA','REFRIGERATOR','MR-527WUF3PZE','MR527WUF3PZE','美的 冰箱 MR-527WUF3PZE','https://www.midea.cn/#/detail?skuId=194875','VERIFIED','COVERED','ENABLED'
  UNION ALL SELECT 'MIDEA','REFRIGERATOR','MR-508WUSPZEA','MR508WUSPZEA','美的 冰箱 MR-508WUSPZEA','https://www.midea.cn/#/detail?skuId=197375','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'MIDEA','WASHING_MACHINE','MB12F1','MB12F1','美的 洗衣机 MB12F1','https://www.midea.cn/#/detail?skuId=206425','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'MIDEA','WASHING_MACHINE','MG11GGEX','MG11GGEX','美的 洗衣机 MG11GGEX','https://www.midea.cn/#/detail?skuId=124229','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'MIDEA','WASHING_MACHINE','MDH12F10','MDH12F10','美的 洗衣机 MDH12F10','https://www.midea.cn/#/detail?skuId=204390','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'MIDEA','AIR_SOURCE_HEAT_PUMP','MHSR180N8-Z1','MHSR180N8Z1','美的 空气能 MHSR180N8-Z1','https://www.midea.cn/#/detail?skuId=14061','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'MIDEA','AIR_SOURCE_HEAT_PUMP','MHSR140N8-Z1','MHSR140N8Z1','美的 空气能 MHSR140N8-Z1','https://www.midea.cn/#/detail?skuId=14062','VERIFIED','PENDING_SOURCE','ENABLED'
  UNION ALL SELECT 'MIDEA','AIR_SOURCE_HEAT_PUMP','MHSR180SN8-Z1','MHSR180SN8Z1','美的 空气能 MHSR180SN8-Z1','https://www.midea.cn/#/detail?skuId=14059','VERIFIED','PENDING_SOURCE','ENABLED'
  -- 格力 12（DISABLED，保留主数据）
  UNION ALL SELECT 'GREE','AIR_CONDITIONER','KFR-35GW/NhGt1BGj','KFR35GWNHGT1BGJ','格力 空调 KFR-35GW/NhGt1BGj','https://mall.gree.com/#/goodsDetail?productNo=1003843367&skuNo=1001210304&shopId=2000001394','VERIFIED','PENDING_SOURCE','DISABLED'
  UNION ALL SELECT 'GREE','AIR_CONDITIONER','KFR-35GW/NhGs1BGt','KFR35GWNHGS1BGT','格力 空调 KFR-35GW/NhGs1BGt','https://mall.gree.com/#/goodsDetail?productNo=1003843358&skuNo=1001210292&shopId=2000001394','VERIFIED','PENDING_SOURCE','DISABLED'
  UNION ALL SELECT 'GREE','AIR_CONDITIONER','KFR-26GW/NhGt1BGj','KFR26GWNHGT1BGJ','格力 空调 KFR-26GW/NhGt1BGj','https://mall.gree.com/#/goodsDetail?productNo=1003843366&skuNo=1001210303&shopId=2000001394','VERIFIED','PENDING_SOURCE','DISABLED'
  UNION ALL SELECT 'GREE','REFRIGERATOR','BCD-506WGNAK3SPDGA','BCD506WGNAK3SPDGA','格力 冰箱 BCD-506WGNAK3SPDGA','https://mall.gree.com/#/goodsDetail?productNo=1003928690&skuNo=1001294279&shopId=2000001394','VERIFIED','PENDING_SOURCE','DISABLED'
  UNION ALL SELECT 'GREE','REFRIGERATOR','BCD-290WGJAG1BPDGB','BCD290WGJAG1BPDGB','格力 冰箱 BCD-290WGJAG1BPDGB','https://mall.gree.com/#/goodsDetail?productNo=1003928534&skuNo=1001294097&shopId=2000001394','VERIFIED','PENDING_SOURCE','DISABLED'
  UNION ALL SELECT 'GREE','REFRIGERATOR','BCD-428WGLAG1FPDCL','BCD428WGLAG1FPDCL','格力 冰箱 BCD-428WGLAG1FPDCL','https://mall.gree.com/#/goodsDetail?productNo=1003843921&skuNo=1001210921&shopId=2000001394','VERIFIED','PENDING_SOURCE','DISABLED'
  UNION ALL SELECT 'GREE','WASHING_MACHINE','XQG100-DWD1401Qa1','XQG100DWD1401QA1','格力 洗衣机 XQG100-DWD1401Qa1','https://mall.gree.com/#/goodsDetail?productNo=1003928618&skuNo=1001294195&shopId=2000001394','VERIFIED','PENDING_SOURCE','DISABLED'
  UNION ALL SELECT 'GREE','WASHING_MACHINE','XQG100-D1401Qa1','XQG100D1401QA1','格力 洗衣机 XQG100-D1401Qa1','https://mall.gree.com/#/goodsDetail?productNo=1003843958&skuNo=1001210968&shopId=2000001394','VERIFIED','PENDING_SOURCE','DISABLED'
  UNION ALL SELECT 'GREE','WASHING_MACHINE','XQG100-RBKD1401Ad1','XQG100RBKD1401AD1','格力 洗衣机 XQG100-RBKD1401Ad1','https://mall.gree.com/#/goodsDetail?productNo=1003843475&skuNo=1001210422&shopId=2000001394','VERIFIED','PENDING_SOURCE','DISABLED'
  UNION ALL SELECT 'GREE','AIR_SOURCE_HEAT_PUMP','SXT200LCJW/Aa-1','SXT200LCJWAA1','格力 空气能 SXT200LCJW/Aa-1','https://mall.gree.com/#/goodsDetail?productNo=1003844026&skuNo=1001211038&shopId=2000001394','VERIFIED','PENDING_SOURCE','DISABLED'
  UNION ALL SELECT 'GREE','AIR_SOURCE_HEAT_PUMP','SXTD300LCJW/R-1q','SXTD300LCJWR1Q','格力 空气能 SXTD300LCJW/R-1q','https://mall.gree.com/#/goodsDetail?productNo=1003843703&skuNo=1001210657&shopId=2000001394','VERIFIED','PENDING_SOURCE','DISABLED'
  UNION ALL SELECT 'GREE','AIR_SOURCE_HEAT_PUMP','SXT300LCJW/Ae1-1','SXT300LCJWAE11','格力 空气能 SXT300LCJW/Ae1-1','https://mall.gree.com/#/goodsDetail?productNo=1003843702&skuNo=1001210656&shopId=2000001394','VERIFIED','PENDING_SOURCE','DISABLED'
  -- 西门子 3（COVERED，共用说明书 9002060091_B.pdf；来源为西门子家电小程序）
  UNION ALL SELECT 'SIEMENS','WASHING_MACHINE','XQG100-WS25XLY08W','XQG100WS25XLY08W','西门子 洗衣干衣机 XQG100-WS25XLY08W','','VERIFIED','COVERED','ENABLED'
  UNION ALL SELECT 'SIEMENS','WASHING_MACHINE','XQG100-WS25XLY18W','XQG100WS25XLY18W','西门子 洗衣干衣机 XQG100-WS25XLY18W','','VERIFIED','COVERED','ENABLED'
  UNION ALL SELECT 'SIEMENS','WASHING_MACHINE','XQG100-WS25XLY88W','XQG100WS25XLY88W','西门子 洗衣干衣机 XQG100-WS25XLY88W','','VERIFIED','COVERED','ENABLED'
) v
JOIN appliance_brand b ON b.code = v.brand_code;

-- ============ 服务项目（10 个）============
INSERT INTO service_item (code, appliance_type, name, description, status, created_at, updated_at) VALUES
  ('AC_INSPECT',      'AIR_CONDITIONER',      '空调上门检测', '基础检测，判断故障范围', 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  ('AC_REFRIGERANT',  'AIR_CONDITIONER',      '制冷剂补充',   '按压力检测后补充制冷剂', 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  ('AC_CLEAN',        'AIR_CONDITIONER',      '空调清洗',     '室内外机清洗', 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  ('REF_INSPECT',     'REFRIGERATOR',         '冰箱上门检测', '基础检测与温度测量', 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  ('REF_PART',        'REFRIGERATOR',         '冰箱配件更换', '门封、温控器、风扇等配件', 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  ('WM_INSPECT',      'WASHING_MACHINE',      '洗衣机上门检测', '基础检测', 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  ('WM_UNCLOG',       'WASHING_MACHINE',      '排水疏通',     '排水泵与管路疏通', 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  ('WM_PART',         'WASHING_MACHINE',      '洗衣机配件更换', '排水泵、皮带等配件', 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  ('HP_INSPECT',      'AIR_SOURCE_HEAT_PUMP', '空气能上门检测', '基础检测', 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  ('HP_PART',         'AIR_SOURCE_HEAT_PUMP', '空气能配件更换', '主板、传感器等配件', 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3));

-- ============ 价格规则（24 条）============
-- 区间价格：min_price/max_price 表示可人工选择的范围；fixed_price 非空时表示固定价
INSERT INTO pricing_rule
  (clause_no, rule_version, service_item_id, fee_type, title, description, unit,
   fixed_price, min_price, max_price, refund_policy, effective_from, effective_to, status, version, created_at, updated_at)
SELECT v.clause_no, 'V1', s.id, v.fee_type, v.title, v.description, v.unit,
       v.fixed_price, v.min_price, v.max_price, v.refund_policy,
       '2026-01-01 00:00:00.000', NULL, 'ENABLED', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
FROM (
  SELECT 'P-AC-VISIT' AS clause_no, 'AC_INSPECT' AS item_code, 'VISIT' AS fee_type, '空调上门费' AS title,
         '上门交通费用，检测后可抵扣维修费' AS description, '次' AS unit, 50.00 AS fixed_price, NULL AS min_price, NULL AS max_price,
         '检测后维修可抵扣' AS refund_policy
  UNION ALL SELECT 'P-AC-INSPECT','AC_INSPECT','INSPECTION','空调检测费','基础检测费用','次',80.00,NULL,NULL,'维修可抵扣'
  UNION ALL SELECT 'P-AC-REF','AC_REFRIGERANT','LABOR','制冷剂补充人工','补充制冷剂的人工费用','次',NULL,180.00,420.00,NULL
  UNION ALL SELECT 'P-AC-REF-MAT','AC_REFRIGERANT','MATERIAL','制冷剂材料','按实际用量计价','千克',NULL,60.00,160.00,NULL
  UNION ALL SELECT 'P-AC-CLEAN','AC_CLEAN','LABOR','空调清洗人工','室内外机清洗','台',NULL,120.00,260.00,NULL
  UNION ALL SELECT 'P-AC-PART','AC_INSPECT','PART','空调配件','按实际配件计价','件',NULL,50.00,1200.00,NULL
  UNION ALL SELECT 'P-REF-VISIT','REF_INSPECT','VISIT','冰箱上门费','上门交通费用','次',50.00,NULL,NULL,'检测后维修可抵扣'
  UNION ALL SELECT 'P-REF-INSPECT','REF_INSPECT','INSPECTION','冰箱检测费','基础检测与测温','次',80.00,NULL,NULL,'维修可抵扣'
  UNION ALL SELECT 'P-REF-PART-SEAL','REF_PART','PART','门封条','门封条更换','条',NULL,120.00,380.00,NULL
  UNION ALL SELECT 'P-REF-PART-FAN','REF_PART','PART','风扇电机','冷冻室风扇电机更换','个',NULL,160.00,420.00,NULL
  UNION ALL SELECT 'P-REF-PART-SENSOR','REF_PART','PART','温度传感器','冷藏/冷冻传感器更换','个',NULL,90.00,220.00,NULL
  UNION ALL SELECT 'P-REF-LABOR','REF_PART','LABOR','冰箱维修人工','配件更换人工费','次',NULL,80.00,260.00,NULL
  UNION ALL SELECT 'P-WM-VISIT','WM_INSPECT','VISIT','洗衣机上门费','上门交通费用','次',50.00,NULL,NULL,'检测后维修可抵扣'
  UNION ALL SELECT 'P-WM-INSPECT','WM_INSPECT','INSPECTION','洗衣机检测费','基础检测','次',80.00,NULL,NULL,'维修可抵扣'
  UNION ALL SELECT 'P-WM-UNCLOG','WM_UNCLOG','LABOR','排水疏通人工','排水泵与管路疏通','次',NULL,100.00,260.00,NULL
  UNION ALL SELECT 'P-WM-PART-PUMP','WM_PART','PART','排水泵','排水泵更换','个',NULL,150.00,380.00,NULL
  UNION ALL SELECT 'P-WM-PART-BELT','WM_PART','PART','传动皮带','皮带更换','条',NULL,60.00,160.00,NULL
  UNION ALL SELECT 'P-WM-LABOR','WM_PART','LABOR','洗衣机维修人工','配件更换人工费','次',NULL,80.00,240.00,NULL
  UNION ALL SELECT 'P-HP-VISIT','HP_INSPECT','VISIT','空气能上门费','上门交通费用','次',60.00,NULL,NULL,'检测后维修可抵扣'
  UNION ALL SELECT 'P-HP-INSPECT','HP_INSPECT','INSPECTION','空气能检测费','基础检测','次',100.00,NULL,NULL,'维修可抵扣'
  UNION ALL SELECT 'P-HP-PART-BOARD','HP_PART','PART','主控板','主控板更换','块',NULL,600.00,1800.00,NULL
  UNION ALL SELECT 'P-HP-PART-SENSOR','HP_PART','PART','温度传感器','温度传感器更换','个',NULL,90.00,260.00,NULL
  UNION ALL SELECT 'P-HP-LABOR','HP_PART','LABOR','空气能维修人工','配件更换人工费','次',NULL,120.00,400.00,NULL
  UNION ALL SELECT 'P-OTHER','AC_INSPECT','OTHER','其他费用','需填写人工审核依据','项',NULL,0.00,500.00,'需人工审核'
) v
JOIN service_item s ON s.code = v.item_code;

-- ============ 高风险规则（5 类）============
-- 阶段 1 五类规则同属最高紧急等级，不做严重度排序
INSERT INTO safety_rule (code, title, template_text, keywords, appliance_types, status, created_at, updated_at) VALUES
  ('ELECTRIC_LEAKAGE', '漏电风险',
   '已停止接触设备，请勿再次触碰，维修人员将尽快与您联系。',
   JSON_ARRAY('漏电','触电','麻手','带电','外壳带电','电人'), NULL, 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  ('SMOKE_FIRE', '冒烟着火风险',
   '请立即停止使用并远离设备，必要时联系消防，维修人员将尽快与您联系。',
   JSON_ARRAY('冒烟','焦味','烧焦','明火','着火','起火','冒火'), NULL, 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  ('GAS_LEAK', '燃气泄漏风险',
   '请勿开关电器和使用明火，立即开窗通风并撤离，联系燃气应急处理。',
   JSON_ARRAY('燃气味','燃气泄漏','煤气味','天然气味','漏气'), NULL, 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  ('REFRIGERANT_LEAK', '疑似制冷剂泄漏',
   '请停止使用并保持通风，远离泄漏点，维修人员将尽快与您联系。',
   JSON_ARRAY('制冷剂泄漏','氟利昂泄漏','雪种泄漏','制冷剂味道'), NULL, 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  ('SEVERE_WATER_INGRESS', '严重进水风险',
   '请避免接触带电设备，切断相关电源前不要触碰设备，维修人员将尽快与您联系。',
   JSON_ARRAY('泡水','进水','漏水严重','灌水','淹水'), NULL, 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3));
