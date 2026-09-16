package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstans;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 哥
 * @since 2026-9-15
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;



    @Override
    public Result queryById(Long id) {
        String key = RedisConstans.CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        if(shopJson != null){
                return Result.fail("店铺不存在");
        }

        Shop shop = getById(id);
       if(shop == null){
            // 缓存中也没有，数据库中也没有
           stringRedisTemplate.opsForValue().set(key, "", 30L, TimeUnit.MINUTES);
           return Result.fail("店铺不存在");
       }

       // 数据库有，写入缓存
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), 30L, TimeUnit.MINUTES);
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺不存在");
        }

        // 更新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(RedisConstans.CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
