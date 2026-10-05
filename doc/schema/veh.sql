-- veh 重点营运车辆动态监控与报警处置管理 -- schema (chen-023)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_023

CREATE TABLE IF NOT EXISTS t_veh_alarm_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '月度报警处置核收册码',
  row_no int DEFAULT NULL COMMENT '原报行次',
  item_code varchar(64) DEFAULT NULL COMMENT '被对上的营运车辆代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行仍未处置条数',
  hours_num decimal(12,2) DEFAULT NULL COMMENT '本行采集报警条数',
  unit_kind varchar(32) DEFAULT NULL COMMENT '计量单位口径（条）',
  status int DEFAULT NULL COMMENT '行落地情形 0未勾 1已收下 2已退回',
  layer_flag int DEFAULT '0' COMMENT '所在层 0现行 1往期',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报警处置核收行';

CREATE TABLE IF NOT EXISTS t_veh_alarm_batch (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '月度报警处置核收册码',
  source_kind int DEFAULT NULL COMMENT '来路 0机构自录 1市里转递',
  report_year int DEFAULT NULL COMMENT '报送年度',
  report_month int DEFAULT NULL COMMENT '报送月度',
  status int DEFAULT NULL COMMENT '册情形 0在核 1核讫 2空册 3已撤册',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='月度报警处置核收册';

CREATE TABLE IF NOT EXISTS t_veh_bind_page (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '车辆联网清单页代号（清单代号，系统排号，本子自带不认）',
  sign_no varchar(64) DEFAULT NULL COMMENT '随页签认码（系统排号，本子自带不认）',
  veh_code varchar(64) DEFAULT NULL COMMENT '车辆营运代号',
  node_no int DEFAULT NULL COMMENT '页所在层级序 0县域 1市域 2省域',
  site_id int DEFAULT NULL COMMENT '所属运输业户',
  site_no varchar(64) DEFAULT NULL COMMENT '所属业户代号',
  should_count int DEFAULT NULL COMMENT '应联项数',
  done_count int DEFAULT NULL COMMENT '已联项数',
  lack_count int DEFAULT NULL COMMENT '还差几项（系统轧出，只回显不录入）',
  content varchar(255) DEFAULT NULL COMMENT '随页交来的联网要件',
  status int DEFAULT NULL COMMENT '联网进展 0新录 1已联齐 2缺项',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_veh_bind_bill_no (bill_no),
  KEY idx_veh_bind_sign_no (sign_no),
  KEY idx_veh_bind_veh_code (veh_code),
  KEY idx_veh_bind_node (node_no, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆联网清单页';

CREATE TABLE IF NOT EXISTS t_veh_due_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '证照到期催办条目代号',
  due_at datetime DEFAULT NULL COMMENT '应开口那一日的止点时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '最可提前几日开口（按办事日数：国家放假、省停办不凑数）',
  content varchar(255) DEFAULT NULL COMMENT '事由与所对业户的记要',
  site_no varchar(64) DEFAULT NULL COMMENT '所对业户代号（对 t_veh_ent.site_no）',
  contact varchar(64) DEFAULT NULL COMMENT '联络人（空着几季没人补的直接判分不出）',
  assignee_no varchar(64) DEFAULT NULL COMMENT '递到谁名下（经办人代号，派出时钉上）',
  dispatched_at datetime DEFAULT NULL COMMENT '递出去那一刻（与状态翻转同进同退，缺一不算办过）',
  block_stage varchar(32) DEFAULT NULL COMMENT '分不出时卡在哪一环（止点/提前日/代号/业户/联络人/派单）',
  block_reason varchar(255) DEFAULT NULL COMMENT '分不出缘由（日后照缘由把这一批捞出来重排）',
  status int DEFAULT NULL COMMENT '条目情形 0搁着(待派) 1递出去了(已派出) 2递不动(分不出)',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_veh_due_pick (del_flag, status, due_at),
  KEY idx_veh_due_site (site_no),
  KEY idx_veh_due_assignee (assignee_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='证照到期催办条目';

CREATE TABLE IF NOT EXISTS t_veh_nonwork_day (
  id bigint NOT NULL COMMENT '主键',
  day date NOT NULL COMMENT '不办事那一日（整日不凑数，夜里跨过去的几个钟头不凑整日）',
  day_kind int NOT NULL COMMENT '来路 0国家放假 1省里下文停办',
  day_name varchar(64) DEFAULT NULL COMMENT '这一日的名义（国庆/春节/省停办通知…）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_veh_nonwork_day_kind (day, day_kind)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不办事日历（国家放假与省停办日，数办事日时逐日剔除）';

CREATE TABLE IF NOT EXISTS t_veh_due_handler (
  id bigint NOT NULL COMMENT '主键',
  handler_no varchar(64) DEFAULT NULL COMMENT '经办人代号',
  handler_name varchar(64) DEFAULT NULL COMMENT '经办人姓名',
  status int DEFAULT '0' COMMENT '经办人情形 0在位可接 1这一季休办',
  quota int DEFAULT NULL COMMENT '这一季手上定额（塞满即停，新桩当场判分不出）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_veh_due_handler_no (handler_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='证照催办经办人册';

CREATE TABLE IF NOT EXISTS t_veh_due_handler_kind (
  id bigint NOT NULL COMMENT '主键',
  handler_no varchar(64) DEFAULT NULL COMMENT '经办人代号',
  site_type varchar(32) DEFAULT NULL COMMENT '经得来的业户类别（对 t_veh_ent.site_type）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_veh_due_handler_kind (handler_no, site_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='经办人经得来的业户类别';

CREATE TABLE IF NOT EXISTS t_veh_ent (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '运输业户代号',
  site_name varchar(128) DEFAULT NULL COMMENT '业户全称',
  site_type varchar(32) DEFAULT NULL COMMENT '业户类别',
  road_name varchar(128) DEFAULT NULL COMMENT '属地那一路(省—市—县)',
  status int DEFAULT NULL COMMENT '名录情形 0在册 1已销户',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运输业户总名录';

-- 严重报警处置单：四站状态机（0定性 1知会 2回告 3归档），收过口即封尾只读。
-- 步次不存别处给的号，只由系统按已过站口数推；active_slot=1 即唯一的那个"在办口"，
-- 一辆车同一类报警同时只容一张没收口的卷（uk 兜并发双进）。
CREATE TABLE IF NOT EXISTS t_veh_handle_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '严重报警处置单代号',
  veh_code varchar(64) DEFAULT NULL COMMENT '车辆营运代号（定性四样之一）',
  alarm_kind varchar(32) DEFAULT NULL COMMENT '报警属哪一类 speed超速 fatigue疲劳驾驶（兼作卷的去重键）',
  alarm_at datetime DEFAULT NULL COMMENT '报警发生那一刻（定性四样之一）',
  metric_value decimal(14,4) DEFAULT NULL COMMENT '当刻那个数（车速km/h 或 连续驾驶时长小时）',
  site_no varchar(64) DEFAULT NULL COMMENT '所对运输业户代号（销户那两说按名录核验）',
  stage int DEFAULT '0' COMMENT '当前停在哪一站 0定性 1知会 2回告 3归档（系统按已过站口数落，报文步次不作数）',
  status int DEFAULT '0' COMMENT '情形 0未起 1在办 2已封尾（与步次同源，唯一落法）',
  notice_count int DEFAULT '0' COMMENT '知会那一站该齐的笔数',
  notice_done int DEFAULT '0' COMMENT '知会已记上的笔数',
  reply_due_hours int DEFAULT NULL COMMENT '回告要守满的钟点（当地定的几个钟头）',
  notice_at datetime DEFAULT NULL COMMENT '知会出站那一刻（回告钟点自这儿起算）',
  review_opinion varchar(500) DEFAULT NULL COMMENT '复查意见（入档站只看这一格落没落）',
  close_kind varchar(32) DEFAULT NULL COMMENT '收口名目 archive复查入档 notTransport不归运输机构督办 cancelled业户已销户',
  closed_at datetime DEFAULT NULL COMMENT '收口那一刻',
  active_slot int DEFAULT '1' COMMENT '在办口 1这张卷还没收口；收口置NULL（唯一索引里多个NULL不相顶，同车同类只容一个1）',
  visit_no int DEFAULT '1' COMMENT '当前停着的这一站是第几回到（同站第二遍照旧段，退站重走另攒新段）',
  claimed_stage int DEFAULT NULL COMMENT '报文捎来的步次（只当来意留痕，入库与回读的步次仍照系统数的）',
  content varchar(255) DEFAULT NULL COMMENT '一段一记（旧式样保留，按语新卷另挂 t_veh_handle_note）',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次挪步动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_veh_handle_open (veh_code, alarm_kind, active_slot),
  KEY idx_veh_handle_stage (stage, status, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='严重报警处置单';

-- 卷上挂的按语：一段一行，哪一站写了什么话、落在哪一刻；
-- 同站第二遍报上来只改头一行（seg_ordinal 不动、不另起新行），退站重写压到卷末另攒序号。
CREATE TABLE IF NOT EXISTS t_veh_handle_note (
  id bigint NOT NULL COMMENT '主键',
  flow_id bigint NOT NULL COMMENT '所属处置单',
  stage int NOT NULL COMMENT '落在哪一站 0定性 1知会 2回告 3归档',
  visit_no int NOT NULL DEFAULT 1 COMMENT '这一站的第几回（退站重走+1，同站第二遍不+1）',
  source_no varchar(64) NOT NULL DEFAULT 'manual' COMMENT '来路号（同一来路第二遍并头一行；同一秒两个来路各起一行）',
  seg_ordinal int NOT NULL COMMENT '段序（从入档往回数当班簿几笔对的就是它）',
  note_text varchar(1000) DEFAULT NULL COMMENT '这一站写下的话',
  written_at datetime NOT NULL COMMENT '落在哪一刻（同秒两条来路各记各的，不并一条）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_veh_handle_note_pos (flow_id, stage, visit_no, source_no),
  KEY idx_veh_handle_note_flow (flow_id, seg_ordinal)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='严重报警处置单按语卷';

CREATE TABLE IF NOT EXISTS t_veh_move_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '移交执法核签单',
  node_no int DEFAULT NULL COMMENT '当前所在档口 0..2（初核/复核/核准）',
  sign_mode int DEFAULT NULL COMMENT '同档算齐办法 0一名即可 1两名须同判',
  need_count int DEFAULT NULL COMMENT '本档应落人数',
  sign_count int DEFAULT NULL COMMENT '本档已落印数',
  status int DEFAULT NULL COMMENT '核签情形 0在核 1已办结 2已挪回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='移交执法核签单';

CREATE TABLE IF NOT EXISTS t_veh_run_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '运行界次判定线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '界名',
  measure_kind varchar(32) DEFAULT NULL COMMENT '所量的是车速还是连续驾驶时长',
  th1_max decimal(12,3) DEFAULT NULL COMMENT '起算数值(界册那一头的单位)',
  th2_max decimal(12,3) DEFAULT NULL COMMENT '够线数值(界册那一头的单位)',
  th3_max decimal(12,3) DEFAULT NULL COMMENT '封顶数值(界册那一头的单位)',
  unit_kind varchar(32) DEFAULT NULL COMMENT '界册那一头的单位',
  eff_start datetime DEFAULT NULL COMMENT '起用那一日',
  eff_end datetime DEFAULT NULL COMMENT '交班那一日(不含)',
  priority int DEFAULT NULL COMMENT '交班次序(数值大的先说话)',
  status int DEFAULT NULL COMMENT '界的情形 0在位 1已停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运行界次判定线';

CREATE TABLE IF NOT EXISTS t_veh_run_line_verdict (
  id bigint NOT NULL COMMENT '主键',
  measure_kind varchar(32) DEFAULT NULL COMMENT '所量类别 speed车速 duration时长',
  raw_value decimal(14,4) DEFAULT NULL COMMENT '车上报回来的原值（车速m/s、时长分钟）',
  converted_value decimal(12,3) DEFAULT NULL COMMENT '折到界册那一头之后的数（取数与比数同一把舍法）',
  ask_at datetime DEFAULT NULL COMMENT '问话的那一刻（补判旧账传旧账自己的那一刻）',
  rule_code varchar(64) DEFAULT NULL COMMENT '说话的界次代号（无可用界/超量程留空）',
  rule_name varchar(128) DEFAULT NULL COMMENT '说话的界名（留痕当时快照）',
  eff_start datetime DEFAULT NULL COMMENT '说话界的起用那一日（快照）',
  th1_max decimal(12,3) DEFAULT NULL COMMENT '起算数值快照（裁决当时，防日后改界）',
  th2_max decimal(12,3) DEFAULT NULL COMMENT '够线数值快照',
  th3_max decimal(12,3) DEFAULT NULL COMMENT '封顶数值快照',
  unit_kind varchar(32) DEFAULT NULL COMMENT '界册单位快照',
  priority int DEFAULT NULL COMMENT '交班次序快照',
  level int DEFAULT NULL COMMENT '层序 1提示 2严管 3立案；无可用界/未到起算/超量程为空',
  level_text varchar(32) DEFAULT NULL COMMENT '结论栏（提示/严管/立案/未到起算/无可用界/超出量程）',
  archived_at datetime DEFAULT NULL COMMENT '判完归档在册的时刻',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  PRIMARY KEY (id),
  KEY idx_rlv_ask (measure_kind, ask_at),
  KEY idx_rlv_rule (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运行界次裁决留痕（判完即归档，追溯以此份为准）';

-- 初始档案数据：t_veh_ent 预置两条名录，id=0 在册、id=1 已销户，
-- 供界次判定与联网清单挂接时按在册口径取用。
INSERT IGNORE INTO t_veh_ent (id, site_no, site_name, site_type, road_name, status, del_flag, create_by, create_time)
VALUES (0, 'YH00', '宁州顺达客运有限公司（道路旅客运输业户，在册）', '道路旅客运输业户', '宁川省—宁州市—临江县', 0, 0, 'seed', NOW()),
       (1, 'YH01', '临江通达危险货物运输站（停运后销户）', '危险货物运输业户', '宁川省—宁州市—临江县', 1, 0, 'seed', NOW());

-- 旧库补列（t_veh_bind_page 为旧版表结构时逐条执行；新库由上面的 CREATE TABLE 带出，勿重复执行）：
-- ALTER TABLE t_veh_bind_page ADD COLUMN sign_no varchar(64) DEFAULT NULL COMMENT '随页签认码（系统排号）' AFTER bill_no;
-- ALTER TABLE t_veh_bind_page ADD COLUMN veh_code varchar(64) DEFAULT NULL COMMENT '车辆营运代号' AFTER sign_no;
-- ALTER TABLE t_veh_bind_page ADD UNIQUE KEY uk_veh_bind_bill_no (bill_no);
-- ALTER TABLE t_veh_bind_page ADD KEY idx_veh_bind_sign_no (sign_no);
-- ALTER TABLE t_veh_bind_page ADD KEY idx_veh_bind_veh_code (veh_code);
-- ALTER TABLE t_veh_bind_page ADD KEY idx_veh_bind_node (node_no, del_flag);

-- 旧库补列（t_veh_due_task 为旧版表结构时逐条执行；新库由上面的 CREATE TABLE 带出，勿重复执行）：
-- ALTER TABLE t_veh_due_task ADD COLUMN site_no varchar(64) DEFAULT NULL COMMENT '所对业户代号' AFTER content;
-- ALTER TABLE t_veh_due_task ADD COLUMN contact varchar(64) DEFAULT NULL COMMENT '联络人' AFTER site_no;
-- ALTER TABLE t_veh_due_task ADD COLUMN assignee_no varchar(64) DEFAULT NULL COMMENT '递到谁名下' AFTER contact;
-- ALTER TABLE t_veh_due_task ADD COLUMN dispatched_at datetime DEFAULT NULL COMMENT '递出去那一刻' AFTER assignee_no;
-- ALTER TABLE t_veh_due_task ADD COLUMN block_stage varchar(32) DEFAULT NULL COMMENT '分不出时卡在哪一环' AFTER dispatched_at;
-- ALTER TABLE t_veh_due_task ADD COLUMN block_reason varchar(255) DEFAULT NULL COMMENT '分不出缘由' AFTER block_stage;
-- ALTER TABLE t_veh_due_task ADD KEY idx_veh_due_pick (del_flag, status, due_at);
-- ALTER TABLE t_veh_due_task ADD KEY idx_veh_due_site (site_no);
-- ALTER TABLE t_veh_due_task ADD KEY idx_veh_due_assignee (assignee_no);
-- 另需补建三张新表：t_veh_nonwork_day / t_veh_due_handler / t_veh_due_handler_kind（照上面 CREATE 语句）。


-- 旧库补列（t_veh_handle_flow 为旧版表结构时逐条执行；新库由上面的 CREATE TABLE 带出，勿重复执行）：
-- ALTER TABLE t_veh_handle_flow ADD COLUMN veh_code varchar(64) DEFAULT NULL COMMENT '车辆营运代号' AFTER biz_no;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN alarm_kind varchar(32) DEFAULT NULL COMMENT '报警类别 speed/fatigue' AFTER veh_code;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN alarm_at datetime DEFAULT NULL COMMENT '报警发生那一刻' AFTER alarm_kind;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN metric_value decimal(14,4) DEFAULT NULL COMMENT '当刻那个数' AFTER alarm_at;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN site_no varchar(64) DEFAULT NULL COMMENT '所对业户代号' AFTER metric_value;
-- ALTER TABLE t_veh_handle_flow MODIFY COLUMN stage int DEFAULT 0 COMMENT '当前停在哪一站（系统数已过站口）';
-- ALTER TABLE t_veh_handle_flow MODIFY COLUMN status int DEFAULT 0 COMMENT '情形 0未起 1在办 2已封尾';
-- ALTER TABLE t_veh_handle_flow ADD COLUMN notice_count int DEFAULT 0 COMMENT '知会该齐的笔数' AFTER status;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN notice_done int DEFAULT 0 COMMENT '知会已记的笔数' AFTER notice_count;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN reply_due_hours int DEFAULT NULL COMMENT '回告要守满的钟点' AFTER notice_done;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN notice_at datetime DEFAULT NULL COMMENT '知会出站那一刻' AFTER reply_due_hours;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN review_opinion varchar(500) DEFAULT NULL COMMENT '复查意见' AFTER notice_at;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN close_kind varchar(32) DEFAULT NULL COMMENT '收口名目' AFTER review_opinion;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN closed_at datetime DEFAULT NULL COMMENT '收口那一刻' AFTER close_kind;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN active_slot int DEFAULT 1 COMMENT '在办口（收口置NULL）' AFTER closed_at;
-- UPDATE t_veh_handle_flow SET active_slot = NULL WHERE status = 2;
-- ALTER TABLE t_veh_handle_flow ADD UNIQUE KEY uk_veh_handle_open (veh_code, alarm_kind, active_slot);
-- ALTER TABLE t_veh_handle_flow ADD KEY idx_veh_handle_stage (stage, status, del_flag);
-- 另需补建新表 t_veh_handle_note（照上面 CREATE 语句，按语卷）。
-- ALTER TABLE t_veh_handle_flow ADD COLUMN visit_no int DEFAULT 1 COMMENT '当前站第几回到' AFTER active_slot;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN claimed_stage int DEFAULT NULL COMMENT '报文捎来的步次（只作来意留痕）' AFTER visit_no;
-- ALTER TABLE t_veh_handle_note ADD COLUMN source_no varchar(64) NOT NULL DEFAULT 'manual' COMMENT '来路号' AFTER visit_no;
