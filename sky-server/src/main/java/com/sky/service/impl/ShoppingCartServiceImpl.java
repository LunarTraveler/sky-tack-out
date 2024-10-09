package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final ShoppingCartMapper shoppingCartMapper;

    private final DishMapper dishMapper;

    private final SetmealMapper setmealMapper;

    /**
     * 添加商品进入购物车
     * @param shoppingCartDTO
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 判断这个菜品或是套餐是否已经加入到购物车(一个用户是有自己的唯一的购物车的)
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        // 其实每一个用户只有一个购物车
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(shoppingCart);

        // 如果已经加入到购物车的话，那么就是对应的数量增加一
        if (shoppingCarts != null && shoppingCarts.size() > 0) {
            ShoppingCart cart = shoppingCarts.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
        } else {
            // 如果没有加入，那么就是插入这条数据(这里还要确定插入的是菜品还是套餐)
            Long dishId = shoppingCartDTO.getDishId();
            Long setmealId = shoppingCartDTO.getSetmealId();
            if (dishId != null) {
                // 本次插入的是菜品(这里好像还要有口味)
                Dish dish = dishMapper.selectById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
                // dish_flavor, dishId 在属性赋值那里赋值
            } else if (setmealId != null) {
                // 本次插入的是套餐
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1); // 这里可以优化一下，默认唯一
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 查看购物车
     * @return
     */
    @Override
    public List<ShoppingCart> list() {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCartFilter = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(shoppingCartFilter);
        return shoppingCarts;
    }

    /**
     * 清空购物车
     */
    @Override
    public void clean() {
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }

    /**
     * 删除购物车中一个商品(其实是sub)
     * @param shoppingCartDTO
     */
    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 构建查询条件查询出来这条购物车，对应的菜品或是套餐进行相应的的数据减少
        ShoppingCart shoppingCartFilter = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCartFilter);
        shoppingCartFilter.setUserId(BaseContext.getCurrentId());

        // 每个用户只有一个购物车（里面有很多个菜品或是套餐）
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(shoppingCartFilter);

        if (shoppingCarts != null && !shoppingCarts.isEmpty()) {
            ShoppingCart shoppingCart = shoppingCarts.get(0);
            Integer number = shoppingCart.getNumber();
            // 注意Java中是有一个常量池的[-127, 128]这是常用的数字，他们是同一个引用对象
            if (number != null && number.equals(1)) {
                shoppingCartMapper.deleteById(shoppingCart.getId());
            } else if (number != null && number > 1){
                shoppingCart.setNumber(number - 1);
                shoppingCartMapper.updateNumberById(shoppingCart);
            }
        }
    }

}
