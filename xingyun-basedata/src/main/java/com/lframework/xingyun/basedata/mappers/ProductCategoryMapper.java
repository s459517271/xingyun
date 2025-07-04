package com.lframework.xingyun.basedata.mappers;

import com.lframework.starter.web.core.mapper.BaseMapper;
import com.lframework.xingyun.basedata.entity.ProductCategory;
import com.lframework.xingyun.basedata.vo.product.category.QueryProductCategorySelectorVo;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author zmj
 * @since 2021-07-05
 */
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {

  /**
   * 查询全部分类信息
   *
   * @return
   */
  List<ProductCategory> getAllProductCategories();

  /**
   * 选择器
   *
   * @return
   */
  List<ProductCategory> selector(@Param("vo") QueryProductCategorySelectorVo vo);
}
