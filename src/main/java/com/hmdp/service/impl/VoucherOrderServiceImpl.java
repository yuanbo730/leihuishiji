package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.User;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.apache.ibatis.io.ResolverUtil;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥0001
 * @since 2026-09-17
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Override
    public Result seckillVoucher(Long voucherId) {
        //查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀未开始");
        }
        //判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已结束");
        }
        //判断库存是否充足

        if (voucher.getStock() <= 0) {
            return Result.fail("库存不足");
        }


        Long userId = UserHolder.getUser().getId();
        synchronized(userId.toString().intern()) {
            //获取当前代理对象
            IVoucherOrderService PROXY = (IVoucherOrderService) AopContext.currentProxy();
            return PROXY.creatVoucherOrder(voucherId);
        }

    }

    @Transactional
    public  Result creatVoucherOrder(Long voucherId) {
        //一人一优惠券
        Long userId = UserHolder.getUser().getId();


            //查询用户是否已购买该订单
            int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();

            if (count > 0) {
                return Result.fail("已购买该优惠券");
            }

            //扣库存
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", voucherId).eq("stock", 0)
                    .update();
            if (!success) {
                return Result.fail("库存不足");
            }

            //保存订单
            VoucherOrder voucherOrder = new VoucherOrder();
            long orderId = redisIdWorker.nextId("worker:");
            voucherOrder.setId(orderId);
            voucherOrder.setUserId(userId);
            voucherOrder.setVoucherId(voucherId);
            save(voucherOrder);
            return Result.ok(orderId);

    }
}