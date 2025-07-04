package com.lframework.xingyun.sc.impl.stock;

import com.github.pagehelper.PageInfo;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.EnumUtil;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.utils.PageHelperUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.sc.entity.ProductStockLog;
import com.lframework.xingyun.sc.enums.ProductStockBizType;
import com.lframework.xingyun.sc.mappers.ProductStockLogMapper;
import com.lframework.xingyun.sc.service.stock.ProductStockLogService;
import com.lframework.xingyun.sc.vo.stock.log.AddLogWithAddStockVo;
import com.lframework.xingyun.sc.vo.stock.log.AddLogWithSubStockVo;
import com.lframework.xingyun.sc.vo.stock.log.QueryProductStockLogVo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductStockLogServiceImpl extends
    BaseMpServiceImpl<ProductStockLogMapper, ProductStockLog>
    implements ProductStockLogService {

  @Override
  public PageResult<ProductStockLog> query(Integer pageIndex, Integer pageSize,
      QueryProductStockLogVo vo) {

    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);

    PageHelperUtil.startPage(pageIndex, pageSize);
    List<ProductStockLog> datas = this.query(vo);

    return PageResultUtil.convert(new PageInfo<>(datas));
  }

  @Override
  public List<ProductStockLog> query(QueryProductStockLogVo vo) {

    return getBaseMapper().query(vo);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void addLogWithAddStock(AddLogWithAddStockVo vo) {

    ProductStockLog record = new ProductStockLog();
    record.setId(IdUtil.getId());
    record.setScId(vo.getScId());
    record.setProductId(vo.getProductId());
    record.setOriStockNum(vo.getOriStockNum());
    record.setCurStockNum(vo.getCurStockNum());
    record.setOriTaxPrice(vo.getOriTaxPrice());
    record.setCurTaxPrice(vo.getCurTaxPrice());
    record.setStockNum(vo.getStockNum());
    record.setTaxAmount(vo.getTaxAmount());
    if (!StringUtil.isBlank(vo.getCreateBy())) {
      record.setCreateBy(vo.getCreateBy());
    }
    if (!StringUtil.isBlank(vo.getCreateById())) {
      record.setCreateById(vo.getCreateById());
    }
    record.setCreateTime(vo.getCreateTime());
    if (!StringUtil.isBlank(vo.getBizId())) {
      record.setBizId(vo.getBizId());
    }
    if (!StringUtil.isBlank(vo.getBizDetailId())) {
      record.setBizDetailId(vo.getBizDetailId());
    }
    if (!StringUtil.isBlank(vo.getBizCode())) {
      record.setBizCode(vo.getBizCode());
    }
    record.setBizType(EnumUtil.getByCode(ProductStockBizType.class, vo.getBizType()));

    getBaseMapper().insert(record);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void addLogWithSubStock(AddLogWithSubStockVo vo) {

    ProductStockLog record = new ProductStockLog();
    record.setId(IdUtil.getId());
    record.setScId(vo.getScId());
    record.setProductId(vo.getProductId());
    record.setOriStockNum(vo.getOriStockNum());
    record.setCurStockNum(vo.getCurStockNum());
    record.setOriTaxPrice(vo.getOriTaxPrice());
    record.setCurTaxPrice(vo.getCurTaxPrice());
    record.setStockNum(-Math.abs(vo.getStockNum()));
    record.setTaxAmount(vo.getTaxAmount().abs().negate());
    if (!StringUtil.isBlank(vo.getCreateBy())) {
      record.setCreateBy(vo.getCreateBy());
    }
    if (!StringUtil.isBlank(vo.getCreateById())) {
      record.setCreateById(vo.getCreateById());
    }
    record.setCreateTime(vo.getCreateTime());
    if (!StringUtil.isBlank(vo.getBizId())) {
      record.setBizId(vo.getBizId());
    }
    if (!StringUtil.isBlank(vo.getBizDetailId())) {
      record.setBizDetailId(vo.getBizDetailId());
    }
    if (!StringUtil.isBlank(vo.getBizCode())) {
      record.setBizCode(vo.getBizCode());
    }
    record.setBizType(EnumUtil.getByCode(ProductStockBizType.class, vo.getBizType()));

    getBaseMapper().insert(record);
  }
}
