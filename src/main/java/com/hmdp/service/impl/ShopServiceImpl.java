package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
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
        // 缓存穿透
        //Shop shop = querywithPassThrough(id);
        // 缓存击穿
        Shop shop = querywithHit(id);
        if (shop == null){
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }
    //缓存穿透代码封装
    public Shop querywithPassThrough(Long id){
        String key = RedisConstans.CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        if(shopJson != null){
                return null;
        }
        // 缓存中也没有
        Shop shop = getById(id);
        if(shop == null){
            stringRedisTemplate.opsForValue().set(key, "", 30L, TimeUnit.MINUTES);
            return shop;
        }
        // 数据库有，写入缓存
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), 30L, TimeUnit.MINUTES);
        return shop;
    }

    //缓存击穿代码封装
    public Shop querywithHit(Long id){
        String key = RedisConstans.CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        if(shopJson != null){
            return null;
        }
        // 试锁，获取数据库数据
        String lockkey = "_ock:shop"+id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockkey);
            if(!isLock){

                    Thread.sleep(1000);
                    return querywithHit(id);


            }


            shop = getById(id);
            Thread.sleep(200);
            if(shop == null){
                stringRedisTemplate.opsForValue().set(key, "", 30L, TimeUnit.MINUTES);
                return shop;
            }
            // 数据库有，写入缓存
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), 30L, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally{
            // 释放锁
            releaseLock(lockkey);
        }
        // 返回数据
        return shop;
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "lock", 30L, TimeUnit.MINUTES);
        return BooleanUtil.isTrue(flag);
    }
    private void releaseLock(String key){
        stringRedisTemplate.delete(key);
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
