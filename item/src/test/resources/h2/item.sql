-- 后台类目表: parana_back_categories
CREATE TABLE `parana_back_categories` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pid` bigint(20) DEFAULT NULL COMMENT '父级id',
  `name` varchar(50) DEFAULT NULL COMMENT '名称',
  `level` tinyint(1) DEFAULT NULL COMMENT '级别',
  `status` tinyint(1) DEFAULT NULL COMMENT '状态,1启用,-1禁用',
  `has_children` bit(1) DEFAULT NULL COMMENT '是否有孩子',
  `has_spu` bit(1) DEFAULT NULL COMMENT '是否有spu关联',
  `outer_id` VARCHAR(256) NULL COMMENT '外部 id',
  `shop_business_id` BIGINT(20) UNSIGNED NULL COMMENT '店铺行业, 关联一级类目',
  `rate`        INTEGER       NULL            COMMENT '抽佣比率 单位 万分之一',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) COMMENT='后台类目表';
CREATE INDEX idx_back_categories_name ON parana_back_categories (name);

-- 前台类目表:
CREATE TABLE `parana_front_categories` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pid` bigint(20) DEFAULT NULL COMMENT '父级id',
  `name` varchar(50) DEFAULT NULL COMMENT '名称',
  `level` tinyint(1) DEFAULT NULL COMMENT '级别',
  `has_children` bit(1) DEFAULT NULL COMMENT '是否有孩子',
  `logo` VARCHAR(256) NULL COMMENT 'logo',
  `background` VARCHAR(256) NULL COMMENT '背景',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) COMMENT='前台类目表';
CREATE INDEX idx_front_categories_name ON parana_front_categories (name);

-- 前后台叶子类目映射表: parana_category_bindings
CREATE TABLE `parana_category_bindings` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `front_category_id` bigint(20) DEFAULT NULL COMMENT '前台叶子类目id',
  `back_category_id` bigint(20) DEFAULT NULL COMMENT '后台叶子类目id',
  `path` varchar(256) DEFAULT NULL COMMENT '后台类目全路径',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) COMMENT='前后台叶子类目映射表';

-- SPU表: parana_spus
CREATE TABLE `parana_spus` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `category_id` bigint(20) DEFAULT NULL COMMENT '后台类目id',
  `name` varchar(100) DEFAULT NULL COMMENT '名称',
  `status` tinyint(1) DEFAULT NULL COMMENT '状态,1启用,-1禁用',
  `brand_id` bigint(20) DEFAULT NULL COMMENT '品牌id',
  `brand_name` varchar(50) DEFAULT NULL COMMENT '品牌名称',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) COMMENT='SPU表';
CREATE INDEX idx_spus_name ON parana_spus (name);

-- 品牌表: parana_brands
CREATE TABLE `parana_brands` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(100) DEFAULT NULL COMMENT '名称',
  `en_name` VARCHAR(100) NULL COMMENT '英文名称',
  `en_cap` CHAR(1) NULL COMMENT '首字母',
  `logo` VARCHAR(128) NULL COMMENT '品牌logo',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `type` INT(2) NULL COMMENT '品牌logo',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) COMMENT='品牌表';
CREATE INDEX idx_brands_name ON parana_brands (name);
CREATE INDEX idx_brands_en_name ON `parana_brands` (`en_name`);
CREATE INDEX idx_brands_en_cap ON `parana_brands` (`en_cap`);

