package com.lframework.xingyun.sc.mappers;

import com.lframework.starter.web.core.mapper.BaseMapper;
import com.lframework.starter.web.core.annotations.permission.DataPermission;
import com.lframework.starter.web.core.annotations.permission.DataPermissions;
import com.lframework.starter.web.core.annotations.sort.Sort;
import com.lframework.starter.web.core.annotations.sort.Sorts;
import com.lframework.starter.web.inner.components.permission.OrderDataPermissionDataPermissionType;
import com.lframework.xingyun.sc.dto.logistics.LogisticsSheetBizOrderDto;
import com.lframework.xingyun.sc.dto.logistics.LogisticsSheetFullDto;
import com.lframework.xingyun.sc.entity.LogisticsSheet;
import com.lframework.xingyun.sc.vo.logistics.LogisticsSheetSelectorVo;
import com.lframework.xingyun.sc.vo.logistics.QueryLogisticsSheetBizOrderVo;
import com.lframework.xingyun.sc.vo.logistics.QueryLogisticsSheetVo;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 物流单 Mapper 接口
 * </p>
 *
 * @author zmj
 */
public interface LogisticsSheetMapper extends BaseMapper<LogisticsSheet> {

  /**
   * 查询列表
   *
   * @param vo
   * @return
   */
  @Sorts({
      @Sort(value = "code", alias = "o", autoParse = true),
      @Sort(value = "logisticsNo", alias = "o", autoParse = true),
      @Sort(value = "createTime", alias = "o", autoParse = true),
      @Sort(value = "deliveryTime", alias = "o", autoParse = true),
  })
  @DataPermissions(type = OrderDataPermissionDataPermissionType.class, value = {
      @DataPermission(template = "order", alias = "o")
  })
  List<LogisticsSheet> query(@Param("vo") QueryLogisticsSheetVo vo);

  /**
   * 根据ID查询
   *
   * @param id
   * @return
   */
  LogisticsSheetFullDto getDetail(String id);

  /**
   * 选择器
   *
   * @param vo
   * @return
   */
  @DataPermissions(type = OrderDataPermissionDataPermissionType.class, value = {
      @DataPermission(template = "order", alias = "o")
  })
  List<LogisticsSheet> selector(@Param("vo") LogisticsSheetSelectorVo vo);

  /**
   * 查询业务单据列表
   *
   * @param vo
   * @return
   */
  @DataPermissions(type = OrderDataPermissionDataPermissionType.class, value = {
      @DataPermission(template = "order", alias = "o")
  })
  List<LogisticsSheetBizOrderDto> queryBizOrder(@Param("vo") QueryLogisticsSheetBizOrderVo vo);
}
