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

CREATE TABLE IF NOT EXISTS t_veh_handle_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '严重报警处置单代号（系统排号，卷上自带的不认）',
  stage int DEFAULT NULL COMMENT '当前步次 0..3（定级/知会/回告/归档），只由系统数过站得出',
  status int DEFAULT NULL COMMENT '处置单情形 0未起 1在办 2已封尾（与步次同源，建单即在办）',
  veh_code varchar(64) DEFAULT NULL COMMENT '车辆营运代号（定性四样之一）',
  alarm_at datetime DEFAULT NULL COMMENT '报警发生那一刻（定性四样之一，墙钟 yyyy-MM-dd HH:mm:ss）',
  measure_kind varchar(32) DEFAULT NULL COMMENT '当刻那个数量的是车速(speed)还是连续驾驶时长(duration)',
  alarm_value decimal(12,3) DEFAULT NULL COMMENT '报警当刻那个数（车速 km/h、连续驾驶时长小时，定性四样之一）',
  alarm_kind varchar(64) DEFAULT NULL COMMENT '报警属哪一类（定性四样之一；同车同类只容一张在办卷）',
  site_no varchar(64) DEFAULT NULL COMMENT '所知会业户代号（对 t_veh_ent.site_no）',
  notify_required int DEFAULT '1' COMMENT '知会站应齐几笔知会笔记（当地定，缺省一笔）',
  notify_count int DEFAULT '0' COMMENT '知会站已落几笔知会笔记',
  reply_count int DEFAULT '0' COMMENT '本趟回告站已落几笔回告（同站第二遍不另起行；退回知会清零，重走重攒）',
  reply_wait_hours int DEFAULT '0' COMMENT '回告站要守的钟点：知会齐后至少等几个钟点（当地定）',
  notify_done_at datetime DEFAULT NULL COMMENT '知会笔记齐、进回告站那一刻（钟点起算点）',
  reply_claim_stage int DEFAULT NULL COMMENT '最后一笔企业回告报文里自报的步次（只当来意留痕，绝不驱动站次）',
  review_opinion varchar(500) DEFAULT NULL COMMENT '复查意见（入档那一站只看这格落没落）',
  close_kind varchar(32) DEFAULT NULL COMMENT '凭什么收的口 REVIEW复查入档/OUTSIDE非运输机构督办/DEREGISTERED业户销户',
  content varchar(255) DEFAULT NULL COMMENT '一段一记（旧格留着，式样不动）',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次挪步动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_veh_handle_active (veh_code, alarm_kind, status, del_flag),
  KEY idx_veh_handle_status (status, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='严重报警处置单';

-- 卷上一段一段的按语：哪一站、哪一种话、落在哪一刻。退回站的旧段留痕不抹，同秒两条各记各的。
CREATE TABLE IF NOT EXISTS t_veh_handle_note (
  id bigint NOT NULL COMMENT '主键',
  flow_id bigint NOT NULL COMMENT '所属处置单 id',
  stage int DEFAULT NULL COMMENT '落段时系统数出来的当站（报文自报步次不进这格）',
  note_kind varchar(16) NOT NULL COMMENT '话种 OPEN定性/NOTIFY知会笔记/REPLY企业回告/REVIEW复查意见/CLOSE收口/MOVE挪步按语',
  note_text varchar(1000) DEFAULT NULL COMMENT '这一段写的话',
  claim_stage int DEFAULT NULL COMMENT '报文来意步次（仅 REPLY 段留痕，别处空）',
  note_at datetime DEFAULT NULL COMMENT '落在哪一刻（墙钟，同秒两段也各是各的行）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_veh_handle_note_flow (flow_id, note_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='严重报警处置单按语段';

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
-- ALTER TABLE t_veh_handle_flow ADD COLUMN veh_code varchar(64) DEFAULT NULL COMMENT '车辆营运代号（定性四样）' AFTER status;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN alarm_at datetime DEFAULT NULL COMMENT '报警发生那一刻' AFTER veh_code;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN measure_kind varchar(32) DEFAULT NULL COMMENT '量别 speed车速 duration连续驾驶时长' AFTER alarm_at;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN alarm_value decimal(12,3) DEFAULT NULL COMMENT '当刻那个数（km/h 或小时）' AFTER measure_kind;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN alarm_kind varchar(64) DEFAULT NULL COMMENT '报警类别' AFTER alarm_value;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN site_no varchar(64) DEFAULT NULL COMMENT '所知会业户代号' AFTER alarm_kind;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN notify_required int DEFAULT 1 COMMENT '知会站应齐几笔' AFTER site_no;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN notify_count int DEFAULT 0 COMMENT '已知会几笔' AFTER notify_required;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN reply_count int DEFAULT 0 COMMENT '本趟已回告几笔（退回清零）' AFTER notify_count;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN reply_wait_hours int DEFAULT 0 COMMENT '回告等待钟点（小时）' AFTER notify_count;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN notify_done_at datetime DEFAULT NULL COMMENT '知会齐那一刻（钟点起算点）' AFTER reply_wait_hours;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN reply_claim_stage int DEFAULT NULL COMMENT '回告报文来意步次（驱动不了站次）' AFTER notify_done_at;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN review_opinion varchar(500) DEFAULT NULL COMMENT '复查意见' AFTER reply_claim_stage;
-- ALTER TABLE t_veh_handle_flow ADD COLUMN close_kind varchar(32) DEFAULT NULL COMMENT '收口凭据 REVIEW/OUTSIDE/DEREGISTERED' AFTER review_opinion;
-- ALTER TABLE t_veh_handle_flow ADD KEY idx_veh_handle_active (veh_code, alarm_kind, status, del_flag);
-- 另需补建一张新表：t_veh_handle_note（照上面 CREATE 语句）。
