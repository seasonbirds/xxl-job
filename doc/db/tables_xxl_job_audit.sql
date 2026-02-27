#
# XXL-JOB Audit Log
# 操作日志审计表
#

## —————————————————————— operation log ——————————————————

CREATE TABLE `xxl_job_op_log`
(
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT,
    `module`        varchar(50)  NOT NULL COMMENT '操作模块：LOGIN-登录、USER-用户管理、JOB_GROUP-执行器管理、JOB_INFO-任务管理',
    `type`          varchar(50)  NOT NULL COMMENT '操作类型',
    `operator`      varchar(50)  NOT NULL COMMENT '操作人',
    `op_time`       datetime     NOT NULL COMMENT '操作时间',
    `op_ip`         varchar(50)  DEFAULT NULL COMMENT '操作IP地址',
    `target_id`     varchar(100) DEFAULT NULL COMMENT '被操作对象ID',
    `target_name`   varchar(255) DEFAULT NULL COMMENT '被操作对象名称',
    `content`       text COMMENT '操作内容描述',
    `job_group`     int(11)      DEFAULT NULL COMMENT '执行器ID（仅任务管理模块使用）',
    PRIMARY KEY (`id`),
    KEY `I_module` (`module`),
    KEY `I_type` (`type`),
    KEY `I_operator` (`operator`),
    KEY `I_op_time` (`op_time`),
    KEY `I_module_time` (`module`, `op_time`),
    KEY `I_job_group` (`job_group`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='操作日志表';
