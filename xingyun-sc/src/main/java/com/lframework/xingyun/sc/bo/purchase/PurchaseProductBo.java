package com.lframework.xingyun.sc.bo.purchase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.sc.dto.purchase.PurchaseProductDto;
import com.lframework.xingyun.sc.entity.ProductStock;
import com.lframework.xingyun.sc.service.stock.ProductStockService;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class PurchaseProductBo extends BaseBo<PurchaseProductDto> {

  /**
   * ID
   */
  @ApiModelProperty("ID")
  private String productId;

  /**
   * 编号
   */
  @ApiModelProperty("编号")
  private String productCode;

  /**
   * 名称
   */
  @ApiModelProperty("名称")
  private String productName;

  /**
   * 分类名称
   */
  @ApiModelProperty("分类名称")
  private String categoryName;

  /**
   * 品牌名称
   */
  @ApiModelProperty("品牌名称")
  private String brandName;

  /**
   * 是否多销售属性
   */
  @ApiModelProperty("是否多销售属性")
  private Boolean multiSaleProp;

  /**
   * SKU
   */
  @ApiModelProperty("SKU")
  private String skuCode;

  /**
   * 简码
   */
  @ApiModelProperty("简码")
  private String externalCode;

  /**
   * 规格
   */
  @ApiModelProperty("规格")
  private String spec;

  /**
   * 单位
   */
  @ApiModelProperty("单位")
  private String unit;

  /**
   * 采购价
   */
  @ApiModelProperty("采购价")
  private BigDecimal purchasePrice;

  /**
   * 含税成本价
   */
  @ApiModelProperty("含税成本价")
  private BigDecimal taxCostPrice;

  /**
   * 库存数量
   */
  @ApiModelProperty("库存数量")
  private Integer stockNum;

  /**
   * 税率（%）
   */
  @ApiModelProperty("税率（%）")
  private BigDecimal taxRate;

  /**
   * 仓库ID
   */
  @ApiModelProperty(value = "仓库ID", hidden = true)
  @JsonIgnore
  private String scId;

  public PurchaseProductBo(String scId, PurchaseProductDto dto) {

    this.scId = scId;

    this.init(dto);
  }

  @Override
  protected void afterInit(PurchaseProductDto dto) {

    this.productId = dto.getId();
    this.productCode = dto.getCode();
    this.productName = dto.getName();

    ProductStockService productStockService = ApplicationUtil.getBean(
        ProductStockService.class);
    ProductStock productStock = productStockService.getByProductIdAndScId(this.getProductId(),
        this.getScId());
    this.taxCostPrice =
        productStock == null ? BigDecimal.ZERO
            : NumberUtil.getNumber(productStock.getTaxPrice(), 2);
    this.stockNum = productStock == null ? 0 : productStock.getStockNum();
  }
}