-- 后台类目品牌子集
CREATE TABLE `parana_brand_subsets` (
  `id`        BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `bc_id`     BIGINT(20) NOT NULL COMMENT '后台类目 id',
  `brand_id`  BIGINT(20) NOT NULL COMMENT '品牌 id',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_brand_subsets_bc_id_bid_UNIQUE` (`bc_id`, `brand_id`)
) COMMENT = '后台类目品牌子集表';

-- 商品表: parana_items
CREATE TABLE `parana_items` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `outer_item_id` VARCHAR(32) NULL COMMENT '外部商品编号',
  `category_id` BIGINT UNSIGNED NOT NULL COMMENT '后台类目 ID',
  `spu_id` bigint(20) NOT NULL COMMENT 'SPU编号',
  `user_id` bigint(20) NOT NULL COMMENT '用户id',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `shop_name` varchar(50) NOT NULL DEFAULT '' COMMENT '店铺名称',
  `brand_id` bigint(20) DEFAULT NULL COMMENT '品牌id',
  `brand_name` varchar(100) DEFAULT '' COMMENT '品牌名称',
  `name` varchar(200) NOT NULL DEFAULT '' COMMENT '商品名称',
  `main_image` varchar(128) DEFAULT NULL COMMENT '主图',
  `price` int(11) NOT NULL COMMENT '卖价',
  `origin_price` int(11) NOT NULL COMMENT '原价',
  `stock_quantity` int(11) DEFAULT NULL COMMENT '库存',
  `sale_quantity` int(11) DEFAULT NULL COMMENT '销量',
  `province_id` int(11) DEFAULT NULL COMMENT '省份id',
  `city_id` int(11) DEFAULT NULL COMMENT '城市id',
  `region_id` int(11) DEFAULT NULL COMMENT '地区id',
  `status` tinyint(1) DEFAULT NULL COMMENT '状态',
  `on_shelf_at` datetime DEFAULT NULL COMMENT '上架时间',
  `off_shelf_at` datetime DEFAULT NULL COMMENT '下架时间',
  `remark` text COMMENT '备注',
  `model` varchar(128) COMMENT '模型',
  `market_tag` varchar(8) DEFAULT NULL COMMENT '商品营销活动标志,二进制字符串',
  `type` SMALLINT DEFAULT NULL COMMENT '商品类型',
  `bit_mark` INT DEFAULT NULL COMMENT '属性开关',
  `distributable` TINYINT(1) NULL COMMENT '是否可被分销',
  `extra` mediumtext COMMENT '商品额外信息,建议json字符串',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) COMMENT='商品表';
CREATE INDEX idx_items_user_id ON parana_items (user_id);
CREATE INDEX idx_items_shop_id ON parana_items (shop_id);
-- CREATE INDEX idx_items_brand_id ON parana_items (brand_id);
CREATE INDEX idx_items_distributable ON `parana_items` (`distributable`);

-- 商品详情: parana_item_details
CREATE TABLE `parana_item_details` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `item_id` bigint(20) DEFAULT NULL COMMENT '商品id',
  `image1` varchar(128) DEFAULT NULL COMMENT '图片1',
  `image2` varchar(128) DEFAULT NULL COMMENT '图片2',
  `image3` varchar(128) DEFAULT NULL COMMENT '图片3',
  `image4` varchar(128) DEFAULT NULL COMMENT '图片4',
  `packing` text COMMENT '包装清单',
  `service` text NULL COMMENT '售后服务',
  `attr_images` text NULL COMMENT '商品销售属性图',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) COMMENT='商品详情';
CREATE INDEX idx_item_details_item_id ON parana_item_details (item_id);

-- 商品模板表: parana_item_templates
CREATE TABLE `parana_item_templates` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `spu_id` bigint(20) DEFAULT NULL COMMENT 'SPU编号',
  `name` varchar(256) DEFAULT NULL COMMENT '商品名称',
  `price` int(11) DEFAULT NULL COMMENT '价格',
  `main_image` varchar(128) DEFAULT NULL COMMENT '主图',
  `image1` varchar(128) DEFAULT NULL COMMENT '图1',
  `image2` varchar(128) DEFAULT NULL COMMENT '图2',
  `image3` varchar(128) DEFAULT NULL COMMENT '图3',
  `image4` varchar(128) DEFAULT NULL COMMENT '图4',
  `json_skus` varchar(2048) DEFAULT NULL COMMENT '以json形式存储的Sku列表',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) COMMENT='商品模板表';
CREATE INDEX idx_item_templates_spu_id ON parana_item_templates (spu_id);

-- 商品运费信息表: parana_item_ship_fees
CREATE TABLE `parana_item_ship_fees` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `item_id` bigint(20) DEFAULT NULL COMMENT '商品id',
  `ship_fee` int(11) DEFAULT NULL COMMENT '运费, 不指定运费模板时用',
  `ship_fee_template_id` bigint(20) DEFAULT NULL COMMENT '运费模板那id',
  `deliver_fee_template_id` BIGINT(20) DEFAULT NULL COMMENT '运费模板id，代替ship_fee_template_id',
  `ship_fee_template_name` varchar(100) DEFAULT NULL COMMENT '运费模板名称',
  `deliver_fee_template_name` VARCHAR(128) DEFAULT NULL COMMENT '运费模板名称，代替ship_fee_template_name',
  `weight` int(11) DEFAULT NULL COMMENT '重量(kg)',
  `volume` int(11) DEFAULT NULL COMMENT '体积(m3)',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) COMMENT='商品运费信息表';
CREATE INDEX idx_item_ship_fees_item_id ON parana_item_ship_fees (item_id);
CREATE INDEX idx_item_ship_fees_template_id ON parana_item_ship_fees (ship_fee_template_id);

-- 商品SKU表: parana_skus
CREATE TABLE `parana_skus` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `sku_code` VARCHAR(40) NOT NULL COMMENT 'SKU 编码 (标准库存单位编码)',
  `item_id` bigint(20) NOT NULL COMMENT '商品id',
  `shop_id` BIGINT UNSIGNED NOT NULL COMMENT '店铺 ID (冗余自商品表)',
  `status` TINYINT(1) NOT NULL COMMENT '商品状态 (冗余自商品表)',
  `model` VARCHAR(50) NULL COMMENT '型号/款式',
  `outer_item_id` VARCHAR(32) NULL COMMENT '外部商品编号',
  `outer_shop_id` VARCHAR(32) NULL COMMENT '外部店铺id',
  `image` varchar(128) DEFAULT NULL COMMENT '图片url',
  `name` VARCHAR(100) NULL COMMENT '名称',
  `desc` VARCHAR(200) NULL COMMENT '描述',
  `origin_price` int(11) DEFAULT NULL COMMENT '原价',
  `price` int(11) DEFAULT NULL COMMENT '价格',
  `attribute_key1` varchar(64) DEFAULT NULL COMMENT '属性键1',
  `attribute_key_id1` BIGINT NULL COMMENT '属性键id1',
  `attribute_name1` varchar(64) DEFAULT NULL COMMENT '属性名1',
  `attribute_value1` varchar(64) DEFAULT NULL COMMENT '属性值1',
  `attribute_key2` varchar(64) DEFAULT NULL COMMENT '属性键2',
  `attribute_key_id2` BIGINT NULL COMMENT '属性键id2',
  `attribute_name2` varchar(64) DEFAULT NULL COMMENT '属性名2',
  `attribute_value2` varchar(64) DEFAULT NULL COMMENT '属性值2',
  `stock_quantity` int(11) DEFAULT NULL COMMENT '库存',
  `extra`     TEXT         DEFAULT NULL COMMENT 'sku额外信息',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) COMMENT='商品SKU表';
CREATE INDEX idx_skus_item_id ON parana_skus (item_id);
CREATE INDEX idx_skus_model ON `parana_skus` (`model`);

-- 交易快照
drop table if exists `parana_item_snapshots`;

create table if not exists `parana_item_snapshots` (
  `id`              BIGINT          NOT NULL      AUTO_INCREMENT COMMENT '自增主键',
  `item_id`         BIGINT          NOT NULL  COMMENT '商品id',
  `item_name`       VARCHAR(200)    NOT NULL  COMMENT '商品名称',
  `seller_id`       BIGINT          NOT NULL  COMMENT '卖家id',
  `main_image`      VARCHAR(128)    NULL      COMMENT '主图',
  `detail`          TEXT            NULL      COMMENT '商品详情',
  `created_at`      DATETIME        NOT NULL  COMMENT '创建时间',
  `updated_at`      DATETIME        NULL      COMMENT '修改时间',
  PRIMARY KEY (`id`)
);

create index idx_item_snapshots_item_id on parana_item_snapshots (item_id);
create index idx_item_snapshots_created_at on parana_item_snapshots (created_at);

-- 商品折扣统计表
CREATE TABLE `parana_club_item_discounts` (
  `id`        BIGINT(20)    NOT NULL AUTO_INCREMENT,
  `item_id`   BIGINT(20)    NOT NULL COMMENT '商品id',
  `seller_id` BIGINT(20)    NOT NULL COMMENT '商家id',
  `item_name` VARCHAR(128)  NOT NULL COMMENT '商品名称',
  `item_image`  VARCHAR(128)  NOT NULL COMMENT '商品主图',
  `item_price`  INT         NOT NULL COMMENT '商品价格',
  `origin_price`  INT       NOT NULL COMMENT '商品原价',
  `special_price` INT       NOT NULL COMMENT '商品折扣价',
  `discount_percent`  INT   NOT NULL COMMENT '折扣百分比 x100后的结果',
  `created_at`  DATETIME    NOT NULL ,
  `updated_at`  DATETIME    NOT NULL ,
  PRIMARY KEY (`id`)
);
CREATE INDEX idx_parana_club_item_discount_seller_id ON `parana_club_item_discounts` (`seller_id`);

-- SKU额外信息表parana_sku_extras
CREATE TABLE `parana_sku_extras` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `item_id` bigint(20) DEFAULT NULL COMMENT '商品ID',
  `sku_id` bigint(20) DEFAULT NULL COMMENT 'SKU ID',
  `special_price` int(11) DEFAULT NULL COMMENT '专享价',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
);
CREATE INDEX parana_sku_extras_idx_item_id ON `parana_sku_extras` (item_id);
CREATE INDEX parana_sku_extras_idx_sku_id ON `parana_sku_extras` (sku_id);

-- SKU 阶梯价格表
CREATE TABLE `parana_sku_prices` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  `item_id`     BIGINT NOT NULL COMMENT '商品 ID',
  `sku_id`      BIGINT NOT NULL COMMENT 'SKU ID',
  `desc`        VARCHAR(64) NULL COMMENT '此档价格描述',
  `price`       INT NOT NULL COMMENT '价格',
  `lv`          INT NOT NULL DEFAULT 1 COMMENT '价格梯度',
  `created_at`  DATETIME NOT NULL ,
  `updated_at`  DATETIME NOT NULL ,
  PRIMARY KEY (`id`)
);
CREATE INDEX `parana_sku_prices_idx_item_id` ON `parana_sku_prices` (`item_id`);
CREATE INDEX `parana_sku_prices_idx_sku_id` ON `parana_sku_prices` (`sku_id`);

-- 后台类目权限
CREATE TABLE `parana_back_category_perms` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT NOT NULL COMMENT '用户 id',
  `user_type`   INT    NOT NULL COMMENT '用户类型',
  `allow`       VARCHAR(4096) NOT NULL DEFAULT '' COMMENT '白名单',
  `deny`        VARCHAR(4096) NOT NULL DEFAULT '' COMMENT '黑名单',
  `brand_allow` VARCHAR(4096) NOT NULL DEFAULT '' COMMENT '品牌白名单',
  `created_at`  DATETIME DEFAULT NULL,
  `updated_at`  DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
);
CREATE INDEX `idx_parana_back_c_perms_user_id` ON `parana_back_category_perms` (`user_id`);
CREATE INDEX `idx_parana_back_c_perms_user_type` ON `parana_back_category_perms` (`user_type`);

-- 运费模版
CREATE TABLE `parana_deliver_fee_templates` (
  `id`      BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '商家id',
  `name`    VARCHAR(64) NOT NULL COMMENT '名称',
  `method`  SMALLINT  NOT NULL COMMENT '送货方式 1-普通物流 2-EMS',
  `valuation` SMALLINT NOT NULL COMMENT '计价方式',
  `fee`     INT   NULL COMMENT '统一价格，当计价方式为固定运费时使用',
  `low_price` INT NULL COMMENT '订单不满该金额时，运费为lowFee',
  `low_fee` INT NULL COMMENT '订单不满low_price时，运费为lowFee',
  `high_price`  INT NULL COMMENT '订单高于该金额时，运费为highFee',
  `high_fee`  INT NULL COMMENT '订单高于high_price时，运费为highFee',
  `middle_fee`  INT NULL COMMENT '订单价格在lowFee，highFee之间时，运费为middleFee',
  `first_fee` INT NULL COMMENT '首件运费',
  `per_fee` INT NULL COMMENT '每增加一件，续费perFee元',
  `is_default`  BIT NULL COMMENT '是否是默认模板',
  `created_at`  DATETIME  NULL ,
  `updated_at`  DATETIME  NULL ,
  PRIMARY KEY (`id`)
)COMMENT = '新版运费模板';

CREATE INDEX idx_deliver_fee_template_user_id ON `parana_deliver_fee_templates`(`user_id`);

-- 特殊区域运费计算
CREATE TABLE `parana_special_delivers` (
  `id`      BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT  NOT NULL COMMENT '商家id',
  `deliver_fee_template_id` BIGINT NOT NULL COMMENT '对应运费模板id',
  `addresses` VARCHAR(1024) NOT NULL COMMENT '对应区域id',
  `address_for_display` TEXT NOT NULL COMMENT '对应区域层级json',
  `method`  SMALLINT  NOT NULL COMMENT '运送方式',
  `valuation` SMALLINT NOT NULL COMMENT '计价方式',
  `fee`     INT   NULL COMMENT '统一价格，当计价方式为固定运费时使用',
  `low_price` INT NULL COMMENT '订单不满该金额时，运费为lowFee',
  `low_fee` INT NULL COMMENT '订单不满low_price时，运费为lowFee',
  `high_price`  INT NULL COMMENT '订单高于该金额时，运费为highFee',
  `high_fee`  INT NULL COMMENT '订单高于high_price时，运费为highFee',
  `middle_fee`  INT NULL COMMENT '订单价格在lowFee，highFee之间时，运费为middleFee',
  `first_fee` INT NULL COMMENT '首件运费',
  `per_fee` INT NULL COMMENT '每增加一件，续费perFee元',
  `created_at`  DATETIME  NULL ,
  `updated_at`  DATETIME  NULL ,
  PRIMARY KEY (`id`)
)COMMENT = '特殊区域运费计算';

CREATE INDEX idx_special_delivers_user_id ON `parana_special_delivers` (`user_id`);


CREATE TABLE `parana_category_attribute_keys` (
  `id`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `category_id` BIGINT(20) UNSIGNED NOT NULL COMMENT '后台类目 ID',
  `key_id`      BIGINT(20) UNSIGNED NOT NULL COMMENT '属性键 ID',
  `key_name`    VARCHAR(64) NOT NULL COMMENT '属性键名称',
  `key_type`    SMALLINT(6) NOT NULL COMMENT '属性键类型: 0 不可枚举, 1 可枚举',
  `created_at`  DATETIME NULL,
  `updated_at`  DATETIME NULL,
  PRIMARY KEY (`id`)
) COMMENT = '后台类目属性键';

CREATE UNIQUE INDEX `idx_cat_attr_keys_cat_key_id_UNIQUE` ON `parana_category_attribute_keys` (`category_id`, `key_id`);
CREATE INDEX `idx_cat_attr_keys_key_type` ON `parana_category_attribute_keys` (`key_type`);

CREATE TABLE `parana_category_attribute_values` (
  `id`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `category_id` BIGINT(20) UNSIGNED NOT NULL COMMENT '后台类目 ID',
  `key_id`      BIGINT(20) UNSIGNED NOT NULL COMMENT '属性键 ID',
  `value_id`    BIGINT(20) UNSIGNED NOT NULL COMMENT '属性值 ID',
  `value`       VARCHAR(64) NOT NULL COMMENT '属性值',
  `logo`        VARCHAR(256) NULL COMMENT '属性值图片',
  `created_at`  DATETIME NULL,
  `updated_at`  DATETIME NULL,
  PRIMARY KEY (`id`)
) COMMENT = '后台类目属性值';

CREATE UNIQUE INDEX `idx_cat_attr_values_cat_key_value_id_UNIQUE` ON `parana_category_attribute_values` (`category_id`, `key_id`, `value_id`);

CREATE TABLE `parana_item_auto_on_shelfs` (
  `id`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `item_id`     BIGINT(20) UNSIGNED NOT NULL COMMENT '商品 ID',
  `left`        DATETIME NOT NULL COMMENT '开始时间',
  `right`       DATETIME NOT NULL COMMENT '结束时间',
  `current`     SMALLINT(6) NOT NULL COMMENT '当前状态, 1: left, 2: mid, 3: right',
  PRIMARY KEY (`id`)
) COMMENT = '商品自动上下架表';


DROP TABLE IF EXISTS `parana_favorite_items`;

CREATE TABLE `parana_favorite_items` (
	`id`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	`item_id`     BIGINT(20) UNSIGNED NOT NULL COMMENT '商品 ID',
	`user_id`     BIGINT(20) UNSIGNED NOT NULL COMMENT '用户id',
	`brand`				VARCHAR(128) DEFAULT NULL COMMENT '品牌',
	`brand_picture` VARCHAR(256) DEFAULT  NULL comment '品牌图片',
	`name`				VARCHAR(128) DEFAULT NULL COMMENT '商品名称',
	`image`				VARCHAR(256) DEFAULT NULL COMMENT '商品主图',
	`price`				BIGINT(20) NULL COMMENT '现价',
	`origin_price` BIGINT(20) NULL COMMENT  '原价',
	`discount` 		BIGINT(20) NULL COMMENT '折扣',
	`paid_number` BIGINT(20) NULL COMMENT '付款人数',
	`created_at`  DATETIME NULL,
	`updated_at`  DATETIME NULL,
	PRIMARY KEY (`id`)
) COMMENT = '收藏商品表';

CREATE UNIQUE INDEX  `idx_parana_favorite_items_user_item_id` ON `parana_favorite_items` (user_id,item_id);

-- 商品导入申请
CREATE TABLE `parana_import_applys` (
  `id`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `seller_id`     BIGINT(20) UNSIGNED NOT NULL COMMENT '商家 ID',
  `seller_name`     VARCHAR(128) NULL COMMENT '商家名称',
  `shop_id`				BIGINT(20) NOT NULL COMMENT '店铺id',
  `shop_name` VARCHAR(256)  NULL comment '店铺名称',
  `file_url`				VARCHAR(128) NOT NULL COMMENT '导入的文件名称',
  `status`				TINYINT DEFAULT 0 NULL COMMENT '申请状态',
  `remark`				VARCHAR(256) NULL COMMENT '商家备注',
  `check_result` LONGTEXT NULL COMMENT  '审核结果',
  `created_at`  DATETIME NULL,
  `updated_at`  DATETIME NULL,
  PRIMARY KEY (`id`)
) COMMENT = '导入申请表';

CREATE INDEX  `idx_parana_import_applys_seller_id` ON `parana_import_applys` (seller_id);
CREATE INDEX  `idx_parana_import_applys_shop_id` ON `parana_import_applys` (shop_id);
