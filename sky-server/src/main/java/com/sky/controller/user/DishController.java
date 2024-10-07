package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController("userDishController")
@RequestMapping("user/dish")
@Api(tags = "C端-菜品浏览接口")
@Slf4j
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 根据分类id查询菜品
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        log.info("根据分类id查询菜品{}", categoryId);

        // 热点数据加上缓存
        String key = "dish_" + categoryId;
        List<DishVO> dishList = (List<DishVO>) redisTemplate.opsForValue().get(key);
        if (dishList != null && dishList.size() > 0) {
            return Result.success(dishList);
        }

        // 把分类的条件id和在销售中这两个条件封装到Dish中(这里要注意了，当时我封装的是map)
        Map<String, Object> params = new HashMap<>();
        params.put("categoryId", categoryId);
        params.put("status", StatusConstant.ENABLE);

        dishList = dishService.listWithFlavor(params);
        redisTemplate.opsForValue().set(key, dishList);
        return Result.success(dishList);
    }


}
