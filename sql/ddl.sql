-- 创建数据库 mcmsuser
CREATE DATABASE IF NOT EXISTS mcmsuser
DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE mcmsuser;

-- 1、薪资配置表 tbl_commision_account
DROP TABLE IF EXISTS tbl_commision_account;
CREATE TABLE tbl_commision_account (
  id            INT PRIMARY KEY AUTO_INCREMENT,
  config_type   VARCHAR(20)  COMMENT 'agent=代理制 / labor=劳动制',
  base_salary   DECIMAL(10,2) COMMENT '基础工资/底薪',
  exhibition_fee DECIMAL(10,2) COMMENT '展业服务费',
  social_subsidy DECIMAL(10,2) COMMENT '社保补贴',
  post_fee      DECIMAL(10,2) COMMENT '岗位服务费',
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO tbl_commision_account(config_type,base_salary,exhibition_fee,social_subsidy,post_fee)
VALUES ('agent',3000,1200,500,800),
       ('labor',2800,1200,500,800);

-- 2、网点表 tbl_salesnet
DROP TABLE IF EXISTS tbl_salesnet;
CREATE TABLE tbl_salesnet (
  salesnet_id    INT PRIMARY KEY AUTO_INCREMENT,
  salesnet_no    VARCHAR(20)  NOT NULL UNIQUE COMMENT '网点编号',
  salesnet_name  VARCHAR(50)  NOT NULL COMMENT '网点名称',
  bank_name      VARCHAR(30)  COMMENT '所属银行',
  city           VARCHAR(20)  COMMENT '城市',
  address        VARCHAR(100) COMMENT '地址',
  status         TINYINT DEFAULT 1 COMMENT '1=营业 0=停业',
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO tbl_salesnet(salesnet_no,salesnet_name,bank_name,city,address) VALUES
('O001','上海火车站支行','交通银行','上海','静安区秣陵路300号'),
('O002','南京西路支行','交通银行','上海','静安区南京西路1266号'),
('O003','陆家嘴支行','招商银行','上海','浦东新区陆家嘴环路1000号'),
('O004','徐家汇支行','工商银行','上海','徐汇区肇嘉浜路1000号'),
('O005','杭州武林支行','交通银行','杭州','拱墅区武林广场1号'),
('O006','苏州工业园支行','招商银行','苏州','工业园区星湖街328号');

-- 3、人员表 tbl_channrole（渠道代理人）
DROP TABLE IF EXISTS tbl_channrole;
CREATE TABLE tbl_channrole (
  channrole_id     INT PRIMARY KEY AUTO_INCREMENT,
  channrole_no     VARCHAR(20) NOT NULL UNIQUE COMMENT '工号',
  channrole_name   VARCHAR(20) NOT NULL COMMENT '姓名',
  phone            VARCHAR(20) COMMENT '手机号',
  rank_level       VARCHAR(20) DEFAULT 'trainee' COMMENT 'trainee见习/formal正式/director业务主任',
  employ_type      VARCHAR(20) DEFAULT 'agent' COMMENT 'agent代理制/labor劳动制',
  entry_date       DATE COMMENT '入职日期',
  status           TINYINT DEFAULT 1 COMMENT '1=在职 0=离职',
  create_time      DATETIME DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO tbl_channrole(channrole_no,channrole_name,phone,rank_level,employ_type,entry_date) VALUES
('A1001','张伟','13800000001','formal','agent','2024-03-15'),
('A1002','李娜','13800000002','director','labor','2023-06-01'),
('A1003','王芳','13800000003','trainee','agent','2026-09-01'),
('A1004','刘洋','13800000004','formal','labor','2024-11-20'),
('A1005','陈静','13800000005','trainee','agent','2026-10-08'),
('A1006','赵磊','13800000006','formal','agent','2025-02-10'),
('A1007','孙敏','13800000007','director','agent','2022-09-05'),
('A1008','周杰','13800000008','trainee','labor','2026-10-12');

-- 4、人网关系表 tbl_salesnet_channrole
DROP TABLE IF EXISTS tbl_salesnet_channrole;
CREATE TABLE tbl_salesnet_channrole (
  id           INT PRIMARY KEY AUTO_INCREMENT,
  channrole_id INT NOT NULL COMMENT '代理人id',
  salesnet_id  INT NOT NULL COMMENT '网点id',
  main_flag    TINYINT DEFAULT 1 COMMENT '1=主挂靠 0=兼挂',
  bind_date    DATE COMMENT '入网点日期',
  status       TINYINT DEFAULT 1 COMMENT '1=有效 0=解绑',
  UNIQUE KEY uk_channrole_salesnet(channrole_id,salesnet_id)
);
INSERT INTO tbl_salesnet_channrole(channrole_id,salesnet_id,main_flag,bind_date) VALUES
(1,1,1,'2024-03-15'),
(2,1,1,'2023-06-01'),
(2,3,0,'2024-01-01'),
(3,1,1,'2026-09-01'),
(4,2,1,'2024-11-20'),
(5,2,1,'2026-10-08'),
(6,3,1,'2025-02-10'),
(7,3,1,'2022-09-05'),
(7,4,0,'2025-05-01'),
(8,4,1,'2026-10-12');

-- 5、保单表 tbl_policy
DROP TABLE IF EXISTS tbl_policy;
CREATE TABLE tbl_policy (
  policy_no    VARCHAR(30) PRIMARY KEY COMMENT '保单号',
  channrole_id INT NOT NULL COMMENT '承保代理人',
  salesnet_id  INT NOT NULL COMMENT '出单网点',
  product_name VARCHAR(50) COMMENT '产品名称',
  prem_amount  DECIMAL(12,2) COMMENT '保费',
  policy_term  VARCHAR(20) COMMENT '趸交/5年期/10年期',
  underwrite_date DATE COMMENT '承保日期',
  commission   DECIMAL(12,2) COMMENT '佣金'
);
INSERT INTO tbl_policy(policy_no,channrole_id,salesnet_id,product_name,prem_amount,policy_term,underwrite_date,commission) VALUES
('P20260001',1,1,'交银安享稳健养老','50000','趸交','2026-09-05',4000),
('P20260002',1,1,'交银人寿终身寿','20000','10年期','2026-09-12',4400),
('P20260003',2,1,'交银安享稳健养老','100000','趸交','2026-09-08',8000),
('P20260004',2,3,'交银财富宝年金','30000','5年期','2026-09-15',6600),
('P20260005',3,1,'交银安享稳健养老','20000','趸交','2026-09-20',1600),
('P20260006',4,2,'交银人寿终身寿','15000','10年期','2026-09-22',3300),
('P20260007',5,2,'交银安享稳健养老','10000','趸交','2026-10-09',800),
('P20260008',6,3,'交银财富宝年金','25000','5年期','2026-10-11',5500),
('P20260009',7,3,'交银人寿终身寿','80000','10年期','2026-10-10',17600),
('P20260010',7,4,'交银安享稳健养老','60000','趸交','2026-10-15',4800),
('P20260011',8,4,'交银安享稳健养老','8000','趸交','2026-10-20',640),
('P20260012',1,1,'交银财富宝年金','20000','5年期','2026-10-08',4400);