-- 后台类目表: parana_back_categories
CREATE TABLE IF NOT EXISTS `parana_back_categories` (
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
CALL CreateIndex("parana_back_categories", "idx_back_categories_name", "name");

-- 前台类目表:
CREATE TABLE IF NOT EXISTS `parana_front_categories` (
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
CALL CreateIndex("parana_front_categories", "idx_front_categories_name", "name");

-- 前后台叶子类目映射表: parana_category_bindings
CREATE TABLE IF NOT EXISTS `parana_category_bindings` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `front_category_id` bigint(20) DEFAULT NULL COMMENT '前台叶子类目id',
  `back_category_id` bigint(20) DEFAULT NULL COMMENT '后台叶子类目id',
  `path` varchar(256) DEFAULT NULL COMMENT '后台类目全路径',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) COMMENT='前后台叶子类目映射表';

-- SPU表: parana_spus
CREATE TABLE IF NOT EXISTS `parana_spus` (
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
CALL CreateIndex("parana_spus", "idx_spus_name", "name");

-- 品牌表: parana_brands
CREATE TABLE IF NOT EXISTS `parana_brands` (
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
CALL CreateIndex("parana_brands", "idx_brands_name", "name");
CALL CreateIndex("parana_brands", "idx_brands_en_name", "en_name");
CALL CreateIndex("parana_brands", "idx_brands_en_cap", "en_cap");


-- 后台类目品牌子集
CREATE TABLE IF NOT EXISTS `parana_brand_subsets` (
  `id`        BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `bc_id`     BIGINT(20) NOT NULL COMMENT '后台类目 id',
  `brand_id`  BIGINT(20) NOT NULL COMMENT '品牌 id',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_brand_subsets_bc_id_bid_UNIQUE` (`bc_id`, `brand_id`)
) COMMENT = '后台类目品牌子集表';

-- 商品表: parana_items
CREATE TABLE IF NOT EXISTS `parana_items` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `outer_item_id` VARCHAR(32) NULL COMMENT '外部商品编号',
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

CALL CreateIndex("parana_items", "idx_items_user_id", "user_id");
CALL CreateIndex("parana_items", "idx_items_shop_id", "shop_id");
CALL CreateIndex("parana_items", "idx_items_distributable", "distributable");


-- 商品详情: parana_item_details
CREATE TABLE IF NOT EXISTS `parana_item_details` (
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
CALL CreateIndex("parana_item_details", "idx_item_details_item_id", "item_id");

-- 商品模板表: parana_item_templates
CREATE TABLE IF NOT EXISTS `parana_item_templates` (
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
CALL CreateIndex("parana_item_templates", "idx_item_templates_spu_id", "spu_id");

-- 商品运费信息表: parana_item_ship_fees
CREATE TABLE IF NOT EXISTS `parana_item_ship_fees` (
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
CALL CreateIndex("parana_item_ship_fees", "idx_item_ship_fees_item_id", "item_id");
CALL CreateIndex("parana_item_ship_fees", "idx_item_ship_fees_template_id", "ship_fee_template_id");


-- 商品SKU表: parana_skus
CREATE TABLE IF NOT EXISTS `parana_skus` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `outer_id` varchar(32) DEFAULT NULL COMMENT '外部id',
  `item_id` bigint(20) NOT NULL COMMENT '商品id',
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
CALL CreateIndex("parana_skus", "idx_skus_item_id", "item_id");

-- 交易快照
CREATE TABLE IF NOT EXISTS `parana_item_snapshots` (
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
CALL CreateIndex("parana_item_snapshots", "idx_item_snapshots_item_id", "item_id");
CALL CreateIndex("parana_item_snapshots", "idx_item_snapshots_created_at", "created_at");


-- 商品折扣统计表
CREATE TABLE IF NOT EXISTS `parana_club_item_discounts` (
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
CALL CreateIndex("parana_club_item_discounts", "idx_parana_club_item_discount_seller_id", "seller_id");

-- SKU额外信息表parana_sku_extras
CREATE TABLE IF NOT EXISTS `parana_sku_extras` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `item_id` bigint(20) DEFAULT NULL COMMENT '商品ID',
  `sku_id` bigint(20) DEFAULT NULL COMMENT 'SKU ID',
  `special_price` int(11) DEFAULT NULL COMMENT '专享价',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
);
CALL CreateIndex("parana_sku_extras", "parana_sku_extras_idx_item_id", "item_id");
CALL CreateIndex("parana_sku_extras", "parana_sku_extras_idx_sku_id", "sku_id");


-- SKU 阶梯价格表
CREATE TABLE IF NOT EXISTS `parana_sku_prices` (
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
CALL CreateIndex("parana_sku_prices", "parana_sku_prices_idx_item_id", "item_id");
CALL CreateIndex("parana_sku_prices", "parana_sku_prices_idx_sku_id", "sku_id");

-- 后台类目权限
CREATE TABLE IF NOT EXISTS `parana_back_category_perms` (
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
CALL CreateIndex("parana_back_category_perms", "idx_parana_back_c_perms_user_id", "user_id");
CALL CreateIndex("parana_back_category_perms", "idx_parana_back_c_perms_user_type", "user_type");

-- 运费模版
CREATE TABLE IF NOT EXISTS `parana_deliver_fee_templates` (
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
CALL CreateIndex("parana_deliver_fee_templates", "idx_deliver_fee_template_user_id", "user_id");

-- 特殊区域运费计算
CREATE TABLE IF NOT EXISTS `parana_special_delivers` (
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
CALL CreateIndex("parana_special_delivers", "idx_special_delivers_user_id", "user_id");


CREATE TABLE IF NOT EXISTS `parana_category_attribute_keys` (
  `id`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `category_id` BIGINT(20) UNSIGNED NOT NULL COMMENT '后台类目 ID',
  `key_id`      BIGINT(20) UNSIGNED NOT NULL COMMENT '属性键 ID',
  `key_name`    VARCHAR(64) NOT NULL COMMENT '属性键名称',
  `key_type`    SMALLINT(6) NOT NULL COMMENT '属性键类型: 0 不可枚举, 1 可枚举',
  `created_at`  DATETIME NULL,
  `updated_at`  DATETIME NULL,
  PRIMARY KEY (`id`)
) COMMENT = '后台类目属性键';
CALL CreateUniqueIndex("parana_category_attribute_keys", "idx_cat_attr_keys_cat_key_id_UNIQUE", "`category_id`, `key_id`");
CALL CreateIndex("parana_category_attribute_keys", "idx_cat_attr_keys_key_type", "key_type");


CREATE TABLE IF NOT EXISTS `parana_category_attribute_values` (
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

CALL CreateUniqueIndex("parana_category_attribute_values", "idx_cat_attr_values_cat_key_value_id_UNIQUE", "`category_id`, `key_id`, `value_id`");


CREATE TABLE IF NOT EXISTS `parana_item_auto_on_shelfs` (
  `id`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `item_id`     BIGINT(20) UNSIGNED NOT NULL COMMENT '商品 ID',
  `left`        DATETIME NOT NULL COMMENT '开始时间',
  `right`       DATETIME NOT NULL COMMENT '结束时间',
  `current`     SMALLINT(6) NOT NULL COMMENT '当前状态, 1: left, 2: mid, 3: right',
  PRIMARY KEY (`id`)
) COMMENT = '商品自动上下架表';

CREATE TABLE IF NOT EXISTS `parana_favorite_items` (
  `id`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `item_id`     BIGINT(20) UNSIGNED NOT NULL COMMENT '商品 ID',
  `user_id`     BIGINT(20) UNSIGNED NOT NULL COMMENT '用户id',
  `brand`       VARCHAR(128) DEFAULT NULL COMMENT '品牌',
  `brand_picture` VARCHAR(256) DEFAULT  NULL comment '品牌图片',
  `name`        VARCHAR(128) DEFAULT NULL COMMENT '商品名称',
  `image`       VARCHAR(256) DEFAULT NULL COMMENT '商品主图',
  `price`       BIGINT(20) NULL COMMENT '现价',
  `origin_price` BIGINT(20) NULL COMMENT  '原价',
  `discount`    BIGINT(20) NULL COMMENT '折扣',
  `paid_number` BIGINT(20) NULL COMMENT '付款人数',
  `created_at`  DATETIME NULL,
  `updated_at`  DATETIME NULL,
  PRIMARY KEY (`id`)
) COMMENT = '收藏商品表';
CALL CreateUniqueIndex("parana_favorite_items", "idx_parana_favorite_items_user_item_id", "user_id,item_id");

-- 商品导入申请
CREATE TABLE IF NOT EXISTS `parana_import_applys` (
  `id`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `seller_id`     BIGINT(20) UNSIGNED NOT NULL COMMENT '商家 ID',
  `seller_name`     VARCHAR(128) NULL COMMENT '商家名称',
  `shop_id`       BIGINT(20) NOT NULL COMMENT '店铺id',
  `shop_name` VARCHAR(256)  NULL comment '店铺名称',
  `file_url`        VARCHAR(128) NOT NULL COMMENT '导入的文件名称',
  `status`        TINYINT DEFAULT 0 NULL COMMENT '申请状态',
  `remark`        VARCHAR(256) NULL COMMENT '商家备注',
  `check_result` LONGTEXT NULL COMMENT  '审核结果',
  `created_at`  DATETIME NULL,
  `updated_at`  DATETIME NULL,
  PRIMARY KEY (`id`)
) COMMENT = '导入申请表';
CALL CreateUniqueIndex("parana_import_applys", "idx_parana_import_applys_seller_id", "seller_id");
CALL CreateUniqueIndex("parana_import_applys", "idx_parana_import_applys_shop_id", "shop_id");

-- ************* ITEM *****************
CREATE TABLE IF NOT EXISTS `parana_item_activities` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_id` BIGINT NOT NULL COMMENT '商品编号',
  `item_name` VARCHAR(128) NOT NULL COMMENT '商品名称',
  `main_image` VARCHAR(256) NULL COMMENT '商品主图',
  `price` INT NULL COMMENT '售价',
  `discount` INT NULL COMMENT '优惠金额',
  `seller_id` BIGINT(20) NULL COMMENT '商家ID',
  `shop_id` BIGINT(20) NULL COMMENT '店铺ID',
  `shop_name` VARCHAR(50) NULL COMMENT '店铺名称',
  `brand_id` BIGINT(20)  NULL COMMENT '品牌ID',
  `brand_name` VARCHAR(64) NULL COMMENT '品牌名称',
  `activity_id` BIGINT(20) NOT NULL COMMENT '活动编号',
  `activity_name` VARCHAR(128) NULL COMMENT '优惠活动名称',
  `status` SMALLINT(6) NOT NULL COMMENT '（0:待发布，1:已发布，2:活动开始，3:活动停止，4:活动结束）',
  `created_at` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` DATETIME NULL DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`)
);


-- 后台类目属性改造
CREATE TABLE IF NOT EXISTS `parana_category_attribute_keys` (
  `id`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `category_id` BIGINT(20) UNSIGNED NOT NULL COMMENT '后台类目 ID',
  `key_id`      BIGINT(20) UNSIGNED NOT NULL COMMENT '属性键 ID',
  `key_name`    VARCHAR(64) NOT NULL COMMENT '属性键名称',
  `key_type`    SMALLINT(6) NOT NULL COMMENT '属性键类型: 0 不可枚举, 1 可枚举',
  `created_at`  DATETIME NULL,
  `updated_at`  DATETIME NULL,
  PRIMARY KEY (`id`)
) COMMENT = '后台类目属性键';

CALL CreateUniqueIndex("parana_category_attribute_keys", "idx_cat_attr_keys_cat_key_id_UNIQUE", "`category_id`, `key_id`");
CALL CreateIndex("parana_category_attribute_keys", "idx_cat_attr_keys_key_type", "key_type");


CREATE TABLE IF NOT EXISTS `parana_category_attribute_values` (
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

CALL CreateUniqueIndex("parana_category_attribute_values", "idx_cat_attr_values_cat_key_value_id_UNIQUE", "`category_id`, `key_id`, `value_id`");

-- 商品收藏
CREATE TABLE IF NOT EXISTS `parana_favorite_items` (
  `id`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `item_id`     BIGINT(20) UNSIGNED NOT NULL COMMENT '商品 ID',
  `user_id`     BIGINT(20) UNSIGNED NOT NULL COMMENT '用户id',
  `brand`       VARCHAR(128) DEFAULT NULL COMMENT '品牌',
  `name`        VARCHAR(128) DEFAULT NULL COMMENT '商品名称',
  `image`       VARCHAR(256) DEFAULT NULL COMMENT '商品主图',
  `price`       BIGINT(20) NULL COMMENT '现价',
  `origin_price` BIGINT(20) NULL COMMENT  '原价',
  `discount`    BIGINT(20) NULL COMMENT '折扣',
  `paid_number` BIGINT(20) NULL COMMENT '付款人数',
  `created_at`  DATETIME NULL,
  `updated_at`  DATETIME NULL,
  PRIMARY KEY (`id`)
) COMMENT = '收藏商品表';

CALL CreateUniqueIndex("parana_favorite_items", "idx_parana_favorite_items_user_item_id", "user_id,item_id");


-- 商品导入申请
CREATE TABLE IF NOT EXISTS `parana_import_applys` (
  `id`          BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `seller_id`     BIGINT(20) UNSIGNED NOT NULL COMMENT '商家 ID',
  `seller_name`     VARCHAR(128) NULL COMMENT '商家名称',
  `shop_id`       BIGINT(20) NOT NULL COMMENT '店铺id',
  `shop_name` VARCHAR(256)  NULL comment '店铺名称',
  `file_url`        VARCHAR(128) NOT NULL COMMENT '导入的文件名称',
  `status`        TINYINT DEFAULT 0 NULL COMMENT '申请状态',
  `remark`        VARCHAR(256) NULL COMMENT '商家备注',
  `check_result` LONGTEXT NULL COMMENT  '审核结果',
  `created_at`  DATETIME NULL,
  `updated_at`  DATETIME NULL,
  PRIMARY KEY (`id`)
) COMMENT = '导入申请表';

CALL CreateIndex("parana_import_applys", "idx_parana_import_applys_seller_id", "seller_id");
CALL CreateIndex("parana_import_applys", "idx_parana_import_applys_shop_id", "shop_id");

-- SKU 表更改列名
CALL ChangeCol("parana_skus", "outer_id", "`sku_code` VARCHAR(40) NOT NULL COMMENT 'SKU 编码 (标准库存单位编码)'");

-- 前台类目增加 logo
CALL AddCol("parana_front_categories", "logo", "VARCHAR(256) NULL COMMENT 'logo' AFTER `has_children`");
-- 增加外部sku店铺id
CALL AddCol("parana_skus", "outer_shop_id", "VARCHAR(32) NULL COMMENT '外部店铺id' AFTER `outer_item_id`");

-- 商品的sku属性
CALL AddCol("parana_item_activities", "sku_id", "BIGINT NOT NULL COMMENT '商品sku编号' AFTER `id`");
CALL AddCol("parana_item_activities", "sku_des", "VARCHAR(128) NULL COMMENT '商品的sku属性信息' AFTER `activity_name`");


-- 商品销售属性图
CALL AddCol("parana_item_details", "attr_images", "text NULL COMMENT '商品销售属性图' AFTER `service`");
-- 增加 SKU 原价
CALL AddCol("parana_skus", "origin_price", "int(11) DEFAULT NULL COMMENT '原价' AFTER `desc`");
-- 商品收藏中的品牌图片
CALL AddCol("parana_favorite_items", "brand_picture", "VARCHAR(256)  NULL comment '品牌图片' after `brand`");
-- 前台类目增加背景
CALL AddCol("parana_front_categories", "background", "VARCHAR(256) NULL COMMENT '背景' AFTER `logo`");
-- 增加店铺行业到后台类目
CALL AddCol("parana_back_categories", "shop_business_id", "BIGINT(20) UNSIGNED NULL COMMENT '店铺行业, 关联一级类目' AFTER `sku_code`");
-- 后台类目添加类目费率
CALL AddCol("parana_back_categories", "rate", "INTEGER  NULL  COMMENT '抽佣比率 单位 万分之一' AFTER `shop_business_id`");

-- 在parana_skus表中添加字段‘商品款式的编号’(model)
CALL AddCol("parana_skus", "model", "VARCHAR(50) NULL COMMENT '型号/款式' AFTER `sku_code`");

-- 初始化数据
CALL InsertRow("parana_back_category_perms", "`user_id`, `user_type`, `allow`, `deny`, `brand_allow`, `created_at`, `updated_at`", "-1, 0, '0', '', '0', '2015-08-03 05:04:05', '2015-08-03 05:04:05'", "`user_id`", "'1'");


-- 商品表增加后台类目 ID 字段
CALL AddCol("parana_items", "category_id", "BIGINT UNSIGNED NOT NULL COMMENT '后台类目 ID' AFTER `outer_item_id`");
-- 修复数据 (商品表新增后台类目 ID 字段)
UPDATE parana_items as i left join parana_spus as s on i.spu_id = s.id set i.category_id = s.category_id;

-- 增加店铺 ID 冗余进 SKU 表
CALL AddCol("parana_skus", "shop_id", "BIGINT UNSIGNED NOT NULL COMMENT '店铺 ID (冗余自商品表)' AFTER `item_id`");

-- 适配历史数据
update parana_skus as s left join parana_items as i on s.item_id = i.id set s.shop_id = i.shop_id;
update parana_skus set sku_code = concat('SKU-', item_id, '-', id) where sku_code is null or sku_code = '';

-- SKU 增加商品状态冗余
CALL AddCol("parana_skus", "status", "TINYINT(1) NOT NULL COMMENT '商品状态 (冗余自商品表)' AFTER `shop_id`");
update parana_skus as s left join parana_items as i on s.item_id = i.id set s.status = i.status;

-- SKU 统一用 model 替换 style_no
CALL CreateIndex("parana_skus", "idx_skus_model", "model");
