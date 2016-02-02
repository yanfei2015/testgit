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
