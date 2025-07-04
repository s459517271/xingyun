package com.lframework.xingyun.sc.impl.purchase;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.exceptions.impl.InputErrorException;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.annotations.timeline.OrderTimeLineLog;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.components.security.SecurityUtil;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.utils.PageHelperUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.starter.web.inner.components.timeline.ApprovePassOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.ApproveReturnOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.CreateOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.UpdateOrderTimeLineBizType;
import com.lframework.starter.web.inner.entity.SysUser;
import com.lframework.starter.web.inner.service.GenerateCodeService;
import com.lframework.starter.web.inner.service.system.SysUserService;
import com.lframework.starter.web.core.utils.OpLogUtil;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.StoreCenter;
import com.lframework.xingyun.basedata.entity.Supplier;
import com.lframework.xingyun.basedata.enums.ManageType;
import com.lframework.xingyun.basedata.enums.SettleType;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.basedata.service.storecenter.StoreCenterService;
import com.lframework.xingyun.basedata.service.supplier.SupplierService;
import com.lframework.xingyun.sc.components.code.GenerateCodeTypePool;
import com.lframework.xingyun.sc.dto.purchase.receive.GetPaymentDateDto;
import com.lframework.xingyun.sc.dto.purchase.receive.ReceiveSheetFullDto;
import com.lframework.xingyun.sc.dto.purchase.receive.ReceiveSheetWithReturnDto;
import com.lframework.xingyun.sc.entity.PurchaseConfig;
import com.lframework.xingyun.sc.entity.PurchaseOrder;
import com.lframework.xingyun.sc.entity.PurchaseOrderDetail;
import com.lframework.xingyun.sc.entity.ReceiveSheet;
import com.lframework.xingyun.sc.entity.ReceiveSheetDetail;
import com.lframework.xingyun.sc.enums.ProductStockBizType;
import com.lframework.xingyun.sc.enums.PurchaseOpLogType;
import com.lframework.xingyun.sc.enums.ReceiveSheetStatus;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.mappers.ReceiveSheetMapper;
import com.lframework.xingyun.sc.service.purchase.PurchaseConfigService;
import com.lframework.xingyun.sc.service.purchase.PurchaseOrderDetailService;
import com.lframework.xingyun.sc.service.purchase.PurchaseOrderService;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetDetailService;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetService;
import com.lframework.xingyun.sc.service.stock.ProductStockService;
import com.lframework.xingyun.sc.vo.purchase.receive.ApprovePassReceiveSheetVo;
import com.lframework.xingyun.sc.vo.purchase.receive.ApproveRefuseReceiveSheetVo;
import com.lframework.xingyun.sc.vo.purchase.receive.CreateReceiveSheetVo;
import com.lframework.xingyun.sc.vo.purchase.receive.QueryReceiveSheetVo;
import com.lframework.xingyun.sc.vo.purchase.receive.QueryReceiveSheetWithReturnVo;
import com.lframework.xingyun.sc.vo.purchase.receive.ReceiveProductVo;
import com.lframework.xingyun.sc.vo.purchase.receive.ReceiveSheetSelectorVo;
import com.lframework.xingyun.sc.vo.purchase.receive.UpdateReceiveSheetVo;
import com.lframework.xingyun.sc.vo.stock.AddProductStockVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceiveSheetServiceImpl extends BaseMpServiceImpl<ReceiveSheetMapper, ReceiveSheet>
    implements ReceiveSheetService {

  @Autowired
  private ReceiveSheetDetailService receiveSheetDetailService;

  @Autowired
  private GenerateCodeService generateCodeService;

  @Autowired
  private StoreCenterService storeCenterService;

  @Autowired
  private SupplierService supplierService;

  @Autowired
  private SysUserService userService;

  @Autowired
  private ProductService productService;

  @Autowired
  private PurchaseOrderService purchaseOrderService;

  @Autowired
  private PurchaseConfigService purchaseConfigService;

  @Autowired
  private PurchaseOrderDetailService purchaseOrderDetailService;

  @Autowired
  private ProductStockService productStockService;

  @Override
  public PageResult<ReceiveSheet> query(Integer pageIndex, Integer pageSize,
      QueryReceiveSheetVo vo) {

    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);

    PageHelperUtil.startPage(pageIndex, pageSize);
    List<ReceiveSheet> datas = this.query(vo);

    return PageResultUtil.convert(new PageInfo<>(datas));
  }

  @Override
  public List<ReceiveSheet> query(QueryReceiveSheetVo vo) {

    return getBaseMapper().query(vo);
  }

  @Override
  public PageResult<ReceiveSheet> selector(Integer pageIndex, Integer pageSize,
      ReceiveSheetSelectorVo vo) {

    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);

    PageHelperUtil.startPage(pageIndex, pageSize);
    List<ReceiveSheet> datas = getBaseMapper().selector(vo);

    return PageResultUtil.convert(new PageInfo<>(datas));
  }

  @Override
  public GetPaymentDateDto getPaymentDate(String supplierId) {

    // 付款日期默认为当前日期的30天后，如当天为2021-10-01，则付款日期默认为2021-11-01
    //（1）供应商的经营方式为“经销”，且结算方式为“任意指定”，则付款日期按照以上规则展示默认值，允许用户更改，但仅能选择当天及当天之后的日期。
    //（2）供应商的经营方式为“经销”，且结算方式为“货到付款”，则付款日期默认为此刻，即收货单的创建时间，可能会遇到跨日的问题，但付款日期，均赋值为收货单的创建日期。
    //（3）供应商的经营方式为非经销模式时，收货单、退货单不涉及付款，则付款日期字段置灰，为空，且不可点击。

    Supplier supplier = supplierService.findById(supplierId);

    GetPaymentDateDto result = new GetPaymentDateDto();

    result.setAllowModify(supplier.getManageType() == ManageType.DISTRIBUTION
        && supplier.getSettleType() == SettleType.ARBITRARILY);
    if (supplier.getManageType() == ManageType.DISTRIBUTION
        && supplier.getSettleType() == SettleType.ARBITRARILY) {
      result.setPaymentDate(LocalDate.now().plusMonths(1));
    } else if (supplier.getManageType() == ManageType.DISTRIBUTION
        && supplier.getSettleType() == SettleType.CASH_ON_DELIVERY) {
      result.setPaymentDate(LocalDate.now());
    }

    return result;
  }

  @Override
  public ReceiveSheetFullDto getDetail(String id) {

    return getBaseMapper().getDetail(id);
  }

  @Override
  public ReceiveSheetWithReturnDto getWithReturn(String id) {

    PurchaseConfig purchaseConfig = purchaseConfigService.get();

    ReceiveSheetWithReturnDto sheet = getBaseMapper().getWithReturn(id,
        purchaseConfig.getPurchaseReturnRequireReceive());
    if (sheet == null) {
      throw new InputErrorException("收货单不存在！");
    }
    return sheet;
  }

  @Override
  public PageResult<ReceiveSheet> queryWithReturn(Integer pageIndex, Integer pageSize,
      QueryReceiveSheetWithReturnVo vo) {

    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);

    PurchaseConfig purchaseConfig = purchaseConfigService.get();

    PageHelperUtil.startPage(pageIndex, pageSize);
    List<ReceiveSheet> datas = getBaseMapper().queryWithReturn(vo,
        purchaseConfig.getPurchaseReturnMultipleRelateReceive());

    return PageResultUtil.convert(new PageInfo<>(datas));
  }

  @OpLog(type = PurchaseOpLogType.class, name = "创建采购收货单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = CreateOrderTimeLineBizType.class, orderId = "#_result", name = "创建收货单")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public String create(CreateReceiveSheetVo vo) {

    ReceiveSheet sheet = new ReceiveSheet();
    sheet.setId(IdUtil.getId());
    sheet.setCode(generateCodeService.generate(GenerateCodeTypePool.RECEIVE_SHEET));

    PurchaseConfig purchaseConfig = purchaseConfigService.get();

    this.create(sheet, vo, purchaseConfig.getReceiveRequirePurchase());

    sheet.setStatus(ReceiveSheetStatus.CREATED);

    getBaseMapper().insert(sheet);

    OpLogUtil.setVariable("code", sheet.getCode());
    OpLogUtil.setExtra(vo);

    return sheet.getId();
  }

  @OpLog(type = PurchaseOpLogType.class, name = "修改采购收货单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = UpdateOrderTimeLineBizType.class, orderId = "#vo.id", name = "修改收货单")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void update(UpdateReceiveSheetVo vo) {

    ReceiveSheet sheet = getBaseMapper().selectById(vo.getId());
    if (sheet == null) {
      throw new InputErrorException("采购收货单不存在！");
    }

    if (sheet.getStatus() != ReceiveSheetStatus.CREATED
        && sheet.getStatus() != ReceiveSheetStatus.APPROVE_REFUSE) {

      if (sheet.getStatus() == ReceiveSheetStatus.APPROVE_PASS) {
        throw new DefaultClientException("采购收货单已审核通过，无法修改！");
      }

      throw new DefaultClientException("采购收货单无法修改！");
    }

    boolean requirePurchase = !StringUtil.isBlank(sheet.getPurchaseOrderId());

    if (requirePurchase) {
      //查询采购收货单明细
      Wrapper<ReceiveSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(
              ReceiveSheetDetail.class)
          .eq(ReceiveSheetDetail::getSheetId, sheet.getId());
      List<ReceiveSheetDetail> details = receiveSheetDetailService.list(queryDetailWrapper);
      for (ReceiveSheetDetail detail : details) {
        if (!StringUtil.isBlank(detail.getPurchaseOrderDetailId())) {
          //先恢复已收货数量
          purchaseOrderDetailService.subReceiveNum(detail.getPurchaseOrderDetailId(),
              detail.getOrderNum());
        }
      }
    }

    // 删除采购收货单明细
    Wrapper<ReceiveSheetDetail> deleteDetailWrapper = Wrappers.lambdaQuery(ReceiveSheetDetail.class)
        .eq(ReceiveSheetDetail::getSheetId, sheet.getId());
    receiveSheetDetailService.remove(deleteDetailWrapper);

    this.create(sheet, vo, requirePurchase);

    sheet.setStatus(ReceiveSheetStatus.CREATED);

    List<ReceiveSheetStatus> statusList = new ArrayList<>();
    statusList.add(ReceiveSheetStatus.CREATED);
    statusList.add(ReceiveSheetStatus.APPROVE_REFUSE);

    Wrapper<ReceiveSheet> updateOrderWrapper = Wrappers.lambdaUpdate(ReceiveSheet.class)
        .set(ReceiveSheet::getApproveBy, null).set(ReceiveSheet::getApproveTime, null)
        .set(ReceiveSheet::getRefuseReason, StringPool.EMPTY_STR)
        .eq(ReceiveSheet::getId, sheet.getId())
        .in(ReceiveSheet::getStatus, statusList);
    if (getBaseMapper().updateAllColumn(sheet, updateOrderWrapper) != 1) {
      throw new DefaultClientException("采购收货单信息已过期，请刷新重试！");
    }

    OpLogUtil.setVariable("code", sheet.getCode());
    OpLogUtil.setExtra(vo);
  }

  @OpLog(type = PurchaseOpLogType.class, name = "审核通过采购收货单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#vo.id", name = "审核通过")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void approvePass(ApprovePassReceiveSheetVo vo) {

    ReceiveSheet sheet = getBaseMapper().selectById(vo.getId());
    if (sheet == null) {
      throw new InputErrorException("采购收货单不存在！");
    }

    if (sheet.getStatus() != ReceiveSheetStatus.CREATED
        && sheet.getStatus() != ReceiveSheetStatus.APPROVE_REFUSE) {

      if (sheet.getStatus() == ReceiveSheetStatus.APPROVE_PASS) {
        throw new DefaultClientException("采购收货单已审核通过，不允许继续执行审核！");
      }

      throw new DefaultClientException("采购收货单无法审核通过！");
    }

    PurchaseConfig purchaseConfig = purchaseConfigService.get();

    if (!purchaseConfig.getReceiveMultipleRelatePurchase()) {
      Wrapper<ReceiveSheet> checkWrapper = Wrappers.lambdaQuery(ReceiveSheet.class)
          .eq(ReceiveSheet::getPurchaseOrderId, sheet.getPurchaseOrderId())
          .ne(ReceiveSheet::getId, sheet.getId());
      if (getBaseMapper().selectCount(checkWrapper) > 0) {
        PurchaseOrder purchaseOrder = purchaseOrderService.getById(sheet.getPurchaseOrderId());
        throw new DefaultClientException(
            "采购订单号：" + purchaseOrder.getCode()
                + "，已关联其他采购收货单，不允许关联多个采购收货单！");
      }
    }

    sheet.setStatus(ReceiveSheetStatus.APPROVE_PASS);

    List<ReceiveSheetStatus> statusList = new ArrayList<>();
    statusList.add(ReceiveSheetStatus.CREATED);
    statusList.add(ReceiveSheetStatus.APPROVE_REFUSE);

    LambdaUpdateWrapper<ReceiveSheet> updateOrderWrapper = Wrappers.lambdaUpdate(ReceiveSheet.class)
        .set(ReceiveSheet::getApproveBy, SecurityUtil.getCurrentUser().getId())
        .set(ReceiveSheet::getApproveTime, LocalDateTime.now())
        .eq(ReceiveSheet::getId, sheet.getId())
        .in(ReceiveSheet::getStatus, statusList);
    if (!StringUtil.isBlank(vo.getDescription())) {
      updateOrderWrapper.set(ReceiveSheet::getDescription, vo.getDescription());
    }
    if (getBaseMapper().updateAllColumn(sheet, updateOrderWrapper) != 1) {
      throw new DefaultClientException("采购收货单信息已过期，请刷新重试！");
    }

    Wrapper<ReceiveSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(ReceiveSheetDetail.class)
        .eq(ReceiveSheetDetail::getSheetId, sheet.getId())
        .orderByAsc(ReceiveSheetDetail::getOrderNo);
    List<ReceiveSheetDetail> details = receiveSheetDetailService.list(queryDetailWrapper);
    for (ReceiveSheetDetail detail : details) {
      AddProductStockVo addProductStockVo = new AddProductStockVo();
      addProductStockVo.setProductId(detail.getProductId());
      addProductStockVo.setScId(sheet.getScId());
      addProductStockVo.setStockNum(detail.getOrderNum());
      addProductStockVo.setTaxPrice(detail.getTaxPrice());
      addProductStockVo.setBizId(sheet.getId());
      addProductStockVo.setBizDetailId(detail.getId());
      addProductStockVo.setBizCode(sheet.getCode());
      addProductStockVo.setBizType(ProductStockBizType.PURCHASE.getCode());

      productStockService.addStock(addProductStockVo);
    }

    OpLogUtil.setVariable("code", sheet.getCode());
    OpLogUtil.setExtra(vo);
  }

  @Transactional(rollbackFor = Exception.class)
  @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#_result", name = "直接审核通过")
  @Override
  public String directApprovePass(CreateReceiveSheetVo vo) {

    ReceiveSheetService thisService = getThis(this.getClass());

    String sheetId = thisService.create(vo);

    ApprovePassReceiveSheetVo approvePassVo = new ApprovePassReceiveSheetVo();
    approvePassVo.setId(sheetId);
    approvePassVo.setDescription(vo.getDescription());

    thisService.approvePass(approvePassVo);

    return sheetId;
  }

  @OpLog(type = PurchaseOpLogType.class, name = "审核拒绝采购收货单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = ApproveReturnOrderTimeLineBizType.class, orderId = "#vo.id", name = "审核拒绝，拒绝理由：{}", params = "#vo.refuseReason")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void approveRefuse(ApproveRefuseReceiveSheetVo vo) {

    ReceiveSheet sheet = getBaseMapper().selectById(vo.getId());
    if (sheet == null) {
      throw new InputErrorException("采购收货单不存在！");
    }

    if (sheet.getStatus() != ReceiveSheetStatus.CREATED) {

      if (sheet.getStatus() == ReceiveSheetStatus.APPROVE_PASS) {
        throw new DefaultClientException("采购收货单已审核通过，不允许继续执行审核！");
      }

      if (sheet.getStatus() == ReceiveSheetStatus.APPROVE_REFUSE) {
        throw new DefaultClientException("采购收货单已审核拒绝，不允许继续执行审核！");
      }

      throw new DefaultClientException("采购收货单无法审核拒绝！");
    }

    sheet.setStatus(ReceiveSheetStatus.APPROVE_REFUSE);

    LambdaUpdateWrapper<ReceiveSheet> updateOrderWrapper = Wrappers.lambdaUpdate(ReceiveSheet.class)
        .set(ReceiveSheet::getApproveBy, SecurityUtil.getCurrentUser().getId())
        .set(ReceiveSheet::getApproveTime, LocalDateTime.now())
        .set(ReceiveSheet::getRefuseReason, vo.getRefuseReason())
        .eq(ReceiveSheet::getId, sheet.getId())
        .eq(ReceiveSheet::getStatus, ReceiveSheetStatus.CREATED);
    if (getBaseMapper().updateAllColumn(sheet, updateOrderWrapper) != 1) {
      throw new DefaultClientException("采购收货单信息已过期，请刷新重试！");
    }

    OpLogUtil.setVariable("code", sheet.getCode());
    OpLogUtil.setExtra(vo);
  }

  @OpLog(type = PurchaseOpLogType.class, name = "删除采购收货单，单号：{}", params = "#code")
  @OrderTimeLineLog(orderId = "#id", delete = true)
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void deleteById(String id) {

    Assert.notBlank(id);
    ReceiveSheet sheet = getBaseMapper().selectById(id);
    if (sheet == null) {
      throw new InputErrorException("采购收货单不存在！");
    }

    if (sheet.getStatus() != ReceiveSheetStatus.CREATED
        && sheet.getStatus() != ReceiveSheetStatus.APPROVE_REFUSE) {

      if (sheet.getStatus() == ReceiveSheetStatus.APPROVE_PASS) {
        throw new DefaultClientException("“审核通过”的采购收货单不允许执行删除操作！");
      }

      throw new DefaultClientException("采购收货单无法删除！");
    }

    if (!StringUtil.isBlank(sheet.getPurchaseOrderId())) {
      //查询采购收货单明细
      Wrapper<ReceiveSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(
              ReceiveSheetDetail.class)
          .eq(ReceiveSheetDetail::getSheetId, sheet.getId());
      List<ReceiveSheetDetail> details = receiveSheetDetailService.list(queryDetailWrapper);
      for (ReceiveSheetDetail detail : details) {
        if (!StringUtil.isBlank(detail.getPurchaseOrderDetailId())) {
          //恢复已收货数量
          purchaseOrderDetailService.subReceiveNum(detail.getPurchaseOrderDetailId(),
              detail.getOrderNum());
        }
      }
    }

    // 删除订单明细
    Wrapper<ReceiveSheetDetail> deleteDetailWrapper = Wrappers.lambdaQuery(ReceiveSheetDetail.class)
        .eq(ReceiveSheetDetail::getSheetId, sheet.getId());
    receiveSheetDetailService.remove(deleteDetailWrapper);

    // 删除订单
    Wrapper<ReceiveSheet> deleteWrapper = Wrappers.lambdaUpdate(ReceiveSheet.class)
        .eq(ReceiveSheet::getId, id)
        .in(ReceiveSheet::getStatus, ReceiveSheetStatus.CREATED, ReceiveSheetStatus.APPROVE_REFUSE);
    if (!remove(deleteWrapper)) {
      throw new DefaultClientException("采购收货单信息已过期，请刷新重试！");
    }

    OpLogUtil.setVariable("code", sheet.getCode());
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public int setUnSettle(String id) {

    Wrapper<ReceiveSheet> updateWrapper = Wrappers.lambdaUpdate(ReceiveSheet.class)
        .set(ReceiveSheet::getSettleStatus, SettleStatus.UN_SETTLE).eq(ReceiveSheet::getId, id)
        .eq(ReceiveSheet::getSettleStatus, SettleStatus.PART_SETTLE);
    int count = getBaseMapper().update(updateWrapper);

    return count;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public int setPartSettle(String id) {

    Wrapper<ReceiveSheet> updateWrapper = Wrappers.lambdaUpdate(ReceiveSheet.class)
        .set(ReceiveSheet::getSettleStatus, SettleStatus.PART_SETTLE).eq(ReceiveSheet::getId, id)
        .in(ReceiveSheet::getSettleStatus, SettleStatus.UN_SETTLE, SettleStatus.PART_SETTLE);
    int count = getBaseMapper().update(updateWrapper);

    return count;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public int setSettled(String id) {

    Wrapper<ReceiveSheet> updateWrapper = Wrappers.lambdaUpdate(ReceiveSheet.class)
        .set(ReceiveSheet::getSettleStatus, SettleStatus.SETTLED).eq(ReceiveSheet::getId, id)
        .in(ReceiveSheet::getSettleStatus, SettleStatus.UN_SETTLE, SettleStatus.PART_SETTLE);
    int count = getBaseMapper().update(updateWrapper);

    return count;
  }

  @Override
  public List<ReceiveSheet> getApprovedList(String supplierId, LocalDateTime startTime,
      LocalDateTime endTime,
      SettleStatus settleStatus) {

    return getBaseMapper().getApprovedList(supplierId, startTime, endTime, settleStatus);
  }

  private void create(ReceiveSheet sheet, CreateReceiveSheetVo vo, boolean receiveRequirePurchase) {

    StoreCenter sc = storeCenterService.findById(vo.getScId());
    if (sc == null) {
      throw new InputErrorException("仓库不存在！");
    }

    sheet.setScId(vo.getScId());

    Supplier supplier = supplierService.findById(vo.getSupplierId());
    if (supplier == null) {
      throw new InputErrorException("供应商不存在！");
    }
    sheet.setSupplierId(vo.getSupplierId());

    if (!StringUtil.isBlank(vo.getPurchaserId())) {
      SysUser purchaser = userService.findById(vo.getPurchaserId());
      if (purchaser == null) {
        throw new InputErrorException("采购员不存在！");
      }

      sheet.setPurchaserId(vo.getPurchaserId());
    }

    PurchaseConfig purchaseConfig = purchaseConfigService.get();

    GetPaymentDateDto paymentDate = this.getPaymentDate(supplier.getId());

    sheet.setPaymentDate(
        vo.getAllowModifyPaymentDate() || paymentDate.getAllowModify() ? vo.getPaymentDate()
            : paymentDate.getPaymentDate());
    sheet.setReceiveDate(vo.getReceiveDate());

    if (receiveRequirePurchase) {

      PurchaseOrder purchaseOrder = purchaseOrderService.getById(vo.getPurchaseOrderId());
      if (purchaseOrder == null) {
        throw new DefaultClientException("采购订单不存在！");
      }

      sheet.setScId(purchaseOrder.getScId());
      sheet.setSupplierId(purchaseOrder.getSupplierId());
      sheet.setPurchaseOrderId(purchaseOrder.getId());

      if (!purchaseConfig.getReceiveMultipleRelatePurchase()) {
        Wrapper<ReceiveSheet> checkWrapper = Wrappers.lambdaQuery(ReceiveSheet.class)
            .eq(ReceiveSheet::getPurchaseOrderId, purchaseOrder.getId())
            .ne(ReceiveSheet::getId, sheet.getId());
        if (getBaseMapper().selectCount(checkWrapper) > 0) {
          throw new DefaultClientException(
              "采购订单号：" + purchaseOrder.getCode()
                  + "，已关联其他采购收货单，不允许关联多个采购收货单！");
        }
      }
    }

    int purchaseNum = 0;
    int giftNum = 0;
    BigDecimal totalAmount = BigDecimal.ZERO;
    int orderNo = 1;
    for (ReceiveProductVo productVo : vo.getProducts()) {
      if (receiveRequirePurchase) {
        if (!StringUtil.isBlank(productVo.getPurchaseOrderDetailId())) {
          PurchaseOrderDetail orderDetail = purchaseOrderDetailService.getById(
              productVo.getPurchaseOrderDetailId());
          productVo.setPurchasePrice(orderDetail.getTaxPrice());
        } else {
          productVo.setPurchasePrice(BigDecimal.ZERO);
        }
      }

      boolean isGift = productVo.getPurchasePrice().doubleValue() == 0D;

      if (receiveRequirePurchase) {
        if (StringUtil.isBlank(productVo.getPurchaseOrderDetailId())) {
          if (!isGift) {
            throw new InputErrorException("第" + orderNo + "行商品必须为“赠品”！");
          }
        }
      }

      if (isGift) {
        giftNum += productVo.getReceiveNum();
      } else {
        purchaseNum += productVo.getReceiveNum();
      }

      totalAmount = NumberUtil.add(totalAmount,
          NumberUtil.mul(productVo.getPurchasePrice(), productVo.getReceiveNum()));

      ReceiveSheetDetail detail = new ReceiveSheetDetail();
      detail.setId(IdUtil.getId());
      detail.setSheetId(sheet.getId());

      Product product = productService.findById(productVo.getProductId());
      if (product == null) {
        throw new InputErrorException("第" + orderNo + "行商品不存在！");
      }

      if (!NumberUtil.isNumberPrecision(productVo.getPurchasePrice(), 2)) {
        throw new InputErrorException("第" + orderNo + "行商品采购价最多允许2位小数！");
      }

      detail.setProductId(productVo.getProductId());
      detail.setOrderNum(productVo.getReceiveNum());
      detail.setTaxPrice(productVo.getPurchasePrice());
      detail.setIsGift(isGift);
      detail.setTaxRate(product.getTaxRate());
      detail.setDescription(
          StringUtil.isBlank(productVo.getDescription()) ? StringPool.EMPTY_STR
              : productVo.getDescription());
      detail.setOrderNo(orderNo);
      if (receiveRequirePurchase && !StringUtil.isBlank(productVo.getPurchaseOrderDetailId())) {
        detail.setPurchaseOrderDetailId(productVo.getPurchaseOrderDetailId());
        purchaseOrderDetailService.addReceiveNum(productVo.getPurchaseOrderDetailId(),
            detail.getOrderNum());
      }

      receiveSheetDetailService.save(detail);
      orderNo++;
    }
    sheet.setTotalNum(purchaseNum);
    sheet.setTotalGiftNum(giftNum);
    sheet.setTotalAmount(totalAmount);
    sheet.setDescription(
        StringUtil.isBlank(vo.getDescription()) ? StringPool.EMPTY_STR : vo.getDescription());
    sheet.setSettleStatus(this.getInitSettleStatus(supplier));
  }

  /**
   * 根据供应商获取初始结算状态
   *
   * @param supplier
   * @return
   */
  private SettleStatus getInitSettleStatus(Supplier supplier) {

    if (supplier.getManageType() == ManageType.DISTRIBUTION) {
      return SettleStatus.UN_SETTLE;
    } else {
      return SettleStatus.UN_REQUIRE;
    }
  }
}
