package com.lframework.xingyun.settle.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.exceptions.impl.InputErrorException;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.components.security.AbstractUserDetails;
import com.lframework.starter.web.core.components.security.SecurityUtil;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.utils.PageHelperUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.annotations.timeline.OrderTimeLineLog;
import com.lframework.starter.web.inner.components.timeline.ApprovePassOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.ApproveReturnOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.CreateOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.UpdateOrderTimeLineBizType;
import com.lframework.starter.web.inner.service.GenerateCodeService;
import com.lframework.starter.web.core.utils.OpLogUtil;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.settle.components.code.GenerateCodeTypePool;
import com.lframework.xingyun.settle.dto.pre.SettlePreSheetFullDto;
import com.lframework.xingyun.settle.entity.SettleOutItem;
import com.lframework.xingyun.settle.entity.SettlePreSheet;
import com.lframework.xingyun.settle.entity.SettlePreSheetDetail;
import com.lframework.xingyun.settle.enums.SettleOpLogType;
import com.lframework.xingyun.settle.enums.SettlePreSheetStatus;
import com.lframework.xingyun.settle.mappers.SettlePreSheetMapper;
import com.lframework.xingyun.settle.service.SettleOutItemService;
import com.lframework.xingyun.settle.service.SettlePreSheetDetailService;
import com.lframework.xingyun.settle.service.SettlePreSheetService;
import com.lframework.xingyun.settle.vo.pre.ApprovePassSettlePreSheetVo;
import com.lframework.xingyun.settle.vo.pre.ApproveRefuseSettlePreSheetVo;
import com.lframework.xingyun.settle.vo.pre.CreateSettlePreSheetVo;
import com.lframework.xingyun.settle.vo.pre.QuerySettlePreSheetVo;
import com.lframework.xingyun.settle.vo.pre.SettlePreSheetItemVo;
import com.lframework.xingyun.settle.vo.pre.UpdateSettlePreSheetVo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlePreSheetServiceImpl extends
    BaseMpServiceImpl<SettlePreSheetMapper, SettlePreSheet>
    implements SettlePreSheetService {

  @Autowired
  private SettlePreSheetDetailService settlePreSheetDetailService;

  @Autowired
  private SettleOutItemService settleOutItemService;

  @Autowired
  private GenerateCodeService generateCodeService;

  @Override
  public PageResult<SettlePreSheet> query(Integer pageIndex, Integer pageSize,
      QuerySettlePreSheetVo vo) {

    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);

    PageHelperUtil.startPage(pageIndex, pageSize);
    List<SettlePreSheet> datas = this.query(vo);

    return PageResultUtil.convert(new PageInfo<>(datas));
  }

  @Override
  public List<SettlePreSheet> query(QuerySettlePreSheetVo vo) {

    return getBaseMapper().query(vo);
  }

  @Override
  public SettlePreSheetFullDto getDetail(String id) {

    return getBaseMapper().getDetail(id);
  }

  @OpLog(type = SettleOpLogType.class, name = "创建供应商预付款单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = CreateOrderTimeLineBizType.class, orderId = "#_result", name = "创建预付款单")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public String create(CreateSettlePreSheetVo vo) {

    SettlePreSheet sheet = new SettlePreSheet();

    sheet.setId(IdUtil.getId());
    sheet.setCode(generateCodeService.generate(GenerateCodeTypePool.SETTLE_PRE_SHEET));

    this.create(sheet, vo);

    sheet.setStatus(SettlePreSheetStatus.CREATED);

    OpLogUtil.setVariable("code", sheet.getCode());
    OpLogUtil.setExtra(vo);

    getBaseMapper().insert(sheet);

    return sheet.getId();
  }

  @OpLog(type = SettleOpLogType.class, name = "修改供应商预付款单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = UpdateOrderTimeLineBizType.class, orderId = "#vo.id", name = "修改预付款单")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void update(UpdateSettlePreSheetVo vo) {

    SettlePreSheet sheet = getBaseMapper().selectById(vo.getId());
    if (sheet == null) {
      throw new DefaultClientException("供应商预付款单不存在！");
    }

    if (sheet.getStatus() != SettlePreSheetStatus.CREATED
        && sheet.getStatus() != SettlePreSheetStatus.APPROVE_REFUSE) {
      if (sheet.getStatus() == SettlePreSheetStatus.APPROVE_PASS) {
        throw new DefaultClientException("供应商预付款单已审核通过，无法修改！");
      } else {
        throw new DefaultClientException("供应商预付款单无法修改！");
      }
    }

    // 删除明细
    Wrapper<SettlePreSheetDetail> deleteDetailWrapper = Wrappers.lambdaQuery(
            SettlePreSheetDetail.class)
        .eq(SettlePreSheetDetail::getSheetId, sheet.getId());
    settlePreSheetDetailService.remove(deleteDetailWrapper);

    this.create(sheet, vo);

    sheet.setStatus(SettlePreSheetStatus.CREATED);

    List<SettlePreSheetStatus> statusList = new ArrayList<>();
    statusList.add(SettlePreSheetStatus.CREATED);
    statusList.add(SettlePreSheetStatus.APPROVE_REFUSE);

    Wrapper<SettlePreSheet> updateWrapper = Wrappers.lambdaUpdate(SettlePreSheet.class)
        .set(SettlePreSheet::getApproveBy, null).set(SettlePreSheet::getApproveTime, null)
        .set(SettlePreSheet::getRefuseReason, StringPool.EMPTY_STR)
        .eq(SettlePreSheet::getId, sheet.getId())
        .in(SettlePreSheet::getStatus, statusList);
    if (getBaseMapper().updateAllColumn(sheet, updateWrapper) != 1) {
      throw new DefaultClientException("供应商预付款单信息已过期，请刷新重试！");
    }

    OpLogUtil.setVariable("code", sheet.getCode());
    OpLogUtil.setExtra(vo);
  }

  @OpLog(type = SettleOpLogType.class, name = "审核通过供应商预付款单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#vo.id", name = "审核通过")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void approvePass(ApprovePassSettlePreSheetVo vo) {

    SettlePreSheet sheet = getBaseMapper().selectById(vo.getId());
    if (sheet == null) {
      throw new DefaultClientException("供应商预付款单不存在！");
    }

    if (sheet.getStatus() != SettlePreSheetStatus.CREATED
        && sheet.getStatus() != SettlePreSheetStatus.APPROVE_REFUSE) {
      if (sheet.getStatus() == SettlePreSheetStatus.APPROVE_PASS) {
        throw new DefaultClientException("供应商预付款单已审核通过，不允许继续执行审核！");
      }
      throw new DefaultClientException("供应商预付款单无法审核通过！");
    }

    sheet.setStatus(SettlePreSheetStatus.APPROVE_PASS);
    sheet.setApproveBy(SecurityUtil.getCurrentUser().getId());
    sheet.setApproveTime(LocalDateTime.now());
    if (!StringUtil.isBlank(vo.getDescription())) {
      sheet.setDescription(vo.getDescription());
    }

    List<SettlePreSheetStatus> statusList = new ArrayList<>();
    statusList.add(SettlePreSheetStatus.CREATED);
    statusList.add(SettlePreSheetStatus.APPROVE_REFUSE);

    Wrapper<SettlePreSheet> updateWrapper = Wrappers.lambdaUpdate(SettlePreSheet.class)
        .eq(SettlePreSheet::getId, sheet.getId()).in(SettlePreSheet::getStatus, statusList);
    if (getBaseMapper().updateAllColumn(sheet, updateWrapper) != 1) {
      throw new DefaultClientException("供应商预付款单信息已过期，请刷新重试！");
    }

    OpLogUtil.setVariable("code", sheet.getCode());
    OpLogUtil.setExtra(vo);
  }

  @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#_result", name = "直接审核通过")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public String directApprovePass(CreateSettlePreSheetVo vo) {

    SettlePreSheetService thisService = getThis(this.getClass());

    String id = thisService.create(vo);

    ApprovePassSettlePreSheetVo approveVo = new ApprovePassSettlePreSheetVo();
    approveVo.setId(id);

    thisService.approvePass(approveVo);

    return id;
  }

  @OpLog(type = SettleOpLogType.class, name = "审核拒绝供应商预付款单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = ApproveReturnOrderTimeLineBizType.class, orderId = "#vo.id", name = "审核拒绝，拒绝理由：{}", params = "#vo.refuseReason")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void approveRefuse(ApproveRefuseSettlePreSheetVo vo) {

    SettlePreSheet sheet = getBaseMapper().selectById(vo.getId());
    if (sheet == null) {
      throw new DefaultClientException("供应商预付款单不存在！");
    }

    if (sheet.getStatus() != SettlePreSheetStatus.CREATED) {
      if (sheet.getStatus() == SettlePreSheetStatus.APPROVE_PASS) {
        throw new DefaultClientException("供应商预付款单已审核通过，不允许继续执行审核！");
      }
      if (sheet.getStatus() == SettlePreSheetStatus.APPROVE_REFUSE) {
        throw new DefaultClientException("供应商预付款单已审核拒绝，不允许继续执行审核！");
      }
      throw new DefaultClientException("供应商预付款单无法审核拒绝！");
    }

    sheet.setStatus(SettlePreSheetStatus.APPROVE_REFUSE);
    sheet.setApproveBy(SecurityUtil.getCurrentUser().getId());
    sheet.setApproveTime(LocalDateTime.now());
    sheet.setRefuseReason(vo.getRefuseReason());

    List<SettlePreSheetStatus> statusList = new ArrayList<>();
    statusList.add(SettlePreSheetStatus.CREATED);
    statusList.add(SettlePreSheetStatus.APPROVE_REFUSE);

    Wrapper<SettlePreSheet> updateWrapper = Wrappers.lambdaUpdate(SettlePreSheet.class)
        .eq(SettlePreSheet::getId, sheet.getId()).in(SettlePreSheet::getStatus, statusList);
    if (getBaseMapper().updateAllColumn(sheet, updateWrapper) != 1) {
      throw new DefaultClientException("供应商预付款单信息已过期，请刷新重试！");
    }

    OpLogUtil.setVariable("code", sheet.getCode());
    OpLogUtil.setExtra(vo);
  }

  @OpLog(type = SettleOpLogType.class, name = "删除供应商预付款单，单号：{}", params = "#code")
  @OrderTimeLineLog(orderId = "#id", delete = true)
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void deleteById(String id) {

    Assert.notBlank(id);
    SettlePreSheet sheet = getBaseMapper().selectById(id);
    if (sheet == null) {
      throw new InputErrorException("供应商预付款单不存在！");
    }

    if (sheet.getStatus() != SettlePreSheetStatus.CREATED
        && sheet.getStatus() != SettlePreSheetStatus.APPROVE_REFUSE) {

      if (sheet.getStatus() == SettlePreSheetStatus.APPROVE_PASS) {
        throw new DefaultClientException("“审核通过”的供应商预付款单不允许执行删除操作！");
      }

      throw new DefaultClientException("供应商预付款单无法删除！");
    }

    // 删除明细
    Wrapper<SettlePreSheetDetail> deleteDetailWrapper = Wrappers.lambdaQuery(
            SettlePreSheetDetail.class)
        .eq(SettlePreSheetDetail::getSheetId, sheet.getId());
    settlePreSheetDetailService.remove(deleteDetailWrapper);

    // 删除单据
    Wrapper<SettlePreSheet> deleteWrapper = Wrappers.lambdaUpdate(SettlePreSheet.class)
        .eq(SettlePreSheet::getId, id).in(SettlePreSheet::getStatus, SettlePreSheetStatus.CREATED,
            SettlePreSheetStatus.APPROVE_REFUSE);
    if (!remove(deleteWrapper)) {
      throw new DefaultClientException("供应商预付款单信息已过期，请刷新重试！");
    }

    OpLogUtil.setVariable("code", sheet.getCode());
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public int setUnSettle(String id) {

    Wrapper<SettlePreSheet> updateWrapper = Wrappers.lambdaUpdate(SettlePreSheet.class)
        .set(SettlePreSheet::getSettleStatus, SettleStatus.UN_SETTLE).eq(SettlePreSheet::getId, id)
        .eq(SettlePreSheet::getSettleStatus, SettleStatus.PART_SETTLE);
    int count = getBaseMapper().update(updateWrapper);

    return count;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public int setPartSettle(String id) {

    Wrapper<SettlePreSheet> updateWrapper = Wrappers.lambdaUpdate(SettlePreSheet.class)
        .set(SettlePreSheet::getSettleStatus, SettleStatus.PART_SETTLE)
        .eq(SettlePreSheet::getId, id)
        .in(SettlePreSheet::getSettleStatus, SettleStatus.UN_SETTLE, SettleStatus.PART_SETTLE);
    int count = getBaseMapper().update(updateWrapper);

    return count;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public int setSettled(String id) {

    Wrapper<SettlePreSheet> updateWrapper = Wrappers.lambdaUpdate(SettlePreSheet.class)
        .set(SettlePreSheet::getSettleStatus, SettleStatus.SETTLED).eq(SettlePreSheet::getId, id)
        .in(SettlePreSheet::getSettleStatus, SettleStatus.UN_SETTLE, SettleStatus.PART_SETTLE);
    int count = getBaseMapper().update(updateWrapper);

    return count;
  }

  @Override
  public List<SettlePreSheet> getApprovedList(String supplierId, LocalDateTime startTime,
      LocalDateTime endTime,
      SettleStatus settleStatus) {

    return getBaseMapper().getApprovedList(supplierId, startTime, endTime, settleStatus);
  }

  private void create(SettlePreSheet sheet, CreateSettlePreSheetVo vo) {

    BigDecimal totalAmount = BigDecimal.ZERO;

    int orderNo = 1;
    for (SettlePreSheetItemVo itemVo : vo.getItems()) {
      SettleOutItem item = settleOutItemService.findById(itemVo.getId());
      if (item == null) {
        throw new DefaultClientException("第" + orderNo + "行项目不存在！");
      }
      SettlePreSheetDetail detail = new SettlePreSheetDetail();
      detail.setId(IdUtil.getId());
      detail.setSheetId(sheet.getId());
      detail.setItemId(itemVo.getId());
      detail.setAmount(itemVo.getAmount());
      detail.setOrderNo(orderNo);

      settlePreSheetDetailService.save(detail);

      totalAmount = NumberUtil.add(totalAmount, detail.getAmount());

      orderNo++;
    }

    AbstractUserDetails currentUser = SecurityUtil.getCurrentUser();

    sheet.setSupplierId(vo.getSupplierId());
    sheet.setTotalAmount(totalAmount);
    sheet.setDescription(
        StringUtil.isBlank(vo.getDescription()) ? StringPool.EMPTY_STR : vo.getDescription());
    sheet.setRefuseReason(StringPool.EMPTY_STR);
    sheet.setSettleStatus(SettleStatus.UN_SETTLE);
  }
}
